import { useEffect, useState } from 'react';
import { fetchUsers } from '../api/management';

export default function UserManagement() {
  const [state, setState] = useState({ loading: true, rows: [], error: '' });

  useEffect(() => {
    let active = true;

    async function loadUsers() {
      try {
        const users = await fetchUsers();
        if (active) {
          setState({ loading: false, rows: Array.isArray(users) ? users : [], error: '' });
        }
      } catch (error) {
        if (active) {
          setState({
            loading: false,
            rows: [],
            error: error.response?.status === 404
              ? 'The backend does not currently expose /api/users.'
              : error.response?.data?.message || 'Unable to load users.',
          });
        }
      }
    }

    loadUsers();

    return () => {
      active = false;
    };
  }, []);

  return (
    <ManagementPage
      title="User Management"
      description="Customer-web user operations will appear here when the backend exposes user endpoints."
      loading={state.loading}
      error={state.error}
      columns={['Name', 'Email', 'Status']}
      rows={state.rows}
    />
  );
}

function ManagementPage({ title, description, loading, error, columns, rows }) {
  return (
    <div className="page-stack">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Administration</p>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
        <button type="button" className="primary-button" disabled>
          New User
        </button>
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Users</h2>
            <p>Connected endpoint: /api/users</p>
          </div>
        </div>

        {loading && <div className="empty-state">Loading users...</div>}
        {!loading && error && <div className="alert warning">{error}</div>}
        {!loading && !error && rows.length === 0 && <div className="empty-state">No users found.</div>}

        {!loading && !error && rows.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  {columns.map((column) => (
                    <th key={column}>{column}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.name}</td>
                    <td>{row.email}</td>
                    <td>{row.active ? 'Active' : 'Inactive'}</td>
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
