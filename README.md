# Resonant

> Enterprise AWS tag compliance platform with real-time monitoring and automated policy enforcement

[![License](https://img.shields.io/badge/License-Elastic_2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61dafb.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.3-blue.svg)](https://www.typescriptlang.org/)

---

## Overview

Resonant helps organizations maintain AWS tagging compliance at scale. Define policies, connect AWS
accounts, scan resources, and track violations—all from a centralized dashboard.

**Key Features:**

- Flexible tag policy engine with severity levels
- Multi-account AWS scanning via IAM role assumption
- Real-time compliance monitoring across S3, EC2, RDS, Lambda
- Violation tracking with ignore/reopen workflows
- Secure credential management with JWT authentication
- Compliance dashboards and reporting

---

## Screenshots

### Dashboard

![Dashboard](/docs/screenshots/dashboard.png)
*Overview of compliance metrics and recent scans*

### Tag Policies

![Tag Policies](/docs/screenshots/policies.png)
*Define and manage tagging requirements*

### Violations

![Violations](/docs/screenshots/violations.png)
*Track non-compliant resources with detailed remediation info*

### Platform Architecture

![PlatformArchitecture](/docs/screenshots/platformArchitecture.png)
*Example of how a deployed architecture could be set up*

---

## Tech Stack

### Backend

- **Framework:** Spring Boot 3.2 (Java 21)
- **Database:** PostgreSQL with Flyway migrations
- **Security:** Spring Security + JWT
- **AWS SDK:** v2 (STS, S3, EC2, RDS)
- **API Docs:** OpenAPI/Swagger

### Frontend

- **Framework:** React 18 + TypeScript
- **Build:** Vite
- **State:** Zustand + TanStack Query
- **UI:** Tailwind CSS + shadcn/ui components
- **Routing:** React Router v6

---

## Prerequisites

- **Java 21+** - [Download](https://adoptium.net/)
- **Node.js 18+** - [Download](https://nodejs.org/)
- **Docker** - [Download](https://www.docker.com/products/docker-desktop)
- **PostgreSQL** - (via Docker Compose)
- **AWS Account** - With IAM permissions for resource tagging

---

## Quick Start

### 1. Clone & Setup

```bash
git clone https://github.com/nwenner/resonant.git
cd resonant
```

### 2. Start Database

```bash
cd backend
docker-compose up -d
```

### 3. Configure Backend

Create `backend/.env`:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/resonant
DATABASE_USERNAME=resonant_user
DATABASE_PASSWORD=resonant_pass
JWT_SECRET=your-base64-encoded-secret
RESONANT_ENCRYPTION_KEY=your-base64-encoded-key  # Optional for access keys
```

### 4. Run Backend

```bash
cd backend
./gradlew bootRun
```

Backend runs on `http://localhost:8080`

### 5. Configure Frontend

Create `frontend/.env`:

```bash
VITE_API_URL=http://localhost:8080
```

### 6. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:3000`

---

## Usage

### 1. Create Account

Navigate to `http://localhost:3000/register` and create a user account.

### 2. Connect AWS Account

- Go to **AWS Accounts** page
- Provide AWS Account ID and IAM Role ARN
- Use cross-account role assumption (recommended) or access keys
- Test connection to verify

### 3. Define Tag Policies

- Go to **Tag Policies** page
- Create policies with required tags and allowed values
- Set severity levels (LOW, MEDIUM, HIGH, CRITICAL)
- Choose target resource types (s3:bucket, ec2:instance, etc.)

### 4. Scan Resources

- Select an AWS account
- Click **Scan Account**
- Monitor real-time progress
- View discovered resources and violations

### 5. Manage Violations

- Review non-compliant resources
- See missing/invalid tags
- Ignore false positives
- Track remediation progress

---

## AWS IAM Setup

### Automated Setup (Recommended)

Resonant includes a **CloudFormation wizard** that automatically creates the required IAM role with
proper permissions and trust policy.

1. Navigate to **AWS Accounts** → **Connect Account**
2. Click **Launch CloudFormation Template**
3. Review and create stack in AWS Console
4. Copy the Role ARN back to Resonant

The CloudFormation template creates:

- Cross-account IAM role with external ID
- Read-only permissions for tagging APIs
- Proper trust relationship

### Manual Setup

If you prefer manual configuration, create an IAM role with this trust policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::YOUR_RESONANT_ACCOUNT:root"
      },
      "Action": "sts:AssumeRole",
      "Condition": {
        "StringEquals": {
          "sts:ExternalId": "your-unique-external-id"
        }
      }
    }
  ]
}
```

**Required Permissions:**

```skills
// blah
// blah
}
```

---

## API Documentation

Swagger UI available at: `http://localhost:8080/swagger-ui.html`

