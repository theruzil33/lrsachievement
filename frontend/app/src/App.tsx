import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './hooks/useAuth'
import { Navbar } from './components/Navbar'
import { HomePage } from './pages/HomePage'
import { StatsPage } from './pages/StatsPage'
import { SettingsPage } from './pages/SettingsPage'
import './App.css'

export default function App() {
  const { user, loading, refetch } = useAuth()

  if (loading) {
    return <div className="page-center">Загрузка...</div>
  }

  if (!user) {
    return (
      <div className="login-page">
        <div className="login-card">
          <h1>LRS Achievements</h1>
          <p>Войдите через Twitch, чтобы видеть свои достижения</p>
          <a className="btn-twitch" href="/auth/twitch/login">
            Войти через Twitch
          </a>
        </div>
      </div>
    )
  }

  return (
    <BrowserRouter>
      <Navbar user={user} onLogout={refetch} />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/stats" element={<StatsPage />} />
          <Route path="/settings" element={<SettingsPage user={user} onRefresh={refetch} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </BrowserRouter>
  )
}
