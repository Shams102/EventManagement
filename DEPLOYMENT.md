# Deployment Guide — Render (Backend + PostgreSQL) + Vercel (Frontend)

This guide covers deploying the Event Management System to production using:

- **Render** — Backend (Spring Boot) + PostgreSQL database
- **Vercel** — Frontend (React/Vite)

---

## Architecture Overview

```
[Browser] → [Vercel: React Frontend] → [Render: Spring Boot API] → [Render: PostgreSQL]
```

| Component | Platform | URL Pattern |
|-----------|----------|-------------|
| Frontend  | Vercel   | `https://your-app.vercel.app` |
| Backend   | Render   | `https://your-api.onrender.com` |
| Database  | Render   | Internal connection string |

---

## Step 1: Create PostgreSQL Database on Render

1. Go to [dashboard.render.com](https://dashboard.render.com)
2. Click **New +** → **PostgreSQL**
3. Configure:
   - **Name**: `eventsphere-db`
   - **Database**: `campus_events`
   - **User**: `campus_user` (or leave default)
   - **Region**: Choose closest to your users
   - **Plan**: Free (for testing) or Starter ($7/mo for production)
4. Click **Create Database**
5. After creation, go to the database dashboard and note:
   - **Internal Database URL** (used by the backend, looks like: `postgres://user:pass@host/dbname`)
   - **External Database URL** (for local development access)

> **Important**: The Internal Database URL must be converted to JDBC format for Spring Boot:
> ```
> Internal URL: postgres://user:password@host:5432/campus_events
> JDBC URL:     jdbc:postgresql://host:5432/campus_events
> Username:     user
> Password:     password
> ```

---

## Step 2: Deploy Backend on Render

1. Go to [dashboard.render.com](https://dashboard.render.com)
2. Click **New +** → **Web Service**
3. Connect your GitHub repository
4. Configure:

| Setting | Value |
|---------|-------|
| **Name** | `eventsphere-api` |
| **Region** | Same as your database |
| **Root Directory** | `backend/event-management` |
| **Runtime** | `Docker` |
| **Plan** | Free (for testing) or Starter ($7/mo) |

5. Add **Environment Variables**:

| Variable | Value | Notes |
|----------|-------|-------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>:5432/campus_events` | From Step 1 (convert Internal URL to JDBC format) |
| `SPRING_DATASOURCE_USERNAME` | `campus_user` | From Step 1 |
| `SPRING_DATASOURCE_PASSWORD` | `<password>` | From Step 1 |
| `APP_SECURITY_JWTSECRET` | `<random-64-char-string>` | Run: `openssl rand -hex 32` |
| `JWT_EXPIRATION_MS` | `3600000` | 1 hour token expiry |
| `JPA_DDL_AUTO` | `update` | Use `validate` after initial setup |
| `CORS_ALLOWED_ORIGINS` | `https://your-app.vercel.app` | **Update after Vercel deploy** |

> **Note**: Render automatically injects the `PORT` environment variable. The app is configured to use it via `${PORT:8080}` in `application.yml`.

6. Click **Create Web Service**
7. Wait for the build and deploy to complete (first build takes ~5 minutes)
8. Note the service URL: `https://eventsphere-api.onrender.com`

### Alternative: Deploy without Docker

If you prefer not using Docker, configure Render with:

| Setting | Value |
|---------|-------|
| **Runtime** | `Java` |
| **Build Command** | `mvn -DskipTests clean package` |
| **Start Command** | `java -jar target/event-management-0.0.1-SNAPSHOT.jar` |

---

## Step 3: Deploy Frontend on Vercel

1. Go to [vercel.com](https://vercel.com) and sign in
2. Click **Add New** → **Project**
3. Import your GitHub repository
4. Configure:

| Setting | Value |
|---------|-------|
| **Root Directory** | `frontend` |
| **Framework Preset** | `Vite` (auto-detected) |
| **Build Command** | `npm run build` |
| **Output Directory** | `dist` |

5. Add **Environment Variable**:

| Variable | Value |
|----------|-------|
| `VITE_API_BASE_URL` | `https://eventsphere-api.onrender.com` |

> **Critical**: `VITE_*` variables are baked into the build at build time. If you change the backend URL, you **must redeploy** the frontend.

6. Click **Deploy**
7. Note the frontend URL: `https://your-app.vercel.app`

---

## Step 4: Connect Frontend to Backend (CORS)

After both services are deployed:

1. Go back to **Render** → your backend web service → **Environment**
2. Update the `CORS_ALLOWED_ORIGINS` variable:
   ```
   CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
   ```
   If you have multiple frontend URLs (e.g., custom domain):
   ```
   CORS_ALLOWED_ORIGINS=https://your-app.vercel.app,https://events.yourdomain.com
   ```
3. Click **Save Changes** — Render will automatically redeploy

---

## Step 5: Post-Deployment Verification Checklist

### ✅ Backend Health Check
- [ ] Visit `https://your-api.onrender.com/api/public/events` — should return JSON (empty array or seeded events)
- [ ] Check Render logs for `Started EventManagementApplication` message
- [ ] No database connection errors in logs

### ✅ Frontend Check
- [ ] Visit `https://your-app.vercel.app` — landing page loads
- [ ] Click "Sign In" — login page loads

### ✅ Authentication Check
- [ ] Login with `admin` / `Admin@123` — should redirect to `/dashboard`
- [ ] Login with `user` / `User@123` — should redirect to `/`
- [ ] Check browser DevTools Network tab — API calls go to Render backend

### ✅ CORS Check
- [ ] Open browser DevTools Console — no CORS errors
- [ ] API responses include `Access-Control-Allow-Origin` header

### ✅ Data Check
- [ ] Events page shows seeded events
- [ ] Room booking page loads rooms and buildings
- [ ] Timetable data is visible

---

## Environment Variables Reference

### Backend (Render)

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | ✅ | PostgreSQL JDBC URL | `jdbc:postgresql://host:5432/campus_events` |
| `SPRING_DATASOURCE_USERNAME` | ✅ | Database username | `campus_user` |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | Database password | `<secret>` |
| `APP_SECURITY_JWTSECRET` | ✅ | JWT signing key (≥32 chars) | `openssl rand -hex 32` |
| `CORS_ALLOWED_ORIGINS` | ✅ | Frontend URL(s), comma-separated | `https://app.vercel.app` |
| `JPA_DDL_AUTO` | ❌ | Hibernate DDL mode | `update` (default) |
| `JWT_EXPIRATION_MS` | ❌ | Token expiry (ms) | `3600000` (default: 1 hour) |
| `MAIL_HOST` | ❌ | SMTP server | `smtp.gmail.com` |
| `MAIL_PORT` | ❌ | SMTP port | `587` |
| `MAIL_USERNAME` | ❌ | SMTP username | `your@gmail.com` |
| `MAIL_PASSWORD` | ❌ | SMTP password | App Password |
| `TWILIO_ACCOUNT_SID` | ❌ | Twilio SID for SMS | |
| `TWILIO_AUTH_TOKEN` | ❌ | Twilio auth token | |
| `TWILIO_FROM_NUMBER` | ❌ | Twilio sender number | `+1234567890` |

### Frontend (Vercel)

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `VITE_API_BASE_URL` | ✅ | Backend API URL | `https://your-api.onrender.com` |

---

## Quick Start (Docker — Local Development)

```bash
# 1. Copy and configure environment
cp .env.example .env
# Edit .env — at minimum change POSTGRES_PASSWORD, APP_SECURITY_JWTSECRET

# 2. Build and start everything
docker compose up -d --build

# 3. Access the app
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080
```

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

- **Engine**: PostgreSQL 15+
- **Schema**: Auto-created by Hibernate (`ddl-auto: update`)
- **Default database**: `campus_events`
- **First run**: Tables are auto-created. A `DataInitializer` seeds demo users and sample events.
- **Production**: After initial setup, change `JPA_DDL_AUTO=validate` to prevent schema drift.

---

## Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| **CORS errors in browser** | Set `CORS_ALLOWED_ORIGINS` to your exact Vercel URL (include `https://`). Redeploy. |
| **"Connection refused" to PostgreSQL** | Verify `SPRING_DATASOURCE_URL` uses JDBC format: `jdbc:postgresql://host:5432/db`. Use Render's **Internal** URL. |
| **500 errors on API calls** | Check Render logs for stack traces. Common: missing env vars, wrong DB credentials. |
| **Frontend shows blank page** | Verify `VITE_API_BASE_URL` is set in Vercel **before** the build. Redeploy if changed. |
| **JWT errors after restart** | JWT secret must be consistent. Set `APP_SECURITY_JWTSECRET` as a permanent env var. |
| **Render free tier cold start** | Free tier spins down after 15 min of inactivity. First request takes ~30s to wake up. |
| **"Relation does not exist" error** | Set `JPA_DDL_AUTO=update` for the first deployment so Hibernate creates tables. |
| **Email notifications not sending** | Configure `MAIL_*` env vars with a real SMTP provider (e.g., Gmail App Password). |
| **Docker build fails on M1 Mac** | Add `platform: linux/amd64` to services in `docker-compose.yml`. |
