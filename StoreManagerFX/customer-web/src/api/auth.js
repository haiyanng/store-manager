import apiClient from './client';

export async function login(email, password) {
  const response = await apiClient.post('/api/auth/login', { email, password });
  return response.data.data;
}

export async function loadCurrentUser() {
  const response = await apiClient.get('/api/auth/me');
  return response.data.data;
}

export async function logout() {
  const response = await apiClient.post('/api/auth/logout');
  return response.data.data;
}
