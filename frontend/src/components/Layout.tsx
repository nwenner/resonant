import {useAuthStore} from '@/store/authStore';
import {useThemeStore} from '@/store/themeStore';
import {useLocation, useNavigate} from 'react-router-dom';
import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuItem, DropdownMenuSeparator} from '@/components/ui/dropdown-menu';
import {
  BarChart3,
  ChevronDown,
  Cloud,
  LogOut,
  Moon,
  Settings,
  Shield,
  Sun,
  Tag,
  User
} from 'lucide-react';
import {ReactNode} from 'react';
import './Layout.css';

interface LayoutProps {
  children: ReactNode;
}

export const Layout = ({children}: LayoutProps) => {
  const {user, clearAuth} = useAuthStore();
  const {theme, toggleTheme} = useThemeStore();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  const navItems = [
    {path: '/dashboard', label: 'Dashboard', icon: BarChart3},
    {path: '/aws-accounts', label: 'AWS Accounts', icon: Cloud},
    {path: '/tag-policies', label: 'Tag Policies', icon: Shield},
    {path: '/settings', label: 'Settings', icon: Settings, disabled: true},
  ];

  return (
      <div className="layout-container">
        {/* Navigation Bar */}
        <nav className="nav-bar">
          <div className="nav-content">
            <div className="nav-inner">
              <div className="brand-container">
                <button
                    onClick={() => navigate('/dashboard')}
                    className="brand-button"
                    aria-label="Go to dashboard"
                >
                  <Tag className="brand-icon"/>
                  <span className="brand-text">Resonant</span>
                </button>

                {/* Navigation Links */}
                <div className="nav-links">
                  {navItems.map((item) => {
                    const Icon = item.icon;
                    const isActive = location.pathname === item.path;

                    return (
                        <Button
                            key={item.path}
                            variant={isActive ? 'default' : 'ghost'}
                            size="sm"
                            onClick={() => !item.disabled && navigate(item.path)}
                            disabled={item.disabled}
                            className={!isActive ? 'nav-link-inactive' : ''}
                        >
                          <Icon className="h-4 w-4 mr-2"/>
                          {item.label}
                        </Button>
                    );
                  })}
                </div>
              </div>

              <div className="user-menu-container">
                <DropdownMenu
                    trigger={
                      <Button variant="ghost" size="sm" className="user-menu-button">
                        <User className="h-4 w-4"/>
                        <span className="user-menu-name">{user?.name}</span>
                        {user?.role === 'ADMIN' && (
                            <span className="admin-badge">
                        Admin
                      </span>
                        )}
                        <ChevronDown className="h-4 w-4"/>
                      </Button>
                    }
                >
                  <div className="user-dropdown-header">
                    <p className="user-dropdown-name">{user?.name}</p>
                    <p className="user-dropdown-email">{user?.email}</p>
                  </div>

                  <DropdownMenuItem onClick={toggleTheme}>
                    {theme === 'light' ? (
                        <>
                          <Moon className="h-4 w-4 mr-2"/>
                          Dark Mode
                        </>
                    ) : (
                        <>
                          <Sun className="h-4 w-4 mr-2"/>
                          Light Mode
                        </>
                    )}
                  </DropdownMenuItem>

                  <DropdownMenuSeparator/>

                  <DropdownMenuItem onClick={handleLogout}>
                    <LogOut className="h-4 w-4 mr-2"/>
                    Logout
                  </DropdownMenuItem>
                </DropdownMenu>
              </div>
            </div>
          </div>
        </nav>

        {/* Main Content */}
        <main className="layout-main">
          {children}
        </main>
      </div>
  );
};