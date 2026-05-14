# План реализации LRS Achievement

## Стек технологий (существующий)
- **Backend**: Java 17, Spring Boot 4.0.6, Maven, Lombok
- **Frontend**: React 19, Vite 8, TypeScript
- **Контейнеризация**: Docker + Docker Compose
- **Авторизация**: Twitch OAuth 2.0 (основная), YouTube Data API v3 (опциональная)

---

## Фазы реализации

### Фаза 0. Подготовка инфраструктуры
### Фаза 1. Backend — ядро
### Фаза 2. Backend — интеграции (Twitch, YouTube)
### Фаза 3. Frontend — каркас и роутинг
### Фаза 4. Frontend — страницы
### Фаза 5. Docker & docker-compose
### Фаза 6. Конфигурационные файлы (YAML ачивки + картинки)

---

## Фаза 0. Подготовка инфраструктуры

### 0.1 Регистрация приложений в Twitch и YouTube

**Twitch:**
1. Зайти на https://dev.twitch.tv/console/apps
2. Создать приложение: название произвольное, redirect URI = `http://localhost:8080/auth/twitch/callback`
3. Сохранить `Client ID` и `Client Secret`

**YouTube (Google):**
1. Зайти на https://console.cloud.google.com/
2. Создать проект → APIs & Services → Enable API → YouTube Data API v3
3. Credentials → Create credentials → OAuth 2.0 Client ID (Web application)
4. Authorized redirect URIs = `http://localhost:8080/auth/youtube/callback`
5. Сохранить `Client ID` и `Client Secret`

### 0.2 Структура каталогов проекта (итоговая)

```
lrsachievement/
├── backend/
│   └── app/
│       ├── src/main/java/com/lrsachievement/
│       │   ├── AppApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── WebConfig.java
│       │   │   └── AchievementConfig.java
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   ├── AchievementController.java
│       │   │   └── UserController.java
│       │   ├── service/
│       │   │   ├── AuthService.java
│       │   │   ├── TwitchService.java
│       │   │   ├── YouTubeService.java
│       │   │   └── AchievementService.java
│       │   ├── model/
│       │   │   ├── Achievement.java
│       │   │   ├── AchievementDefinition.java
│       │   │   ├── UserSession.java
│       │   │   └── AchievementsConfig.java
│       │   └── repository/
│       │       └── UserSessionRepository.java
│       ├── src/main/resources/
│       │   ├── application.yml
│       │   ├── achievements.yml          ← конфиг ачивок
│       │   └── achievement-images/       ← папка с картинками
│       └── pom.xml
├── frontend/
│   └── app/
│       ├── src/
│       │   ├── main.tsx
│       │   ├── App.tsx
│       │   ├── api/
│       │   │   ├── achievements.ts
│       │   │   └── auth.ts
│       │   ├── components/
│       │   │   ├── AchievementCard.tsx
│       │   │   ├── Navbar.tsx
│       │   │   └── Tooltip.tsx
│       │   ├── pages/
│       │   │   ├── HomePage.tsx
│       │   │   ├── StatsPage.tsx
│       │   │   └── SettingsPage.tsx
│       │   └── hooks/
│       │       └── useAuth.ts
│       ├── package.json
│       └── vite.config.ts
├── docker-compose.yml
└── plan.md
```

---

## Фаза 1. Backend — ядро

### 1.1 Зависимости (pom.xml)

Добавить в `pom.xml`:

```xml
<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Session (in-memory, без БД) -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-core</artifactId>
</dependency>

<!-- HTTP-клиент для Twitch/YouTube API -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- YAML-парсинг ачивок -->
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
</dependency>
```

### 1.2 application.yml

