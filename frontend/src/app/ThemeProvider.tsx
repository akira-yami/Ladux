import { createContext, useContext, useEffect, useState, type ReactNode } from "react";

type Theme = "light" | "dark";
const storageKey = "ladux-theme";
const ThemeContext = createContext<{ theme: Theme; toggleTheme: () => void } | null>(null);

function readTheme(): Theme {
  try { return localStorage.getItem(storageKey) === "dark" ? "dark" : "light"; }
  catch { return "light"; }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(readTheme);

  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    try { localStorage.setItem(storageKey, theme); } catch { /* Storage may be disabled. */ }
  }, [theme]);

  useEffect(() => {
    const syncTheme = (event: StorageEvent) => {
      if (event.key === storageKey) setTheme(event.newValue === "dark" ? "dark" : "light");
    };
    window.addEventListener("storage", syncTheme);
    return () => window.removeEventListener("storage", syncTheme);
  }, []);

  return <ThemeContext.Provider value={{ theme, toggleTheme: () => setTheme(value => value === "light" ? "dark" : "light") }}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("useTheme must be used within ThemeProvider");
  return context;
}
