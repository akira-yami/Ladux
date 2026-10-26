import { describe, expect, it } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import ChatbotMessage from "./ChatbotMessage";

const render = (text: string) => renderToStaticMarkup(<ChatbotMessage text={text} />);

describe("chatbot message formatting", () => {
    it("renders Vietnamese headings, emphasis and lists without showing Markdown markers", () => {
        const html = render("### Gợi ý cho bạn\n\n**Ưu điểm** và *đánh đổi*\n\n- RAM 16 GB\n- SSD 512 GB\n\n1. Chọn cấu hình\n2. Xem chi tiết");

        expect(html).toContain("<h3>Gợi ý cho bạn</h3>");
        expect(html).toContain("<strong>Ưu điểm</strong>");
        expect(html).toContain("<em>đánh đổi</em>");
        expect(html).toContain("<ul>");
        expect(html).toContain("<li>RAM 16 GB</li>");
        expect(html).toContain("<ol>");
        expect(html).not.toContain("**");
        expect(html).not.toContain("###");
    });

    it("renders comparison tables with accessible headers inside a focusable scroll region", () => {
        const html = render("| Thông số | Máy A | Máy B |\n| --- | ---: | --- |\n| RAM | **16 GB** | 32 GB |\n| Pin | Chưa đủ dữ liệu | 50 Wh |");

        expect(html).toContain('role="region"');
        expect(html).toContain('tabindex="0"');
        expect(html).toContain('<th scope="col">Thông số</th>');
        expect(html).toContain('style="text-align:right" scope="col"');
        expect(html).toContain("<strong>16 GB</strong>");
        expect(html).toContain('<td style="text-align:right">Chưa đủ dữ liệu</td>');
        expect(html).not.toContain("| ---");
    });

    it("preserves greeting line breaks, plain text, and real hyphens in product names", () => {
        const html = render("Xin chào! 👋\nLaptop 2-in-1 dùng Wi-Fi 6.\nGiá khoảng 20 - 25 triệu.");

        expect(html).toContain("Xin chào! 👋<br/>");
        expect(html).toContain("2-in-1");
        expect(html).toContain("Wi-Fi 6.");
        expect(html).toContain("20 - 25 triệu.");
    });

    it("keeps code and block quotes readable", () => {
        const html = render("> Chưa đủ dữ liệu\n\nDùng `SmartFit`.\n\n```text\nRAM >= 16 GB\n```");

        expect(html).toContain("<blockquote>");
        expect(html).toContain("<code>SmartFit</code>");
        expect(html).toContain('<pre><code class="language-text">');
        expect(html).not.toContain("```");
    });

    it("ignores raw HTML and unsafe URLs, and does not load model-provided images", () => {
        const html = render('<script>alert(1)</script>\n\n<img src="x" onerror="alert(1)">\n\n[Bấm](javascript:alert%281%29)\n\n![Laptop](https://example.com/tracker.png)');

        expect(html).not.toContain("<script");
        expect(html).not.toContain("<img");
        expect(html).not.toContain("onerror");
        expect(html).not.toContain("javascript:");
        expect(html).not.toContain("tracker.png");
        expect(html).toContain("Bấm");
        expect(html).toContain("Laptop");
    });

    it("renders useful links with their labels and safe new-tab attributes", () => {
        const html = render("[Xem laptop](/products/1?variantId=101)");

        expect(html).toContain('href="/products/1?variantId=101"');
        expect(html).toContain('target="_blank" rel="noopener noreferrer"');
        expect(html).toContain(">Xem laptop</a>");
    });
});
