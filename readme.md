# LRS Achievement

Приложение для отображения ачивок пользователей на основе активности в Twitch и YouTube.

## Подготовка

### 1. Зарегистрировать приложения

**Twitch:**
1. Зайти на https://dev.twitch.tv/console/apps
2. Создать приложение, указать Redirect URI
3. Сохранить `Client ID` и `Client Secret`

**YouTube (опционально):**
1. Зайти на https://console.cloud.google.com
2. Создать проект → APIs & Services → Enable → YouTube Data API v3
3. Credentials → OAuth 2.0 Client ID (Web application)
4. Сохранить `Client ID` и `Client Secret`

### 2. Настроить ачивки

Отредактируй `config/achievements.yml` — замени `your_channel_name` и `PLxxxxxxxxxxxxxxxxxx` на реальные значения. Туда же положи картинки ачивок в папку `config/images/`.

---

## Запуск локально через Maven

### 1. Создать файл с секретами

Создай `backend/app/src/main/resources/application-local.yml` (файл в `.gitignore`, в репозиторий не попадёт):

```yaml
twitch:
  client-id: твой_twitch_client_id
  client-secret: твой_twitch_client_secret
  redirect-uri: http://localhost:8080/auth/twitch/callback

youtube:
  client-id: твой_google_client_id
  client-secret: твой_google_client_secret
  redirect-uri: http://localhost:8080/auth/youtube/callback

achievements:
  config-path: ../../../config/achievements.yml
  images-path: file:../../../config/images
```

### 2. Запустить backend

```bash
cd backend/app
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Запустить frontend

```bash
cd frontend/app
npm install
npm run dev
```

Приложение доступно на http://localhost:5173

> Vite проксирует запросы `/api` и `/auth` на `http://localhost:8080`.

---

## Запуск через Docker Compose

### 1. Создать `.env` файл

Скопируй `.env.example` и заполни реальными значениями:

```bash
cp .env.example .env
```

Содержимое `.env`:

```
TWITCH_CLIENT_ID=твой_twitch_client_id
TWITCH_CLIENT_SECRET=твой_twitch_client_secret
YOUTUBE_CLIENT_ID=твой_google_client_id
YOUTUBE_CLIENT_SECRET=твой_google_client_secret
```

> Docker Compose автоматически читает `.env` из той же папки и подставляет переменные в `docker-compose.yml`.

### 2. Запустить

```bash
docker compose up --build
```

Приложение доступно на http://localhost

### 3. Остановить

```bash
docker compose down
```

---

## Redirect URI в зависимости от способа запуска

| Способ запуска | Twitch Redirect URI | YouTube Redirect URI |
|---|---|---|
| Maven (локально) | `http://localhost:8080/auth/twitch/callback` | `http://localhost:8080/auth/youtube/callback` |
| Docker Compose | `http://localhost/auth/twitch/callback` | `http://localhost/auth/youtube/callback` |

Убедись, что в настройках приложения на Twitch и Google указан правильный URI для выбранного способа запуска.
