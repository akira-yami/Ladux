import Markdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import remarkBreaks from "remark-breaks";
import "./ChatbotMessage.css";

const components: Components = {
    table: ({ children }) => (
        <div className="chatbot-table-scroll custom-scrollbar" role="region" aria-label="Bảng thông tin, cuộn ngang để xem thêm" tabIndex={0}>
            <table>{children}</table>
        </div>
    ),
    th: ({ node, ...props }) => <th {...props} scope="col" />,
    a: ({ children, href, title }) => href ? (
        <a href={href} title={title} target="_blank" rel="noopener noreferrer">{children}</a>
    ) : <span>{children}</span>,
    img: ({ alt }) => <span>{alt}</span>,
};

export default function ChatbotMessage({ text }: { text: string }) {
    return (
        <div className="chatbot-message select-text">
            <Markdown remarkPlugins={[remarkGfm, remarkBreaks]} components={components} skipHtml>
                {text}
            </Markdown>
        </div>
    );
}
