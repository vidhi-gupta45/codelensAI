import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { repoApi } from '../api/client';
import type { RepoAnalysis, ArchitectureResponse } from '../types';
import { Navbar } from '../components/Navbar';
import { ArchitectureView } from '../components/ArchitectureView';
import { ChatView } from '../components/ChatView';
import { 
  GitFork, 
  Layers, 
  MessageSquareCode, 
  ExternalLink, 
  Loader2, 
  AlertCircle, 
  ArrowLeft
} from 'lucide-react';

export const RepoDetailsPage: React.FC = () => {
  const { repoId } = useParams<{ repoId: string }>();
  const [activeTab, setActiveTab] = useState<'architecture' | 'qa'>('architecture');
  const [repoDetails, setRepoDetails] = useState<RepoAnalysis | null>(null);
  const [architecture, setArchitecture] = useState<ArchitectureResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!repoId) return;
    loadRepoData();
  }, [repoId]);

  const loadRepoData = async () => {
    try {
      setLoading(true);
      if (!repoId) return;

      const [detailsData, archData] = await Promise.all([
        repoApi.getRepoDetails(repoId),
        repoApi.getArchitecture(repoId),
      ]);

      setRepoDetails(detailsData);
      setArchitecture(archData);
    } catch (err: any) {
      console.error('Failed to load repository workspace:', err);
      setError(err.response?.data?.message || 'Failed to load repository workspace details.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        
        {/* Back Link & Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-4">
          <div className="flex items-center space-x-3">
            <Link
              to="/dashboard"
              className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white hover:bg-slate-850 transition-colors"
            >
              <ArrowLeft className="h-4 w-4" />
            </Link>

            <div>
              <div className="flex items-center space-x-2 text-xs font-mono text-slate-400">
                <span>{repoDetails?.owner || 'Repository'}</span>
                <span>/</span>
              </div>
              <h1 className="text-xl font-bold text-white flex items-center gap-2">
                <GitFork className="h-5 w-5 text-sky-400" />
                <span>{repoDetails?.repoName || 'Workspace'}</span>
                {repoDetails?.repoUrl && (
                  <a
                    href={repoDetails.repoUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-slate-500 hover:text-sky-400 transition-colors"
                  >
                    <ExternalLink className="h-4 w-4" />
                  </a>
                )}
              </h1>
            </div>
          </div>

          {/* Workspace Tabs */}
          <div className="flex rounded-xl bg-slate-900/90 p-1 border border-slate-800">
            <button
              onClick={() => setActiveTab('architecture')}
              className={`flex items-center space-x-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all ${
                activeTab === 'architecture'
                  ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20 shadow-md'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Layers className="h-4 w-4" />
              <span>Architecture & Files</span>
            </button>

            <button
              onClick={() => setActiveTab('qa')}
              className={`flex items-center space-x-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all ${
                activeTab === 'qa'
                  ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20 shadow-md'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <MessageSquareCode className="h-4 w-4" />
              <span>AI Code Q&A</span>
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            </button>
          </div>
        </div>

        {/* Content Body */}
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 text-slate-500">
            <Loader2 className="h-8 w-8 animate-spin text-sky-400 mb-3" />
            <p className="text-sm">Loading codebase architecture & RAG workspace...</p>
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-6 text-center text-xs text-rose-300">
            <AlertCircle className="h-8 w-8 text-rose-400 mx-auto mb-2" />
            <h3 className="text-base font-bold text-rose-200 mb-1">Error Loading Repository</h3>
            <p className="mb-4">{error}</p>
            <Link
              to="/dashboard"
              className="inline-flex items-center space-x-2 rounded-xl bg-slate-800 px-4 py-2 text-xs font-semibold text-slate-200 hover:bg-slate-750"
            >
              Back to Dashboard
            </Link>
          </div>
        ) : (
          <div>
            {activeTab === 'architecture' && architecture && (
              <ArchitectureView architecture={architecture} />
            )}

            {activeTab === 'qa' && repoId && (
              <ChatView repoId={repoId} />
            )}
          </div>
        )}

      </main>
    </div>
  );
};
