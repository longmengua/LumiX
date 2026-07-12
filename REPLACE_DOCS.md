# LumiX 文件替換說明

這個壓縮包是全新文件架構，不需要保留舊 `docs/`。

## 建議替換方式

在 repo 根目錄執行：

```bash
# 1. 確認工作區乾淨
git status --short

# 2. 備份舊文件，方便需要時比對
git checkout -b docs/production-architecture-reset
mkdir -p .backup
mv docs .backup/docs.before-production-reset.$(date +%Y%m%d%H%M%S) 2>/dev/null || true

# 3. 解壓本包，把 docs/ 與 AI agent 檔案放到 repo 根目錄
#    解壓後應該看到：docs/、AGENTS.md、AI_AGENT.md、AI_START_HERE.md、AI_PROGRESS.md、AI_MODEL_GATE.md

# 4. 檢查文件純文字圖：若出現 Mermaid 或 PlantUML 區塊，請改成純文字圖
grep -R -i "mermaid\|plantuml" docs AGENTS.md AI_AGENT.md AI_START_HERE.md AI_PROGRESS.md AI_MODEL_GATE.md || true

# 5. 檢查變更
git status --short
git diff --stat

# 6. commit
git add docs AGENTS.md AI_AGENT.md AI_START_HERE.md AI_PROGRESS.md AI_MODEL_GATE.md AI_CONTINUE_PROMPT_V3.md README_DOCS_REPLACEMENT.md
git commit -m "docs: reset production architecture and agent workflow"
```

## Codex / mini 開工入口

1. 先讀根目錄 `AGENTS.md`。
2. 再讀 `AI_AGENT.md`。
3. 再讀 `docs/ai/AI_CONTEXT_ROUTING.md`。
4. 目前只允許從 `docs/phases/PHASE_17_ORDER_INTAKE/` 開工。

## 重要原則

- 不要跳 Phase。
- 不要把 stub、TODO、mock 當成完成。
- 涉及帳本、凍結、結算、錢包出帳、費率、風控的變更，需要人審。
- 圖表全部使用純文字圖，不使用 Mermaid、PlantUML 或圖片。
