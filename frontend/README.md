# Resonant Frontend

React + TypeScript frontend for the Resonant AWS tag compliance platform.

## Tech Stack

- **React 18** with TypeScript
- **Vite** - Fast build tool and dev server
- **React Router** - Client-side routing
- **TanStack Query (React Query)** - Server state management
- **Zustand** - Lightweight client state management
- **Tailwind CSS** - Utility-first CSS framework
- **shadcn/ui** - High-quality UI components
- **Axios** - HTTP client
- **Lucide React** - Beautiful icon set

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── ui/              # Reusable UI components
│   │   │   ├── Button.tsx
│   │   │   ├── Input.tsx
│   │   │   └── Card.tsx
│   │   └── ProtectedRoute.tsx
│   ├── pages/
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   └── Dashboard.tsx
│   ├── store/
│   │   └── authStore.ts     # Zustand auth state
│   ├── lib/
│   │   ├── api.ts           # Axios config & API calls
│   │   └── utils.ts         # Utility functions
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── public/
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
└── README.md
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend server running on `http://localhost:8080`

### Installation

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

The app will be available at `http://localhost:3000`

### Build for Production

```bash
npm run build
```

The production build will be in the `dist/` directory.

## Features

### Authentication
- JWT-based authentication
- Login and registration pages
- Automatic token refresh on page reload
- Protected routes that redirect to login
- Logout functionality

### Dashboard
- Overview of AWS tag compliance metrics
- Real-time compliance statistics
- Recent non-compliant resources
- Active tag policies with progress bars
- User profile display with role badge

### API Integration
- Axios instance with request/response interceptors
- Automatic JWT token attachment to requests
- 401 error handling with automatic redirect
- Proxy configuration for backend API calls

## API Endpoints Used

The frontend integrates with these backend endpoints:

- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/auth/me` - Get current user
- `GET /api/users` - Get all users (admin)
- `GET /api/users/:id` - Get user by ID
- `PUT /api/users/:id` - Update user
- `DELETE /api/users/:id` - Delete user

## Configuration

### Vite Proxy

The Vite dev server is configured to proxy API requests to the backend:

```typescript
// vite.config.ts
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

This means requests to `http://localhost:3000/api/*` are forwarded to `http://localhost:8080/api/*`.

### Environment Variables

Create a `.env` file if you need to customize settings:

```env
VITE_API_URL=http://localhost:8080
```

## State Management

### Auth State (Zustand)

```typescript
const { user, isAuthenticated, setAuth, clearAuth } = useAuthStore();
```

- Persists to localStorage
- Automatically rehydrates on page load
- Exposes user info and authentication status

### Server State (React Query)

```typescript
const loginMutation = useMutation({
  mutationFn: authApi.login,
  onSuccess: (response) => {
    setAuth(response.data.user, response.data.token);
  },
});
```

- Handles API calls and caching
- Built-in loading and error states
- Automatic refetch strategies

## Styling

The app uses Tailwind CSS with a custom design system:

- Primary color: Blue (`#3B82F6`)
- Background: Slate gray (`#F8FAFC`)
- Component library: shadcn/ui components
- Responsive design with mobile-first approach

## Development

### Adding New Pages

1. Create page component in `src/pages/`
2. Add route in `src/App.tsx`
3. Use `<ProtectedRoute>` wrapper if authentication required

### Adding New API Endpoints

Add to `src/lib/api.ts`:

```typescript
export const resourcesApi = {
  getAll: () => api.get<Resource[]>('/resources'),
  // ... more endpoints
};
```

### Adding UI Components

Use the existing shadcn/ui patterns in `src/components/ui/`. Follow the same structure for consistency.

## Troubleshooting

### CORS Issues
If you see CORS errors, ensure your Spring Boot backend has proper CORS configuration for `http://localhost:3000`.

### 401 Unauthorized
- Check that the backend is running on port 8080
- Verify JWT token is being stored correctly
- Check browser DevTools → Application → Local Storage

### Proxy Not Working
- Ensure Vite dev server is running
- Check `vite.config.ts` proxy settings
- Restart the dev server after config changes

## Next Steps

Consider adding:
- AWS resource management pages
- Tag policy configuration UI
- Compliance reporting and charts
- Real-time notifications
- Admin user management
- Dark mode support
- Multi-account AWS setup

## License

Private - Wenroe/Resonant Platform