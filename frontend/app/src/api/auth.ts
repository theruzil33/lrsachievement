import axios from 'axios'

export interface MeResponse {
  twitchLogin: string
  youtubeConnected: boolean
}

export async function getMe(): Promise<MeResponse> {
  const { data } = await axios.get<MeResponse>('/api/me', { withCredentials: true })
  return data
}

export async function disconnectYoutube(): Promise<void> {
  await axios.post('/auth/youtube/disconnect', {}, { withCredentials: true })
}

export async function logout(): Promise<void> {
  await axios.post('/auth/logout', {}, { withCredentials: true })
}
