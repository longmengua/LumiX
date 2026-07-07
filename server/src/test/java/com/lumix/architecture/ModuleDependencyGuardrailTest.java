package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 驗證目前 skeleton 的 package dependency 沒有明顯違規。
 *
 * 這個測試只做 source-level guardrail，不取代未來更完整的架構掃描工具。
 */
class ModuleDependencyGuardrailTest {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+([a-zA-Z0-9_.*]+);$");

    private final ModuleDependencyPolicy policy = new ModuleDependencyPolicy();

    /**
     * 確認現有主要 module 不會出現明顯違規依賴。
     *
     * 這個測試保護的是 skeleton 的方向性，而不是 runtime 行為。
     */
    @Test
    void currentSourceImportsRespectGuardrails() throws IOException {
        Path sourceRoot = resolveSourceRoot();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            List<Path> javaFiles = files
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                .toList();

            for (Path file : javaFiles) {
                String sourcePackage = toPackageName(sourceRoot, file);
                List<String> imports = Files.readAllLines(file, StandardCharsets.UTF_8);

                for (String line : imports) {
                    Matcher matcher = IMPORT_PATTERN.matcher(line.trim());
                    if (!matcher.matches()) {
                        continue;
                    }

                    String importedPackage = matcher.group(1);
                    if (importedPackage.endsWith(".*")) {
                        importedPackage = importedPackage.substring(0, importedPackage.length() - 1);
                    } else {
                        int lastDot = importedPackage.lastIndexOf('.');
                        importedPackage = lastDot > 0 ? importedPackage.substring(0, lastDot + 1) : importedPackage;
                    }

                    assertTrue(
                        policy.isAllowedImport(sourcePackage, importedPackage),
                        "Disallowed import from " + sourcePackage + " to " + importedPackage + " in " + file
                    );
                }
            }
        }
    }

    /**
     * 確認高風險 module 會被標記出來，方便後續加嚴規則。
     */
    @Test
    void highRiskModulePrefixesAreRecognized() {
        assertTrue(policy.isHighRiskModule("com.lumix.ledger."));
        assertTrue(policy.isHighRiskModule("com.lumix.withdrawal."));
        assertTrue(policy.isHighRiskModule("com.lumix.settlement."));
        assertFalse(policy.isHighRiskModule("com.lumix.api."));
    }

    /**
     * 讓測試同時支援從 repo root 與 `server/` 目錄執行。
     */
    private static Path resolveSourceRoot() {
        Path repoRelative = Path.of("server/src/main/java");
        if (Files.isDirectory(repoRelative)) {
            return repoRelative;
        }

        return Path.of("src/main/java");
    }

    /**
     * 從檔案路徑還原 Java package 名稱。
     *
     * 這個 helper 只服務架構檢查，不應被拿去做 runtime 資料處理。
     */
    private static String toPackageName(Path sourceRoot, Path javaFile) {
        Path relative = sourceRoot.relativize(javaFile).getParent();
        if (relative == null) {
            return "";
        }

        return relative.toString().replace('/', '.').replace('\\', '.');
    }
}
