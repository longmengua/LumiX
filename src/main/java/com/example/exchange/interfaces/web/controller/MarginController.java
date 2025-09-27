package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.command.TransferMarginCommand;
import com.example.exchange.application.usecase.TransferMarginUseCase;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 劃轉相關 REST API */
@RestController
@RequestMapping("/api/margin")
public class MarginController {

    private final TransferMarginUseCase usecase;

    public MarginController(TransferMarginUseCase usecase) {
        this.usecase = usecase;
    }

    @PostMapping("/transfer")
    public ApiResponse<String> transfer(@Valid @RequestBody TransferRequest r) {
        usecase.handle(new TransferMarginCommand(r.uid(), r.symbol(), r.toIsolated(), r.amount()));
        return ApiResponse.ok("ok");
    }
}
