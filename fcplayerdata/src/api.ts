import type { PlayerDetail, PlayerSummary, SeasonPayload } from './types'

class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  if (init?.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(path, {
    ...init,
    headers,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  const data = text ? JSON.parse(text) as { message?: string } & T : null

  if (!response.ok) {
    throw new ApiError(data?.message || `Request failed (${response.status})`, response.status)
  }

  return data as T
}

export const api = {
  listPlayers: () => request<PlayerSummary[]>('/api/players'),

  createPlayer: (name: string) =>
    request<PlayerDetail>('/api/players', {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),

  getPlayer: (id: number) => request<PlayerDetail>(`/api/players/${id}`),

  renamePlayer: (id: number, name: string) =>
    request<PlayerSummary>(`/api/players/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ name }),
    }),

  deletePlayer: (id: number) =>
    request<void>(`/api/players/${id}`, { method: 'DELETE' }),

  reorderPlayers: (ids: number[]) =>
    request<PlayerSummary[]>('/api/players/order', {
      method: 'PUT',
      body: JSON.stringify({ ids }),
    }),

  addSeason: (playerId: number) =>
    request<PlayerDetail>(`/api/players/${playerId}/seasons`, {
      method: 'POST',
      body: JSON.stringify({}),
    }),

  updateSeason: (playerId: number, seasonId: number, payload: SeasonPayload) =>
    request<PlayerDetail>(`/api/players/${playerId}/seasons/${seasonId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  deleteSeason: (playerId: number, seasonId: number) =>
    request<PlayerDetail>(`/api/players/${playerId}/seasons/${seasonId}`, {
      method: 'DELETE',
    }),
}

export { ApiError }
