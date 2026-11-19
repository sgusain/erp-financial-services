package com.erp.ai.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.erp.ai.tool.client.TransactionView;

/**
 * Search by meaning over ERP transactions. "office supplies" can find "Staples printer paper"
 * because embeddings of similar meaning are close together in the vector store.
 * ERP data is company-wide (no per-user ownership), and the transactions are read with the caller's
 * own token, so ERP roles still decide what can be indexed.
 */
@Service
public class TransactionSearchService {

    static final String TYPE = "transaction";

    private final VectorStore vectorStore;
    private final ErpDataService erp;

    public TransactionSearchService(VectorStore vectorStore, ErpDataService erp) {
        this.vectorStore = vectorStore;
        this.erp = erp;
    }

    /** Re-indexes recent transactions. Old transaction chunks are removed first, so it never duplicates. */
    public int index(String token, int days) {
        List<TransactionView> transactions = erp.recentTransactions(token, null, days);
        vectorStore.delete("type == '" + TYPE + "'");
        List<Document> documents = transactions.stream().map(this::toDocument).toList();
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
        return documents.size();
    }

    public List<Document> search(String query, int limit) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(Math.min(Math.max(limit, 1), 20))
                .filterExpression("type == '" + TYPE + "'")
                .build());
    }

    private Document toDocument(TransactionView t) {
        String text = t.type() + " " + t.amount() + " - " + t.description();
        return new Document(text, Map.of(
                "type", TYPE,
                "sourceId", String.valueOf(t.id()),
                "accountId", String.valueOf(t.accountId())));
    }
}
