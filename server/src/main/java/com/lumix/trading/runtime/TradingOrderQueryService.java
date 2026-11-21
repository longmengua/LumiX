package com.lumix.trading.runtime;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客戶委託只讀查詢，owner 由 API 的已認證 principal 提供；不接受其他使用者或帳戶 ID 作篩選。
 *
 * <p>交易確認需要 read-your-writes，因此使用 primary transaction，而非 readOnly replica routing。
 * 資料只來自 orders，沒有 mock、固定餘額或由外部參考簿推導的成交。</p>
 */
@Service
public class TradingOrderQueryService {
    private static final String SELECT = "SELECT o.order_id,o.market_symbol,o.side,o.order_type,o.time_in_force,"
            + "o.status,o.price,o.quantity,o.filled_quantity,o.remaining_quantity,o.created_at,o.updated_at "
            + "FROM orders o JOIN accounts a ON a.account_id=o.account_id AND a.user_id=o.user_id "
            + "WHERE o.user_id=? AND a.account_type=? AND a.account_category='USER' ";
    private final JdbcTemplate jdbc;

    public TradingOrderQueryService(JdbcTemplate jdbc) { this.jdbc = Objects.requireNonNull(jdbc); }

    /** 有界 keyset 分頁；游標僅決定位置，無法改變任何 owner 或現貨／合約帳戶隔離條件。 */
    @Transactional
    public Page list(String owner, String kind, Integer requestedLimit, String before) {
        identifier(owner);
        String accountType = accountType(kind);
        int limit = requestedLimit == null ? 20 : requestedLimit;
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("order page limit is invalid");
        Cursor cursor = before == null ? null : decode(before);
        List<OrderView> rows = cursor == null
                ? jdbc.query(SELECT + "ORDER BY o.created_at DESC,o.order_id DESC LIMIT ?", this::map, owner, accountType, limit + 1)
                : jdbc.query(SELECT + "AND (o.created_at,o.order_id) < (?,?) "
                                + "ORDER BY o.created_at DESC,o.order_id DESC LIMIT ?", this::map,
                        owner, accountType, Timestamp.from(cursor.createdAt()), cursor.orderId(), limit + 1);
        List<OrderView> items = rows.stream().limit(limit).toList();
        String next = rows.size() > limit ? encode(items.getLast()) : null;
        return new Page(items, next);
    }

    /** 以相同 owner／account kind 條件查單筆；他人訂單與不存在訂單一律回相同 not-found。 */
    @Transactional
    public OrderView detail(String owner, String kind, String orderId) {
        identifier(owner);
        identifier(orderId);
        List<OrderView> rows = jdbc.query(SELECT + "AND o.order_id=?", this::map, owner, accountType(kind), orderId);
        if (rows.size() != 1) throw new NoSuchElementException("order not found");
        return rows.getFirst();
    }

    private OrderView map(ResultSet row, int number) throws SQLException {
        return new OrderView(row.getString("order_id"), row.getString("market_symbol"), row.getString("side"),
                row.getString("order_type"), row.getString("time_in_force"), row.getString("status"),
                decimal(row.getBigDecimal("price")), decimal(row.getBigDecimal("quantity")),
                decimal(row.getBigDecimal("filled_quantity")), decimal(row.getBigDecimal("remaining_quantity")),
                row.getTimestamp("created_at").toInstant(), row.getTimestamp("updated_at").toInstant());
    }

    private static String accountType(String kind) {
        if ("spot".equals(kind)) return "SPOT";
        if ("futures".equals(kind)) return "FUTURES";
        throw new IllegalArgumentException("unsupported trading account kind");
    }

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static String encode(OrderView row) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (row.createdAt() + "|" + row.orderId()).getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decode(String input) {
        if (input.isBlank() || input.length() > 200) throw new IllegalArgumentException("invalid order cursor");
        try {
            String value = new String(Base64.getUrlDecoder().decode(input), StandardCharsets.UTF_8);
            int delimiter = value.indexOf('|');
            if (delimiter < 1 || delimiter > 30) throw new IllegalArgumentException("invalid cursor boundary");
            String order = value.substring(delimiter + 1);
            identifier(order);
            Instant time = Instant.parse(value.substring(0, delimiter));
            return new Cursor(time, order);
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("invalid order cursor", invalid);
        }
    }

    private static void identifier(String value) {
        if (value == null || value.isBlank() || value.length() > 64 || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("invalid order identifier");
        }
    }

    /** 數值一律以 decimal string 回傳；不暴露帳戶 ID、session、對手方或內部冪等鍵。 */
    public record OrderView(String orderId, String marketSymbol, String side, String orderType, String timeInForce,
                            String status, String price, String quantity, String filledQuantity, String remainingQuantity,
                            Instant createdAt, Instant updatedAt) { }

    /** nextCursor 為 null 代表這個查詢快照沒有更多資料，不提供推測的總筆數。 */
    public record Page(List<OrderView> items, String nextCursor) { }
    private record Cursor(Instant createdAt, String orderId) { }
}
