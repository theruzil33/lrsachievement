import type { MeResponse } from '../api/auth'
import { disconnectYoutube } from '../api/auth'
import './SettingsPage.css'

interface Props {
  user: MeResponse
  onRefresh: () => void
}

export function SettingsPage({ user, onRefresh }: Props) {
  async function handleDisconnectYoutube() {
    await disconnectYoutube()
    onRefresh()
  }

  return (
    <div className="settings-page">
      <h1>Настройки</h1>

      <div className="settings-card">
        <h2>Twitch</h2>
        <p>Вы вошли как <strong>@{user.twitchLogin}</strong></p>
      </div>

      <div className="settings-card">
        <h2>YouTube</h2>
        {user.youtubeConnected ? (
          <div className="settings-connected">
            <p>✅ YouTube подключён</p>
            <button className="btn-danger" onClick={handleDisconnectYoutube}>
              Отвязать YouTube
            </button>
          </div>
        ) : (
          <div className="settings-disconnected">
            <p>YouTube не привязан. Привяжите аккаунт, чтобы разблокировать YouTube-ачивки.</p>
            <a className="btn-primary" href="/auth/youtube/connect">
              Привязать YouTube
            </a>
          </div>
        )}
      </div>
    </div>
  )
}
