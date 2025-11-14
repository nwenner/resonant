import * as React from 'react';
import {cn} from '@/lib/utils';

interface DropdownMenuProps {
  trigger: React.ReactNode;
  children: React.ReactNode;
}

export const DropdownMenu: React.FC<DropdownMenuProps> = ({trigger, children}) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const menuRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  return (
      <div className="relative" ref={menuRef}>
        <div onClick={() => setIsOpen(!isOpen)}>
          {trigger}
        </div>

        {isOpen && (
            <div
                className="absolute right-0 mt-2 w-56 rounded-md shadow-lg bg-popover ring-1 ring-border z-50">
              <div className="py-1" role="menu">
                {children}
              </div>
            </div>
        )}
      </div>
  );
};

interface DropdownMenuItemProps {
  onClick?: () => void;
  children: React.ReactNode;
  className?: string;
}

export const DropdownMenuItem: React.FC<DropdownMenuItemProps> = ({
                                                                    onClick,
                                                                    children,
                                                                    className
                                                                  }) => {
  return (
      <button
          onClick={onClick}
          className={cn(
              'w-full text-left px-4 py-2 text-sm text-popover-foreground hover:bg-accent hover:text-accent-foreground flex items-center',
              className
          )}
          role="menuitem"
      >
        {children}
      </button>
  );
};

export const DropdownMenuSeparator = () => {
  return <div className="h-px my-1 bg-border"/>;
};
