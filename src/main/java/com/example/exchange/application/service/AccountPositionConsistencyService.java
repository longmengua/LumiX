/*
 * 檔案用途：restore 後檢查 account 與 open position 是否一致。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AccountPositionConsistencyIssue;
import com.example.exchange.domain.model.dto.AccountPositionConsistencyReport;
import com.example.exchange.domain.model.dto.Account;
import com.example.exchange.domain.model.dto.Position;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountPositionConsistencyService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;

    public AccountPositionConsistencyReport validateAfterRestore() {
        List<Account> accounts = accountRepository.findAll();
        List<Position> openPositions = positionRepository.findOpenPositions();
        Map<Long, Account> accountsByUid = accounts.stream()
                .collect(Collectors.toMap(Account::uid, Function.identity(), (left, right) -> left));
        List<AccountPositionConsistencyIssue> issues = new ArrayList<>();

        Map<Long, BigDecimal> marginByUid = openPositions.stream()
                .collect(Collectors.groupingBy(
                        Position::getUid,
                        Collectors.reducing(BigDecimal.ZERO, position -> safe(position.getMargin()), BigDecimal::add)
                ));
        for (Position position : openPositions) {
            if (!accountsByUid.containsKey(position.getUid())) {
                issues.add(new AccountPositionConsistencyIssue(
                        position.getUid(),
                        position.getSymbol() == null ? null : position.getSymbol().code(),
                        "POSITION_WITHOUT_ACCOUNT",
                        null,
                        safe(position.getMargin()),
                        "open position exists without a restored account"
                ));
            }
        }
        for (Account account : accounts) {
            BigDecimal openPositionMargin = marginByUid.getOrDefault(account.uid(), BigDecimal.ZERO);
            if (account.crossPositionMargin().compareTo(openPositionMargin) < 0) {
                issues.add(new AccountPositionConsistencyIssue(
                        account.uid(),
                        null,
                        "ACCOUNT_MARGIN_BELOW_OPEN_POSITION_MARGIN",
                        account.crossPositionMargin(),
                        openPositionMargin,
                        "restored account position margin is lower than open-position margin sum"
                ));
            }
        }
        return new AccountPositionConsistencyReport(
                accounts.size(),
                openPositions.size(),
                issues.size(),
                issues.isEmpty(),
                Instant.now(),
                issues
        );
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
