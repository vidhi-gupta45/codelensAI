import React, { useState } from 'react';
import type { FileNodeDto } from '../types';
import { Folder, FolderOpen, FileCode, ChevronRight, ChevronDown, Code } from 'lucide-react';

interface FileTreeProps {
  files: FileNodeDto[];
}

interface TreeNode {
  name: string;
  path: string;
  isDirectory: boolean;
  file?: FileNodeDto;
  children: Record<string, TreeNode>;
}

export const FileTree: React.FC<FileTreeProps> = ({ files }) => {
  const [expandedPaths, setExpandedPaths] = useState<Record<string, boolean>>({});

  // Build tree hierarchy from flat list of file paths
  const rootNode: TreeNode = {
    name: 'root',
    path: '',
    isDirectory: true,
    children: {},
  };

  files.forEach((file) => {
    const parts = file.path.split('/');
    let current = rootNode;

    parts.forEach((part, index) => {
      const isLast = index === parts.length - 1;
      const currentPath = parts.slice(0, index + 1).join('/');

      if (!current.children[part]) {
        current.children[part] = {
          name: part,
          path: currentPath,
          isDirectory: isLast ? file.isDirectory : true,
          file: isLast ? file : undefined,
          children: {},
        };
      }

      current = current.children[part];
    });
  });

  const toggleExpand = (path: string) => {
    setExpandedPaths((prev) => ({
      ...prev,
      [path]: !prev[path],
    }));
  };

  const renderNode = (node: TreeNode, depth: number = 0) => {
    const isExpanded = expandedPaths[node.path] ?? depth < 1; // Auto-expand top levels
    const hasChildren = Object.keys(node.children).length > 0;

    return (
      <div key={node.path || node.name} className="select-none text-xs">
        <div
          onClick={() => node.isDirectory && toggleExpand(node.path)}
          style={{ paddingLeft: `${depth * 14 + 12}px` }}
          className={`flex items-center justify-between py-1.5 pr-3 rounded-lg hover:bg-slate-800/60 cursor-pointer transition-colors ${
            node.isDirectory ? 'text-slate-300 font-medium' : 'text-slate-400 font-normal'
          }`}
        >
          <div className="flex items-center space-x-2 truncate">
            {node.isDirectory ? (
              <>
                <span className="text-slate-500">
                  {isExpanded ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronRight className="h-3.5 w-3.5" />}
                </span>
                {isExpanded ? (
                  <FolderOpen className="h-4 w-4 text-amber-400/90 shrink-0" />
                ) : (
                  <Folder className="h-4 w-4 text-amber-400/70 shrink-0" />
                )}
                <span className="truncate">{node.name}</span>
              </>
            ) : (
              <>
                <span className="w-3.5 shrink-0" />
                <FileCode className="h-4 w-4 text-sky-400/80 shrink-0" />
                <span className="truncate text-slate-300">{node.name}</span>
              </>
            )}
          </div>

          {!node.isDirectory && node.file && (
            <div className="flex items-center space-x-2 shrink-0 ml-2">
              {node.file.language && (
                <span className="rounded bg-slate-800 px-1.5 py-0.5 text-[10px] font-mono text-sky-400 border border-slate-700/50">
                  {node.file.language}
                </span>
              )}
              <span className="text-[10px] text-slate-500 font-mono">
                {formatFileSize(node.file.size)}
              </span>
            </div>
          )}
        </div>

        {node.isDirectory && isExpanded && hasChildren && (
          <div>
            {Object.values(node.children)
              .sort((a, b) => {
                if (a.isDirectory === b.isDirectory) return a.name.localeCompare(b.name);
                return a.isDirectory ? -1 : 1;
              })
              .map((child) => renderNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-3 overflow-x-auto max-h-[500px]">
      <div className="flex items-center justify-between border-b border-slate-800/80 pb-2 mb-2 px-2">
        <span className="text-xs font-semibold text-slate-400 flex items-center gap-1.5">
          <Code className="h-4 w-4 text-sky-400" /> Codebase Structure ({files.length} files)
        </span>
      </div>
      {files.length === 0 ? (
        <p className="text-xs text-slate-500 italic p-3 text-center">No file tree available</p>
      ) : (
        Object.values(rootNode.children)
          .sort((a, b) => (a.isDirectory === b.isDirectory ? a.name.localeCompare(b.name) : a.isDirectory ? -1 : 1))
          .map((child) => renderNode(child, 0))
      )}
    </div>
  );
};

function formatFileSize(bytes: number): string {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}
