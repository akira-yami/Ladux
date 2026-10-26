
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./app/App.tsx";
import { ThemeProvider } from "./app/ThemeProvider";
import "./styles/index.css";
import { canonicalDevUrl } from "./utils/canonicalDevUrl";

const canonicalUrl = canonicalDevUrl(
    import.meta.env.DEV,
    import.meta.env.VITE_DEV_CANONICAL_ORIGIN,
    window.location.href,
    window.history.state?.usr?.from,
);

if (canonicalUrl) {
    window.location.replace(canonicalUrl);
} else {
    createRoot(document.getElementById("root")!).render(
        <ThemeProvider><BrowserRouter>
            <App />
        </BrowserRouter></ThemeProvider>
    );
}
