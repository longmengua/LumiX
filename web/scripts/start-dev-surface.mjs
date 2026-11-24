import { execFileSync, spawn } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const [surface] = process.argv.slice(2);
const repositoryEnv = fileURLToPath(new URL('../../.env', import.meta.url));

// 前後台與 API 共用 repo root 的服務埠設定，避免啟動腳本與反向代理各自保存固定數字。
if (existsSync(repositoryEnv)) {
  for (const line of readFileSync(repositoryEnv, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Z][A-Z0-9_]*)=(.*)\s*$/);
    if (match && process.env[match[1]] === undefined) process.env[match[1]] = match[2];
  }
}

if (!['client', 'admin'].includes(surface)) {
  throw new Error('用法：start-dev-surface.mjs <client|admin>');
}

const port = surface === 'admin'
  ? (process.env.LUMIX_ADMIN_WEB_PORT || '8100')
  : (process.env.LUMIX_WEB_PORT || '8090');
if (!/^\d+$/.test(port) || Number(port) < 1 || Number(port) > 65535) {
  throw new Error(`${surface === 'admin' ? 'LUMIX_ADMIN_WEB_PORT' : 'LUMIX_WEB_PORT'} 必須是 1 到 65535 的整數`);
}

// 此 launcher 只服務本機開發；production runtime 不得以 port kill 取代受治理的部署／停止流程。
if (['NODE_ENV', 'LUMIX_ENV'].some((key) => process.env[key] === 'production')) {
  throw new Error('拒絕執行：dev surface launcher 不可在 production 環境終止既有程序。');
}

let pids = [];
try {
  const output = process.platform === 'win32'
    ? execFileSync('powershell.exe', ['-NoProfile', '-Command',
      `(Get-NetTCPConnection -LocalPort ${port} -State Listen -ErrorAction SilentlyContinue).OwningProcess`], { encoding: 'utf8' })
    : execFileSync('lsof', ['-tiTCP:' + port, '-sTCP:LISTEN'], { encoding: 'utf8' });
  pids = output.split(/\s+/).filter(Boolean);
} catch (error) {
  // lsof 在找不到監聽程序時以非零狀態結束，這是可預期的空閒埠情況。
  if (error.status !== 1 || process.platform === 'win32') {
    throw error;
  }
}

// 固定 port 是前後台隔離契約的一部分；開發啟動前釋放同一 port，並搭配 strictPort 禁止自動漂移。
for (const pid of pids) {
  console.log(`釋放本機開發埠 ${port}（PID: ${pid}）`);
  process.kill(Number(pid), 'SIGTERM');
}

// 等待舊 listener 真正釋放 socket，否則 strictPort 會在較慢的 OS 排程下誤判為啟動失敗。
if (pids.length > 0) {
  await new Promise((resolve) => setTimeout(resolve, 250));
}

const vite = fileURLToPath(new URL('../node_modules/vite/bin/vite.js', import.meta.url));
const child = spawn(process.execPath, [vite, '--port', port, '--strictPort'], {
  env: { ...process.env, VITE_LUMIX_SURFACE: surface },
  stdio: 'inherit',
});

child.on('exit', (code, signal) => {
  process.exitCode = code ?? (signal ? 1 : 0);
});
