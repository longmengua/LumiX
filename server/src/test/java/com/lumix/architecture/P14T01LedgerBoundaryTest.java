package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Phase 14-T01 只建立 ledger boundary skeleton，不把 posting runtime 偷塞進來。
 *
 * 這個測試保護的是資金路徑邊界，而不是 ledger business logic。
 */
class P14T01LedgerBoundaryTest {

    private static final List<String> MODULE_PACKAGES = List.of(
            "com/lumix/ledger",
            "com/lumix/ledger/application",
            "com/lumix/ledger/domain",
            "com/lumix/ledger/persistence"
    );

    /**
     * 讓測試同時支援從 repo root 與 `server/` 目錄執行。
     *
     * 這樣驗證路徑不會綁死在某一種啟動方式，避免架構測試本身變成脆弱的環境假設。
     */
    private static Path resolveSourceRoot() {
        Path repoRelative = Path.of("server/src/main/java");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("src/main/java");
    }

    /**
     * 確認 ledger boundary 的 package marker 都已就位。
     *
     * 如果這些檔案消失，後續 phase 就會失去 package 分層的明確註記。
     */
    @Test
    void packageMarkersExist() {
        Path sourceRoot = resolveSourceRoot();
        for (String packagePath : MODULE_PACKAGES) {
            Path packageInfo = sourceRoot.resolve(packagePath).resolve("package-info.java");
            assertTrue(Files.isRegularFile(packageInfo), "Missing package marker: " + packageInfo);
        }
    }

    /**
     * 確認 ledger package 目前沒有 posting runtime implementation。
     *
     * 這個限制讓 Phase 14-T01 保持乾淨：先定義 boundary 與 prerequisite gate，
     * 不先把資金異動流程接進來。
     */
    @Test
    void ledgerPackageDoesNotContainPostingRuntime() throws IOException {
        Path ledgerRoot = resolveSourceRoot().resolve("com/lumix/ledger");
        Set<String> forbiddenNames = Set.of(
                "DefaultLedgerPostingService.java",
                "LedgerPostingService.java",
                "DefaultLedgerPostingEngine.java",
                "LedgerPostingEngine.java",
                "LedgerReconciliationService.java"
        );

        try (Stream<Path> files = Files.walk(ledgerRoot)) {
            long forbiddenFileCount = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> forbiddenNames.contains(path.getFileName().toString()))
                    .count();
            assertTrue(forbiddenFileCount == 0L, "Posting runtime skeleton must not exist yet: " + ledgerRoot);
        }
    }

    /**
     * 確認 `LedgerService` 只保留 prerequisite inspection 語意。
     *
     * 這個測試直接禁止 runtime method 名稱出現在 ledger service contract 內，
     * 避免 P14-T01 skeleton 被誤接成正式帳本入口。
     */
    @Test
    void ledgerServiceContractDoesNotExposeRuntimeMethods() throws IOException {
        Path ledgerService = resolveSourceRoot().resolve("com/lumix/ledger/LedgerService.java");
        String source = Files.readString(ledgerService);

        assertFalse(source.contains("postJournal("), "LedgerService must not expose postJournal runtime method");
        assertFalse(source.contains("reserve("), "LedgerService must not expose reserve runtime method");
        assertFalse(source.contains("release("), "LedgerService must not expose release runtime method");
        assertFalse(source.contains("commit("), "LedgerService must not expose commit runtime method");
        assertFalse(source.contains("rollback("), "LedgerService must not expose rollback runtime method");
    }
}
