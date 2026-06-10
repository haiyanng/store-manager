import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { API_BASE_URL } from '../api/client';

export default function AppLayout() {
  const { user, role, signOut } = useAuth();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">CW</span>
          <div>
            <strong>Customer Web</strong>
            <span>Admin Console</span>
          </div>
        </div>

        <nav className="side-nav" aria-label="Primary navigation">
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/users">User Management</NavLink>
          <NavLink to="/roles">Role Management</NavLink>
        </nav>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div>
            <p className="eyebrow">API</p>
            <strong>{API_BASE_URL}</strong>
          </div>

          <div className="profile-chip">
            <div className="avatar">{user?.name?.slice(0, 1) || 'U'}</div>
            <div>
              <strong>{user?.name}</strong>
              <span>{role?.display_name || role?.name}</span>
            </div>
            <button type="button" className="ghost-button" onClick={signOut}>
              Logout
            </button>
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
