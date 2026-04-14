# Event Management System

A full-stack event management platform for handling event creation, room booking, timetable constraints, and notifications.

---

## Tech Stack

### Frontend

* React.js (Vite)
* Tailwind CSS
* React Router
* Axios

### Backend

* Java 17
* Spring Boot 3.3.4
* Spring Security (JWT)
* Spring Data JPA
* PostgreSQL
* Flyway (DB migrations)

---

## Key Features

* **Authentication & RBAC**

  * GENERAL_USER, FACULTY, CLUB_ASSOCIATE, ADMIN

* **Event Management**

  * Create, edit, cancel events
  * Supports **single-day, multi-day, and overnight events**

* **Room Booking System**

  * Slot-based allocation
  * Conflict detection (rooms + timetable)
  * Admin approval workflow

* **Timetable Constraints**

  * Fixed class schedules
  * Building-level operating hours

* **Notifications**

  * In-app notifications
  * Email / SMS (optional config)

---

## Project Structure

```
EventManagement/
├── frontend/
├── backend/event-management/
├── docker-compose.yml
├── .env.example
├── DEPLOYMENT.md
```

---

## Getting Started

### Prerequisites

* Java 17+
* Node.js 18+
* PostgreSQL 15+

---

### Setup

```bash
git clone <repository-url>
cd EventManagement
```

### Backend

```bash
cd backend/event-management
mvn clean install
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

---

### Default URLs

* Frontend: http://localhost:3000
* Backend: http://localhost:8080

---

## Docker (Recommended)

```bash
cp .env.example .env
docker compose up -d --build
```

---

## Demo Accounts

| Username      | Password    | Role           |
| ------------- | ----------- | -------------- |
| central_admin | Central@123 | ADMIN          |
| faculty       | Faculty@123 | FACULTY        |
| club          | Club@123    | CLUB_ASSOCIATE |
| user          | User@123    | GENERAL_USER   |

---

## Deployment

See `DEPLOYMENT.md` for full deployment steps (Render + Vercel).

---

## Notes

* Uses Flyway for database migrations
* Supports multi-day and overnight booking logic
* Slot-based allocation ensures accurate room assignment

---

## License

MIT License
