import { useEffect, useState } from 'react'
import { getStats, type StatsResponse } from '../api/achievements'
import './StatsPage.css'

export function StatsPage() {
  const [stats, setStats] = useState<StatsResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getStats()
      .then(setStats)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="page-center">Загрузка...</div>
  if (!stats) return <div className="page-center error">Не удалось загрузить статистику</div>

  const percent = stats.total > 0 ? Math.round((stats.earned / stats.total) * 100) : 0

  return (
    <div className="stats-page">
      <h1>Статистика</h1>

      <div className="stats-card">
        <h2>Общий прогресс</h2>
        <p className="stats-count">{stats.earned} из {stats.total}</p>
        <div className="progress-bar-track">
          <div className="progress-bar-fill" style={{ width: `${percent}%` }} />
        </div>
        <p className="stats-percent">{percent}%</p>
      </div>

      <div className="stats-platforms">
        <div className="stats-card">
          <h2>Twitch</h2>
          <p>{stats.twitch.earned} из {stats.twitch.total}</p>
          <div className="progress-bar-track">
            <div
              className="progress-bar-fill twitch"
              style={{
                width: stats.twitch.total > 0
                  ? `${Math.round((stats.twitch.earned / stats.twitch.total) * 100)}%`
                  : '0%'
              }}
            />
          </div>
        </div>

        <div className="stats-card">
          <h2>YouTube</h2>
          {stats.youtube.locked ? (
            <p className="locked-hint">YouTube не подключён — привяжите аккаунт в настройках</p>
          ) : (
            <>
              <p>{stats.youtube.earned} из {stats.youtube.total}</p>
              <div className="progress-bar-track">
                <div
                  className="progress-bar-fill youtube"
                  style={{
                    width: stats.youtube.total > 0
                      ? `${Math.round((stats.youtube.earned / stats.youtube.total) * 100)}%`
                      : '0%'
                  }}
                />
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
