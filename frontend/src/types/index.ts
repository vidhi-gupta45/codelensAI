export interface User {
  id: string;
  email: string;
  username?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  userId: string;
  email: string;
  role?: string;
}

export type JobStatus = 'QUEUED' | 'CLONING' | 'PARSING' | 'EMBEDDING' | 'SUMMARIZING' | 'COMPLETED' | 'FAILED';
export type AnalysisStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface AnalysisJob {
  id: string;
  status: JobStatus;
  progress: number; // 0 to 100
  currentStage: string;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
  repoAnalysis?: {
    id: string;
    repoUrl?: string;
  };
}

export interface AnalyzeRepoRequest {
  repoUrl: string;
}

export interface AnalyzeRepoResponse {
  jobId: string;
  repoId: string;
  repoUrl: string;
  status: JobStatus;
  message: string;
}

export interface RepoAnalysis {
  id: string;
  repoUrl: string;
  owner: string;
  repoName: string;
  status: AnalysisStatus;
  summary?: string;
  lastAnalyzedAt?: string;
  createdAt: string;
  totalFiles?: number;
  totalChunks?: number;
}

export interface FileNodeDto {
  id: string;
  path: string;
  name: string;
  language: string;
  size: number;
  isDirectory: boolean;
  parentPath?: string;
}

export interface ArchitectureResponse {
  repoId: string;
  repoUrl: string;
  owner: string;
  repoName: string;
  summary: string;
  techStack: string[];
  layers: Record<string, string[]>;
  entryPoints: string[];
  fileTree: FileNodeDto[];
}

export interface Citation {
  filePath: string;
  startLine: number;
  endLine: number;
  snippet: string;
  relevanceScore?: number;
}

export interface AskRequest {
  question: string;
}

export interface AskResponse {
  question: string;
  answer: string;
  citations: Citation[];
  processingTimeMs?: number;
}

export interface QAHistoryItem {
  id: string;
  question: string;
  answer: string;
  createdAt: string;
  citations: Citation[];
}
