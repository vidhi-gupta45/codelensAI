import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { repoApi } from '../api/client';
import type { AnalysisJob } from '../types';
import { Navbar } from '../components/Navbar';
import { Loader2, CheckCircle2, AlertCircle, Sparkles, GitBranch, Cpu, Database, FileText, ArrowRight } from 'lucide-react';

const PIPELINE_STAGES = [
  { id: 'QUEUED', label: 'Analysis Job Queued', icon: GitBranch },
  { id: 'CLONING', label: 'Cloning Repository Code', icon: GitBranch },
  { id: 'PARSING', label: 'Parsing Files & AST Chunks', icon: Cpu },
  { id: 'EMBEDDING', label: 'Generating Vector Embeddings', icon: Database },
  { id: 'SUMMARIZING', label: 'Synthesizing Architecture Overview', icon: FileText },
  { id: 'COMPLETED', label: 'Index & RAG System Ready', icon: CheckCircle2 },
];

export const JobProgressPage: React.FC = () => {
  const { jobId } = useParams<{ jobId: string }>();
  const [job, setJob] = useState<AnalysisJob | null>(null);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!jobId) return;

    fetchJobStatus();
    const interval = setInterval(() => {
      fetchJobStatus();
    }, 2000);

    return () => clearInterval(interval);
  }, [jobId]);

  const fetchJobStatus = async () => {
    try {
      if (!jobId) return;
      const data = await repoApi.getJobStatus(jobId);
      setJob(data);

      if (data.status === 'COMPLETED' && data.repoAnalysis?.id) {
        setTimeout(() => {
          navigate(`/repos/${data.repoAnalysis!.id}`);
        }, 1500);
      } else if (data.status === 'FAILED') {
        setError(data.errorMessage || 'Repository analysis failed during pipeline processing.');
      }
    } catch (err: any) {
      console.error('Failed to fetch job status:', err);
    }
  };

  const progress = job ? Math.min(Math.max(job.progress || 0, 5), 100) : 0;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-3xl w-full mx-auto px-4 py-12 flex flex-col justify-center">
        
        <div className="rounded-3xl border border-slate-800 bg-slate-900/90 p-8 shadow-2xl backdrop-blur-xl relative overflow-hidden">
          <div className="absolute top-0 right-0 w-80 h-80 bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />

          {/* Header */}
          <div className="text-center mb-8">
            <div className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-sky-500/10 text-sky-400 border border-sky-500/20 mb-3 shadow-lg shadow-sky-500/10">
              <Sparkles className="h-6 w-6 animate-pulse text-sky-400" />
            </div>
            <h1 className="text-2xl font-bold text-white tracking-tight">Analyzing Repository</h1>
            <p className="text-xs text-slate-400 mt-1">
              CodeLens AI is indexing the codebase, mapping architectural layers, and storing vector embeddings.
            </p>
          </div>

          {/* Progress Bar */}
          <div className="space-y-2 mb-8">
            <div className="flex justify-between text-xs font-semibold">
              <span className="text-slate-300">{job?.currentStage || 'Initializing pipeline...'}</span>
              <span className="text-sky-400 font-mono">{progress}%</span>
            </div>
            <div className="h-3 w-full rounded-full bg-slate-950 p-0.5 border border-slate-800">
              <div
                className="h-full rounded-full bg-gradient-to-r from-sky-500 via-indigo-500 to-emerald-400 transition-all duration-500 shadow-md shadow-sky-500/30"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>

          {/* Failed State */}
          {error ? (
            <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-4 text-xs text-rose-300 flex items-start space-x-3 mb-6">
              <AlertCircle className="h-5 w-5 text-rose-400 shrink-0 mt-0.5" />
              <div>
                <h4 className="font-bold text-rose-200 text-sm mb-1">Analysis Failed</h4>
                <p>{error}</p>
                <button
                  onClick={() => navigate('/dashboard')}
                  className="mt-3 rounded-lg bg-rose-500/20 px-3 py-1.5 font-semibold text-rose-200 hover:bg-rose-500/30 transition-colors"
                >
                  Return to Dashboard
                </button>
              </div>
            </div>
          ) : (
            /* Pipeline Stages Stepper */
            <div className="space-y-3">
              {PIPELINE_STAGES.map((stage, idx) => {
                const Icon = stage.icon;
                const isCurrent = job?.status === stage.id;
                const isPassed = getStageOrder(job?.status) > idx;

                return (
                  <div
                    key={stage.id}
                    className={`flex items-center justify-between p-3.5 rounded-xl border transition-all ${
                      isCurrent
                        ? 'border-sky-500/40 bg-sky-500/10 text-sky-300 shadow-md shadow-sky-500/10'
                        : isPassed
                        ? 'border-slate-800 bg-slate-950/40 text-slate-400'
                        : 'border-slate-850/40 bg-slate-950/20 text-slate-600'
                    }`}
                  >
                    <div className="flex items-center space-x-3 text-xs font-medium">
                      {isPassed ? (
                        <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
                      ) : isCurrent ? (
                        <Loader2 className="h-4 w-4 animate-spin text-sky-400 shrink-0" />
                      ) : (
                        <Icon className="h-4 w-4 text-slate-600 shrink-0" />
                      )}
                      <span>{stage.label}</span>
                    </div>

                    <span className="text-[10px] font-mono text-slate-500">
                      {isPassed ? 'Completed' : isCurrent ? 'In Progress' : 'Pending'}
                    </span>
                  </div>
                );
              })}
            </div>
          )}

          {job?.status === 'COMPLETED' && (
            <div className="mt-6 text-center">
              <button
                onClick={() => job?.repoAnalysis?.id && navigate(`/repos/${job.repoAnalysis.id}`)}
                className="inline-flex items-center space-x-2 rounded-xl bg-emerald-500 px-5 py-2.5 text-xs font-bold text-white shadow-lg shadow-emerald-500/20 hover:bg-emerald-400 transition-all"
              >
                <span>Analysis Complete! Entering Workspace</span>
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          )}

        </div>

      </main>
    </div>
  );
};

function getStageOrder(status?: string): number {
  switch (status) {
    case 'QUEUED': return 0;
    case 'CLONING': return 1;
    case 'PARSING': return 2;
    case 'EMBEDDING': return 3;
    case 'SUMMARIZING': return 4;
    case 'COMPLETED': return 5;
    default: return 0;
  }
}
