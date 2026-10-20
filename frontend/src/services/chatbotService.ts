import apiClient from "./apiClient";

export interface ChatbotProduct {
    productId: number;
    variantId: number;
    name: string;
    price: number;
    imageUrl: string | null;
}

export interface ContextualChatResponse {
    text: string;
    products: ChatbotProduct[];
}

export const chatbotService = {
    chat: (query: string, conversationId: string): Promise<string> =>
        apiClient.post("/chatbot/chat", undefined, {
            params: { query, conversationId },
        }) as unknown as Promise<string>,
    contextualChat: (query: string, conversationId: string, variantIds: number[] = []): Promise<ContextualChatResponse> =>
        apiClient.post("/chatbot/contextual-chat", { query, conversationId, variantIds }) as unknown as Promise<ContextualChatResponse>,
};
