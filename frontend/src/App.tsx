import { useState, useEffect, useCallback, useRef } from 'react';
import {
  Search,
  FolderOpen,
  Activity,
  FileText,
  X,
  Plus,
  RefreshCw,
  Trash2,
  ExternalLink,
  ChevronDown,
  ChevronUp,
  Zap,
  CheckCircle,
  Clock,
  Sparkles
} from 'lucide-react';
import * as api from './api';
import type {
  SearchResponse,
  SearchResult,
  FolderConfig,
  IndexStats,
  IndexingProgress,
  BenchmarkResult,
  HistoryEntry
} from './api';

type Tab = 'search' | 'folders' | 'benchmark';

export default function App() {
  const [tab, setTab] = useState<Tab>('search');
  const [backendUp, setBackendUp] = useState<boolean>(true);
  const [toasts, setToasts] = useState<{ id: number; msg: string; type: 'success' | 'error' }[]>([]);

  const toast = useCallback((msg: string, type: 'success' | 'error' = 'success') => {
    const id = Date.now();
    setToasts(t => [...t, { id, msg, type }]);
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 3000);
  }, []);

  useEffect(() => {
    api.checkHealth()
      .then(() => setBackendUp(true))
      .catch(() => setBackendUp(false));
  }, []);

  return (
    <>
      <header className="header">
        <div className="brand">
          <FileText size={18} color="#3b82f6" />
          <span>Findex</span>
          <span className="brand-badge">Local Engine</span>
          <span
            style={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              backgroundColor: backendUp ? '#10b981' : '#ef4444',
              display: 'inline-block',
              marginLeft: 4
            }}
            title={backendUp ? 'Backend Connected' : 'Backend Disconnected'}
          />
        </div>

        <nav className="nav-tabs">
          <button
            className={`nav-tab ${tab === 'search' ? 'active' : ''}`}
            onClick={() => setTab('search')}
          >
            <Search size={15} />
            <span>Search</span>
          </button>
          <button
            className={`nav-tab ${tab === 'folders' ? 'active' : ''}`}
            onClick={() => setTab('folders')}
          >
            <FolderOpen size={15} />
            <span>Folders</span>
          </button>
          <button
            className={`nav-tab ${tab === 'benchmark' ? 'active' : ''}`}
            onClick={() => setTab('benchmark')}
          >
            <Activity size={15} />
            <span>Benchmark & Stats</span>
          </button>
        </nav>
      </header>

      <main className="container">
        {tab === 'search' && <SearchTab toast={toast} />}
        {tab === 'folders' && <FoldersTab toast={toast} />}
        {tab === 'benchmark' && <BenchmarkTab toast={toast} />}
      </main>

      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className={`toast ${t.type}`}>{t.msg}</div>
        ))}
      </div>
    </>
  );
}

