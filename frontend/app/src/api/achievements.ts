import axios from 'axios'

export interface Achievement {
  id: string
  name: string
  description: string
  imageUrl: string
  platform: 'TWITCH' | 'YOUTUBE'
  earned: boolean
  locked: boolean
}

export interface StatsResponse {
  total: number
  earned: number
  twitch: { total: number; earned: number; locked: boolean }
  youtube: { total: number; earned: number; locked: boolean }
}

export async function getAchievements(): Promise<Achievement[]> {
  const { data } = await axios.get<Achievement[]>('/api/achievements', { withCredentials: true })
  return data
}

export async function getStats(): Promise<StatsResponse> {
  const { data } = await axios.get<StatsResponse>('/api/stats', { withCredentials: true })
  return data
}
