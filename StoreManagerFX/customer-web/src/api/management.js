import apiClient from './client';

export async function fetchUsers() {
  const response = await apiClient.get('/api/users');
  return response.data.data;
}

export async function fetchRoles() {
  const response = await apiClient.get('/api/roles');
  return response.data.data;
}
