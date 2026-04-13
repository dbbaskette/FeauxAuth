const getAuth = () => sessionStorage.getItem('feauxauth_credentials')

export const api = {
  async fetch(url, options = {}) {
    const auth = getAuth()
    if (!auth) {
      window.location.href = '/admin/login'
      throw new Error('Not authenticated')
    }

    const response = await fetch(url, {
      ...options,
      headers: {
        'Authorization': `Basic ${auth}`,
        'Content-Type': 'application/json',
        ...options.headers,
      },
    })

    if (response.status === 401) {
      sessionStorage.removeItem('feauxauth_credentials')
      window.location.href = '/admin/login'
      throw new Error('Unauthorized')
    }

    return response
  },

  async get(url) {
    const res = await this.fetch(url)
    if (!res.ok) throw new Error(`GET ${url} failed: ${res.status}`)
    return res.json()
  },

  async post(url, body) {
    const res = await this.fetch(url, {
      method: 'POST',
      body: JSON.stringify(body),
    })
    if (!res.ok) throw new Error(`POST ${url} failed: ${res.status}`)
    return res.json()
  },

  async put(url, body) {
    const res = await this.fetch(url, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
    if (!res.ok) throw new Error(`PUT ${url} failed: ${res.status}`)
    return res.json()
  },

  async del(url) {
    return this.fetch(url, { method: 'DELETE' })
  },

  login(username, password) {
    const credentials = btoa(`${username}:${password}`)
    sessionStorage.setItem('feauxauth_credentials', credentials)
  },

  logout() {
    sessionStorage.removeItem('feauxauth_credentials')
  },

  isAuthenticated() {
    return !!getAuth()
  },
}
