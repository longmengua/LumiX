/*
 * 檔案用途：REST Controller，暴露 HTTP API 並委派給應用或領域服務。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.TransferMarginCommand;
import com.example.exchange.application.service.AccountRiskService;
import com.example.exchange.application.service.MarginService;
import com.example.exchange.application.usecase.TransferMarginUseCase;
import com.example.exchange.domain.model.dto.AccountRiskSnapshot;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletTransfer;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.DepositRequest;
import com.example.exchange.interfaces.web.dto.TransferRequest;
import com.example.exchange.interfaces.web.dto.WithdrawalRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 劃轉相關 REST API */
@RestController
@RequestMapping("/api/margin")
public class MarginController {

    private final TransferMarginUseCase usecase;
    private final MarginService marginService;
    private final AccountRiskService accountRiskService;

    public MarginController(
            TransferMarginUseCase usecase,
            MarginService marginService,
            AccountRiskService accountRiskService
    ) {
        this.usecase = usecase;
        this.marginService = marginService;
        this.accountRiskService = accountRiskService;
    }

    @PostMapping("/deposit")
    public ApiResponse<WalletTransfer> deposit(@Valid @RequestBody DepositRequest request) {
        return ApiResponse.ok(marginService.deposit(request.uid(), request.amount()));
    }

    @PostMapping("/withdraw")
    public ApiResponse<WalletTransfer> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        return ApiResponse.ok(marginService.withdraw(request.uid(), request.amount()));
    }

    @PostMapping("/transfer")
    public ApiResponse<String> transfer(@Valid @RequestBody TransferRequest r) {
        usecase.handle(new TransferMarginCommand(r.uid(), r.symbol(), r.toIsolated(), r.amount()));
        return ApiResponse.ok("ok");
    }

    @GetMapping("/account")
    public ApiResponse<Account> account(@RequestParam Long uid) {
        return ApiResponse.ok(marginService.findAccount(uid).orElse(null));
    }

    @GetMapping("/ledger")
    public ApiResponse<List<WalletLedgerEntry>> ledger(@RequestParam Long uid) {
        return ApiResponse.ok(marginService.findLedger(uid));
    }

    @GetMapping("/transfers")
    public ApiResponse<List<WalletTransfer>> transfers(@RequestParam Long uid) {
        return ApiResponse.ok(marginService.findTransfers(uid));
    }

    @GetMapping("/risk")
    public ApiResponse<AccountRiskSnapshot> risk(@RequestParam Long uid) {
        return ApiResponse.ok(accountRiskService.snapshot(uid));
    }
}
