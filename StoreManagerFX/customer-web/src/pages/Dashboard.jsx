import { useAuth } from '../context/AuthContext.jsx';

export default function Dashboard() {
  const { user, role } = useAuth();

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Dashboard</p>
          <h1>Business overview</h1>
        </div>
      </section>

      <section className="metric-grid">
        <article className="metric-card">
          <span>Current User</span>
          <strong>{user?.name}</strong>
          <p>{user?.email}</p>
        </article>

        <article className="metric-card">
          <span>Role</span>
          <strong>{role?.display_name}</strong>
          <p>{role?.name}</p>
        </article>

        <article className="metric-card">
          <span>Account Status</span>
          <strong>{user?.active ? 'Active' : 'Inactive'}</strong>
          <p>Loaded from /api/auth/me</p>
        </article>
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>API contract</h2>
            <p>The backend currently exposes authentication endpoints only.</p>
          </div>
        </div>

        <div className="contract-list">
          <span>POST /api/auth/login</span>
          <span>GET /api/auth/me</span>
          <span>POST /api/auth/logout</span>
          <span>POST /api/auth/register</span>
        </div>
      </section>
    </div>
  );
}
