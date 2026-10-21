import { useState, useEffect, useCallback, useRef } from 'react';
import {
  Search,
  FolderOpen,
  Zap,
  FileText,
  X,
  ExternalLink,
  Copy,
  Check,
  Plus,
  RefreshCw,
  Trash2,
  SlidersHorizontal,
  FileCode,
  HardDrive
} from 'lucide-react';
import * as api from './api';
import type {
  SearchResponse,
  SearchResult,
  FolderConfig,
  IndexStats,
  BenchmarkResult
} from './api';

export default function App() {
  const [query, setQuery] = useState('');
  const [rankingMode, setRankingMode] = useState('bm25');
  const [fileType, setFileType] = useState('');
  const [response, setResponse] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState<number>(0);
  const [backendUp, setBackendUp] = useState(true);

  const [foldersModal, setFoldersModal] = useState(false);
  const [benchModal, setBenchModal] = useState(false);
  const [folders, setFolders] = useState<FolderConfig[]>([]);
  const [newFolderPath, setNewFolderPath] = useState('');
  const [newFolderName, setNewFolderName] = useState('');
  const [stats, setStats] = useState<IndexStats | null>(null);
  const [benchQuery, setBenchQuery] = useState('data OR java');
  const [benchResult, setBenchResult] = useState<BenchmarkResult | null>(null);
  const [benchLoading, setBenchLoading] = useState(false);

  const [copied, setCopied] = useState(false);
  const [toasts, setToasts] = useState<{ id: number; msg: string }[]>([]);

  const searchInputRef = useRef<HTMLInputElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  const toast = useCallback((msg: string) => {
    const id = Date.now();
    setToasts(t => [...t, { id, msg }]);
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 2500);
  }, []);

  const loadHealthAndStats = useCallback(async () => {
    try {
      await api.checkHealth();
      setBackendUp(true);
      const s = await api.getStats();
      setStats(s);
    } catch {
      setBackendUp(false);
    }
  }, []);

  const loadFolders = useCallback(async () => {
    try {
      const data = await api.getFolders();
      setFolders(data);
    } catch {}
  }, []);

  useEffect(() => {
    loadHealthAndStats();
    loadFolders();
  }, [loadHealthAndStats, loadFolders]);

  const runSearch = useCallback(async (q: string, modeOverride?: string, typeOverride?: string) => {
    const targetQ = q.trim();
    if (!targetQ) {
      setResponse(null);
      return;
    }
    setLoading(true);
    try {
      const mode = modeOverride ?? rankingMode;
      const type = typeOverride !== undefined ? typeOverride : fileType;
      const res = await api.searchDocuments(targetQ, mode, type || undefined);
      setResponse(res);
      setSelectedIndex(0);
    } catch (e: unknown) {
      toast((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, [rankingMode, fileType, toast]);

  const onSearchChange = (val: string) => {
    setQuery(val);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (!val.trim()) {
      setResponse(null);
      return;
    }
    debounceRef.current = setTimeout(() => {
      runSearch(val);
    }, 180);
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === '/' && document.activeElement !== searchInputRef.current && !foldersModal && !benchModal) {
        e.preventDefault();
        searchInputRef.current?.focus();
      } else if (e.key === 'Escape') {
        if (foldersModal) setFoldersModal(false);
        else if (benchModal) setBenchModal(false);
        else if (query) {
          setQuery('');
          setResponse(null);
        }
      } else if (response && response.results.length > 0 && !foldersModal && !benchModal) {
        if (e.key === 'ArrowDown') {
          e.preventDefault();
          setSelectedIndex(prev => Math.min(prev + 1, response.results.length - 1));
        } else if (e.key === 'ArrowUp') {
          e.preventDefault();
          setSelectedIndex(prev => Math.max(prev - 1, 0));
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [foldersModal, benchModal, query, response]);

  const handleOpenDoc = async (id: number) => {
    try {
      await api.openDocument(id);
      toast('Document opened');
    } catch (e: unknown) {
      toast((e as Error).message);
    }
  };

  const handleCopyPath = (filePath: string) => {
    navigator.clipboard.writeText(filePath);
    setCopied(true);
    toast('File path copied');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleAddFolder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFolderPath.trim()) return;
    try {
      await api.addFolder(newFolderPath.trim(), newFolderName.trim() || undefined);
      setNewFolderPath('');
      setNewFolderName('');
      toast('Folder registered and indexed');
      loadFolders();
      loadHealthAndStats();
    } catch (err: unknown) {
      toast((err as Error).message);
    }
  };

  const handleDeleteFolder = async (id: string) => {
    try {
      await api.deleteFolder(id);
      toast('Folder removed');
      loadFolders();
      loadHealthAndStats();
    } catch (err: unknown) {
      toast((err as Error).message);
    }
  };

  const handleReindexFolder = async (id: string) => {
    try {
      await api.reindexFolder(id);
      toast('Reindexing started');
      loadFolders();
      loadHealthAndStats();
    } catch (err: unknown) {
      toast((err as Error).message);
    }
  };

  const handleRebuildAll = async () => {
    try {
      await api.rebuildIndex();
      toast('Rebuilding all indexes');
      loadFolders();
      loadHealthAndStats();
    } catch (err: unknown) {
      toast((err as Error).message);
    }
  };

  const runBenchmarkTest = async () => {
    if (!benchQuery.trim()) return;
    setBenchLoading(true);
    try {
      const res = await api.runBenchmark(benchQuery.trim());
      setBenchResult(res);
      toast('Benchmark finished');
    } catch (err: unknown) {
      toast((err as Error).message);
    } finally {
      setBenchLoading(false);
    }
  };

  const selectedResult: SearchResult | undefined = response?.results[selectedIndex];

  return (
    <div className="desktop-app">
      <header className="top-bar">
        <div className="brand-section">
          <FileCode size={20} color="#3b82f6" />
          <span>Findex</span>
          <span
            style={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              backgroundColor: backendUp ? '#10b981' : '#ef4444',
              display: 'inline-block'
            }}
            title={backendUp ? 'Engine Online' : 'Engine Offline'}
          />
        </div>

        <div className="search-field-container">
          <Search size={16} color="var(--text-muted)" style={{ marginRight: 8, flexShrink: 0 }} />
          <input
            ref={searchInputRef}
            type="text"
            className="search-field-input"
            placeholder="Search documents... (e.g. java AND classes, algorithms, -python)"
            value={query}
            onChange={e => onSearchChange(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter') runSearch(query);
            }}
            autoFocus
          />
          {query ? (
            <button
              style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: 2 }}
              onClick={() => {
                setQuery('');
                setResponse(null);
                searchInputRef.current?.focus();
              }}
            >
              <X size={15} />
            </button>
          ) : (
            <span className="kbd-badge">/</span>
          )}
        </div>

        <div className="actions-section">
          <select
            className="filter-select"
            value={fileType}
            onChange={e => {
              const val = e.target.value;
              setFileType(val);
              if (query.trim()) runSearch(query, rankingMode, val);
            }}
          >
            <option value="">All Formats</option>
            <option value="txt">.txt</option>
            <option value="java">.java</option>
            <option value="md">.md</option>
            <option value="pdf">.pdf</option>
          </select>

          <select
            className="filter-select"
            value={rankingMode}
            onChange={e => {
              const m = e.target.value;
              setRankingMode(m);
              if (query.trim()) runSearch(query, m, fileType);
            }}
          >
            <option value="bm25">BM25</option>
            <option value="tfidf">TF-IDF</option>
          </select>

          <button className="icon-btn" onClick={() => setFoldersModal(true)}>
            <FolderOpen size={14} />
            <span>Folders ({folders.length})</span>
          </button>

          <button className="icon-btn" onClick={() => { setBenchModal(true); loadHealthAndStats(); }}>
            <Zap size={14} />
            <span>Benchmark</span>
          </button>
        </div>
      </header>

      <div className="main-split">
        <aside className="results-pane">
          <div className="results-status-bar">
            <span>
              {response ? `${response.totalResults} results (${response.timeTakenMs} ms)` : 'Search results'}
            </span>
            {loading && <RefreshCw size={12} className="spin" />}
          </div>

          <div className="results-scroll">
            {response && response.results.length > 0 ? (
              response.results.map((r, idx) => (
                <div
                  key={r.document.id}
                  className={`item-card ${selectedIndex === idx ? 'selected' : ''}`}
                  onClick={() => setSelectedIndex(idx)}
                >
                  <div className="item-header">
                    <div className="item-title-group">
                      <span className="tag-badge">{r.document.extension}</span>
                      <span className="item-name">{r.document.fileName}</span>
                    </div>
                    <span className="item-score">{r.score.toFixed(3)}</span>
                  </div>

                  <div
                    className="item-snippet"
                    dangerouslySetInnerHTML={{ __html: r.snippet || r.document.previewText }}
                  />

                  <div className="item-path">{r.document.filePath}</div>
                </div>
              ))
            ) : response && response.results.length === 0 ? (
              <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-muted)', fontSize: 13 }}>
                No documents matched "{response.query}".
              </div>
            ) : (
              <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-muted)', fontSize: 13 }}>
                <p style={{ marginBottom: 16 }}>Type a keyword or click a query to search:</p>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {[
                    ['java AND classes', 'Boolean AND intersection'],
                    ['algorithms OR inheritance', 'Boolean OR union'],
                    ['programming -python', 'Exclusion with prefix -'],
                    ['"classes and inheritance"', 'Exact phrase matching']
                  ].map(([q, desc]) => (
                    <button
                      key={q}
                      className="icon-btn"
                      style={{ justifyContent: 'space-between', width: '100%' }}
                      onClick={() => {
                        setQuery(q);
                        runSearch(q);
                      }}
                    >
                      <span style={{ fontFamily: 'JetBrains Mono, monospace', color: 'var(--accent)' }}>{q}</span>
                      <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{desc}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </aside>

        <main className="inspector-pane">
          {selectedResult ? (
            <>
              <div className="inspector-header">
                <div className="inspector-meta">
                  <div className="inspector-title">{selectedResult.document.fileName}</div>
                  <div className="inspector-path">{selectedResult.document.filePath}</div>
                </div>

                <div className="inspector-actions">
                  <button
                    className="btn-secondary"
                    onClick={() => handleCopyPath(selectedResult.document.filePath)}
                  >
                    {copied ? <Check size={13} color="var(--success)" /> : <Copy size={13} />}
                    <span>{copied ? 'Copied' : 'Copy Path'}</span>
                  </button>

                  <button
                    className="btn-primary"
                    onClick={() => handleOpenDoc(selectedResult.document.id)}
                  >
                    <ExternalLink size={13} />
                    <span>Open File</span>
                  </button>
                </div>
              </div>

              <div className="inspector-body">
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, marginBottom: 16 }}>
                  <div className="stat-box">
                    <span className="stat-txt">Relevance</span>
                    <span className="stat-num">{selectedResult.score}</span>
                  </div>
                  <div className="stat-box">
                    <span className="stat-txt">Matches</span>
                    <span className="stat-num">{selectedResult.matchCount}</span>
                  </div>
                  <div className="stat-box">
                    <span className="stat-txt">Total Terms</span>
                    <span className="stat-num">{selectedResult.document.termCount}</span>
                  </div>
                  <div className="stat-box">
                    <span className="stat-txt">File Size</span>
                    <span className="stat-num">{selectedResult.document.fileSize} B</span>
                  </div>
                </div>

                <div style={{ marginBottom: 14 }}>
                  <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: 6 }}>
                    Score Breakdown
                  </div>
                  <div style={{ background: 'var(--bg-app)', border: '1px solid var(--border)', borderRadius: 6, padding: '8px 12px', fontSize: 12, fontFamily: 'JetBrains Mono, monospace', color: 'var(--text-secondary)' }}>
                    {selectedResult.scoreExplanation || 'Direct Boolean match'}
                  </div>
                </div>

                <div>
                  <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: 6 }}>
                    Document Content Preview
                  </div>
                  <div
                    className="preview-code-block"
                    dangerouslySetInnerHTML={{ __html: selectedResult.snippet || selectedResult.document.previewText }}
                  />
                </div>
              </div>
            </>
          ) : (
            <div className="empty-inspector">
              <FileText size={32} color="var(--text-muted)" style={{ marginBottom: 8 }} />
              <p>Select a document on the left to inspect content and score breakdown.</p>
              <p style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 4 }}>
                Navigate items with <kbd className="kbd-badge">↑</kbd> and <kbd className="kbd-badge">↓</kbd> keys.
              </p>
            </div>
          )}
        </main>
      </div>

      {foldersModal && (
        <div className="modal-backdrop" onClick={() => setFoldersModal(false)}>
          <div className="modal-window" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <span className="modal-title">Indexed Folders ({folders.length})</span>
              <button
                style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
                onClick={() => setFoldersModal(false)}
              >
                <X size={16} />
              </button>
            </div>

            <div className="modal-body">
              <form onSubmit={handleAddFolder} className="input-row">
                <input
                  type="text"
                  className="form-input"
                  placeholder="Absolute folder path (e.g. C:/Users/joy/Desktop/findex/documents)"
                  value={newFolderPath}
                  onChange={e => setNewFolderPath(e.target.value)}
                />
                <input
                  type="text"
                  className="form-input"
                  style={{ maxWidth: 140 }}
                  placeholder="Name (optional)"
                  value={newFolderName}
                  onChange={e => setNewFolderName(e.target.value)}
                />
                <button type="submit" className="btn-primary" disabled={!newFolderPath.trim()}>
                  <Plus size={13} />
                  <span>Add</span>
                </button>
              </form>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => {
                    setNewFolderPath('C:/Users/joy/Desktop/findex/documents');
                    setNewFolderName('Sample Documents');
                  }}
                >
                  Auto-fill Sample Documents
                </button>

                <button type="button" className="btn-secondary" onClick={handleRebuildAll}>
                  <RefreshCw size={12} />
                  <span>Rebuild All</span>
                </button>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 4 }}>
                {folders.map(f => (
                  <div key={f.id} className="folder-item">
                    <div style={{ minWidth: 0, flex: 1 }}>
                      <div style={{ fontWeight: 600, color: '#fff' }}>{f.name}</div>
                      <div style={{ fontSize: 11, fontFamily: 'JetBrains Mono, monospace', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {f.path}
                      </div>
                      <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 2 }}>
                        {f.fileCount} documents indexed
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: 6 }}>
                      <button className="btn-secondary" onClick={() => handleReindexFolder(f.id)} title="Reindex folder">
                        <RefreshCw size={12} />
                      </button>
                      <button
                        style={{ background: 'var(--danger-soft)', border: '1px solid rgba(239,68,68,0.2)', color: 'var(--danger)', borderRadius: 6, padding: '4px 8px', cursor: 'pointer' }}
                        onClick={() => handleDeleteFolder(f.id)}
                        title="Delete folder"
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {benchModal && (
        <div className="modal-backdrop" onClick={() => setBenchModal(false)}>
          <div className="modal-window" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <span className="modal-title">Engine Performance & Benchmark</span>
              <button
                style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
                onClick={() => setBenchModal(false)}
              >
                <X size={16} />
              </button>
            </div>

            <div className="modal-body">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
                <div className="stat-box">
                  <span className="stat-txt">Documents</span>
                  <span className="stat-num">{stats?.totalDocuments ?? 0}</span>
                </div>
                <div className="stat-box">
                  <span className="stat-txt">Unique Terms</span>
                  <span className="stat-num">{stats?.uniqueTerms ?? 0}</span>
                </div>
                <div className="stat-box">
                  <span className="stat-txt">Postings</span>
                  <span className="stat-num">{stats?.totalPostings ?? 0}</span>
                </div>
                <div className="stat-box">
                  <span className="stat-txt">JVM Memory</span>
                  <span className="stat-num">{stats ? `${stats.memoryUsageMb}M` : '...'}</span>
                </div>
              </div>

              <div style={{ marginTop: 8 }}>
                <div style={{ fontSize: 12, fontWeight: 600, color: '#fff', marginBottom: 6 }}>
                  Empirical Benchmark (Inverted Index vs. Linear Scan)
                </div>
                <div className="input-row">
                  <input
                    type="text"
                    className="form-input"
                    value={benchQuery}
                    onChange={e => setBenchQuery(e.target.value)}
                    placeholder="Benchmark query (e.g. data OR java)"
                  />
                  <button className="btn-primary" onClick={runBenchmarkTest} disabled={benchLoading || !benchQuery.trim()}>
                    <Zap size={13} />
                    <span>{benchLoading ? 'Running...' : 'Run Benchmark'}</span>
                  </button>
                </div>
              </div>

              {benchResult && (
                <div style={{ background: 'var(--bg-app)', border: '1px solid var(--border)', borderRadius: 8, padding: 12 }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--success)', marginBottom: 8 }}>
                    Speedup: {benchResult.speedupFactor}x Faster with Inverted Index
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, fontSize: 12 }}>
                    <div style={{ background: 'var(--bg-elevated)', padding: 10, borderRadius: 6 }}>
                      <div style={{ color: 'var(--text-muted)' }}>Inverted Index</div>
                      <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--accent)', marginTop: 2 }}>
                        {benchResult.invertedIndexMs} ms
                      </div>
                    </div>
                    <div style={{ background: 'var(--bg-elevated)', padding: 10, borderRadius: 6 }}>
                      <div style={{ color: 'var(--text-muted)' }}>Linear Scan baseline</div>
                      <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--warning)', marginTop: 2 }}>
                        {benchResult.linearScanMs} ms
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className="toast-msg">{t.msg}</div>
        ))}
      </div>
    </div>
  );
}
