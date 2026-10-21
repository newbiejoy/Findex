const API_BASE = 'http://localhost:8080/api';

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${url}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(err.error || err.message || 'Request failed');
  }
  return res.json();
}

export interface SearchResult {
  document: {
    id: number;
    folderId: string;
    filePath: string;
    fileName: string;
    extension: string;
    fileSize: number;
    lastModified: number;
    termCount: number;
    previewText: string;
  };
  score: number;
  matchedTerms: string[];
  snippet: string;
  matchCount: number;
  scoreExplanation: string;
}

export interface SearchResponse {
  query: string;
  mode: string;
  totalResults: number;
  timeTakenMs: number;
  results: SearchResult[];
}

export interface FolderConfig {
  id: string;
  path: string;
  name: string;
  fileCount: number;
  addedAt: number;
  lastIndexedAt: number;
}

export interface IndexStats {
  totalDocuments: number;
  uniqueTerms: number;
  totalPostings: number;
  avgDocLength: number;
  memoryUsageMb: number;
  lastIndexedAt: number;
}

export interface IndexingProgress {
  status: 'IDLE' | 'SCANNING' | 'INDEXING' | 'COMPLETED' | 'FAILED';
  totalFiles: number;
  indexedFiles: number;
  currentFile: string;
  percent: number;
  errorMessage: string | null;
  durationMs: number;
}

export interface TermDetails {
  term: string;
  documentFrequency: number;
  totalOccurrences: number;
  samplePostings: { docId: number; fileName: string; termFrequency: number; positions: number[] }[];
}

export interface BenchmarkResult {
  query: string;
  linearScanMs: number;
  invertedIndexMs: number;
  speedupFactor: number;
  matchingDocuments: number;
  totalDocumentsScanned: number;
  topResults: SearchResult[];
}

export interface HistoryEntry {
  id: number;
  query: string;
  mode: string;
  resultCount: number;
  latencyMs: number;
  searchedAt: number;
}

export const searchDocuments = (q: string, mode = 'bm25', type?: string, folderId?: string, limit = 30) => {
  const params = new URLSearchParams({ q, mode, limit: String(limit) });
  if (type) params.set('type', type);
  if (folderId) params.set('folderId', folderId);
  return request<SearchResponse>(`/search?${params}`);
};

export const getSuggestions = (q: string, limit = 8) =>
  request<string[]>(`/suggestions?q=${encodeURIComponent(q)}&limit=${limit}`);

export const getFolders = () => request<FolderConfig[]>('/folders');
export const addFolder = (path: string, name?: string) =>
  request<FolderConfig>('/folders', { method: 'POST', body: JSON.stringify({ path, name }) });
export const deleteFolder = (id: string) =>
  request<{ status: string }>(`/folders/${id}`, { method: 'DELETE' });
export const reindexFolder = (id: string) =>
  request<{ status: string }>(`/folders/${id}/reindex`, { method: 'POST' });

export const rebuildIndex = () =>
  request<{ status: string }>('/index/rebuild', { method: 'POST' });
export const getIndexStatus = () => request<IndexingProgress>('/index/status');
export const getStats = () => request<IndexStats>('/stats');
export const getTerms = (prefix = '', offset = 0, limit = 50) =>
  request<TermDetails[]>(`/terms?prefix=${encodeURIComponent(prefix)}&offset=${offset}&limit=${limit}`);
export const getTermDetails = (term: string) => request<TermDetails>(`/terms/${encodeURIComponent(term)}`);

export const openDocument = (id: number) =>
  request<{ status: string; path: string }>(`/documents/${id}/open`, { method: 'POST' });

export const runBenchmark = (q = 'java') =>
  request<BenchmarkResult>(`/benchmark?q=${encodeURIComponent(q)}`, { method: 'POST' });
export const getEvaluation = (mode = 'bm25', k = 5) =>
  request<Record<string, unknown>>(`/evaluation?mode=${mode}&k=${k}`);

export const getHistory = (limit = 20) => request<HistoryEntry[]>(`/history?limit=${limit}`);
export const clearHistory = () => request<{ status: string }>('/history', { method: 'DELETE' });

export const checkHealth = () => request<{ status: string; version: string }>('/health');
