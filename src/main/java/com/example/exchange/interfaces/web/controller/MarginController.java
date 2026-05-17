/*
 * 檔案用途：REST Controller，暴露 HTTP API 並委派給應用或領域服務。
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.TransferMarginCommand;
import com.example.exchange.application.service.MarginService;
import com.example.exchange.application.usecase.TransferMarginUseCase;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.DepositRequest;
import com.example.exchange.interfaces.web.dto.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 劃轉相關 REST API */
@RestController
@RequestMapping("/api/margin")
public class MarginController {

    private final TransferMarginUseCase usecase;
    private final MarginService marginService;

    public MarginController(TransferMarginUseCase usecase, MarginService marginService) {
        this.usecase = usecase;
        this.marginService = marginService;
    }

    @PostMapping("/deposit")
    public ApiResponse<String> deposit(@Valid @RequestBody DepositRequest request) {
        marginService.deposit(request.uid(), request.amount());
        return ApiResponse.ok("ok");
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
}
