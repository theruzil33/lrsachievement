import { useEffect, useState } from 'react'
import { getMe, type MeResponse } from '../api/auth'

interface AuthState {
  user: MeResponse | null
  loading: boolean
  refetch: () => void
}

export function useAuth(): AuthState {
  const [user, setUser] = useState<MeResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [tick, setTick] = useState(0)

  useEffect(() => {
    setLoading(true)
    getMe()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [tick])

  return { user, loading, refetch: () => setTick(t => t + 1) }
}
