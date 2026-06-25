/*
 * File purpose: Admin-only MVP endpoint for issuing test funds before real deposit/withdraw rails exist.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.MarginService;
import com.example.exchange.domain.model.dto.WalletTransfer;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.TestFundsAirdropRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/test-funds")
public class AdminTestFundsController {

    private final MarginService marginService;

    public AdminTestFundsController(MarginService marginService) {
        this.marginService = marginService;
    }

    @PostMapping("/airdrop")
    public ApiResponse<WalletTransfer> airdrop(@Valid @RequestBody TestFundsAirdropRequest request) {
        // MVP test funds intentionally reuse the existing deposit ledger path so balances stay replayable.
        return ApiResponse.ok(marginService.deposit(request.uid(), request.amount()));
    }
}
