# Frontend Setup Guide

Quick setup instructions for the Resonant frontend.

## 📋 Step-by-Step Setup

### 1. Create the Frontend Directory

From your `resonant/` root directory:

```bash
mkdir frontend
cd frontend
```

### 2. Initialize the Project

Copy all the provided files into the `frontend/` directory with this structure:

```
frontend/
├── src/
│   ├── components/
│   │   ├── ui/
│   │   │   ├── Button.tsx
│   │   │   ├── Card.tsx
│   │   │   └── Input.tsx
│   │   └── ProtectedRoute.tsx
│   ├── lib/
│   │   ├── api.ts
│   │   └── utils.ts
│   ├── pages/
│   │   ├── Dashboard.tsx
│   │   ├── Login.tsx
│   │   └── Register.tsx
│   ├── store/
│   │   └── authStore.ts
│   ├── App.tsx
│   ├── index.css
│   └── main.tsx
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
├── tsconfig.node.json
├── postcss.config.js
├── .eslintrc.cjs
├── .gitignore
└── README.md
```

### 3. Install Dependencies

```bash
npm install
```

This will install all required packages including React, TypeScript, Vite, Tailwind, and more.

### 4. Start the Backend

Make sure your Spring Boot backend is running:

```bash
cd ../backend
./gradlew bootRun
# or if using Docker Compose
docker-compose up -d
./gradlew bootRun
```

The backend should be running on `http://localhost:8080`.

### 5. Start the Frontend

In a new terminal, from the `frontend/` directory:

```bash
npm run dev
```

The frontend will be available at `http://localhost:3000`.

## 🧪 Testing the Setup

### 1. Register a New User

1. Navigate to `http://localhost:3000`
2. You'll be redirected to `/login`
3. Click "Sign up" link
4. Fill in the registration form:
   - Name: Test User
   - Email: test@example.com
   - Password: password123
5. Click "Create account"

### 2. Login

If registration succeeds, you'll be automatically logged in and redirected to the dashboard.

Alternatively, use the login form:
- Email: test@example.com
- Password: password123

### 3. Verify Dashboard

You should see:
- Welcome message with your name
- Four stat cards with mock data
- Recent non-compliant resources
- Tag policies with progress bars
- Logout button in the top right

## 🔧 Backend CORS Configuration

If you encounter CORS errors, add this to your Spring Boot backend:

```java
// backend/src/main/java/com/wenroe/resonant/config/WebConfig.java
package com.wenroe.resonant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
```

## 📊 Expected API Responses

### POST /api/auth/register

**Request:**
```json
{
  "name": "Test User",
  "email": "test@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "test@example.com",
    "name": "Test User",
    "role": "USER",
    "enabled": true
  }
}
```

### POST /api/auth/login

**Request:**
```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "test@example.com",
    "name": "Test User",
    "role": "USER",
    "enabled": true
  }
}
```

### GET /api/auth/me

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "test@example.com",
  "name": "Test User",
  "role": "USER",
  "enabled": true
}
```

## 🐛 Common Issues

### Port 3000 Already in Use

```bash
# Kill the process using port 3000
lsof -ti:3000 | xargs kill -9

# Or change the port in vite.config.ts
server: {
  port: 3001, // Change to any available port
}
```

### Module Not Found Errors

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Tailwind Styles Not Working

1. Ensure `index.css` is imported in `main.tsx`
2. Check that `tailwind.config.js` content paths are correct
3. Restart the dev server

### Backend Connection Issues

1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check Vite proxy configuration in `vite.config.ts`
3. Look for errors in browser DevTools → Network tab

### JWT Token Issues

1. Open DevTools → Application → Local Storage
2. Check for `token` and `user` entries
3. If invalid, clear localStorage and login again:
```javascript
// In browser console
localStorage.clear();
location.reload();
```

## 🎨 Customization

### Change Primary Color

Edit `tailwind.config.js` and `src/index.css`:

```javascript
// tailwind.config.js
theme: {
  extend: {
    colors: {
      primary: {
        DEFAULT: "hsl(262, 83%, 58%)", // Purple instead of blue
        foreground: "hsl(210, 40%, 98%)",
      },
    },
  },
}
```

### Add New Routes

```typescript
// src/App.tsx
<Route element={<ProtectedRoute />}>
  <Route path="/dashboard" element={<Dashboard />} />
  <Route path="/resources" element={<Resources />} />  // New route
  <Route path="/policies" element={<Policies />} />    // New route
</Route>
```

### Modify Dashboard Stats

Edit `src/pages/Dashboard.tsx` - the `stats` array:

```typescript
const stats = [
  {
    title: 'Your Custom Metric',
    value: '123',
    description: 'Custom description',
    icon: YourIcon,
    color: 'text-green-600',
    bgColor: 'bg-green-50',
  },
  // ...
];
```

## 🚀 Production Deployment

### Build for Production

```bash
npm run build
```

This creates optimized files in `dist/` directory.

### Serve with Nginx

Example Nginx configuration:

```nginx
server {
    listen 80;
    server_name resonant.yourdomain.com;
    root /var/www/resonant/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Environment Variables for Production

Create `.env.production`:

```env
VITE_API_URL=https://api.resonant.yourdomain.com
```

Update `src/lib/api.ts` to use environment variable:

```typescript
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});
```

## 📝 Development Workflow

### Recommended VS Code Extensions

- ESLint
- Prettier - Code formatter
- Tailwind CSS IntelliSense
- ES7+ React/Redux/React-Native snippets
- Auto Rename Tag

### Code Quality

```bash
# Lint your code
npm run lint

# Format with Prettier (add to package.json)
npm run format
```

### Git Workflow

```bash
git add .
git commit -m "feat: add user authentication"
git push origin main
```

## 🔐 Security Notes

1. **JWT Storage**: Currently using localStorage. For higher security, consider:
   - HttpOnly cookies
   - Secure session storage
   - Token refresh mechanism

2. **HTTPS**: Always use HTTPS in production

3. **Environment Variables**: Never commit `.env` files with secrets

4. **CORS**: Restrict allowed origins in production

5. **Input Validation**: Add client-side validation for all forms

## 📚 Additional Resources

- [Vite Documentation](https://vitejs.dev/)
- [React Router](https://reactrouter.com/)
- [TanStack Query](https://tanstack.com/query/)
- [Tailwind CSS](https://tailwindcss.com/)
- [shadcn/ui](https://ui.shadcn.com/)
- [Zustand](https://github.com/pmndrs/zustand)

## ✅ Verification Checklist

- [ ] Backend is running on port 8080
- [ ] PostgreSQL database is running
- [ ] `npm install` completed without errors
- [ ] Frontend dev server starts on port 3000
- [ ] Can register a new user
- [ ] Can login with registered user
- [ ] Dashboard displays correctly
- [ ] JWT token is stored in localStorage
- [ ] Logout redirects to login page
- [ ] Protected routes redirect when not authenticated
- [ ] No console errors in browser DevTools

## 🎯 Next Development Steps

1. **Real AWS Integration**
   - Add AWS SDK integration
   - Create resource listing pages
   - Implement tag scanning

2. **Tag Policy Management**
   - CRUD operations for policies
   - Policy editor UI
   - Compliance rules engine

3. **Reporting**
   - Compliance reports
   - Export to CSV/PDF
   - Email notifications

4. **User Management** (Admin only)
   - User list page
   - Edit user roles
   - Disable/enable users

5. **Enhanced Dashboard**
   - Real-time updates with WebSockets
   - Interactive charts
   - Filterable resource lists

Good luck with your Resonant platform! 🚀