```yaml
server:
  port: 8080

twitch:
  client-id: ${TWITCH_CLIENT_ID}
  client-secret: ${TWITCH_CLIENT_SECRET}
  redirect-uri: ${TWITCH_REDIRECT_URI:http://localhost:8080/auth/twitch/callback}

youtube:
  client-id: ${YOUTUBE_CLIENT_ID}
  client-secret: ${YOUTUBE_CLIENT_SECRET}
  redirect-uri: ${YOUTUBE_REDIRECT_URI:http://localhost:8080/auth/youtube/callback}

achievements:
  config-path: ${ACHIEVEMENTS_CONFIG_PATH:classpath:achievements.yml}
  images-path: ${ACHIEVEMENTS_IMAGES_PATH:classpath:achievement-images/}

spring:
  session:
    store-type: none   # in-memory, без Redis/JDBC
```

### 1.3 Модель данных

**UserSession.java** — единственное, что хранится в памяти:
```java
// Что хранится в сессии:
// - twitchLogin (String) — логин Twitch пользователя
// - twitchAccessToken (String) — токен для запросов к Twitch API
// - youtubAccessToken (String, nullable) — токен YouTube (если привязан)
// - youtubeRefreshToken (String, nullable)
```

> Никакой БД нет. Данные живут только в HTTP-сессии (серверная сторона).

**AchievementDefinition.java** — POJO, загружается из YAML:
```java
// id, name, description, imageFile, platform (TWITCH/YOUTUBE), rule (тип правила)
// rule-параметры: channelName, threshold, playlistId и т.д.
```

**Achievement.java** — DTO для фронта:
```java
// id, name, description, imageUrl, platform, earned (boolean)
```

### 1.4 Загрузка конфигурации ачивок (AchievementConfig.java)

- При старте приложения Spring читает `achievements.yml` через `SnakeYAML`
- Преобразует в `List<AchievementDefinition>` и кладёт в `@Bean`
- Картинки отдаются как статические ресурсы с URL `/api/images/{filename}`

---

## Фаза 2. Backend — интеграции (Twitch и YouTube)

### 2.1 Twitch OAuth 2.0 Authorization Code Flow

**Шаг 1 — Редирект на Twitch:**
- `GET /auth/twitch/login`
- Backend формирует URL: `https://id.twitch.tv/oauth2/authorize?client_id=...&redirect_uri=...&response_type=code&scope=user:read:email+channel:read:subscriptions`
- Возвращает 302 редирект

**Шаг 2 — Callback от Twitch:**
- `GET /auth/twitch/callback?code=...`
- Backend делает POST к `https://id.twitch.tv/oauth2/token` с `code`
- Получает `access_token`
- Делает GET к `https://api.twitch.tv/helix/users` → получает `login` и `id`
- Сохраняет в HTTP-сессию: `twitchLogin`, `twitchId`, `twitchAccessToken`
- Редирект на фронт `/`

**Шаг 3 — Logout:**
- `POST /auth/logout`
- Инвалидирует HTTP-сессию
- Редирект на `/`

### 2.2 YouTube OAuth 2.0 (опциональная привязка)

**Шаг 1 — Редирект на Google:**
- `GET /auth/youtube/connect` (доступен только авторизованным через Twitch)
- Backend формирует URL: `https://accounts.google.com/o/oauth2/v2/auth?...&scope=https://www.googleapis.com/auth/youtube.readonly`

**Шаг 2 — Callback:**
- `GET /auth/youtube/callback?code=...`
- Получает `access_token` + `refresh_token`
- Сохраняет только токены в сессию (имя пользователя YouTube не хранится)
- Редирект на `/settings`

**Шаг 3 — Отвязка:**
- `POST /auth/youtube/disconnect`
- Удаляет YouTube-токены из сессии

### 2.3 TwitchService — проверка ачивок

Методы для проверки правил:

| Правило | Twitch API endpoint | Параметры |
|---|---|---|
| Подписка на канал | `GET /helix/subscriptions/user` | `broadcaster_id`, `user_id` |
| Первое сообщение в чате | Невозможно через REST API — используем флаг из сессии или webhook* | — |
| N сообщений в чате | Аналогично* | threshold |
| Стрик просмотров | EventSub или эвристика по истории* | streak |

> **Примечание по чату**: Twitch REST API не позволяет получить историю сообщений пользователя. Для сообщений в чате необходим Twitch EventSub (WebSocket) или упрощённый подход — хранить счётчик в сессии при активной сессии просмотра. На первой итерации можно реализовать только проверку подписок, стрик просмотров через EventSub оставить как TODO.

