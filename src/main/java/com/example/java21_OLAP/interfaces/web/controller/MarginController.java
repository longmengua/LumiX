package com.example.java21_OLAP.interfaces.web.controller;

import com.example.java21_OLAP.application.command.TransferMarginCommand;
import com.example.java21_OLAP.application.usecase.TransferMarginUseCase;
import com.example.java21_OLAP.interfaces.web.dto.ApiResponse;
import com.example.java21_OLAP.interfaces.web.dto.TransferRequest;
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
