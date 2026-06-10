/*
 * File purpose: Verify admin-issued test funds route through the existing deposit ledger path.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.MarginService;
import com.example.exchange.domain.model.entity.WalletTransfer;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.TestFundsAirdropRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminTestFundsControllerTest {

    @Test
    @DisplayName("admin test-funds airdrop delegates to deposit ledger flow")
    void airdropDelegatesToDeposit() {
        MarginService marginService = mock(MarginService.class);
        WalletTransfer transfer = WalletTransfer.builder()
                .uid(10001L)
                .asset("USDT")
                .amount(new BigDecimal("10000.00"))
                .type(WalletTransfer.Type.DEPOSIT)
                .build();
        transfer.confirm();
        // Scenario: ops issues MVP test funds while real deposit rails are still a TODO.
        when(marginService.deposit(10001L, new BigDecimal("10000.00"))).thenReturn(transfer);
        AdminTestFundsController controller = new AdminTestFundsController(marginService);

        ApiResponse<WalletTransfer> response =
                controller.airdrop(new TestFundsAirdropRequest(10001L, new BigDecimal("10000.00")));

        // Expected result: controller does not mutate balances directly; MarginService writes transfer + ledger.
        assertThat(response.isOk()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo(WalletTransfer.Status.CONFIRMED);
        assertThat(response.getData().getAmount()).isEqualByComparingTo("10000.00");
        verify(marginService).deposit(10001L, new BigDecimal("10000.00"));
    }
}
