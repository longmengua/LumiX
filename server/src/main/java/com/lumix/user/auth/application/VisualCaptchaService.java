package com.lumix.user.auth.application;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.user.auth.application.SliderCaptchaService.CaptchaPurpose;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 可配置的圖形驗證題型入口。
 *
 * <p>題型選擇、答案及瀏覽器綁定都在 server-side 完成；前端取得的只有可呈現的圖片與 challenge id，
 * 因此不能藉由自行指定較簡單的類型取得通行 token。</p>
 */
@Service
@Profile("infrastructure")
public class VisualCaptchaService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(2);
    private static final String CHALLENGE_KEY_PREFIX = "lumix:auth:visual-captcha:challenge:";
    private static final int ICON_SIZE = 64;
    private static final int GRID_TILE_SIZE = 80;

    private final RedisTemplate<String, String> redisTemplate;
    private final SliderCaptchaService sliderCaptchaService;
    private final UserAuthenticationProperties properties;

    public VisualCaptchaService(
        RedisTemplate<String, String> redisTemplate,
        SliderCaptchaService sliderCaptchaService,
        UserAuthenticationProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.sliderCaptchaService = sliderCaptchaService;
        this.properties = properties;
    }

    /** 依部署設定隨機建立一種題型；不接受 caller 傳入的 type，避免降級攻擊。 */
    public CaptchaChallengeResponse create(LoginRequestMetadata metadata) {
        return switch (selectType()) {
            case SLIDER -> sliderChallenge(metadata);
            case ICON_MATCH -> iconMatchChallenge(metadata);
            case IMAGE_GRID -> imageGridChallenge(metadata);
        };
    }

    /**
     * 消耗一次性題目並核發既有用途限定 token。
     *
     * <p>所有失敗路徑都先消耗 challenge，避免同一張圖片被反覆嘗試；答案類型與選取數量也必須與建立時一致。</p>
     */
    public SliderCaptchaService.CaptchaVerificationResponse verify(
        CaptchaVerificationRequest request, LoginRequestMetadata metadata
    ) {
        if (request == null || request.type() == null || !bounded(request.captchaId(), 64) || request.purpose() == null) {
            throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
        }
        if (request.type() == CaptchaType.SLIDER) {
            if (request.offsetX() == null) throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
            return sliderCaptchaService.verify(request.captchaId(), request.offsetX(), request.purpose(), metadata);
        }

        String challenge = getAndDelete(CHALLENGE_KEY_PREFIX + request.captchaId());
        String[] values = challenge == null ? new String[0] : challenge.split("\\|", -1);
        if (values.length != 3 || !request.type().name().equals(values[0]) || !metadata.userAgentDigest().equals(values[2])) {
            throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
        }
        String selected = normalizedSelection(request.selectedIndexes(), request.type() == CaptchaType.IMAGE_GRID ? 9 : 6);
        if (!values[1].equals(selected)) throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);

        // 通行 token 沿用既有用途、TTL 與 Redis 原子消耗邊界，避免新題型形成另一條較弱的認證路徑。
        return sliderCaptchaService.issuePassToken(request.purpose(), metadata);
    }

    /** 帳號 endpoint 一律由同一個 token consumer 驗證，與題型本身無關。 */
    public void consume(String captchaToken, CaptchaPurpose purpose, LoginRequestMetadata metadata) {
        sliderCaptchaService.consume(captchaToken, purpose, metadata);
    }

    private CaptchaChallengeResponse sliderChallenge(LoginRequestMetadata metadata) {
        SliderCaptchaService.SliderChallengeResponse slider = sliderCaptchaService.create(metadata);
        return new CaptchaChallengeResponse(
            slider.captchaId(), CaptchaType.SLIDER, slider.backgroundImage(), slider.pieceImage(), slider.pieceY(),
            slider.width(), slider.height(), slider.pieceWidth(), slider.pieceHeight(), null, List.of(), null, List.of()
        );
    }

    private CaptchaChallengeResponse iconMatchChallenge(LoginRequestMetadata metadata) {
        String captchaId = UUID.randomUUID().toString();
        Glyph target = randomGlyph();
        int expectedIndex = randomInt(0, 5);
        List<String> candidates = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            Glyph glyph = index == expectedIndex ? target : differentGlyph(target);
            candidates.add(renderGlyph(glyph, randomColor(), randomInt(-35, 35)));
        }
        put(CHALLENGE_KEY_PREFIX + captchaId, "ICON_MATCH|" + expectedIndex + "|" + metadata.userAgentDigest());
        return new CaptchaChallengeResponse(
            captchaId, CaptchaType.ICON_MATCH, null, null, null, null, null, null, null,
            renderGlyph(target, new Color(226, 232, 240), 0), candidates, "請選出與上方圖示相同的圖案。", List.of()
        );
    }

    private CaptchaChallengeResponse imageGridChallenge(LoginRequestMetadata metadata) {
        String captchaId = UUID.randomUUID().toString();
        List<Integer> expectedIndexes = new ArrayList<>();
        List<String> tiles = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            boolean car = index < 3 || (index >= 3 && SECURE_RANDOM.nextBoolean());
            if (car) expectedIndexes.add(index);
            tiles.add(renderGridTile(car ? GridSubject.CAR : randomNonCar()));
        }
        put(CHALLENGE_KEY_PREFIX + captchaId, "IMAGE_GRID|" + joinIndexes(expectedIndexes) + "|" + metadata.userAgentDigest());
        return new CaptchaChallengeResponse(
            captchaId, CaptchaType.IMAGE_GRID, null, null, null, null, null, null, null,
            null, List.of(), "請選出所有車子。", tiles
        );
    }

    private CaptchaType selectType() {
        List<CaptchaType> enabled = new ArrayList<>();
        for (String configured : properties.getCaptcha().getEnabledTypes()) {
            try {
                enabled.add(CaptchaType.valueOf(configured.trim().toUpperCase(Locale.ROOT)));
            } catch (RuntimeException ignored) {
                throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
            }
        }
        if (enabled.isEmpty()) throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
        String selectionMode = properties.getCaptcha().getSelectionMode();
        if ("FIRST".equalsIgnoreCase(selectionMode)) return enabled.getFirst();
        if ("RANDOM".equalsIgnoreCase(selectionMode)) return enabled.get(SECURE_RANDOM.nextInt(enabled.size()));
        throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
    }

    private static String normalizedSelection(List<Integer> indexes, int maximumExclusive) {
        if (indexes == null || indexes.isEmpty() || indexes.size() > maximumExclusive) throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
        List<Integer> normalized = new ArrayList<>(indexes);
        normalized.sort(Comparator.naturalOrder());
        for (int index = 0; index < normalized.size(); index++) {
            int value = normalized.get(index);
            if (value < 0 || value >= maximumExclusive || (index > 0 && value == normalized.get(index - 1))) {
                throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
            }
        }
        return joinIndexes(normalized);
    }

    private static String joinIndexes(List<Integer> indexes) {
        return indexes.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
    }

    private void put(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value, CHALLENGE_TTL);
        } catch (RedisConnectionFailureException exception) {
            throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private String getAndDelete(String key) {
        try {
            return redisTemplate.opsForValue().getAndDelete(key);
        } catch (RedisConnectionFailureException exception) {
            throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private static String renderGlyph(Glyph glyph, Color color, int rotation) {
        BufferedImage image = canvas(ICON_SIZE);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);
        graphics.translate(ICON_SIZE / 2.0, ICON_SIZE / 2.0);
        graphics.rotate(Math.toRadians(rotation));
        graphics.setColor(color);
        drawGlyph(graphics, glyph, 0, 0, 22);
        graphics.dispose();
        return dataUrl(image);
    }

    private static String renderGridTile(GridSubject subject) {
        BufferedImage image = canvas(GRID_TILE_SIZE);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);
        graphics.setColor(new Color(randomInt(27, 58), randomInt(46, 82), randomInt(75, 120)));
        graphics.fillRect(0, 0, GRID_TILE_SIZE, GRID_TILE_SIZE);
        graphics.setColor(new Color(226, 232, 240));
        switch (subject) {
            case CAR -> {
                graphics.fillRoundRect(14, 35, 52, 20, 6, 6);
                graphics.fillRoundRect(25, 24, 30, 17, 8, 8);
                graphics.setColor(new Color(15, 23, 42));
                graphics.fillOval(19, 48, 11, 11);
                graphics.fillOval(50, 48, 11, 11);
            }
            case TREE -> {
                graphics.fillOval(18, 12, 44, 44);
                graphics.setColor(new Color(148, 95, 50));
                graphics.fillRect(35, 48, 10, 20);
            }
            case HOUSE -> {
                graphics.fillRect(20, 34, 40, 30);
                Path2D roof = new Path2D.Double();
                roof.moveTo(14, 35); roof.lineTo(40, 13); roof.lineTo(66, 35); roof.closePath();
                graphics.fill(roof);
            }
            case BICYCLE -> {
                graphics.drawOval(15, 42, 22, 22); graphics.drawOval(43, 42, 22, 22);
                graphics.drawLine(26, 53, 39, 32); graphics.drawLine(39, 32, 52, 53); graphics.drawLine(26, 53, 52, 53);
            }
        }
        graphics.dispose();
        return dataUrl(image);
    }

    private static BufferedImage canvas(int size) {
        return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    }

    private static void drawGlyph(Graphics2D graphics, Glyph glyph, int centerX, int centerY, int size) {
        switch (glyph) {
            case CIRCLE -> graphics.fillOval(centerX - size, centerY - size, size * 2, size * 2);
            case SQUARE -> graphics.fillRoundRect(centerX - size, centerY - size, size * 2, size * 2, 8, 8);
            case TRIANGLE -> {
                Path2D triangle = new Path2D.Double();
                triangle.moveTo(centerX, centerY - size); triangle.lineTo(centerX + size, centerY + size); triangle.lineTo(centerX - size, centerY + size); triangle.closePath();
                graphics.fill(triangle);
            }
            case DIAMOND -> {
                Path2D diamond = new Path2D.Double();
                diamond.moveTo(centerX, centerY - size); diamond.lineTo(centerX + size, centerY); diamond.lineTo(centerX, centerY + size); diamond.lineTo(centerX - size, centerY); diamond.closePath();
                graphics.fill(diamond);
            }
        }
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static String dataUrl(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render visual captcha", exception);
        }
    }

    private static Glyph randomGlyph() { return Glyph.values()[randomInt(0, Glyph.values().length - 1)]; }
    private static Glyph differentGlyph(Glyph target) { Glyph candidate; do { candidate = randomGlyph(); } while (candidate == target); return candidate; }
    private static GridSubject randomNonCar() { return GridSubject.values()[randomInt(1, GridSubject.values().length - 1)]; }
    private static Color randomColor() { return new Color(randomInt(80, 240), randomInt(80, 220), randomInt(80, 230)); }
    private static int randomInt(int min, int max) { return min + SECURE_RANDOM.nextInt(max - min + 1); }
    private static boolean bounded(String value, int max) { return value != null && !value.isBlank() && value.length() <= max; }

    public enum CaptchaType { SLIDER, ICON_MATCH, IMAGE_GRID }
    private enum Glyph { CIRCLE, SQUARE, TRIANGLE, DIAMOND }
    private enum GridSubject { CAR, TREE, HOUSE, BICYCLE }
    public record CaptchaVerificationRequest(String captchaId, CaptchaType type, Integer offsetX, List<Integer> selectedIndexes, CaptchaPurpose purpose) { }
    public record CaptchaChallengeResponse(
        String captchaId, CaptchaType type, String backgroundImage, String pieceImage, Integer pieceY,
        Integer width, Integer height, Integer pieceWidth, Integer pieceHeight, String referenceImage,
        List<String> candidateImages, String prompt, List<String> gridImages
    ) { }
}
