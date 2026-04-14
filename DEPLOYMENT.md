# Deployment Guide — Render + Vercel

Deploy backend (Spring Boot) on Render and frontend (React) on Vercel.

---

## Architecture

```
Frontend (Vercel) → Backend API (Render) → PostgreSQL (Render)
```

---

## 1. Setup PostgreSQL (Render)

* Create new PostgreSQL instance
* Database: `campus_events`

Convert connection string:

```
postgres://user:pass@host:5432/campus_events
↓
jdbc:postgresql://host:5432/campus_events
```

---

## 2. Deploy Backend (Render)

Settings:

| Field          | Value                    |
| -------------- | ------------------------ |
| Root Directory | backend/event-management |
| Runtime        | Docker                   |

---

### Environment Variables

```
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/campus_events
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>

APP_SECURITY_JWTSECRET=<random-64-char>

CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app

JPA_DDL_AUTO=update
JWT_EXPIRATION_MS=3600000
```

---

## 3. Deploy Frontend (Vercel)

Settings:

| Field          | Value         |
| -------------- | ------------- |
| Root Directory | frontend      |
| Build Command  | npm run build |
| Output         | dist          |

---

### Env Variable

```
VITE_API_BASE_URL=https://your-backend.onrender.com
```

---

## 4. Connect CORS

Update backend env:

```
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
```

---

## 5. Verify

* Backend `/api/public/events` returns JSON
* Login works
* Events load
* Room booking works

---

## Notes

* Free tier may have cold starts (~30s)
* Redeploy frontend if API URL changes
* Use `JPA_DDL_AUTO=validate` in production
