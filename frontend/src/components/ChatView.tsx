import React, { useState, useEffect, useRef } from 'react';
import { repoApi } from '../api/client';
import type { AskResponse, Citation } from '../types';
import { CitationCard } from './CitationCard';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Send, Bot, Loader2, Sparkles, AlertTriangle, Clock, Code } from 'lucide-react';

interface ChatViewProps {
  repoId: string;
}

interface ChatMessage {
  id: string;
  sender: 'user' | 'ai';
  text: string;
  citations?: Citation[];
  timestamp?: string;
}

const SAMPLE_PROMPTS = [
  'How does the authentication flow work in this codebase?',
  'Explain the overall architecture and main entry points.',
  'What database models or entities are defined?',
  'Where are API routes and controller handlers located?',
];

export const ChatView: React.FC<ChatViewProps> = ({ repoId }) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetchingHistory, setFetchingHistory] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    loadHistory();
  }, [repoId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  const loadHistory = async () => {
    try {
      setFetchingHistory(true);
      const historyItems = await repoApi.getQAHistory(repoId);
      const formattedMessages: ChatMessage[] = [];

      historyItems.forEach((item) => {
        formattedMessages.push({
          id: `${item.id}-q`,
          sender: 'user',
          text: item.question,
          timestamp: item.createdAt,
        });
        formattedMessages.push({
          id: `${item.id}-a`,
          sender: 'ai',
          text: item.answer,
          citations: item.citations,
          timestamp: item.createdAt,
        });
      });

      setMessages(formattedMessages);
    } catch (err) {
      console.error('Failed to load QA history:', err);
    } finally {
      setFetchingHistory(false);
    }
  };

  const handleSend = async (questionText?: string) => {
    const q = (questionText || question).trim();
    if (!q || loading) return;

    const userMsgId = Date.now().toString();
    const newMsg: ChatMessage = {
      id: userMsgId,
      sender: 'user',
      text: q,
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, newMsg]);
    if (!questionText) setQuestion('');
    setLoading(true);
    setError(null);

    try {
      const response: AskResponse = await repoApi.askQuestion(repoId, q);
      const aiMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'ai',
        text: response.answer,
        citations: response.citations,
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, aiMsg]);
    } catch (err: any) {
      console.error('Ask Question error:', err);
      if (err.response?.status === 429) {
        setError('Rate limit exceeded (20 questions / hour). Please wait before asking another question.');
      } else {
        setError(err.response?.data?.message || 'Failed to process question. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-[750px] rounded-2xl border border-slate-800 bg-slate-900/90 shadow-2xl overflow-hidden">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-800/80 px-6 py-4 bg-slate-950/60">
        <div className="flex items-center space-x-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-sky-500 to-indigo-600 text-white shadow-md shadow-sky-500/20">
            <Bot className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              Codebase RAG Assistant
              <span className="flex items-center gap-1 text-[10px] font-semibold text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-full">
                <Sparkles className="h-2.5 w-2.5" /> Vector Context Active
              </span>
            </h3>
            <p className="text-xs text-slate-400">Ask any questions about this repository's code, APIs, and logic</p>
          </div>
        </div>

        <div className="text-right hidden sm:block">
          <span className="text-[11px] text-slate-400 flex items-center gap-1">
            <Clock className="h-3 w-3 text-sky-400" /> Limit: 20 Qs / hr
          </span>
        </div>
      </div>

      {/* Messages Scroll Area */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-slate-950/30">
        {fetchingHistory ? (
          <div className="flex h-full items-center justify-center text-slate-500 text-xs">
            <Loader2 className="h-5 w-5 animate-spin mr-2 text-sky-400" />
            Loading conversation history...
          </div>
        ) : messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center max-w-md mx-auto space-y-4 py-8">
            <div className="h-12 w-12 rounded-2xl bg-sky-500/10 border border-sky-500/20 flex items-center justify-center text-sky-400 shadow-lg shadow-sky-500/10">
              <Code className="h-6 w-6" />
            </div>
            <div>
              <h4 className="text-base font-bold text-white mb-1">Ask anything about this repo</h4>
              <p className="text-xs text-slate-400">
                CodeLens AI uses vector embeddings to search code files and generate exact answers with line-by-line citations.
              </p>
            </div>

            {/* Prompt Chips */}
            <div className="w-full pt-2">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block mb-2">
                Suggested Prompts
              </span>
              <div className="flex flex-col space-y-2 text-left">
                {SAMPLE_PROMPTS.map((promptText) => (
                  <button
                    key={promptText}
                    onClick={() => handleSend(promptText)}
                    className="rounded-xl border border-slate-800 bg-slate-900/80 px-3.5 py-2 text-xs text-slate-300 hover:border-sky-500/40 hover:bg-slate-850 hover:text-sky-300 transition-all flex items-center justify-between group"
                  >
                    <span>{promptText}</span>
                    <Sparkles className="h-3.5 w-3.5 text-slate-500 group-hover:text-sky-400 transition-colors" />
                  </button>
                ))}
              </div>
            </div>
          </div>
        ) : (
          messages.map((msg) => (
            <div
              key={msg.id}
              className={`flex space-x-3.5 ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}
            >
              {msg.sender === 'ai' && (
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-indigo-500/20 border border-indigo-500/30 text-indigo-400 font-bold text-xs">
                  AI
                </div>
              )}

              <div
                className={`max-w-3xl rounded-2xl p-4 text-xs leading-relaxed ${
                  msg.sender === 'user'
                    ? 'bg-sky-600 text-white rounded-br-none shadow-lg shadow-sky-600/20'
                    : 'bg-slate-900 border border-slate-800 text-slate-200 rounded-bl-none shadow-md'
                }`}
              >
                {msg.sender === 'user' ? (
                  <p className="whitespace-pre-wrap">{msg.text}</p>
                ) : (
                  <div className="prose prose-invert prose-xs max-w-none space-y-2">
                    <ReactMarkdown remarkPlugins={[remarkGfm]}>
                      {msg.text}
                    </ReactMarkdown>

                    {/* Citations section */}
                    {msg.citations && msg.citations.length > 0 && (
                      <div className="mt-4 pt-3 border-t border-slate-800/80">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-2 flex items-center gap-1">
                          <Code className="h-3 w-3 text-sky-400" /> Cited Code Sources ({msg.citations.length})
                        </span>
                        <div className="space-y-2">
                          {msg.citations.map((citation, idx) => (
                            <CitationCard key={idx} citation={citation} index={idx} />
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>

              {msg.sender === 'user' && (
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-sky-500/20 border border-sky-500/30 text-sky-400 font-bold text-xs">
                  U
                </div>
              )}
            </div>
          ))
        )}

        {loading && (
          <div className="flex space-x-3.5 justify-start">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-indigo-500/20 border border-indigo-500/30 text-indigo-400">
              <Loader2 className="h-4 w-4 animate-spin" />
            </div>
            <div className="rounded-2xl rounded-bl-none border border-slate-800 bg-slate-900 px-4 py-3 text-xs text-slate-400 flex items-center space-x-2">
              <Sparkles className="h-3.5 w-3.5 text-sky-400 animate-pulse" />
              <span>Analyzing code chunks and generating response...</span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Error Banner */}
      {error && (
        <div className="bg-rose-500/10 border-t border-rose-500/20 px-4 py-2.5 text-xs text-rose-300 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError(null)} className="text-slate-400 hover:text-white">✕</button>
        </div>
      )}

      {/* Footer Input Area */}
      <div className="border-t border-slate-800/80 p-4 bg-slate-950/80">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSend();
          }}
          className="relative flex items-center"
        >
          <input
            type="text"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Ask a question about authentication, database models, API handlers..."
            className="w-full rounded-xl border border-slate-800 bg-slate-900 pl-4 pr-12 py-3 text-xs text-slate-200 placeholder-slate-500 focus:border-sky-500 focus:outline-none focus:ring-1 focus:ring-sky-500 transition-all shadow-inner"
            disabled={loading}
          />
          <button
            type="submit"
            disabled={!question.trim() || loading}
            className="absolute right-2 flex h-8 w-8 items-center justify-center rounded-lg bg-sky-500 text-white shadow-md shadow-sky-500/20 hover:bg-sky-400 disabled:opacity-40 transition-all"
          >
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
          </button>
        </form>
      </div>

    </div>
  );
};
