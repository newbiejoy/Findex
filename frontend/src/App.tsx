import { useState, useEffect, useCallback, useRef } from 'react';
import * as api from './api';
import type {
  SearchResponse,
  SearchResult,
  FolderConfig,
  BenchmarkResult
} from './api';

export default function App() {
  const [query, setQuery] = useState('');
  const [rankingMode, setRankingMode] = useState('bm25');
  const [fileType, setFileType] = useState('');
  const [response, setResponse] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [backendUp, setBackendUp] = useState(true);

  const [view, setView] = useState<'search' | 'folders' | 'benchmark'>('search');
  const [folders, setFolders] = useState<FolderConfig[]>([]);
  const [newFolderPath, setNewFolderPath] = useState('');
  const [newFolderName, setNewFolderName] = useState('');
  const [benchQuery, setBenchQuery] = useState('data OR java');
  const [benchResult, setBenchResult] = useState<BenchmarkResult | null>(null);
  const [benchLoading, setBenchLoading] = useState(false);
  const [expandedResult, setExpandedResult] = useState<number | null>(null);

  const [notification, setNotification] = useState('');
  const searchRef = useRef<HTMLInputElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  const notify = useCallback((msg: string) => {
    setNotification(msg);
    setTimeout(() => setNotification(''), 2200);
  }, []);

  useEffect(() => {
    api.checkHealth().then(() => setBackendUp(true)).catch(() => setBackendUp(false));
    api.getFolders().then(setFolders).catch(() => {});
  }, []);

  const doSearch = useCallback(async (q: string, mode?: string, type?: string) => {
    if (!q.trim()) { setResponse(null); return; }
    setLoading(true);
    try {
      const res = await api.searchDocuments(q.trim(), mode || rankingMode, (type !== undefined ? type : fileType) || undefined);
      setResponse(res);
      setExpandedResult(null);
    } catch (e: unknown) { notify((e as Error).message); }
    finally { setLoading(false); }
  }, [rankingMode, fileType, notify]);

  const onQueryChange = (val: string) => {
    setQuery(val);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (!val.trim()) { setResponse(null); return; }
    debounceRef.current = setTimeout(() => doSearch(val), 200);
  };

  const addFolder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFolderPath.trim()) return;
    try {
      await api.addFolder(newFolderPath.trim(), newFolderName.trim() || undefined);
      setNewFolderPath('');
      setNewFolderName('');
      notify('Folder added and indexed');
      const f = await api.getFolders();
      setFolders(f);
    } catch (err: unknown) { notify((err as Error).message); }
  };

  const removeFolder = async (id: string) => {
    try {
      await api.deleteFolder(id);
      notify('Folder removed');
      const f = await api.getFolders();
      setFolders(f);
    } catch (err: unknown) { notify((err as Error).message); }
  };

  const reindex = async (id: string) => {
    try {
      await api.reindexFolder(id);
      notify('Reindexing...');
      const f = await api.getFolders();
      setFolders(f);
    } catch (err: unknown) { notify((err as Error).message); }
  };

  const runBenchmark = async () => {
    if (!benchQuery.trim()) return;
    setBenchLoading(true);
    try {
      const res = await api.runBenchmark(benchQuery.trim());
      setBenchResult(res);
    } catch (err: unknown) { notify((err as Error).message); }
    finally { setBenchLoading(false); }
  };

  const openDoc = async (id: number) => {
    try { await api.openDocument(id); notify('Opened'); }
    catch (e: unknown) { notify((e as Error).message); }
  };

  const copyPath = (p: string) => {
    navigator.clipboard.writeText(p);
    notify('Path copied');
  };

  return (
    <div className="app">
      <nav className="nav">
        <div className="nav-left">
          <span className="logo">Findex</span>
          <span className={`dot ${backendUp ? 'on' : 'off'}`} />
        </div>
        <div className="nav-tabs">
          <button className={`tab ${view === 'search' ? 'active' : ''}`} onClick={() => setView('search')}>Search</button>
          <button className={`tab ${view === 'folders' ? 'active' : ''}`} onClick={() => { setView('folders'); api.getFolders().then(setFolders).catch(() => {}); }}>Folders</button>
          <button className={`tab ${view === 'benchmark' ? 'active' : ''}`} onClick={() => setView('benchmark')}>Benchmark</button>
        </div>
      </nav>

      <main className="content">
        {view === 'search' && (
          <div className="search-page">
            <div className="search-bar-wrap">
              <input
                ref={searchRef}
                className="search-input"
                type="text"
                placeholder="Search your documents..."
                value={query}
                onChange={e => onQueryChange(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') doSearch(query); }}
                autoFocus
              />
              {query && (
                <button className="clear-btn" onClick={() => { setQuery(''); setResponse(null); searchRef.current?.focus(); }}>✕</button>
              )}
            </div>

            <div className="filters">
              <select value={fileType} onChange={e => { setFileType(e.target.value); if (query.trim()) doSearch(query, rankingMode, e.target.value); }}>
                <option value="">All types</option>
                <option value="txt">.txt</option>
                <option value="java">.java</option>
                <option value="md">.md</option>
                <option value="pdf">.pdf</option>
              </select>
              <select value={rankingMode} onChange={e => { setRankingMode(e.target.value); if (query.trim()) doSearch(query, e.target.value, fileType); }}>
                <option value="bm25">BM25</option>
                <option value="tfidf">TF-IDF</option>
              </select>
            </div>

            {loading && <div className="status-line">Searching...</div>}

            {response && (
              <div className="results-info">
                {response.totalResults} result{response.totalResults !== 1 ? 's' : ''} in {response.timeTakenMs} ms
              </div>
            )}

            {response && response.results.length === 0 && (
              <div className="empty-msg">No results for "{response.query}"</div>
            )}

            {response && response.results.length > 0 && (
              <div className="results">
                {response.results.map((r: SearchResult) => (
                  <div key={r.document.id} className={`result ${expandedResult === r.document.id ? 'expanded' : ''}`}>
                    <div className="result-top" onClick={() => setExpandedResult(expandedResult === r.document.id ? null : r.document.id)}>
                      <div className="result-title">
                        <span className="ext">{r.document.extension}</span>
                        <span className="fname">{r.document.fileName}</span>
                      </div>
                      <span className="score">{r.score.toFixed(3)}</span>
                    </div>
                    <div className="snippet" dangerouslySetInnerHTML={{ __html: r.snippet || r.document.previewText }} />
                    <div className="fpath">{r.document.filePath}</div>

                    {expandedResult === r.document.id && (
                      <div className="result-detail">
                        <div className="detail-row">
                          <span>Matches: {r.matchCount}</span>
                          <span>Terms: {r.document.termCount}</span>
                          <span>Size: {r.document.fileSize} B</span>
                        </div>
                        {r.scoreExplanation && <div className="explanation">{r.scoreExplanation}</div>}
                        <div className="detail-actions">
                          <button onClick={() => copyPath(r.document.filePath)}>Copy path</button>
                          <button onClick={() => openDoc(r.document.id)}>Open file</button>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            {!response && !loading && (
              <div className="hints">
                <p>Try searching:</p>
                {['java AND classes', 'algorithms OR inheritance', 'programming -python', '"classes and inheritance"'].map(q => (
                  <button key={q} className="hint-btn" onClick={() => { setQuery(q); doSearch(q); }}>{q}</button>
                ))}
              </div>
            )}
          </div>
        )}

        {view === 'folders' && (
          <div className="folders-page">
            <h2>Indexed Folders</h2>
            <form onSubmit={addFolder} className="add-folder-form">
              <input
                type="text"
                placeholder="Folder path (e.g. C:/Users/joy/Documents)"
                value={newFolderPath}
                onChange={e => setNewFolderPath(e.target.value)}
              />
              <input
                type="text"
                placeholder="Name (optional)"
                value={newFolderName}
                onChange={e => setNewFolderName(e.target.value)}
                style={{ maxWidth: 160 }}
              />
              <button type="submit" disabled={!newFolderPath.trim()}>Add</button>
            </form>

            {folders.length === 0 ? (
              <p className="empty-msg">No folders indexed yet. Add a folder above to get started.</p>
            ) : (
              <div className="folder-list">
                {folders.map(f => (
                  <div key={f.id} className="folder-row">
                    <div className="folder-info">
                      <span className="folder-name">{f.name}</span>
                      <span className="folder-path">{f.path}</span>
                      <span className="folder-count">{f.fileCount} files</span>
                    </div>
                    <div className="folder-actions">
                      <button onClick={() => reindex(f.id)}>Reindex</button>
                      <button className="danger" onClick={() => removeFolder(f.id)}>Remove</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {view === 'benchmark' && (
          <div className="benchmark-page">
            <h2>Benchmark</h2>
            <p className="bench-desc">Compare inverted index lookup speed vs linear scan.</p>
            <div className="bench-form">
              <input
                type="text"
                placeholder="Query to benchmark"
                value={benchQuery}
                onChange={e => setBenchQuery(e.target.value)}
              />
              <button onClick={runBenchmark} disabled={benchLoading || !benchQuery.trim()}>
                {benchLoading ? 'Running...' : 'Run'}
              </button>
            </div>

            {benchResult && (
              <div className="bench-results">
                <div className="bench-card">
                  <span className="bench-label">Inverted Index</span>
                  <span className="bench-value">{benchResult.invertedIndexMs} ms</span>
                </div>
                <div className="bench-card">
                  <span className="bench-label">Linear Scan</span>
                  <span className="bench-value">{benchResult.linearScanMs} ms</span>
                </div>
                <div className="bench-card highlight">
                  <span className="bench-label">Speedup</span>
                  <span className="bench-value">{benchResult.speedupFactor}×</span>
                </div>
                <div className="bench-card">
                  <span className="bench-label">Docs matched</span>
                  <span className="bench-value">{benchResult.matchingDocuments} / {benchResult.totalDocumentsScanned}</span>
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {notification && <div className="toast">{notification}</div>}
    </div>
  );
}
