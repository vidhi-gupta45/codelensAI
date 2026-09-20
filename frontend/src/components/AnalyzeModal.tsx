import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { repoApi } from '../api/client';
import { X, GitFork, Sparkles, Loader2, Link as LinkIcon, AlertCircle } from 'lucide-react';

interface AnalyzeModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const PRESET_REPOS = [
  { name: 'ExpressJS', url: 'https://github.com/expressjs/express' },
  { name: 'FastAPI', url: 'https://github.com/fastapi/fastapi' },
  { name: 'Spring Petclinic', url: 'https://github.com/spring-projects/spring-petclinic' },
];

export const AnalyzeModal: React.FC<AnalyzeModalProps> = ({ isOpen, onClose }) => {
  const [repoUrl, setRepoUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!repoUrl.trim()) {
      setError('Please enter a GitHub repository URL');
      return;
    }

    if (!repoUrl.includes('github.com')) {
      setError('URL must be a public GitHub repository (e.g. https://github.com/owner/repo)');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await repoApi.analyzeRepo(repoUrl.trim());
      onClose();
      navigate(`/jobs/${response.jobId}`);
    } catch (err: any) {
      console.error('Failed to submit repository:', err);
      setError(err.response?.data?.message || 'Failed to start repository analysis. Please check URL and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-2xl shadow-sky-950/40">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800/80 pb-4">
          <div className="flex items-center space-x-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-sky-500/10 text-sky-400 border border-sky-500/20">
              <GitFork className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">Analyze Repository</h3>
              <p className="text-xs text-slate-400">Index public codebase for architecture extraction & RAG</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="mt-4 flex items-start space-x-2.5 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3.5 text-xs text-rose-300">
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              GitHub Repository URL
            </label>
            <div className="relative">
              <LinkIcon className="absolute left-3.5 top-3 h-4 w-4 text-slate-500" />
              <input
                type="url"
                value={repoUrl}
                onChange={(e) => setRepoUrl(e.target.value)}
                placeholder="https://github.com/owner/repository"
                className="w-full rounded-xl border border-slate-800 bg-slate-950/80 pl-10 pr-4 py-2.5 text-sm text-slate-200 placeholder-slate-500 focus:border-sky-500 focus:outline-none focus:ring-1 focus:ring-sky-500 transition-all"
                disabled={loading}
              />
            </div>
          </div>

          {/* Quick Presets */}
          <div>
            <span className="block text-[11px] font-medium text-slate-400 mb-2">Or test with popular repositories:</span>
            <div className="flex flex-wrap gap-2">
              {PRESET_REPOS.map((preset) => (
                <button
                  key={preset.name}
                  type="button"
                  onClick={() => setRepoUrl(preset.url)}
                  className="rounded-lg border border-slate-800 bg-slate-950/60 px-2.5 py-1 text-xs text-slate-300 hover:border-sky-500/40 hover:bg-slate-850 hover:text-sky-400 transition-all"
                >
                  + {preset.name}
                </button>
              ))}
            </div>
          </div>

          {/* Submit Action */}
          <div className="flex items-center justify-end space-x-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl px-4 py-2 text-xs font-medium text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition-colors"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex items-center space-x-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 px-5 py-2 text-xs font-semibold text-white shadow-lg shadow-sky-500/20 hover:from-sky-400 hover:to-indigo-500 disabled:opacity-50 transition-all"
            >
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Starting Analysis...</span>
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4" />
                  <span>Start Analysis</span>
                </>
              )}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
};
