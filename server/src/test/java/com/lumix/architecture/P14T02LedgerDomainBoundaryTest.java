package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Phase 14-T02 只建立 ledger journal draft 與 invariant contract，不把 runtime 寫入能力帶進來。
 */
class P14T02LedgerDomainBoundaryTest {

    private static final List<String> DOMAIN_FILES = List.of(
            "com/lumix/ledger/domain/LedgerDirection.java",
            "com/lumix/ledger/domain/LedgerBusinessReferenceType.java",
            "com/lumix/ledger/domain/LedgerEntryDraft.java",
            "com/lumix/ledger/domain/LedgerJournalDraft.java",
            "com/lumix/ledger/domain/LedgerInvariantViolation.java",
            "com/lumix/ledger/domain/LedgerInvariantPolicy.java"
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
     * 確認 ledger domain 的 package marker 都已就位。
     *
     * 如果這些檔案消失，後續 phase 就會失去 package 分層的明確註記。
     */
    @Test
    void packageMarkersExist() {
        Path sourceRoot = resolveSourceRoot();
        Path packageInfo = sourceRoot.resolve("com/lumix/ledger/domain/package-info.java");
        assertTrue(Files.isRegularFile(packageInfo), "Missing package marker: " + packageInfo);
    }

    /**
     * 確認 domain contract 只包含 draft / invariant 內容，不要偷塞 repository 或 transaction 語意。
     *
     * 這個限制讓 P14-T02 保持乾淨：先把 journal draft 與 invariant contract 定義好，
     * 不先接正式資金路徑。
     */
    @Test
    void domainContractDoesNotContainRuntimeTokens() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        Set<String> forbiddenTokens = Set.of(
                "@Transactional",
                "LedgerPostingService",
                "postJournal(",
                "reserve(",
                "release(",
                "commit(",
                "rollback(",
                "Repository",
                "repository",
                "balance_projections",
                "updateBalance(",
                "adjustBalance("
        );

        for (String relativePath : DOMAIN_FILES) {
            Path sourceFile = sourceRoot.resolve(relativePath);
            String source = Files.readString(sourceFile);
            for (String forbiddenToken : forbiddenTokens) {
                assertFalse(source.contains(forbiddenToken),
                        "Forbidden token found in domain contract: " + forbiddenToken + " @ " + sourceFile);
            }
        }
    }
}
