import { NavLink } from 'react-router-dom'
import type { MeResponse } from '../api/auth'
import { logout } from '../api/auth'
import './Navbar.css'

interface Props {
  user: MeResponse
  onLogout: () => void
}

export function Navbar({ user, onLogout }: Props) {
  async function handleLogout() {
    await logout()
    onLogout()
  }

  return (
    <nav className="navbar">
      <span className="navbar-brand">LRS Achievements</span>
      <div className="navbar-links">
        <NavLink to="/" end>Ачивки</NavLink>
        <NavLink to="/stats">Статистика</NavLink>
        <NavLink to="/settings">Настройки</NavLink>
      </div>
      <div className="navbar-user">
        <span>@{user.twitchLogin}</span>
        <button onClick={handleLogout}>Выйти</button>
      </div>
    </nav>
  )
}
