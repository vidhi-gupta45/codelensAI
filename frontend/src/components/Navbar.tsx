import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Code2, LogOut, PlusCircle, LayoutDashboard } from 'lucide-react';

interface NavbarProps {
  onOpenAnalyzeModal?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenAnalyzeModal }) => {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/auth');
  };

  if (!isAuthenticated) return null;

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        
        {/* Brand Logo */}
        <Link to="/" className="flex items-center space-x-3 group">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-sky-500 via-indigo-500 to-purple-600 p-0.5 shadow-lg shadow-sky-500/20 group-hover:shadow-sky-500/35 transition-all duration-300">
            <div className="flex h-full w-full items-center justify-center rounded-[10px] bg-slate-950">
              <Code2 className="h-5 w-5 text-sky-400 group-hover:scale-110 transition-transform duration-300" />
            </div>
          </div>
          <div className="flex flex-col">
            <span className="text-lg font-bold tracking-tight text-white flex items-center gap-1.5">
              CodeLens <span className="text-xs font-semibold px-1.5 py-0.5 rounded bg-sky-500/10 text-sky-400 border border-sky-500/20">AI</span>
            </span>
            <span className="text-[10px] text-slate-400 tracking-wide">Codebase Intelligence</span>
          </div>
        </Link>

        {/* Center Nav Links */}
        <nav className="hidden md:flex items-center space-x-1">
          <Link
            to="/dashboard"
            className={`flex items-center space-x-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
              location.pathname === '/dashboard'
                ? 'bg-slate-800/80 text-sky-400 border border-slate-700/50'
                : 'text-slate-300 hover:text-white hover:bg-slate-900'
            }`}
          >
            <LayoutDashboard className="h-4 w-4" />
            <span>Dashboard</span>
          </Link>
        </nav>

        {/* Right Action buttons & Profile */}
        <div className="flex items-center space-x-4">
          {onOpenAnalyzeModal && (
            <button
              onClick={onOpenAnalyzeModal}
              className="flex items-center space-x-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-md shadow-sky-500/20 hover:from-sky-400 hover:to-indigo-500 hover:shadow-sky-500/35 active:scale-95 transition-all duration-200"
            >
              <PlusCircle className="h-4 w-4" />
              <span>Analyze Repo</span>
            </button>
          )}

          <div className="h-5 w-px bg-slate-800" />

          {/* User Profile */}
          <div className="flex items-center space-x-3">
            <div className="hidden sm:flex flex-col text-right">
              <span className="text-xs font-semibold text-slate-200">{user?.username}</span>
              <span className="text-[10px] text-slate-400">{user?.email}</span>
            </div>
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-800 text-sky-400 border border-slate-700 font-bold text-xs">
              {user?.username?.charAt(0).toUpperCase() || 'U'}
            </div>
            <button
              onClick={handleLogout}
              title="Log Out"
              className="p-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
