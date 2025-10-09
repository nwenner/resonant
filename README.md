# Task Manager API

A comprehensive task management system built with Spring Boot, PostgreSQL, and JWT authentication.

## Features

- **User Authentication**: JWT-based authentication and authorization
- **Task Management**: Create, update, delete, and track tasks
- **Project Organization**: Organize tasks into projects
- **Collaboration**: Comments, attachments, and task assignments
- **Audit Trail**: Complete audit logging of all changes
- **RESTful API**: Well-documented REST endpoints
- **Security**: Role-based access control (RBAC)
- **API Documentation**: Swagger/OpenAPI integration

## Tech Stack

- **Backend**: Spring Boot 3.2.0
- **Database**: PostgreSQL 16
- **Security**: Spring Security + JWT
- **ORM**: Spring Data JPA + Hibernate
- **Build Tool**: Gradle 8.5
- **Java Version**: 17
- **Documentation**: SpringDoc OpenAPI
- **Testing**: JUnit 5, Testcontainers

## Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Gradle 8.5+ (or use the wrapper)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/nwenner/resonant
cd backend/docker
```

### 2. Start the Database

```bash
docker-compose up -d postgres
```

This will start:
- PostgreSQL on port 5432
- PgAdmin on port 5050 (optional, for database management)

### 3. Configure Environment Variables

Create a `.env` file or set environment variables:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=your-very-secure-secret-key-at-least-256-bits-long
```

### 4. Build the Project

```bash
./gradlew clean build
```

### 5. Run the Application

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The API will be available at: `http://localhost:8080/api/v1`

### 6. Access API Documentation

Open your browser and navigate to:
```
http://localhost:8080/api/v1/swagger-ui.html
```

## Development

### Running with Different Profiles

**Development Mode:**
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

**Test Mode:**
```bash
./gradlew test
```

### Database Management

**Access PgAdmin:**
- URL: http://localhost:5050
- Email: admin@taskmanager.com
- Password: admin

**Connect to Database:**
- Host: postgres (or localhost if connecting from host)
- Port: 5432
- Database: taskmanager
- Username: postgres
- Password: postgres

### Project Structure

```
src/main/java/com/taskmanager/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data Transfer Objects
├── entity/         # JPA entities
├── exception/      # Exception handling
├── mapper/         # DTO mappers
├── repository/     # Data repositories
├── security/       # Security configuration
├── service/        # Business logic
└── util/           # Utility classes
```

## API Endpoints

### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login and get JWT token
- `POST /auth/refresh` - Refresh access token

### Users
- `GET /users/me` - Get current user profile
- `PUT /users/me` - Update current user profile
- `GET /users` - Get all users (Admin only)

### Tasks
- `GET /tasks` - Get all tasks (with filters)
- `GET /tasks/{id}` - Get task by ID
- `POST /tasks` - Create new task
- `PUT /tasks/{id}` - Update task
- `DELETE /tasks/{id}` - Delete task
- `PATCH /tasks/{id}/status` - Update task status
- `PATCH /tasks/{id}/assign` - Assign task to user

### Projects
- `GET /projects` - Get all projects
- `GET /projects/{id}` - Get project by ID
- `POST /projects` - Create new project
- `PUT /projects/{id}` - Update project
- `DELETE /projects/{id}` - Delete project
- `GET /projects/{id}/tasks` - Get project tasks

### Comments
- `GET /tasks/{taskId}/comments` - Get task comments
- `POST /tasks/{taskId}/comments` - Add comment to task
- `PUT /comments/{id}` - Update comment
- `DELETE /comments/{id}` - Delete comment

### Attachments
- `GET /tasks/{taskId}/attachments` - Get task attachments
- `POST /tasks/{taskId}/attachments` - Upload attachment
- `DELETE /attachments/{id}` - Delete attachment

## Testing

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests TaskServiceTest
```

### Run Integration Tests
```bash
./gradlew test --tests *IntegrationTest
```

## Docker Deployment

### Build Docker Image
```bash
docker build -t resonant:latest .
```

### Run with Docker Compose
```bash
docker-compose up -d
```

This will start:
- Application container
- PostgreSQL database
- PgAdmin (optional)

### Stop Services
```bash
docker-compose down
```

### Clean Up Volumes
```bash
docker-compose down -v
```

## Security

### JWT Configuration

The application uses JWT tokens for authentication. Configure the secret key:

```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: 86400000  # 24 hours
  refresh-expiration: 604800000  # 7 days
```

**Important:** Always use a strong secret key (minimum 256 bits) in production.

### Role-Based Access Control

- **ADMIN**: Full system access
- **MANAGER**: Can manage projects and assign tasks
- **USER**: Can create and manage own tasks

## Monitoring

### Actuator Endpoints

- Health: `http://localhost:8080/api/v1/actuator/health`
- Info: `http://localhost:8080/api/v1/actuator/info`
- Metrics: `http://localhost:8080/api/v1/actuator/metrics`

## Troubleshooting

### Database Connection Issues

1. Check if PostgreSQL is running:
```bash
docker ps | grep postgres
```

2. Check logs:
```bash
docker logs taskmanager-postgres
```

3. Verify connection settings in `application.yml`

### JWT Token Issues

1. Ensure JWT secret is properly configured
2. Check token expiration time
3. Verify token format in Authorization header: `Bearer <token>`

### Build Issues

1. Clean and rebuild:
```bash
./gradlew clean build --refresh-dependencies
```

2. Clear Gradle cache:
```bash
rm -rf ~/.gradle/caches/
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Contact

For questions or support, please contact the development team.