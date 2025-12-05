import {LucideIcon} from "lucide-react";

export type StatItem = {
  title: string;
  value: string;
  description: string;
  icon: LucideIcon;
  variant: 'success' | 'error' | 'warning' | 'info' | 'secondary';
  onClick?: () => void;
  className?: string;
};