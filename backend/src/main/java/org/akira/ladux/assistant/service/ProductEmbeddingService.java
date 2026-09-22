package org.akira.ladux.assistant.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.assistant.mapper.ProductDocumentMapper;
import org.akira.ladux.catalog.entity.Product;
import org.akira.ladux.catalog.repository.ProductRepository;
import org.akira.ladux.shared.exception.ResourceNotFoundException;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductEmbeddingService {
    private final ProductRepository productRepository;
    private final ProductDocumentMapper productDocumentMapper;
    private final VectorStore vectorStore;

    @Transactional
    public void indexProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id = " + id));
        vectorStore.add(List.of(productDocumentMapper.toDocument(product)));
    }

    @Transactional
    public int indexAllProducts() {
        List<Document> documents = productRepository.findAllForEmbedding().stream()
                .map(productDocumentMapper::toDocument)
                .toList();

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
        return documents.size();
    }
}
