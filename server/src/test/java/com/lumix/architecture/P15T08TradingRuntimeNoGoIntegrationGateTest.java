package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 保留 Phase 15 no-go 文件入口驗證。 */
class P15T08TradingRuntimeNoGoIntegrationGateTest {

    @Test
    void phase15READMEAndTaskIndexIncludeT08() throws IOException {
        Path docsRoot = resolveDocsRoot();
        String readme = Files.readString(docsRoot.resolve("README.md"), StandardCharsets.UTF_8);
        assertTrue(readme.contains("T08 trading runtime no-go integration gate"));
        assertTrue(Files.isRegularFile(docsRoot.resolve("trading-runtime-no-go-integration.md")));
        assertTrue(Files.isRegularFile(docsRoot.resolve("tasks/P15-T08.md")));
    }

    private static Path resolveDocsRoot() {
        Path root = Path.of("docs/phases/PHASE_15_BALANCE_RECON");
        return Files.isDirectory(root) ? root : Path.of("../docs/phases/PHASE_15_BALANCE_RECON");
    }
}
