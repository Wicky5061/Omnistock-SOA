// Authentication helper utilities for OmniStock Frontend

export function getToken() {
  return localStorage.getItem('omnistock_token')
}

export function getUser() {
  return {
    token: localStorage.getItem('omnistock_token'),
    username: localStorage.getItem('omnistock_username'),
    role: localStorage.getItem('omnistock_role'),
  }
}

export function isLoggedIn() {
  return !!getToken()
}

export function setAuth(token, username, role) {
  localStorage.setItem('omnistock_token', token)
  localStorage.setItem('omnistock_username', username || '')
  localStorage.setItem('omnistock_role', role || 'ROLE_USER')
  window.dispatchEvent(new Event('authChange'))
}

export function clearAuth() {
  localStorage.removeItem('omnistock_token')
  localStorage.removeItem('omnistock_username')
  localStorage.removeItem('omnistock_role')
  window.dispatchEvent(new Event('authChange'))
}

export async function authFetch(url, options = {}) {
  const token = getToken()
  const headers = {
    ...(options.headers || {}),
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  return fetch(url, {
    ...options,
    headers,
  })
}
