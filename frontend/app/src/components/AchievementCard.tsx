import type { Achievement } from '../api/achievements'
import './AchievementCard.css'

interface Props {
  achievement: Achievement
}

export function AchievementCard({ achievement }: Props) {
  const { name, description, imageUrl, earned, locked } = achievement

  return (
    <div className={`achievement-card ${earned ? 'earned' : 'unearned'}`}>
      <div className="achievement-img-wrap">
        <img
          src={imageUrl}
          alt={name}
          className="achievement-img"
          style={{ filter: earned ? 'none' : 'grayscale(100%)' }}
        />
        {locked && <span className="achievement-lock">🔒</span>}
        <div className="achievement-tooltip">{description}</div>
      </div>
      <p className="achievement-name">{name}</p>
    </div>
  )
}
