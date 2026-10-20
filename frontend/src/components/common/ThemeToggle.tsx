import { Moon, Sun } from "lucide-react";
import { useTheme } from "../../app/ThemeProvider";

export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  const label = theme === "dark" ? "Bật giao diện sáng" : "Bật giao diện tối";
  return <button type="button" className="theme-toggle" onClick={toggleTheme} aria-label={label} title={label} aria-pressed={theme === "dark"}>
    {theme === "dark" ? <Sun size={18} strokeWidth={1.7} /> : <Moon size={18} strokeWidth={1.7} />}
  </button>;
}