### 2.4 YouTubeService — проверка ачивок

| Правило | YouTube API endpoint |
|---|---|
| Просмотр видео из плейлиста | `GET /youtube/v3/playlistItems` + `GET /youtube/v3/videos` (история просмотров через activitiesAPI) |
| Просмотр всех видео плейлиста | Пересечение playlists items и watched videos |

> **Примечание**: YouTube Data API `activities` возвращает только публичные активности. Для проверки истории просмотров нужен scope `youtube.readonly` — он даёт доступ к liked videos и плейлистам, но не к полной истории. Реалистичная реализация: проверять наличие видео в "Понравившихся" или в плейлисте "История" (если доступен).

### 2.5 AchievementService — оркестратор

```
AchievementService.getAchievements(session):
  1. Загрузить все AchievementDefinition из конфига
  2. Для каждого definition:
     a. Если platform == YOUTUBE и youtube не привязан → earned = false, locked = true
     b. Иначе вызвать соответствующий сервис (Twitch/YouTube) для проверки правила
     c. Собрать Achievement DTO с earned, locked, imageUrl
  3. Вернуть List<Achievement>
```

---

## Фаза 3. Frontend — каркас и роутинг

### 3.1 Зависимости (package.json)

Добавить:
```json
"react-router-dom": "^7.x",
"axios": "^1.x"
```

### 3.2 Маршруты (App.tsx)

```
/              → HomePage    (список ачивок)
/stats         → StatsPage   (статистика)
/settings      → SettingsPage (привязка YouTube)
```

Защита маршрутов: если пользователь не авторизован через Twitch — редирект на страницу логина (или показать кнопку "Войти через Twitch" прямо на главной).

### 3.3 useAuth hook

```typescript
// Хранит: { twitchLogin, youtubeConnected, isLoading }
// GET /api/me → получить текущего пользователя из сессии
// Если 401 → пользователь не авторизован
```

### 3.4 Vite proxy (vite.config.ts)

```typescript
server: {
  proxy: {
    '/api': 'http://localhost:8080',
    '/auth': 'http://localhost:8080',
  }
}
```

---

## Фаза 4. Frontend — страницы

### 4.1 HomePage (главная страница)

**Поведение:**
- При загрузке: `GET /api/achievements` → список ачивок с `earned`, `locked`, `imageUrl`, `name`, `description`
- Рендер сетки карточек (`grid`)
- Если `earned = true` → картинка яркая (100% насыщенность)
- Если `earned = false` и `locked = false` → картинка чёрно-белая (`filter: grayscale(100%)`)
- Если `locked = true` (YouTube не привязан) → чёрно-белая + иконка замка

**Tooltip:**
- При hover на карточку — всплывает описание из YML (`description`)
- Реализация: CSS-only tooltip или библиотека Floating UI

**AchievementCard.tsx:**
```
props: { id, name, description, imageUrl, earned, locked }
render:
  <div class="card">
    <img style={earned ? {} : {filter:'grayscale(100%)'}} src={imageUrl} />
    {locked && <LockIcon />}
    <Tooltip text={description} />
    <p>{name}</p>
  </div>
```

### 4.2 StatsPage (статистика)

**Контент:**
- Общий прогресс: `X из Y ачивок получено` (прогресс-бар)
- Разбивка по платформам:
  - Twitch: `X из Y`
  - YouTube: `X из Y` (или "не подключён")
- Список полученных ачивок с датой (если хранить дату не нужно — просто список названий)
- Ближайшие незаработанные ачивки (топ-3 по близости к получению, если применимо)

**API:** `GET /api/stats` → `{ total, earned, byPlatform: { twitch: {total, earned}, youtube: {total, earned, locked} } }`

### 4.3 SettingsPage (настройки)

