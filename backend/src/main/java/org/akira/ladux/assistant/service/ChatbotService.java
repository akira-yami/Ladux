package org.akira.ladux.assistant.service;

import java.util.List;
import java.util.Objects;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.entity.ProductVariant;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ProductRepository productRepository;
    private final ChatMemory chatMemory = MessageWindowChatMemory.builder().build();

    public ChatbotService(
            ChatClient.Builder builder,
            VectorStore vectorStore,
            ProductRepository productRepository
    ) {
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.vectorStore = vectorStore;
        this.productRepository = productRepository;
    }

    public String chat(String query, String conversationId) {
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(5).build()
        );

        List<Integer> ids = documents.stream()
                .map(document -> document.getMetadata().get("productId"))
                .filter(Objects::nonNull)
                .map(value -> Integer.parseInt(value.toString()))
                .distinct()
                .toList();

        List<Product> products = ids.isEmpty()
                ? List.of()
                : productRepository.findSummariesByIdIn(ids);

        String context = buildContext(products);

        return chatClient
                .prompt()
                .system("""
                        Bạn là trợ lý bán hàng của Ladux.

                        Chỉ sử dụng dữ liệu sản phẩm được cung cấp
                        để tư vấn về sản phẩm Ladux.

                        Nếu không có đủ thông tin thì hãy nói rõ
                        rằng hệ thống chưa có dữ liệu phù hợp.

                        Dữ liệu sản phẩm:
                        %s
                        """.formatted(context))
                .user(query)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    private String buildContext(List<Product> products) {
        StringBuilder context = new StringBuilder();

        for (Product product : products) {
            context.append("SẢN PHẨM\n");
            context.append("Tên: ").append(product.getName()).append("\n");
            context.append("Brand: ").append(product.getBrand().getName()).append("\n");
            context.append("CPU: ").append(product.getCpu()).append("\n");
            context.append("GPU: ").append(product.getGpu()).append("\n");

            for (ProductVariant variant : product.getVariants()) {
                context.append("- RAM: ").append(variant.getRam())
                        .append(", ROM: ").append(variant.getRom())
                        .append(", giá: ")
                        .append(variant.getDiscountPrice() != null
                                ? variant.getDiscountPrice()
                                : variant.getPrice())
                        .append(", tồn kho: ")
                        .append(variant.getStockQuantity())
                        .append("\n");
            }

            context.append("\n");
        }

        return context.toString();
    }
}
