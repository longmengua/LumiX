#!/usr/bin/env bash
set -euo pipefail

COMMAND="${1:-status}"
LABEL="${2:-default}"
CODEX_HOME_DIR="${CODEX_HOME:-"$HOME/.codex"}"
STATE_DIR="${CODEX_USAGE_STATE_DIR:-"${TMPDIR:-/tmp}/codex-usage-${USER:-user}"}"
SESSION_ID="${CODEX_SESSION_ID:-}"

mkdir -p "$STATE_DIR"

node - "$COMMAND" "$LABEL" "$CODEX_HOME_DIR" "$STATE_DIR" "$SESSION_ID" <<'NODE'
const fs = require("fs");
const path = require("path");

const [command, rawLabel, codexHome, stateDir, sessionId] = process.argv.slice(2);
const label = String(rawLabel || "default").replace(/[^A-Za-z0-9._-]/g, "_");
const sessionsDir = path.join(codexHome, "sessions");
const statePath = path.join(stateDir, `${label}.json`);

function walk(dir, files = []) {
  if (!fs.existsSync(dir)) return files;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(fullPath, files);
    else if (entry.isFile() && entry.name.endsWith(".jsonl")) files.push(fullPath);
  }
  return files;
}

function latestSessionFile() {
  const files = walk(sessionsDir);
  const candidates = sessionId ? files.filter((file) => path.basename(file).includes(sessionId)) : files;
  if (candidates.length === 0) throw new Error(`No Codex session jsonl found under ${sessionsDir}`);
  return candidates.map((file) => ({ file, mtimeMs: fs.statSync(file).mtimeMs })).sort((a, b) => b.mtimeMs - a.mtimeMs)[0].file;
}

function readLatestUsage(file) {
  const lines = fs.readFileSync(file, "utf8").trim().split(/\n/).reverse();
  for (const line of lines) {
    if (!line.includes('"token_count"')) continue;
    let event;
    try { event = JSON.parse(line); } catch { continue; }
    const payload = event.payload;
    if (event.type === "event_msg" && payload && payload.type === "token_count") {
      return { timestamp: event.timestamp, sessionFile: file, info: payload.info, rateLimits: payload.rate_limits };
    }
  }
  throw new Error(`No token_count event found in ${file}`);
}

function usageNumbers(snapshot) {
  const info = snapshot.info || {};
  const total = info.total_token_usage || {};
  const last = info.last_token_usage || {};
  const limits = snapshot.rateLimits || {};
  return {
    contextWindow: Number(info.model_context_window || 0),
    contextUsed: Number(last.total_tokens || 0),
    totalTokens: Number(total.total_tokens || 0),
    inputTokens: Number(total.input_tokens || 0),
    cachedInputTokens: Number(total.cached_input_tokens || 0),
    outputTokens: Number(total.output_tokens || 0),
    reasoningOutputTokens: Number(total.reasoning_output_tokens || 0),
    fiveHourUsedPercent: limits.primary ? Number(limits.primary.used_percent || 0) : null,
    weeklyUsedPercent: limits.secondary ? Number(limits.secondary.used_percent || 0) : null,
  };
}

function pct(value, total) {
  return total ? `${((value / total) * 100).toFixed(1)}%` : "n/a";
}

function signed(value) {
  if (value === null || Number.isNaN(value)) return "n/a";
  return value >= 0 ? `+${value}` : `${value}`;
}

function printSnapshot(title, snapshot) {
  const n = usageNumbers(snapshot);
  const contextLeft = Math.max(n.contextWindow - n.contextUsed, 0);
  console.log(title);
  console.log(`timestamp: ${snapshot.timestamp}`);
  console.log(`session: ${path.basename(snapshot.sessionFile)}`);
  console.log(`context: ${n.contextUsed.toLocaleString()} used / ${n.contextWindow.toLocaleString()} (${pct(contextLeft, n.contextWindow)} left)`);
  console.log(`session total: ${n.totalTokens.toLocaleString()} tokens`);
  console.log(`input: ${n.inputTokens.toLocaleString()} (${n.cachedInputTokens.toLocaleString()} cached)`);
  console.log(`output: ${n.outputTokens.toLocaleString()} (${n.reasoningOutputTokens.toLocaleString()} reasoning)`);
  if (n.fiveHourUsedPercent !== null) console.log(`5h limit: ${n.fiveHourUsedPercent.toFixed(1)}% used / ${(100 - n.fiveHourUsedPercent).toFixed(1)}% left`);
  if (n.weeklyUsedPercent !== null) console.log(`weekly limit: ${n.weeklyUsedPercent.toFixed(1)}% used / ${(100 - n.weeklyUsedPercent).toFixed(1)}% left`);
}

function printDelta(start, end) {
  const a = usageNumbers(start);
  const b = usageNumbers(end);
  console.log(`Codex usage delta: ${label}`);
  console.log(`from: ${start.timestamp}`);
  console.log(`to:   ${end.timestamp}`);
  console.log(`context delta: ${signed(b.contextUsed - a.contextUsed)} tokens`);
  console.log(`session total delta: ${signed(b.totalTokens - a.totalTokens)} tokens`);
  console.log(`input delta: ${signed(b.inputTokens - a.inputTokens)} tokens`);
  console.log(`cached input delta: ${signed(b.cachedInputTokens - a.cachedInputTokens)} tokens`);
  console.log(`output delta: ${signed(b.outputTokens - a.outputTokens)} tokens`);
  console.log(`reasoning output delta: ${signed(b.reasoningOutputTokens - a.reasoningOutputTokens)} tokens`);
  if (a.fiveHourUsedPercent !== null && b.fiveHourUsedPercent !== null) console.log(`5h limit used delta: ${signed((b.fiveHourUsedPercent - a.fiveHourUsedPercent).toFixed(1))} pp`);
  if (a.weeklyUsedPercent !== null && b.weeklyUsedPercent !== null) console.log(`weekly limit used delta: ${signed((b.weeklyUsedPercent - a.weeklyUsedPercent).toFixed(1))} pp`);
}

try {
  const sessionFile = latestSessionFile();
  const latest = readLatestUsage(sessionFile);

  if (command === "status") {
    printSnapshot("Codex usage status", latest);
  } else if (command === "start") {
    fs.writeFileSync(statePath, JSON.stringify(latest, null, 2));
    printSnapshot(`Codex usage baseline saved: ${label}`, latest);
    console.log(`state: ${statePath}`);
  } else if (command === "end") {
    if (!fs.existsSync(statePath)) throw new Error(`No baseline found for label "${label}". Run: shells/codex-usage.sh start ${label}`);
    printDelta(JSON.parse(fs.readFileSync(statePath, "utf8")), latest);
  } else {
    console.error("Usage: shells/codex-usage.sh status | start <label> | end <label>");
    process.exit(2);
  }
} catch (error) {
  console.error(`codex-usage: ${error.message}`);
  process.exit(1);
}
NODE

