
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./app/App.tsx";
import { ThemeProvider } from "./app/ThemeProvider";
import "./styles/index.css";

createRoot(document.getElementById("root")!).render(
    <ThemeProvider><BrowserRouter>
        <App />
    </BrowserRouter></ThemeProvider>
);
