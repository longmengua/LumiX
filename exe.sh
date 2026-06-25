pwd

echo "\n--- ROOT FILES ---"
ls -la

echo "\n--- PACKAGE FILES ---"
find . -maxdepth 4 -name "package.json" -print

echo "\n--- EXISTING AI / CODEX DOCS ---"
find . -maxdepth 5 \( \
  -iname "*codex*" -o \
  -iname "*ai*" -o \
  -iname "*exchange*" -o \
  -iname "*frontend*" -o \
  -iname "*backend*" -o \
  -iname "*model*" \
\) -type f | sort

echo "\n--- MARKDOWN DOCS ---"
find . -maxdepth 5 -name "*.md" -type f | sort

echo "\n--- SRC STRUCTURE ---"
find . -maxdepth 4 \( \
  -path "*/node_modules" -o \
  -path "*/.git" -o \
  -path "*/dist" -o \
  -path "*/build" \
\) -prune -o -type d -print | sort | head -200

echo "\n--- ROOT PACKAGE.JSON ---"
if [ -f package.json ]; then cat package.json; fi