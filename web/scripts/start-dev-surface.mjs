import { execFileSync, spawn } from 'node:child_process';

const [surface, port] = process.argv.slice(2);

if (!['client', 'admin'].includes(surface) || !['8088', '8089'].includes(port)) {
  throw new Error('用法：start-dev-surface.mjs <client|admin> <8088|8089>');
}

// 此 launcher 只服務本機開發；production runtime 不得以 port kill 取代受治理的部署／停止流程。
if (['NODE_ENV', 'LUMIX_ENV'].some((key) => process.env[key] === 'production')) {
  throw new Error('拒絕執行：dev surface launcher 不可在 production 環境終止既有程序。');
}

let pids = [];
try {
  const output = execFileSync('lsof', ['-tiTCP:' + port, '-sTCP:LISTEN'], { encoding: 'utf8' });
  pids = output.split(/\s+/).filter(Boolean);
} catch (error) {
  // lsof 在找不到監聽程序時以非零狀態結束，這是可預期的空閒埠情況。
  if (error.status !== 1) {
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

const vite = process.platform === 'win32' ? 'vite.cmd' : 'vite';
const child = spawn(vite, ['--port', port, '--strictPort'], {
  env: { ...process.env, VITE_LUMIX_SURFACE: surface },
  stdio: 'inherit',
});

child.on('exit', (code, signal) => {
  process.exitCode = code ?? (signal ? 1 : 0);
});
