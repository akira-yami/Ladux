import React, { useState, useEffect, useRef } from "react";
import { X, Send, CheckCheck, ChevronRight, Laptop, ShoppingBag, Tag, Headphones } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";
import botAvatarImg from "../../assets/ladux/assistant.svg";
import { productPath } from "../../app/routePaths";
import { env } from "../../config/env";
import type { ChatbotProduct } from "../../services/chatbotService";
import { contextVariantIds } from "./chatbotContext";
import { chatbotService } from "../../services";
import ChatbotMessage from "./ChatbotMessage";

interface Message {
    id: string;
    sender: "bot" | "user";
    text: string;
    timestamp: string;
    products?: ChatbotProduct[];
}

const PRESET_PILLS = [
    { label: "Tư vấn laptop", query: "Tư vấn cho mình laptop đồ họa tầm 25 triệu nhé.", icon: Laptop },
    { label: "Kiểm tra đơn hàng", query: "Cho mình kiểm tra trạng thái đơn hàng.", icon: ShoppingBag },
    { label: "Khuyến mãi hôm nay", query: "Hôm nay cửa hàng có chương trình khuyến mãi gì?", icon: Tag },
    { label: "Liên hệ hỗ trợ", query: "Cho mình thông tin liên hệ hỗ trợ khách hàng.", icon: Headphones },
];

