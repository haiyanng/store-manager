import { useEffect, useState } from 'react';
import { fetchRoles } from '../api/management';

export default function RoleManagement() {
  const [state, setState] = useState({ loading: true, rows: [], error: '' });

  useEffect(() => {
    let active = true;

    async function loadRoles() {
      try {
        const roles = await fetchRoles();
        if (active) {
          setState({ loading: false, rows: Array.isArray(roles) ? roles : [], error: '' });
        }
      } catch (error) {
        if (active) {
          setState({
            loading: false,
            rows: [],
            error: error.response?.status === 404
              ? 'The backend does not currently expose /api/roles.'
              : error.response?.data?.message || 'Unable to load roles.',
          });
        }
      }
    }

    loadRoles();

    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Administration</p>
          <h1>Role Management</h1>
          <p>Role records are seeded in MySQL, but no role HTTP endpoint is exposed yet.</p>
        </div>
        <button type="button" className="primary-button" disabled>
          New Role
        </button>
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Roles</h2>
            <p>Connected endpoint: /api/roles</p>
          </div>
        </div>

        {state.loading && <div className="empty-state">Loading roles...</div>}
        {!state.loading && state.error && <div className="alert warning">{state.error}</div>}
        {!state.loading && !state.error && state.rows.length === 0 && (
          <div className="empty-state">No roles found.</div>
        )}

        {!state.loading && !state.error && state.rows.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Display Name</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {state.rows.map((role) => (
                  <tr key={role.id}>
                    <td>{role.name}</td>
                    <td>{role.display_name}</td>
                    <td>{role.created_at}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
