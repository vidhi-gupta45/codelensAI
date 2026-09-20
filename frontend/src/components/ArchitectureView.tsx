import React from 'react';
import type { ArchitectureResponse } from '../types';
import { FileTree } from './FileTree';
import { Cpu, Layers, ExternalLink, Code2, PlayCircle, Server, Database, Layout } from 'lucide-react';

interface ArchitectureViewProps {
  architecture: ArchitectureResponse;
}

const LAYER_ICONS: Record<string, React.ReactNode> = {
  'API Layer': <Server className="h-4 w-4 text-sky-400" />,
  'Business Logic Layer': <Cpu className="h-4 w-4 text-indigo-400" />,
  'Data Access Layer': <Database className="h-4 w-4 text-emerald-400" />,
  'Data Model Layer': <Layers className="h-4 w-4 text-purple-400" />,
  'UI / Frontend Layer': <Layout className="h-4 w-4 text-amber-400" />,
};

export const ArchitectureView: React.FC<ArchitectureViewProps> = ({ architecture }) => {
  return (
    <div className="space-y-6">
      
      {/* High-level Summary Card */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-sky-500/5 rounded-full blur-3xl pointer-events-none" />
        
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800/80 pb-4">
          <div>
            <div className="flex items-center space-x-2 text-xs text-sky-400 font-semibold uppercase tracking-wider mb-1">
              <span>Architecture Overview</span>
            </div>
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              {architecture.owner} / {architecture.repoName}
              <a
                href={architecture.repoUrl}
                target="_blank"
                rel="noreferrer"
                className="text-slate-400 hover:text-sky-400 transition-colors"
                title="View on GitHub"
              >
                <ExternalLink className="h-4 w-4" />
              </a>
            </h2>
          </div>

          {/* Tech Stack Badges */}
          <div className="flex flex-wrap gap-1.5 items-center">
            <span className="text-xs text-slate-400 font-medium mr-1 flex items-center gap-1">
              <Code2 className="h-3.5 w-3.5 text-sky-400" /> Tech Stack:
            </span>
            {architecture.techStack && architecture.techStack.length > 0 ? (
              architecture.techStack.map((tech) => (
                <span
                  key={tech}
                  className="rounded-lg bg-sky-500/10 border border-sky-500/20 px-2.5 py-1 text-xs font-semibold text-sky-300"
                >
                  {tech}
                </span>
              ))
            ) : (
              <span className="text-xs text-slate-500">General Codebase</span>
            )}
          </div>
        </div>

        {/* AI Executive Summary */}
        <div className="mt-4">
          <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">Executive Summary</h4>
          <p className="text-sm text-slate-300 leading-relaxed bg-slate-950/60 p-4 rounded-xl border border-slate-800">
            {architecture.summary || 'Architecture synthesis completed. Detailed breakdown below.'}
          </p>
        </div>
      </div>

      {/* Grid: Architecture Layers & Entry Points */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Architectural Layers Breakdown */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl">
          <h3 className="text-sm font-bold text-white flex items-center gap-2 mb-4">
            <Layers className="h-4 w-4 text-indigo-400" /> Architectural Layers & Packages
          </h3>

          <div className="space-y-3">
            {Object.entries(architecture.layers || {}).map(([layerName, files]) => {
              if (!files || files.length === 0) return null;
              const icon = LAYER_ICONS[layerName] || <Layers className="h-4 w-4 text-slate-400" />;

              return (
                <div key={layerName} className="rounded-xl border border-slate-800/80 bg-slate-950/50 p-3.5">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-2 font-semibold text-xs text-slate-200">
                      {icon}
                      <span>{layerName}</span>
                    </div>
                    <span className="rounded-full bg-slate-800 px-2 py-0.5 text-[10px] font-mono text-slate-400">
                      {files.length} files
                    </span>
                  </div>

                  <div className="space-y-1 max-h-36 overflow-y-auto pr-1">
                    {files.slice(0, 10).map((file) => (
                      <div key={file} className="text-[11px] font-mono text-slate-400 truncate hover:text-slate-200">
                        • {file}
                      </div>
                    ))}
                    {files.length > 10 && (
                      <span className="text-[10px] text-slate-500 italic">
                        + {files.length - 10} more files
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Entry Points & File Explorer Tree */}
        <div className="space-y-6">
          
          {/* Entry Points */}
          <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl">
            <h3 className="text-sm font-bold text-white flex items-center gap-2 mb-3">
              <PlayCircle className="h-4 w-4 text-emerald-400" /> Application Entry Points
            </h3>
            {architecture.entryPoints && architecture.entryPoints.length > 0 ? (
              <div className="space-y-1.5">
                {architecture.entryPoints.map((entry) => (
                  <div
                    key={entry}
                    className="flex items-center space-x-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 px-3 py-2 text-xs font-mono text-emerald-300"
                  >
                    <PlayCircle className="h-3.5 w-3.5 shrink-0 text-emerald-400" />
                    <span className="truncate">{entry}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-500 italic">Standard project root structure</p>
            )}
          </div>

          {/* Interactive File Tree */}
          <FileTree files={architecture.fileTree || []} />

        </div>

      </div>

    </div>
  );
};