/* ==================== SEARCH TAB ==================== */
function SearchTab({ toast }: { toast: (m: string, t?: 'success' | 'error') => void }) {
  const [query, setQuery] = useState('');
  const [rankingMode, setRankingMode] = useState('bm25');
  const [fileType, setFileType] = useState('');
  const [response, setResponse] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const debounceTimer = useRef<ReturnType<typeof setTimeout>>();

  // Global '/' shortcut to focus search input
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === '/' && document.activeElement !== inputRef.current) {
        e.preventDefault();
        inputRef.current?.focus();
      } else if (e.key === 'Escape') {
        inputRef.current?.blur();
        setShowSuggestions(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const executeSearch = useCallback(async (q: string, modeOverride?: string, typeOverride?: string) => {
    const targetQuery = q.trim();
    if (!targetQuery) return;
    setLoading(true);
    setShowSuggestions(false);
    try {
      const mode = modeOverride ?? rankingMode;
      const type = typeOverride !== undefined ? typeOverride : fileType;
      const res = await api.searchDocuments(targetQuery, mode, type || undefined);
      setResponse(res);
    } catch (e: unknown) {
      toast((e as Error).message, 'error');
    } finally {
      setLoading(false);
    }
  }, [rankingMode, fileType, toast]);

  const handleInputChange = (val: string) => {
    setQuery(val);
    if (debounceTimer.current) clearTimeout(debounceTimer.current);

    if (val.trim().length >= 2) {
      debounceTimer.current = setTimeout(async () => {
        try {
          const sug = await api.getSuggestions(val);
          setSuggestions(sug);
          setShowSuggestions(sug.length > 0);
        } catch {
          // ignore suggestions error
        }
      }, 150);
    } else {
      setShowSuggestions(false);
    }
  };

  const handleOpenDoc = async (id: number) => {
    try {
      await api.openDocument(id);
      toast('Opened in system editor');
    } catch (e: unknown) {
      toast((e as Error).message, 'error');
    }
  };

  return (
    <div>
      <div className="search-wrapper">
        <div className="search-input-box">
          <Search size={18} className="search-icon" />
          <input
            ref={inputRef}
            type="text"
            className="search-input"
            placeholder="Search documents... (e.g. java AND classes, algorithms, or -python)"
            value={query}
            onChange={e => handleInputChange(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter') {
                executeSearch(query);
              }
            }}
            autoFocus
          />
          {query ? (
            <button
              className="clear-btn"
              onClick={() => {
                setQuery('');
                setResponse(null);
                setShowSuggestions(false);
                inputRef.current?.focus();
              }}
              title="Clear"
            >
              <X size={16} />
            </button>
          ) : (
            <span className="kbd-shortcut">/</span>
          )}
        </div>

        {/* Suggestions Popup */}
        {showSuggestions && suggestions.length > 0 && (
          <div
            style={{
              position: 'relative',
              background: 'var(--bg-surface)',
              border: '1px solid var(--border)',
              borderRadius: 8,
              marginTop: 6,
              overflow: 'hidden',
              boxShadow: '0 4px 16px rgba(0,0,0,0.4)',
              zIndex: 40
            }}
          >
            {suggestions.map(s => (
              <div
                key={s}
                style={{
                  padding: '8px 14px',
                  fontSize: 13,
                  cursor: 'pointer',
                  borderBottom: '1px solid rgba(255,255,255,0.04)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8
                }}
                onMouseDown={() => {
                  setQuery(s);
                  executeSearch(s);
                }}
              >
                <Search size={12} color="var(--text-muted)" />
                <span>{s}</span>
              </div>
            ))}
          </div>
        )}

        {/* Filter and Mode Controls */}
        <div className="controls-bar">
          <div className="filter-group">
            <span style={{ fontSize: 12, color: 'var(--text-muted)', marginRight: 4 }}>Filter:</span>
            {['', 'txt', 'java', 'md', 'pdf'].map(ext => (
              <button
                key={ext}
                className={`filter-chip ${fileType === ext ? 'active' : ''}`}
                onClick={() => {
                  setFileType(ext);
                  if (query.trim()) executeSearch(query, rankingMode, ext);
                }}
              >
                {ext === '' ? 'All Files' : `.${ext}`}
              </button>
            ))}
          </div>

          <div className="filter-group">
            <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>Ranking:</span>
            <select
              className="select-compact"
              value={rankingMode}
              onChange={e => {
                const newMode = e.target.value;
                setRankingMode(newMode);
                if (query.trim()) executeSearch(query, newMode);
              }}
            >
              <option value="bm25">BM25 (Default)</option>
              <option value="tfidf">TF-IDF</option>
            </select>

            <button
              className="btn btn-primary btn-sm"
              style={{ marginLeft: 6 }}
              disabled={loading || !query.trim()}
              onClick={() => executeSearch(query)}
            >
              {loading ? <RefreshCw size={12} className="spin" /> : 'Search'}
            </button>
          </div>
        </div>
      </div>

      {/* Results or Hints */}
      {response ? (
        <div>
          <div className="results-meta">
            <span>
              {response.totalResults} document{response.totalResults === 1 ? '' : 's'} found in{' '}
              <strong style={{ color: 'var(--text-primary)' }}>{response.timeTakenMs} ms</strong>{' '}
              ({response.mode.toUpperCase()} model)
            </span>
          </div>

          {response.results.length === 0 ? (
            <div className="empty-state">
              <p>No matching documents found for "{response.query}".</p>
              <p style={{ fontSize: 12, marginTop: 6, color: 'var(--text-muted)' }}>
                Try adjusting your Boolean terms or searching for broader keywords.
              </p>
            </div>
          ) : (
            <div className="results-list">
              {response.results.map(r => (
                <ResultCardItem key={r.document.id} result={r} onOpen={handleOpenDoc} />
              ))}
            </div>
          )}
        </div>
      ) : (
        <div className="empty-state">
          <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
            Instant search across registered local code, markdown, and text notes.
          </p>

          <div className="query-tips">
            <div
              className="tip-card"
              onClick={() => {
                setQuery('java AND classes');
                executeSearch('java AND classes');
              }}
            >
              <div className="tip-query">java AND classes</div>
              <div className="tip-desc">Boolean intersection: both terms must be present</div>
            </div>

            <div
              className="tip-card"
              onClick={() => {
                setQuery('algorithms OR inheritance');
                executeSearch('algorithms OR inheritance');
              }}
            >
              <div className="tip-query">algorithms OR inheritance</div>
              <div className="tip-desc">Boolean union: matches either term with ranked scores</div>
            </div>

            <div
              className="tip-card"
              onClick={() => {
                setQuery('programming -python');
                executeSearch('programming -python');
              }}
            >
              <div className="tip-query">programming -python</div>
              <div className="tip-desc">Exclusion: finds programming without python</div>
            </div>

            <div
              className="tip-card"
              onClick={() => {
                setQuery('"classes and inheritance"');
                executeSearch('"classes and inheritance"');
              }}
            >
              <div className="tip-query">"classes and inheritance"</div>
              <div className="tip-desc">Positional phrase search: exact adjacent sequence</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function ResultCardItem({
  result,
  onOpen
}: {
  result: SearchResult;
  onOpen: (id: number) => void;
}) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="result-card">
      <div className="result-header">
        <div className="result-title-group">
          <span className="file-badge">{result.document.extension}</span>
          <span className="result-title">{result.document.fileName}</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className="score-badge">Score: {result.score}</span>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => onOpen(result.document.id)}
            title="Open in default editor"
          >
            <ExternalLink size={12} />
            <span>Open</span>
          </button>
        </div>
      </div>

      <div className="result-path">{result.document.filePath}</div>

      <div
        className="result-snippet"
        dangerouslySetInnerHTML={{ __html: result.snippet || 'No snippet preview available' }}
      />

      <div className="result-footer">
        <div className="matched-tags">
          {result.matchedTerms.map(t => (
            <span key={t} className="matched-tag">{t}</span>
          ))}
          <span style={{ marginLeft: 6 }}>{result.matchCount} occurrences</span>
        </div>

        <button
          style={{
            background: 'transparent',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer',
            fontSize: 11,
            display: 'flex',
            alignItems: 'center',
            gap: 4
          }}
          onClick={() => setExpanded(!expanded)}
        >
          <span>Score Breakdown</span>
          {expanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
        </button>
      </div>

      {expanded && (
        <div
          style={{
            marginTop: 10,
            padding: '8px 12px',
            background: 'var(--bg-app)',
            borderRadius: 6,
            fontSize: 11,
            fontFamily: 'JetBrains Mono, monospace',
            color: 'var(--text-muted)',
            border: '1px solid var(--border)'
          }}
        >
          {result.scoreExplanation}
        </div>
      )}
    </div>
  );
}

/* ==================== FOLDERS TAB ==================== */
function FoldersTab({ toast }: { toast: (m: string, t?: 'success' | 'error') => void }) {
  const [folders, setFolders] = useState<FolderConfig[]>([]);
  const [pathInput, setPathInput] = useState('');
  const [nameInput, setNameInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState<IndexingProgress | null>(null);

  const fetchFolders = useCallback(async () => {
    try {
      const data = await api.getFolders();
      setFolders(data);
    } catch {
      // ignore
    }
  }, []);

  const fetchStatus = useCallback(async () => {
    try {
      const p = await api.getIndexStatus();
      setProgress(p);
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    fetchFolders();
    fetchStatus();
    const timer = setInterval(fetchStatus, 1500);
    return () => clearInterval(timer);
  }, [fetchFolders, fetchStatus]);

  const handleAddFolder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pathInput.trim()) return;
    setLoading(true);
    try {
      await api.addFolder(pathInput.trim(), nameInput.trim() || undefined);
      setPathInput('');
      setNameInput('');
      toast('Folder registered and indexed');
      fetchFolders();
    } catch (err: unknown) {
      toast((err as Error).message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleReindex = async (id: string) => {
    try {
      await api.reindexFolder(id);
      toast('Reindexing started');
      fetchFolders();
    } catch (err: unknown) {
      toast((err as Error).message, 'error');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.deleteFolder(id);
      toast('Folder removed');
      fetchFolders();
    } catch (err: unknown) {
      toast((err as Error).message, 'error');
    }
  };

  const handleRebuildAll = async () => {
    try {
      await api.rebuildIndex();
      toast('Full index rebuild triggered');
      fetchFolders();
    } catch (err: unknown) {
      toast((err as Error).message, 'error');
    }
  };

  return (
    <div>
      {/* Index Status Banner */}
      {progress && (
        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border)',
            borderRadius: 8,
            padding: '12px 16px',
            marginBottom: 20,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: 13
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span
              style={{
                fontSize: 11,
                fontFamily: 'JetBrains Mono, monospace',
                fontWeight: 600,
                padding: '2px 8px',
                borderRadius: 4,
                background:
                  progress.status === 'INDEXING' || progress.status === 'SCANNING'
                    ? 'var(--warning-soft)'
                    : progress.status === 'COMPLETED'
                    ? 'var(--success-soft)'
                    : 'var(--bg-elevated)',
                color:
                  progress.status === 'INDEXING' || progress.status === 'SCANNING'
                    ? 'var(--warning)'
                    : progress.status === 'COMPLETED'
                    ? 'var(--success)'
                    : 'var(--text-secondary)'
              }}
            >
              {progress.status}
            </span>
            <span style={{ color: 'var(--text-secondary)' }}>
              {progress.status === 'INDEXING'
                ? `Indexing ${progress.currentFile || ''} (${progress.indexedFiles}/${progress.totalFiles})`
                : progress.status === 'COMPLETED'
                ? `${progress.indexedFiles} files indexed in ${progress.durationMs} ms`
                : 'Index is up to date'}
            </span>
          </div>

          <button className="btn btn-secondary btn-sm" onClick={handleRebuildAll}>
            <RefreshCw size={12} />
            <span>Rebuild All</span>
          </button>
        </div>
      )}

      {/* Register Folder Card */}
      <div className="card">
        <h3 className="card-title">Register a Directory</h3>
        <p className="card-desc">
          Add a local folder containing text files, Markdown, Java code, or PDFs to make them searchable.
        </p>

        <form onSubmit={handleAddFolder} className="form-row">
          <input
            type="text"
            className="input-text"
            placeholder="Absolute folder path (e.g. C:/Users/joy/Desktop/findex/documents)"
            value={pathInput}
            onChange={e => setPathInput(e.target.value)}
          />
          <input
            type="text"
            className="input-text"
            style={{ maxWidth: 180 }}
            placeholder="Folder Name (optional)"
            value={nameInput}
            onChange={e => setNameInput(e.target.value)}
          />
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || !pathInput.trim()}
          >
            <Plus size={14} />
            <span>{loading ? 'Adding...' : 'Register'}</span>
          </button>
        </form>

        <div style={{ marginTop: 10 }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => {
              setPathInput('C:/Users/joy/Desktop/findex/documents');
              setNameInput('Sample Documents');
            }}
          >
            <Sparkles size={11} color="var(--accent)" />
            <span>Auto-fill Sample Documents</span>
          </button>
        </div>
      </div>

      {/* Registered Folders List */}
      <div className="card">
        <h3 className="card-title">Registered Directories ({folders.length})</h3>
        <p className="card-desc">All directories actively maintained in the inverted index.</p>

        {folders.length === 0 ? (
          <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>No folders registered yet.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {folders.map(f => (
              <div
                key={f.id}
                style={{
                  background: 'var(--bg-app)',
                  border: '1px solid var(--border)',
                  borderRadius: 8,
                  padding: '12px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 12
                }}
              >
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <FolderOpen size={16} color="var(--accent)" />
                    <strong style={{ fontSize: 14 }}>{f.name}</strong>
                    <span className="file-badge">{f.fileCount} docs</span>
                  </div>
                  <div className="result-path" style={{ marginTop: 4, marginBottom: 0 }}>
                    {f.path}
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => handleReindex(f.id)}
                    title="Reindex this folder"
                  >
                    <RefreshCw size={12} />
                    <span>Reindex</span>
                  </button>
                  <button
                    className="btn btn-danger btn-sm"
                    onClick={() => handleDelete(f.id)}
                    title="Remove folder"
                  >
                    <Trash2 size={12} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

/* ==================== BENCHMARK & STATS TAB ==================== */
function BenchmarkTab({ toast }: { toast: (m: string, t?: 'success' | 'error') => void }) {
  const [stats, setStats] = useState<IndexStats | null>(null);
  const [benchQuery, setBenchQuery] = useState('data OR java');
  const [benchResult, setBenchResult] = useState<BenchmarkResult | null>(null);
  const [benchLoading, setBenchLoading] = useState(false);
  const [history, setHistory] = useState<HistoryEntry[]>([]);

  const fetchStats = useCallback(async () => {
    try {
      const s = await api.getStats();
      setStats(s);
    } catch {
      // ignore
    }
  }, []);

  const fetchHistory = useCallback(async () => {
    try {
      const h = await api.getHistory(15);
      setHistory(h);
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    fetchStats();
    fetchHistory();
  }, [fetchStats, fetchHistory]);

  const runBenchmarkTest = async () => {
    if (!benchQuery.trim()) return;
    setBenchLoading(true);
    try {
      const res = await api.runBenchmark(benchQuery.trim());
      setBenchResult(res);
      toast('Benchmark completed');
      fetchHistory();
    } catch (e: unknown) {
      toast((e as Error).message, 'error');
    } finally {
      setBenchLoading(false);
    }
  };

  const handleClearHistory = async () => {
    try {
      await api.clearHistory();
      setHistory([]);
      toast('Search history cleared');
    } catch (e: unknown) {
      toast((e as Error).message, 'error');
    }
  };

  return (
    <div>
      {/* Engine Metrics Grid */}
      <div className="stats-grid">
        <div className="stat-item">
          <div className="stat-label">Total Documents</div>
          <div className="stat-value">{stats ? stats.totalDocuments : '...'}</div>
        </div>
        <div className="stat-item">
          <div className="stat-label">Unique Terms</div>
          <div className="stat-value">{stats ? stats.uniqueTerms.toLocaleString() : '...'}</div>
        </div>
        <div className="stat-item">
          <div className="stat-label">Total Postings</div>
          <div className="stat-value">{stats ? stats.totalPostings.toLocaleString() : '...'}</div>
        </div>
        <div className="stat-item">
          <div className="stat-label">Avg Doc Length</div>
          <div className="stat-value">{stats ? stats.avgDocLength.toFixed(1) : '...'}</div>
        </div>
        <div className="stat-item">
          <div className="stat-label">JVM Memory</div>
          <div className="stat-value">{stats ? `${stats.memoryUsageMb} MB` : '...'}</div>
        </div>
      </div>

      {/* Empirical Benchmark Card */}
      <div className="card">
        <h3 className="card-title">Empirical Performance Benchmark</h3>
        <p className="card-desc">
          Executes identical queries against both the Inverted Index Engine and the Linear Scan
          baseline engine to measure algorithmic speedup.
        </p>

        <div className="form-row">
          <input
            type="text"
            className="input-text"
            value={benchQuery}
            onChange={e => setBenchQuery(e.target.value)}
            placeholder="Query string (e.g. data OR java)"
          />
          <button
            className="btn btn-primary"
            onClick={runBenchmarkTest}
            disabled={benchLoading || !benchQuery.trim()}
          >
            <Zap size={14} />
            <span>{benchLoading ? 'Benchmarking...' : 'Run Benchmark'}</span>
          </button>
        </div>

        {benchResult && (
          <div className="benchmark-box">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span className="speedup-badge">
                Inverted Index Speedup: {benchResult.speedupFactor}x Faster
              </span>
              <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                Matched {benchResult.matchingDocuments} of {benchResult.totalDocumentsScanned} documents
              </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 8 }}>
              <div
                style={{
                  background: 'var(--bg-elevated)',
                  padding: '12px 16px',
                  borderRadius: 6,
                  border: '1px solid var(--border)'
                }}
              >
                <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Inverted Index Engine
                </div>
                <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--accent)', marginTop: 4 }}>
                  {benchResult.invertedIndexMs} ms
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 4 }}>
                  Direct hash-postings lookup with AST pruning
                </div>
              </div>

              <div
                style={{
                  background: 'var(--bg-elevated)',
                  padding: '12px 16px',
                  borderRadius: 6,
                  border: '1px solid var(--border)'
                }}
              >
                <div style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Linear Scan Engine
                </div>
                <div style={{ fontSize: 22, fontWeight: 700, color: '#f59e0b', marginTop: 4 }}>
                  {benchResult.linearScanMs} ms
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 4 }}>
                  Sequential string scanning over entire document store
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Query History Log */}
      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <div>
            <h3 className="card-title">Search Query History</h3>
            <p className="card-desc" style={{ marginBottom: 0 }}>
              Recent queries and recorded response latencies stored in local SQLite database.
            </p>
          </div>
          {history.length > 0 && (
            <button className="btn btn-secondary btn-sm" onClick={handleClearHistory}>
              <Trash2 size={12} />
              <span>Clear History</span>
            </button>
          )}
        </div>

        {history.length === 0 ? (
          <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>No search history recorded yet.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {history.map(h => (
              <div
                key={h.id}
                style={{
                  background: 'var(--bg-app)',
                  padding: '8px 12px',
                  borderRadius: 6,
                  border: '1px solid var(--border)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  fontSize: 12
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Clock size={12} color="var(--text-muted)" />
                  <span style={{ fontFamily: 'JetBrains Mono, monospace', color: 'var(--text-primary)' }}>
                    {h.query}
                  </span>
                  <span className="file-badge">{h.mode.toUpperCase()}</span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: 12, color: 'var(--text-muted)' }}>
                  <span>{h.resultCount} result{h.resultCount === 1 ? '' : 's'}</span>
                  <span style={{ fontFamily: 'JetBrains Mono, monospace' }}>{h.latencyMs} ms</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
