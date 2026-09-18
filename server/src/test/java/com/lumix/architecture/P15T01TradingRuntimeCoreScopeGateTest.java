package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 保留 posting boundary 與 JDBC append adapter 的隔離驗證。 */
class P15T01TradingRuntimeCoreScopeGateTest {

    @Test
    void postingBoundaryStillDoesNotReferenceAppendAdapter() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        String commandBoundary = Files.readString(sourceRoot.resolve("com/lumix/ledger/application/posting/LedgerPostingCommandBoundary.java"), StandardCharsets.UTF_8);
        String defaultBoundary = Files.readString(sourceRoot.resolve("com/lumix/ledger/application/posting/DefaultLedgerPostingCommandBoundary.java"), StandardCharsets.UTF_8);
        String adapter = Files.readString(sourceRoot.resolve("com/lumix/ledger/persistence/adapter/LedgerAppendOnlyJdbcAdapter.java"), StandardCharsets.UTF_8);
        assertFalse(commandBoundary.contains("LedgerAppendOnlyJdbcAdapter"));
        assertFalse(defaultBoundary.contains("LedgerAppendOnlyJdbcAdapter"));
        assertFalse(adapter.contains("LedgerPostingCommandBoundary"));
    }

    private static Path resolveSourceRoot() {
        Path root = Path.of("server/src/main/java");
        return Files.isDirectory(root) ? root : Path.of("src/main/java");
    }
}