---

## Development

### Run Tests

```bash
# Backend
cd backend
./gradlew test

# Frontend
cd frontend
npm test
```

### Build for Production

```bash
# Backend
./gradlew clean build

# Frontend
npm run build
```

### Database Migrations

Flyway migrations in `backend/src/main/resources/db/migration/`

```bash
# Migrations run automatically on startup
# To manually migrate:
./gradlew flywayMigrate
```

---

## Project Structure

```
resonant/
├── backend/
│   ├── src/main/java/com/wenroe/resonant/
│   │   ├── controller/       # REST endpoints
│   │   ├── service/          # Business logic
│   │   ├── repository/       # Data access
│   │   ├── model/entity/     # JPA entities
│   │   ├── dto/              # Request/Response objects
│   │   ├── security/         # JWT & auth
│   │   └── config/           # Spring configuration
│   ├── src/main/resources/
│   │   └── db/migration/     # Flyway SQL scripts
│   └── build.gradle
│
└── frontend/
    ├── src/
    │   ├── components/       # React components
    │   ├── pages/            # Route pages
    │   ├── store/            # Zustand stores
    │   ├── lib/              # API client & utils
    │   └── App.tsx
    └── package.json
```

---

## Environment Variables

### Backend (.env)

| Variable                  | Description                        | Required |
|---------------------------|------------------------------------|----------|
| `DATABASE_URL`            | PostgreSQL connection string       | Yes      |
| `DATABASE_USERNAME`       | Database user                      | Yes      |
| `DATABASE_PASSWORD`       | Database password                  | Yes      |
| `JWT_SECRET`              | Base64-encoded secret for JWT      | Yes      |
| `RESONANT_ENCRYPTION_KEY` | Key for encrypting AWS credentials | No*      |

*Only required if using access keys instead of IAM roles

### Frontend (.env)

| Variable       | Description          | Required |
|----------------|----------------------|----------|
| `VITE_API_URL` | Backend API base URL | Yes      |

---

## Roadmap

- [x] User authentication & authorization
- [x] Tag policy management
- [x] AWS account integration
- [x] S3 resource scanning
- [x] Compliance violation tracking
- [ ] Dev Resource
- [ ] EC2, RDS, Lambda scanners
- [ ] EC2 instance scheduler - Automated start/stop based on cron schedules to reduce costs
- [ ] Scheduled/background scans
- [ ] Automated remediation workflows
- [ ] Email notifications
- [ ] Cost allocation reports
- [ ] Multi-region support
- [ ] Audit logs

---

## Contributing

This is a portfolio/commercial project. If you'd like to contribute or discuss licensing, please
reach out.

---

## License

Copyright © 2025 Wenroe Technologies LLC

Licensed under the Elastic License 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.elastic.co/licensing/elastic-license

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

### What This Means

- ✅ **You can**: View the code, learn from it, fork for personal projects
- ✅ **You can**: Use internally at your company
- ❌ **You cannot**: Host this as a competing SaaS service
- ❌ **You cannot**: Remove licensing notices or rebrand as your own product

For commercial licensing inquiries, contact: [contact@wenroe.com]

---

## Support

- **Issues:** [GitHub Issues](https://github.com/nwenner/resonant/issues)
- **Documentation:** [docs.resonantcloud.dev](https://docs.resonantcloud.dev)
- **Website:** [resonantcloud.dev](https://resonantcloud.dev)

---

**Built & Maintained by Wenroe Technologies LLC**