**Контент:**
- Секция "Twitch": показывает текущий логин, кнопка "Выйти"
- Секция "YouTube":
  - Если не привязан: описание + кнопка "Привязать YouTube" → редирект на `/auth/youtube/connect`
  - Если привязан: "YouTube подключён ✓" + кнопка "Отвязать"
- После отвязки YouTube — ачивки YouTube становятся заблокированными

**API:**
- `GET /api/me` → `{ twitchLogin, youtubeConnected }`
- `POST /auth/youtube/disconnect`
- `POST /auth/logout`

### 4.4 Navbar (компонент)

```
[LRS Achievements]   [Главная] [Статистика] [Настройки]   [Выйти / @twitchLogin]
```

---

## Фаза 5. Docker & Docker Compose

### 5.1 backend/app/Dockerfile

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Монтируем achievements.yml и images снаружи через volume
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 5.2 frontend/app/Dockerfile

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json .
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
# nginx конфиг для SPA (все пути → index.html) + проксирование /api на backend
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### 5.3 frontend/app/nginx.conf

```nginx
server {
    listen 80;

    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
    }

    location /auth/ {
        proxy_pass http://backend:8080/auth/;
        proxy_set_header Host $host;
    }
}
```

### 5.4 docker-compose.yml

```yaml
version: '3.9'

services:
  backend:
    build: ./backend/app
    environment:
      TWITCH_CLIENT_ID: ${TWITCH_CLIENT_ID}
      TWITCH_CLIENT_SECRET: ${TWITCH_CLIENT_SECRET}
      TWITCH_REDIRECT_URI: http://localhost/auth/twitch/callback
      YOUTUBE_CLIENT_ID: ${YOUTUBE_CLIENT_ID}
      YOUTUBE_CLIENT_SECRET: ${YOUTUBE_CLIENT_SECRET}
      YOUTUBE_REDIRECT_URI: http://localhost/auth/youtube/callback
      ACHIEVEMENTS_CONFIG_PATH: file:/config/achievements.yml
      ACHIEVEMENTS_IMAGES_PATH: file:/config/images/
    volumes:
      - ./config/achievements.yml:/config/achievements.yml:ro
      - ./config/images:/config/images:ro
    expose:
      - "8080"

  frontend:
    build: ./frontend/app
    ports:
      - "80:80"
    depends_on:
      - backend
```

### 5.5 .env файл (не коммитить в git!)

```
TWITCH_CLIENT_ID=xxx
TWITCH_CLIENT_SECRET=xxx
YOUTUBE_CLIENT_ID=xxx
YOUTUBE_CLIENT_SECRET=xxx
```

---

## Фаза 6. Конфигурация ачивок (YAML + картинки)

### 6.1 Структура achievements.yml

```yaml
achievements:
  - id: twitch_subscribe_channel_x
    name: "Подписчик канала X"
    description: "Подпишись на канал X на Twitch"
    imageFile: "subscribe_channel_x.png"
    platform: TWITCH
    rule: SUBSCRIPTION
    channelName: "channel_x"

  - id: twitch_first_message
    name: "Первое слово"
    description: "Напиши своё первое сообщение на канале X"
    imageFile: "first_message.png"
    platform: TWITCH
    rule: FIRST_MESSAGE
    channelName: "channel_x"

  - id: twitch_messages_10
    name: "Болтун"
    description: "Напиши 10 сообщений на канале X"
    imageFile: "messages_10.png"
    platform: TWITCH
    rule: MESSAGE_COUNT
    channelName: "channel_x"
    threshold: 10

  - id: twitch_streak_7
    name: "Недельный марафон"
    description: "Смотри стрим 7 дней подряд"
    imageFile: "streak_7.png"
    platform: TWITCH
    rule: VIEW_STREAK
    threshold: 7

  - id: youtube_playlist_one_video
    name: "Первый шаг"
    description: "Посмотри хотя бы одно видео из плейлиста Y"
    imageFile: "playlist_one.png"
    platform: YOUTUBE
    rule: PLAYLIST_ONE_VIDEO
    playlistId: "PLxxxxxxxx"

  - id: youtube_playlist_all
    name: "Марафонец"
    description: "Посмотри все видео из плейлиста Y"
    imageFile: "playlist_all.png"
    platform: YOUTUBE
    rule: PLAYLIST_ALL_VIDEOS
    playlistId: "PLxxxxxxxx"
```

