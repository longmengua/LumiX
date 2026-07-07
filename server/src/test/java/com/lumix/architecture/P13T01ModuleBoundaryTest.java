package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Phase 13 只建立 package boundary，不把 runtime 實作偷偷塞進新 module。
 *
 * 這個測試保護的是架構邊界，而不是 business logic。
 */
class P13T01ModuleBoundaryTest {

    private static final List<String> MODULE_PACKAGES = List.of(
        "com/lumix",
        "com/lumix/common",
        "com/lumix/security",
        "com/lumix/user",
        "com/lumix/account",
        "com/lumix/asset",
        "com/lumix/market",
        "com/lumix/wallet",
        "com/lumix/ledger",
        "com/lumix/reservation",
        "com/lumix/order",
        "com/lumix/trade",
        "com/lumix/outbox",
        "com/lumix/audit",
        "com/lumix/admin",
        "com/lumix/openapi",
        "com/lumix/spot",
        "com/lumix/idempotency"
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
     * 確認 boundary marker 都已就位。
     *
     * 如果這些檔案消失，代表後續 task 會失去 package 邊界的明確註記。
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
     * 確認 Phase 13 新增的 skeleton 沒有被偷偷塞進其他 Java 類別。
     *
     * 這個限制讓 boundary task 保持乾淨：先定義模組，不先做 runtime 實作。
     */
    @Test
    void newModuleSkeletonsContainOnlyPackageInfo() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        Set<String> skeletonPackages = Set.of(
            "com/lumix/security",
            "com/lumix/user",
            "com/lumix/asset",
            "com/lumix/reservation",
            "com/lumix/order",
            "com/lumix/trade",
            "com/lumix/outbox",
            "com/lumix/audit",
            "com/lumix/admin"
        );

        for (String packagePath : skeletonPackages) {
            Path packageDir = sourceRoot.resolve(packagePath);
            assertTrue(Files.isDirectory(packageDir), "Missing package directory: " + packageDir);

            try (Stream<Path> files = Files.list(packageDir)) {
                long javaFileCount = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                    .count();
                assertEquals(0L, javaFileCount, "Skeleton package must stay empty: " + packageDir);
            }
        }
    }
}
