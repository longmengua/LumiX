package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Phase 18-T05 只提供 mock mark-price 與 funding preview，不偷接正式行情或資金異動 runtime。
 */
class P18T05FuturesSandboxMarkPriceFundingScopeGateTest {

    /**
     * 確認 sandbox source 不引入正式行情、撮合、部位異動、帳本或 persistence transaction 實作。
     */
    @Test
    void sandboxSourcesContainNoProductionMarketOrMoneyMutationRuntime() throws IOException {
        Path sourceRoot = resolveSourceRoot().resolve("com/lumix/trading/core/futures/sandbox");
        List<String> forbiddenTokens = List.of(
                "import com.lumix.market.",
                "import com.lumix.trading.core.futures.matching.",
                "import com.lumix.trading.core.futures.position.update.",
                "import com.lumix.trading.core.reservation.",
                "import com.lumix.ledger.",
                "@Repository",
                "@Transactional",
                "JdbcTemplate",
                "EntityManager",
                "Connection",
                "PreparedStatement"
        );

        try (var files = Files.walk(sourceRoot)) {
            for (Path javaFile : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(javaFile, StandardCharsets.UTF_8);
                for (String forbiddenToken : forbiddenTokens) {
                    assertFalse(source.contains(forbiddenToken),
                            () -> javaFile + " contains forbidden T05 token: " + forbiddenToken);
                }
            }
        }
    }

    /**
     * 確認 phase 文件把 T05 記為人工 snapshot 與純試算，而不是正式價格或 funding 結算能力。
     */
    @Test
    void phase18ReadmeRecordsT05MockPriceAndFundingPreviewBoundary() throws IOException {
        String readme = Files.readString(resolveDocsRoot().resolve("README.md"), StandardCharsets.UTF_8);

        assertTrue(readme.contains("T05 mock mark price and funding - completed"));
        assertTrue(readme.contains("manual snapshot and pure funding preview only"));
        assertTrue(readme.contains("T05 不連接正式行情"));
    }

    private static Path resolveSourceRoot() {
        Path repoRelative = Path.of("server/src/main/java");
        return Files.isDirectory(repoRelative) ? repoRelative : Path.of("src/main/java");
    }

    private static Path resolveDocsRoot() {
        Path repoRelative = Path.of("docs/phases/PHASE_18_MATCHING_CONTRACT");
        return Files.isDirectory(repoRelative) ? repoRelative : Path.of("../docs/phases/PHASE_18_MATCHING_CONTRACT");
    }
}
