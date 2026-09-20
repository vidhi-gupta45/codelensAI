import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { repoApi } from '../api/client';
import type { RepoAnalysis } from '../types';
import { Navbar } from '../components/Navbar';
import { AnalyzeModal } from '../components/AnalyzeModal';
import { 
  GitFork, 
  Sparkles, 
  Search, 
  Clock, 
  FolderGit2, 
  CheckCircle2, 
  Loader2, 
  AlertCircle, 
  ChevronRight,
  Plus
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const [repos, setRepos] = useState<RepoAnalysis[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    fetchRepos();
  }, []);

  const fetchRepos = async () => {
    try {
      setLoading(true);
      const data = await repoApi.getUserRepos();
      setRepos(data);
    } catch (err) {
      console.error('Failed to fetch user repositories:', err);
    } finally {
      setLoading(false);
    }
  };

  const filteredRepos = repos.filter(
    (repo) =>
      repo.repoName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      repo.owner?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      repo.repoUrl?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar onOpenAnalyzeModal={() => setIsModalOpen(true)} />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        
        {/* Banner Section */}
        <div className="relative rounded-3xl border border-slate-800 bg-gradient-to-r from-slate-900 via-indigo-950/40 to-slate-900 p-8 shadow-2xl overflow-hidden">
          <div className="absolute -top-12 -right-12 w-64 h-64 bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />
          
          <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div>
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-sky-500/10 border border-sky-500/20 text-xs font-semibold text-sky-400 mb-3">
                <Sparkles className="h-3.5 w-3.5" /> AI-Powered Code Base Intelligence
              </span>
              <h1 className="text-3xl font-extrabold text-white tracking-tight">Repository Hub</h1>
              <p className="text-sm text-slate-400 mt-1 max-w-xl">
                Explore indexed GitHub repositories, inspect auto-generated architecture layers, and query code using AI vector search.
              </p>
            </div>

            <button
              onClick={() => setIsModalOpen(true)}
              className="inline-flex items-center justify-center space-x-2 rounded-2xl bg-gradient-to-r from-sky-500 to-indigo-600 px-6 py-3.5 text-sm font-semibold text-white shadow-xl shadow-sky-500/25 hover:from-sky-400 hover:to-indigo-500 active:scale-95 transition-all duration-200"
            >
              <Plus className="h-5 w-5" />
              <span>Analyze New Repository</span>
            </button>
          </div>

          {/* Quick Metrics Bar */}
          <div className="mt-8 grid grid-cols-2 md:grid-cols-4 gap-4 border-t border-slate-800/80 pt-6">
            <div>
              <span className="text-xs text-slate-400">Total Analyzed Repos</span>
              <p className="text-xl font-bold text-white mt-0.5">{repos.length}</p>
            </div>
            <div>
              <span className="text-xs text-slate-400">Completed Indexes</span>
              <p className="text-xl font-bold text-emerald-400 mt-0.5">
                {repos.filter((r) => r.status === 'COMPLETED').length}
              </p>
            </div>
            <div>
              <span className="text-xs text-slate-400">Processing Jobs</span>
              <p className="text-xl font-bold text-amber-400 mt-0.5">
                {repos.filter((r) => r.status === 'PROCESSING' || r.status === 'PENDING').length}
              </p>
            </div>
            <div>
              <span className="text-xs text-slate-400">RAG Vector Support</span>
              <p className="text-xl font-bold text-sky-400 mt-0.5">Active</p>
            </div>
          </div>
        </div>

        {/* Search & Filter Bar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="relative w-full sm:w-96">
            <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-500" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search repositories by name or owner..."
              className="w-full rounded-xl border border-slate-800 bg-slate-900 pl-10 pr-4 py-2.5 text-xs text-slate-200 placeholder-slate-500 focus:border-sky-500 focus:outline-none focus:ring-1 focus:ring-sky-500 transition-all"
            />
          </div>

          <span className="text-xs text-slate-400 font-medium">
            Showing {filteredRepos.length} of {repos.length} repositories
          </span>
        </div>

        {/* Repository Grid */}
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 text-slate-500">
            <Loader2 className="h-8 w-8 animate-spin text-sky-400 mb-3" />
            <p className="text-sm">Loading your repositories...</p>
          </div>
        ) : filteredRepos.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-800 bg-slate-900/40 p-12 text-center">
            <FolderGit2 className="h-12 w-12 text-slate-600 mx-auto mb-3" />
            <h3 className="text-lg font-bold text-white mb-1">No Repositories Found</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto mb-6">
              {searchQuery ? 'No repositories match your search filter.' : 'You haven\'t analyzed any GitHub repositories yet.'}
            </p>
            <button
              onClick={() => setIsModalOpen(true)}
              className="inline-flex items-center space-x-2 rounded-xl bg-sky-500/10 border border-sky-500/20 px-4 py-2.5 text-xs font-semibold text-sky-400 hover:bg-sky-500/20 transition-all"
            >
              <Plus className="h-4 w-4" />
              <span>Analyze First Repository</span>
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredRepos.map((repo) => (
              <div
                key={repo.id}
                onClick={() => navigate(`/repos/${repo.id}`)}
                className="group relative rounded-2xl border border-slate-800 bg-slate-900/80 p-6 shadow-lg hover:border-sky-500/40 hover:bg-slate-850 hover:shadow-sky-950/20 cursor-pointer transition-all duration-300 flex flex-col justify-between"
              >
                <div>
                  {/* Top Row: Owner & Status Badge */}
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-xs font-mono text-slate-400">{repo.owner}</span>
                    <StatusBadge status={repo.status} />
                  </div>

                  {/* Repo Title */}
                  <h3 className="text-lg font-bold text-white group-hover:text-sky-400 transition-colors flex items-center gap-2">
                    <GitFork className="h-4 w-4 text-sky-400 shrink-0" />
                    <span className="truncate">{repo.repoName}</span>
                  </h3>

                  {/* Summary / Description snippet */}
                  <p className="text-xs text-slate-400 line-clamp-2 mt-2 leading-relaxed">
                    {repo.summary || 'AI indexing completed. Click to explore architecture and Q&A.'}
                  </p>
                </div>

                {/* Footer Info */}
                <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-500">
                  <span className="flex items-center gap-1">
                    <Clock className="h-3 w-3" />
                    {repo.createdAt ? new Date(repo.createdAt).toLocaleDateString() : 'Recently'}
                  </span>

                  <div className="flex items-center space-x-1 text-sky-400 font-semibold group-hover:translate-x-1 transition-transform">
                    <span>Open Workspace</span>
                    <ChevronRight className="h-3.5 w-3.5" />
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

      </main>

      <AnalyzeModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} />
    </div>
  );
};

const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  switch (status) {
    case 'COMPLETED':
      return (
        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-0.5 text-[10px] font-semibold text-emerald-400">
          <CheckCircle2 className="h-3 w-3" /> Ready
        </span>
      );
    case 'PROCESSING':
    case 'PENDING':
      return (
        <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 border border-amber-500/20 px-2.5 py-0.5 text-[10px] font-semibold text-amber-400">
          <Loader2 className="h-3 w-3 animate-spin" /> Processing
        </span>
      );
    case 'FAILED':
      return (
        <span className="inline-flex items-center gap-1 rounded-full bg-rose-500/10 border border-rose-500/20 px-2.5 py-0.5 text-[10px] font-semibold text-rose-400">
          <AlertCircle className="h-3 w-3" /> Failed
        </span>
      );
    default:
      return (
        <span className="inline-flex items-center gap-1 rounded-full bg-slate-800 px-2.5 py-0.5 text-[10px] font-semibold text-slate-400">
          {status}
        </span>
      );
  }
};
