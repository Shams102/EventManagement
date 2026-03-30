# Deployment Guide

## Quick Start (Docker — Recommended)

```bash
# 1. Copy and configure environment
cp .env.example .env
# Edit .env — at minimum change MYSQL_ROOT_PASSWORD, APP_SECURITY_JWTSECRET

# 2. Build and start everything
docker compose up -d --build

# 3. Access the app
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080
```

---

## Recommended Production Stack

| Component | Recommended | Alternative |
|-----------|-------------|-------------|
| Frontend  | **Vercel** (free tier) | Netlify, Cloudflare Pages |
| Backend   | **Railway** ($5/mo) | Render, Fly.io, AWS EC2 |
| Database  | **Railway MySQL** | PlanetScale, AWS RDS |

---

## Option A: Deploy Everything to Railway (Easiest Setup)
*This is the lowest-effort method because everything lives in one Railway project with minimal configuration.*

### 1. Database
1. Create a new project on [railway.app](https://railway.app)
2. Click **"+ New"** → **Database** → **MySQL**
3. Railway automatically sets up the environment connection variables for you.

### 2. Backend
1. In the same Railway project, click **"+ New"** → **GitHub Repo** and select this repository.
2. Go to **Settings** → Set the **Root Directory** to `backend/event-management`
3. Railway auto-detects the Dockerfile.
4. Go to **Variables** and add:
```text
SPRING_DATASOURCE_URL=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=${MYSQL_USER}
SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD}
APP_SECURITY_JWTSECRET=<any-random-32-char-string>
JPA_DDL_AUTO=update
CORS_ALLOWED_ORIGINS=https://<your-frontend>.up.railway.app
```
*(Leave `CORS_ALLOWED_ORIGINS` placeholder or update it later once the frontend gives you its URL.)*

### 3. Frontend
1. Back in the same Railway project, click **"+ New"** → **GitHub Repo** and select the **same** repository again.
2. Go to **Settings** → Set the **Root Directory** to `frontend`
3. Railway auto-detects the frontend Dockerfile.
4. Go to **Variables** and add:
```text
VITE_API_BASE_URL=https://<your-backend-railway-url>.up.railway.app
```
*(Make sure this is set **before** it finishes deploying, as this variable gets baked into the React build).*

*(Note for Nginx proxy: The `nginx.conf` proxy block in the frontend is safely ignored in this setup because the browser will call the frontend's built-in fetch calls directly using the URL provided).*

---

## Option B: Deploy to Railway (Backend) + Vercel (Frontend)

### 1. Database & Backend (Railway)
Follow the same **Database** and **Backend** steps from Option A above, but set `CORS_ALLOWED_ORIGINS` to your future Vercel URL:
```text
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
```

### 2. Frontend (Vercel)
1. Import this repo on [vercel.com](https://vercel.com)
2. Set **Root Directory** to `frontend`
3. Set **Build Command** to `npm run build`
4. Set **Output Directory** to `dist`
5. Add environment variable:
```text
VITE_API_BASE_URL=https://<your-backend-railway-url>.up.railway.app
```
6. Deploy

---

## Option B: Deploy with Docker Compose (VPS)

1. SSH into your server (Ubuntu 22+)
2. Install Docker and Docker Compose
3. Clone the repository
4. Configure `.env`:

```bash
cp .env.example .env
nano .env
```

**Must change:**
- `MYSQL_ROOT_PASSWORD` — strong password
- `SPRING_DATASOURCE_PASSWORD` — match the MySQL password
- `APP_SECURITY_JWTSECRET` — run `openssl rand -hex 32`
- `CORS_ALLOWED_ORIGINS` — your domain (e.g., `https://events.yourdomain.com`)
- `VITE_API_BASE_URL` — leave empty if using nginx proxy (default), or set to backend URL

5. Build and start:

```bash
docker compose up -d --build
```

6. Set up a reverse proxy (Nginx/Caddy) with SSL pointing to port 3000

---

## Environment Variables Reference

### Required
| Variable | Description | Example |
|----------|-------------|---------|
| `MYSQL_ROOT_PASSWORD` | MySQL root password | `s3cur3Pa$$w0rd` |
| `SPRING_DATASOURCE_PASSWORD` | Same as MySQL password | `s3cur3Pa$$w0rd` |
| `APP_SECURITY_JWTSECRET` | JWT signing key (≥32 chars) | `openssl rand -hex 32` |
| `CORS_ALLOWED_ORIGINS` | Frontend URL(s), comma-separated | `https://app.example.com` |

### Optional
| Variable | Description | Default |
|----------|-------------|---------|
| `BACKEND_PORT` | Host port for backend | `8080` |
| `FRONTEND_PORT` | Host port for frontend | `3000` |
| `JPA_DDL_AUTO` | Hibernate DDL mode | `update` |
| `JWT_EXPIRATION_MS` | Token expiry (ms) | `3600000` (1 hour) |
| `MAIL_HOST` | SMTP server | empty |
| `MAIL_USERNAME` | SMTP username | empty |
| `MAIL_PASSWORD` | SMTP password | empty |
| `VITE_API_BASE_URL` | Backend URL for browser | empty (uses nginx proxy) |

---

## Build & Run Commands

### Backend (standalone)
```bash
cd backend/event-management
mvn clean package -DskipTests
java -jar target/event-management-0.0.1-SNAPSHOT.jar
```

### Frontend (standalone)
```bash
cd frontend
npm install
npm run dev          # Development
npm run build        # Production build → dist/
npm run preview      # Preview production build
```

---

## Database

- **Engine**: MySQL 8.0
- **Schema**: Auto-created by Hibernate (`ddl-auto: update`)
- **Default database**: `campus_events`
- **First run**: Tables are auto-created. A `DataInitializer` seeds an admin user and sample room data.
- **Production**: After initial setup, change `JPA_DDL_AUTO=validate` to prevent schema drift.

---

## Common Issues

| Issue | Fix |
|-------|-----|
| CORS errors in browser | Set `CORS_ALLOWED_ORIGINS` to your exact frontend URL (include protocol) |
| "Connection refused" to MySQL | Ensure MySQL is running and port matches `SPRING_DATASOURCE_URL` |
| JWT errors after restart | JWT secret must be consistent; don't change it after users have tokens |
| Frontend shows blank page | Check `VITE_API_BASE_URL` is set correctly; ensure backend is reachable |
| Docker build fails on M1 Mac | Add `platform: linux/amd64` to services in docker-compose.yml |
| Email notifications not sending | Configure `MAIL_*` env vars with a real SMTP provider (e.g., Gmail App Password) |
