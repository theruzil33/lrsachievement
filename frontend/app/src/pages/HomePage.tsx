import { useEffect, useState } from 'react'
import { getAchievements, type Achievement } from '../api/achievements'
import { AchievementCard } from '../components/AchievementCard'
import './HomePage.css'

interface SectionProps {
  title: string
  badge: string
  achievements: Achievement[]
}

function AchievementSection({ title, badge, achievements }: SectionProps) {
  return (
    <section className="achievement-section">
      <h2 className="section-title">
        <span className="section-badge">{badge}</span>
        {title}
        <span className="section-count">{achievements.filter(a => a.earned).length} / {achievements.length}</span>
      </h2>
      <div className="achievements-grid">
        {achievements.map(a => (
          <AchievementCard key={a.id} achievement={a} />
        ))}
      </div>
    </section>
  )
}

export function HomePage() {
  const [achievements, setAchievements] = useState<Achievement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getAchievements()
      .then(setAchievements)
      .catch(() => setError('Не удалось загрузить ачивки'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="page-center">Загрузка...</div>
  if (error) return <div className="page-center error">{error}</div>

  const twitch = achievements.filter(a => a.platform === 'TWITCH')
  const youtube = achievements.filter(a => a.platform === 'YOUTUBE')

  return (
    <div className="home-page">
      <h1>Все достижения</h1>
      {twitch.length > 0 && (
        <AchievementSection title="Twitch" badge="🟣" achievements={twitch} />
      )}
      {youtube.length > 0 && (
        <AchievementSection title="YouTube" badge="🔴" achievements={youtube} />
      )}
    </div>
  )
}
