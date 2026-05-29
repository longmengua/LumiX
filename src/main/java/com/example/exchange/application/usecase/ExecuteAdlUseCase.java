/*
 * 檔案用途：UseCase 入口，承接 ADL forced execution command 並套用 command transaction boundary。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.ExecuteAdlCommand;
import com.example.exchange.application.service.AdlForcedExecutionService;
import com.example.exchange.application.service.CommandTransactionBoundary;
import com.example.exchange.domain.model.dto.AdlExecutionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecuteAdlUseCase {

    private final AdlForcedExecutionService adlForcedExecutionService;
    private CommandTransactionBoundary commandTransactionBoundary;

    @Autowired(required = false)
    public void setCommandTransactionBoundary(CommandTransactionBoundary commandTransactionBoundary) {
        this.commandTransactionBoundary = commandTransactionBoundary;
    }

    public AdlExecutionResult handle(ExecuteAdlCommand command) {
        if (commandTransactionBoundary != null) {
            return commandTransactionBoundary.execute("execute-adl", () -> handleInsideTransaction(command));
        }
        return handleInsideTransaction(command);
    }

    private AdlExecutionResult handleInsideTransaction(ExecuteAdlCommand command) {
        return adlForcedExecutionService.execute(command.commandId(), command.plan());
    }
}