### 6.2 Папка с картинками

```
config/
├── achievements.yml
└── images/
    ├── subscribe_channel_x.png
    ├── first_message.png
    ├── messages_10.png
    ├── streak_7.png
    ├── playlist_one.png
    └── playlist_all.png
```

- Рекомендуемый размер: 256×256 px или 512×512 px, формат PNG с прозрачностью
- Backend отдаёт картинки по URL: `GET /api/images/{filename}`
- Frontend ссылается на них как `/api/images/subscribe_channel_x.png`

---

## Backend API — итоговая таблица эндпоинтов

| Метод | URL | Описание | Auth |
|---|---|---|---|
| GET | `/auth/twitch/login` | Редирект на Twitch OAuth | Нет |
| GET | `/auth/twitch/callback` | Callback от Twitch | Нет |
| GET | `/auth/youtube/connect` | Редирект на Google OAuth | Twitch |
| GET | `/auth/youtube/callback` | Callback от YouTube | Twitch |
| POST | `/auth/youtube/disconnect` | Отвязать YouTube | Twitch |
| POST | `/auth/logout` | Выйти | Twitch |
| GET | `/api/me` | Текущий пользователь | Twitch |
| GET | `/api/achievements` | Список ачивок с earned | Twitch |
| GET | `/api/stats` | Статистика по ачивкам | Twitch |
| GET | `/api/images/{filename}` | Картинка ачивки | Нет |

---

## Порядок реализации (рекомендуемый)

```
1. [ ] Фаза 0: зарегистрировать приложения Twitch + YouTube
2. [ ] Фаза 1: настроить pom.xml, application.yml, базовые модели
3. [ ] Фаза 1: реализовать загрузку achievements.yml и отдачу картинок
4. [ ] Фаза 2: реализовать Twitch OAuth (login/callback/logout)
5. [ ] Фаза 4: реализовать минимальный фронт — кнопка логина + список ачивок
6. [ ] Фаза 2: реализовать проверку ачивок Twitch (подписка, стрик)
7. [ ] Фаза 2: реализовать YouTube OAuth (connect/callback/disconnect)
8. [ ] Фаза 2: реализовать проверку ачивок YouTube (плейлисты)
9. [ ] Фаза 4: страница настроек (привязка YouTube)
10. [ ] Фаза 4: страница статистики
11. [ ] Фаза 5: Dockerfile backend + frontend + docker-compose
12. [ ] Фаза 6: наполнить achievements.yml и добавить картинки
13. [ ] Итоговое тестирование end-to-end в Docker
```

---

## Ключевые архитектурные решения

| Вопрос | Решение | Причина |
|---|---|---|
| Хранилище данных | HTTP-сессия (in-memory) | Задача не требует БД, токены живут в сессии |
| Авторизация | Spring Security + сессионные куки | Простота, стандартный подход |
| Twitch история чата | EventSub WebSocket или заглушка | REST API Twitch не даёт историю |
| YouTube история просмотров | YouTube Activities API + liked videos | Полная история недоступна через API |
| CORS | Spring `@CrossOrigin` или `WebMvcConfigurer` | Vite dev server на другом порту |
| Картинки | Статические ресурсы Spring / volume в Docker | Простая раздача без CDN |

---

## Потенциальные проблемы и решения

1. **Twitch токен истекает** → реализовать refresh через `/helix/oauth2/token` с `grant_type=refresh_token`
2. **YouTube токен истекает** → хранить `refresh_token` в сессии, обновлять при 401
3. **Сессия теряется при перезапуске** → при необходимости переключить Spring Session на Redis (добавить `spring-session-data-redis` и Redis сервис в compose)
4. **Twitch история просмотров** → EventSub требует публичного URL; в dev-окружении использовать ngrok для тунелирования
5. **Rate limits API** → кэшировать результаты проверки ачивок в сессии на N минут
