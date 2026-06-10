/*
 * File purpose: Verify margin account API returns a stable DTO instead of serializing the domain aggregate.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.AccountRiskService;
import com.example.exchange.application.service.AccountRiskSnapshotService;
import com.example.exchange.application.service.BonusCreditService;
import com.example.exchange.application.service.MarginService;
import com.example.exchange.application.service.TurnoverReconciliationService;
import com.example.exchange.application.service.TurnoverService;
import com.example.exchange.application.service.WalletLedgerReplayService;
import com.example.exchange.application.usecase.TransferMarginUseCase;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.interfaces.web.dto.AccountResponse;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarginControllerAccountResponseTest {

    @Test
    @DisplayName("account endpoint maps domain account to serializable balance response")
    void accountEndpointReturnsStableResponseDto() {
        MarginService marginService = mock(MarginService.class);
        Account account = new Account(10001L);
        // Scenario: registered user receives test funds and then the browser loads account balances.
        account.deposit(new BigDecimal("10000.00"));
        account.reserveOrder(new BigDecimal("250.00"));
        when(marginService.findAccount(10001L)).thenReturn(Optional.of(account));
        MarginController controller = controller(marginService);

        ApiResponse<AccountResponse> response = controller.account(10001L);

        // Expected result: response exposes stable JSON-friendly fields used by exchange.html.
        assertThat(response.isOk()).isTrue();
        assertThat(response.getData().uid()).isEqualTo(10001L);
        assertThat(response.getData().balance()).isEqualByComparingTo("10000.00");
        assertThat(response.getData().available()).isEqualByComparingTo("9750.00");
        assertThat(response.getData().frozen()).isEqualByComparingTo("250.00");
    }

    /** Builds the controller with unrelated collaborators mocked because this test targets only account mapping. */
    private static MarginController controller(MarginService marginService) {
        return new MarginController(
                mock(TransferMarginUseCase.class),
                marginService,
                mock(AccountRiskService.class),
                mock(AccountRiskSnapshotService.class),
                mock(WalletLedgerReplayService.class),
                mock(BonusCreditService.class),
                mock(TurnoverService.class),
                mock(TurnoverReconciliationService.class)
        );
    }
}
