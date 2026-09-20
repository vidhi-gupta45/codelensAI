import axios from 'axios';
import type {
  AuthResponse,
  AnalyzeRepoResponse,
  AnalysisJob,
  RepoAnalysis,
  ArchitectureResponse,
  AskResponse,
  QAHistoryItem,
} from '../types';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token to requests if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('codelens_auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Intercept 401 Unauthorized responses
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('codelens_auth_token');
      localStorage.removeItem('codelens_user');
      if (window.location.pathname !== '/auth') {
        window.location.href = '/auth';
      }
    }
    return Promise.reject(error);
  }
);

// Auth Services
export const authApi = {
  login: async (email: string, password: string): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/login', { email, password });
    return res.data;
  },
  register: async (email: string, password: string): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/auth/register', { email, password });
    return res.data;
  },
};

// Repository Services
export const repoApi = {
  analyzeRepo: async (repoUrl: string): Promise<AnalyzeRepoResponse> => {
    const res = await api.post<AnalyzeRepoResponse>('/repos/analyze', { repoUrl });
    return res.data;
  },
  getJobStatus: async (jobId: string): Promise<AnalysisJob> => {
    const res = await api.get<AnalysisJob>(`/repos/jobs/${jobId}`);
    return res.data;
  },
  getUserRepos: async (): Promise<RepoAnalysis[]> => {
    const res = await api.get<RepoAnalysis[]>('/repos');
    return res.data;
  },
  getRepoDetails: async (repoId: string): Promise<RepoAnalysis> => {
    const res = await api.get<RepoAnalysis>(`/repos/${repoId}`);
    return res.data;
  },
  getArchitecture: async (repoId: string): Promise<ArchitectureResponse> => {
    const res = await api.get<ArchitectureResponse>(`/repos/${repoId}/architecture`);
    return res.data;
  },
  askQuestion: async (repoId: string, question: string): Promise<AskResponse> => {
    const res = await api.post<AskResponse>(`/repos/${repoId}/ask`, { question });
    return res.data;
  },
  getQAHistory: async (repoId: string): Promise<QAHistoryItem[]> => {
    const res = await api.get<QAHistoryItem[]>(`/repos/${repoId}/history`);
    return res.data;
  },
};

export default api;
