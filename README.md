# Event Management System

A comprehensive event management platform built with a modern tech stack to facilitate event creation, room booking, timetable management, and notifications.

## Tech Stack

### Frontend
- React.js
- Tailwind CSS
- React Router
- Axios for API calls
- Framer Motion for animations

### Backend
- Java 17
- Spring Boot 3.3.4
- Spring Security (JWT Authentication)
- Spring Data JPA
- PostgreSQL Database
- Maven
- Role-Based Access Control (RBAC)

## Features

- **User Authentication**: Login, registration with role-based access
- **Event Management**: Create, view, edit, and cancel events
- **Room Booking**: Book rooms with preference-based allocation
- **Timetable Management**: Fixed class schedules with conflict detection
- **Notifications**: In-app, email, and SMS notifications with threading
- **Role-Based Access Control**: 
  - GENERAL_USER: Basic access, event registration
  - FACULTY: Room booking, event creation
  - CLUB_ASSOCIATE: Club event management
  - ADMIN: Full system access, approvals

## Project Structure

```
EventManagement/
├── frontend/                     # React frontend application
├── backend/event-management/     # Spring Boot backend API
├── docker-compose.yml            # Docker Compose for local development
├── .env.example                  # Environment variables template
├── DEPLOYMENT.md                 # Full deployment guide
└── README.md                     # This file
```

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Node.js (v18 or higher)
- npm
- PostgreSQL 15+

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd EventManagement
```

2. Set up the backend:
```bash
cd backend/event-management
# Configure database connection in src/main/resources/application.yml
# Or set environment variables (see .env.example)
mvn clean install
```

3. Install frontend dependencies:
```bash
cd frontend
npm install
```

### Running the Application

1. Start PostgreSQL and create a database:
```bash
# Using psql or pgAdmin, create the database:
CREATE DATABASE campus_events;
```

2. Start the backend server:
```bash
cd backend/event-management
mvn spring-boot:run
```

3. Start the frontend development server:
```bash
cd frontend
npm run dev
```

The application will be available at:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080

### Using Docker Compose

```bash
cp .env.example .env
# Edit .env — at minimum change POSTGRES_PASSWORD and APP_SECURITY_JWTSECRET
docker compose up -d --build
```

## Demo Accounts

| Username | Password     | Role            |
|----------|-------------|-----------------|
| admin    | Admin@123   | ADMIN           |
| faculty  | Faculty@123 | FACULTY         |
| club     | Club@123    | CLUB_ASSOCIATE  |
| user     | User@123    | GENERAL_USER    |

## Authentication Flow

- New users register with default role: GENERAL_USER
- Login redirects based on user role:
  - GENERAL_USER → Home page (/)
  - FACULTY/CLUB_ASSOCIATE/ADMIN → Dashboard (/dashboard)

## Deployment

For detailed deployment instructions targeting **Render** (backend + PostgreSQL) and **Vercel** (frontend), see [DEPLOYMENT.md](DEPLOYMENT.md).

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.
