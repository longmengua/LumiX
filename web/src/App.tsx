const phaseHighlights = [
  'React + TypeScript 骨架',
  'Vite 啟動與建置腳本',
  '後續 Phase 1-12 的前端基礎',
];

export default function App() {
  return (
    <main className="app-shell">
      <section className="hero-card">
        <p className="eyebrow">LumiX</p>
        <h1>交易所前端骨架已建立</h1>
        <p className="lead">
          目前只完成 Phase 1 的基礎工程，後續會依 Phase 2 起逐步補上 App Shell、路由與頁面。
        </p>

        <div className="status-grid">
          {phaseHighlights.map((item) => (
            <div className="status-tile" key={item}>
              {item}
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
