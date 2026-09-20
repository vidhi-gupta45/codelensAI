import React, { useState } from 'react';
import type { Citation } from '../types';
import { FileCode, Copy, Check, Hash, Sparkles } from 'lucide-react';

interface CitationCardProps {
  citation: Citation;
  index: number;
}

export const CitationCard: React.FC<CitationCardProps> = ({ citation, index }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(citation.snippet);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const relevancePercent = citation.relevanceScore
    ? Math.round(citation.relevanceScore * 100)
    : null;

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-3 text-xs shadow-md transition-all hover:border-slate-700/80">
      
      {/* Header */}
      <div className="flex items-center justify-between pb-2 border-b border-slate-800/80">
        <div className="flex items-center space-x-2 truncate">
          <div className="flex h-6 w-6 items-center justify-center rounded-lg bg-sky-500/10 text-sky-400 font-bold text-[10px]">
            #{index + 1}
          </div>
          <FileCode className="h-4 w-4 text-sky-400 shrink-0" />
          <span className="font-mono text-slate-200 font-semibold truncate" title={citation.filePath}>
            {citation.filePath}
          </span>
        </div>

        <div className="flex items-center space-x-2 shrink-0">
          <span className="flex items-center gap-1 rounded bg-slate-800 px-2 py-0.5 font-mono text-[10px] text-slate-300 border border-slate-700/50">
            <Hash className="h-3 w-3 text-slate-400" />
            L{citation.startLine} - L{citation.endLine}
          </span>

          {relevancePercent !== null && (
            <span className="flex items-center gap-1 rounded bg-emerald-500/10 px-2 py-0.5 font-semibold text-[10px] text-emerald-400 border border-emerald-500/20">
              <Sparkles className="h-2.5 w-2.5" />
              {relevancePercent}% Match
            </span>
          )}

          <button
            onClick={handleCopy}
            title="Copy snippet"
            className="rounded p-1 text-slate-400 hover:bg-slate-800 hover:text-white transition-colors"
          >
            {copied ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
          </button>
        </div>
      </div>

      {/* Snippet Code Block */}
      <div className="mt-2 rounded-lg bg-slate-900/90 p-2.5 font-mono text-[11px] text-slate-300 overflow-x-auto border border-slate-850 max-h-48 leading-relaxed">
        <pre>{citation.snippet}</pre>
      </div>
    </div>
  );
};