export default function AIChatbot() {
    const navigate = useNavigate();
    const location = useLocation();
    const [variantIds, setVariantIds] = useState<number[]>([]);
    useEffect(() => {
        const updateContext = (event: Event) => setVariantIds(contextVariantIds((event as CustomEvent<unknown>).detail));
        window.addEventListener("ladux:recommendation-context", updateContext);
        return () => window.removeEventListener("ladux:recommendation-context", updateContext);
    }, []);

    const [isOpen, setIsOpen] = useState(false);
    useEffect(() => {
        const open = () => setIsOpen(true);
        window.addEventListener("ladux:open-assistant", open);
        return () => window.removeEventListener("ladux:open-assistant", open);
    }, []);
    const [messages, setMessages] = useState<Message[]>([
        {
            id: "msg-1",
            sender: "bot",
            text: "Xin chào! 👋\nMình có thể hỗ trợ gì cho bạn hôm nay?",
            timestamp: getFormattedTime(),
        },
    ]);
    const [inputValue, setInputValue] = useState("");
    const [isTyping, setIsTyping] = useState(false);

    const messagesEndRef = useRef<HTMLDivElement>(null);
    const conversationIdRef = useRef(`chat-${Date.now()}-${Math.random().toString(36).slice(2)}`);

    function getFormattedTime() {
        const now = new Date();
        const hours = String(now.getHours()).padStart(2, "0");
        const minutes = String(now.getMinutes()).padStart(2, "0");
        return `${hours}:${minutes}`;
    }

    // Automatically close chatbot window when navigating to a new route
    useEffect(() => {
        setIsOpen(false);
    }, [location.pathname]);

    // Close chatbot window on Escape key press
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === "Escape" && isOpen) {
                setIsOpen(false);
            }
        };
        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [isOpen]);

    // Auto-scroll messages
    useEffect(() => {
        if (isOpen) {
            messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
        }
    }, [messages, isOpen, isTyping]);

    const handleSendMessage = async (textToSend?: string) => {
        const query = (textToSend || inputValue).trim();
        if (!query || query.length > 2000 || isTyping) return;

        const userMsg: Message = {
            id: `msg-${Date.now()}`,
            sender: "user",
            text: query,
            timestamp: getFormattedTime(),
        };

        setMessages((prev) => [...prev, userMsg]);
        if (!textToSend) setInputValue("");
        setIsTyping(true);

        try {
            const response = await chatbotService.contextualChat(query, conversationIdRef.current, variantIds);
            setMessages((prev) => [
                ...prev,
                {
                    id: `bot-${Date.now()}`,
                    sender: "bot",
                    text: response.text,
                    products: response.products,
                    timestamp: getFormattedTime(),
                },
            ]);
        } catch (error) {
            console.error("Không thể gọi chatbot:", error);
            setMessages((prev) => [
                ...prev,
                {
                    id: `bot-${Date.now()}`,
                    sender: "bot",
                    text: "Xin lỗi, chatbot đang tạm thời không phản hồi. Bạn vui lòng thử lại sau nhé!",
                    timestamp: getFormattedTime(),
                },
            ]);
        } finally {
            setIsTyping(false);
        }
    };

    return (
        <div className={`fixed z-50 flex flex-col items-center gap-3 select-none ${isOpen ? "bottom-5 right-5" : "bottom-6 right-3 sm:right-4"}`}>
            {/* 1. TOP: CHATBOT ICON BUTTON */}
            {!isOpen && (
                <button
                    onClick={() => setIsOpen(true)}
                    title="Bấm để mở Chatbot"
                    className="group relative flex h-12 w-12 sm:h-14 sm:w-14 flex-shrink-0 cursor-pointer items-center justify-center rounded-full overflow-hidden border border-sky-200 bg-white shadow-lg transition-transform hover:scale-110 focus:outline-none"
                >
                    <img
                        src={botAvatarImg}
                        alt="LADUX AI Assistant"
                        className="h-full w-full object-cover pointer-events-none"
                    />
                </button>
            )}

            {/* EXPANDED CHATBOT WINDOW */}
            {isOpen && (
                <div className="flex h-[min(580px,calc(100dvh-40px))] w-[calc(100vw-40px)] max-w-[420px] flex-col overflow-hidden rounded-[22px] border border-sky-100 bg-white shadow-sm animate-in fade-in zoom-in-95 duration-200">

                    {/* CHAT HEADER */}
                    <div className="flex items-center justify-between border-b border-sky-100 bg-white px-4 py-3 text-slate-800 shadow-sm">
                        <div className="flex items-center gap-3">
                            <div className="relative h-11 w-11 flex-shrink-0 overflow-hidden rounded-full border border-sky-200 bg-white shadow-sm">
                                <img
                                    src={botAvatarImg}
                                    alt="Avatar"
                                    className="h-full w-full object-cover"
                                />
                            </div>
                            <div className="flex flex-col">
                                <h3 className="text-base font-bold leading-snug tracking-wide text-slate-700">
                                    Hỗ trợ trực tuyến
                                </h3>
                                <div className="flex items-center gap-1.5 text-xs font-medium text-slate-500">
                                    <span className="h-2.5 w-2.5 rounded-full bg-sky-600 shadow-sm" />
                                    <span>Online</span>
                                </div>
                            </div>
                        </div>

                        <button
                            onClick={() => setIsOpen(false)}
                            className="flex h-8 w-8 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-sky-50 hover:text-slate-700"
                            title="Đóng Chatbot"
                        >
                            <X className="h-5 w-5" />
                        </button>
                    </div>

                    {variantIds.length > 0 && <div className="flex items-center justify-between gap-2 border-b border-sky-100 bg-sky-50 px-4 py-2 text-[11px] text-sky-800">
                        <span>Đang tư vấn {variantIds.length} cấu hình từ SmartFit / So sánh</span>
                        <button type="button" onClick={() => setVariantIds([])} className="shrink-0 font-semibold underline" aria-label="Xóa ngữ cảnh cấu hình">Xóa</button>
                    </div>}

                    {/* CHAT MESSAGES BODY */}
                    <div className="min-h-0 flex-1 w-full max-w-full space-y-3.5 overflow-y-auto bg-white p-3 custom-scrollbar">
                        {messages.map((msg) => (
                            <div key={msg.id} className="space-y-2 w-full max-w-full overflow-hidden">
                                {msg.sender === "bot" ? (
                                    <div className="flex min-w-0 w-full justify-start">
                                        <div className="min-w-0 w-full overflow-hidden rounded-[17px] rounded-tl-[5px] border border-sky-100 bg-white p-3 text-[13px] leading-[1.55] text-slate-700 shadow-sm">
                                            <ChatbotMessage text={msg.text} />

                                            {/* Product Suggestions Cards */}
                                            {msg.products && msg.products.length > 0 && (
                                                <div className="mt-3 w-full max-w-full space-y-2 overflow-hidden border-t border-sky-100 pt-2">
                                                    {msg.products.map((prod) => (
                                                        <button
                                                            type="button"
                                                            key={`${prod.productId}-${prod.variantId}`}
                                                            onClick={(e) => {
                                                                e.stopPropagation();
                                                                setIsOpen(false);
                                                                navigate(`${productPath(prod.productId)}?variantId=${prod.variantId}`);
                                                            }}
                                                            className="group flex w-full max-w-full cursor-pointer items-center gap-2.5 overflow-hidden rounded-xl border border-sky-100 bg-white p-2 text-left transition-all hover:border-sky-700 hover:bg-sky-50 focus-visible:outline-2 focus-visible:outline-sky-600"
                                                        >
                                                            <img
                                                                src={prod.imageUrl ? (/^https?:\/\//i.test(prod.imageUrl) ? prod.imageUrl : `${env.backendOrigin}/${prod.imageUrl.replace(/^\//, "")}`) : botAvatarImg}
                                                                alt={prod.name}
                                                                className="h-11 w-11 flex-shrink-0 rounded-lg bg-white p-1 object-contain"
                                                            />
                                                            <div className="flex-1 min-w-0 overflow-hidden">
                                                                <h4 className="max-w-full truncate text-xs font-semibold text-slate-700 transition-colors group-hover:text-sky-700">
                                                                    {prod.name}
                                                                </h4>
                                                                <p className="mt-0.5 text-xs font-bold text-sky-700">
                                                                    {prod.price.toLocaleString("vi-VN")} đ
                                                                </p>
                                                            </div>
                                                            <ChevronRight className="h-4 w-4 flex-shrink-0 text-slate-500 transition-colors group-hover:text-sky-700" />
                                                        </button>
                                                    ))}
                                                </div>
                                            )}

                                            <span className="mt-1.5 block text-right text-[10px] text-slate-500">
                                                {msg.timestamp}
                                            </span>
                                        </div>
                                    </div>
                                ) : (
                                    /* User Message */
                                    <div className="flex w-full justify-end">
                                        <div className="max-w-[85%] rounded-[17px] rounded-tr-[5px] border border-sky-200 bg-sky-50 p-3 text-[13px] font-medium leading-[1.55] text-slate-700 shadow-sm">
                                            <p className="whitespace-pre-line leading-relaxed break-words overflow-wrap-anywhere">
                                                {msg.text}
                                            </p>
                                            <div className="mt-1 flex items-center justify-end gap-1 text-[10px] text-slate-500">
                                                <span>{msg.timestamp}</span>
                                                <CheckCheck className="h-3.5 w-3.5 text-sky-700" />
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ))}

                        {/* PRESET QUICK PILLS - 2x2 GRID */}
                        <div className="pt-2 pb-1 grid grid-cols-2 gap-2 w-full">
                            {PRESET_PILLS.map((pill, idx) => {
                                const IconComponent = pill.icon;
                                return (
                                    <button
                                        key={idx}
                                        onClick={() => handleSendMessage(pill.query)}
                                        disabled={isTyping}
                                        className="flex w-full min-h-9 items-center gap-2 truncate rounded-full border border-sky-100 bg-white px-3 py-2 text-left text-[11px] font-medium text-slate-700 shadow-sm transition-all hover:border-sky-700 hover:bg-sky-50 active:scale-95"
                                    >
                                        <IconComponent className="h-4 w-4 flex-shrink-0 text-sky-700" />
                                        <span className="truncate">{pill.label}</span>
                                    </button>
                                );
                            })}
                        </div>

                        {/* Typing indicator */}
                        {isTyping && (
                            <div className="flex items-center gap-2 pl-2 text-xs text-slate-500">
                                <span className="h-2 w-2  rounded-full bg-sky-600" />
                                <span>Đang soạn phản hồi...</span>
                            </div>
                        )}

                        <div ref={messagesEndRef} />
                    </div>

                    {/* INPUT FOOTER AREA */}
                    <div className="w-full border-t border-sky-100 bg-white p-3">
                        <form
                            onSubmit={(e) => {
                                e.preventDefault();
                                handleSendMessage();
                            }}
                            className="flex items-center gap-2 w-full"
                        >
                            <input
                                type="text"
                                maxLength={2000}
                                aria-label="Câu hỏi cho trợ lý Ladux"
                                value={inputValue}
                                onChange={(e) => setInputValue(e.target.value)}
                                placeholder="Nhập câu hỏi của bạn..."
                                className="min-w-0 flex-1 rounded-full border border-sky-100 bg-white px-4 py-2.5 text-[13px] text-slate-700 placeholder-slate-500 transition-all focus:border-sky-700 focus:outline-none focus:ring-2 focus:ring-sky-700/20"
                            />
                            <button
                                type="submit"
                                disabled={!inputValue.trim() || isTyping}
                                className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full border border-sky-700 bg-sky-600 text-white shadow-sm transition-all hover:bg-sky-600 active:scale-95 disabled:opacity-40 disabled:hover:bg-sky-600"
                                title="Gửi tin nhắn"
                            >
                                <Send className="h-4 w-4 text-white" />
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* 2. MIDDLE: ZALO FLOATING BUTTON */}
            {!isOpen && (
                <a
                    href="https://zalo.me/0352060306"
                    target="_blank"
                    rel="noopener noreferrer"
                    title="Hỗ trợ qua Zalo"
                    className="hidden sm:flex h-12 w-12 sm:h-14 sm:w-14 flex-shrink-0 items-center justify-center rounded-full bg-[#0068ff] text-slate-800 shadow-sm transition-all duration-200 hover:scale-110 hover:bg-[#0052cc] active:scale-95 transform-gpu select-none"
                >
                    <svg
                        className="w-7 h-7 sm:w-8 sm:h-8 pointer-events-none select-none transform-gpu"
                        viewBox="0 0 118 60"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                    >
                        <path
                            d="M14 15H38L20 41H38V47H14V41L32 21H14V15Z M53 23C46.5 23 41 28.5 41 35C41 41.5 46.5 47 53 47C56.2 47 59.1 45.7 61 43.6V47H66V23H61V26.4C59.1 24.3 56.2 23 53 23ZM54 28.5C57.6 28.5 60.5 31.4 60.5 35C60.5 38.6 57.6 41.5 54 41.5C50.4 41.5 47.5 38.6 47.5 35C47.5 31.4 50.4 28.5 54 28.5Z M70 15H75.5V47H70V15Z M91 23C84.4 23 79 28.4 79 35C79 41.6 84.4 47 91 47C97.6 47 103 41.6 103 35C103 28.4 97.6 23 91 23ZM91 28.5C94.6 28.5 97.5 31.4 97.5 35C97.5 38.6 94.6 41.5 91 41.5C87.4 41.5 84.5 38.6 84.5 35C84.5 31.4 87.4 28.5 91 28.5Z"
                            fill="#FFFFFF"
                        />
                    </svg>
                </a>
            )}

            {/* 3. BOTTOM: TELEGRAM FLOATING BUTTON */}
            {!isOpen && (
                <a
                    href="https://t.me/phananhkhang"
                    target="_blank"
                    rel="noopener noreferrer"
                    title="Hỗ trợ qua Telegram"
                    className="hidden sm:flex h-12 w-12 sm:h-14 sm:w-14 flex-shrink-0 items-center justify-center rounded-full bg-[#2AABEE] shadow-sm transition-all duration-200 hover:scale-110 hover:bg-[#229edc] active:scale-95 transform-gpu select-none"
                >
                    <svg className="pointer-events-none select-none h-7 w-7 sm:h-8 sm:w-8 transform-gpu" viewBox="0 20 200 220" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M98 175c-3.887 0-3.227-1.464-4.568-5.17l-11.56-38.016 88.665-52.616c4.077-2.705.882-4.215-3.568-1.576l-109.52 69.176-35.313-11.036c-7.68-2.4-7.808-7.68 1.6-11.36l138.08-53.2c6.4-2.4 12 1.44 9.6 11.2l-23.52 110.88c-1.76 8.32-6.72 10.4-13.6 6.56l-38.88-28.64-18.72 18.08c-2.08 2.08-3.84 3.84-7.84 3.84z" fill="#ffffff" />
                    </svg>
                </a>
            )}
        </div>
    );
}
