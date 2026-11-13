package com.capstone.fraudcase.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the synthetic policy pack from {@code classpath:policies/*.md},
 * validates required metadata, splits on {@code ##} section headings and feeds
 * every configured policy retriever so all modes cite the same chunks.
 */
@Component
public class PolicyIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicyIngestionService.class);

    private final List<PolicyRetriever> retrievers;
    private final List<PolicyChunk> chunks = new ArrayList<>();

    public PolicyIngestionService(List<PolicyRetriever> retrievers) {
        this.retrievers = retrievers;
    }

    public List<PolicyChunk> chunks() {
        return List.copyOf(chunks);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:policies/*.md");
        for (Resource resource : resources) {
            parse(resource.getContentAsString(StandardCharsets.UTF_8));
        }
        for (PolicyRetriever retriever : retrievers) {
            retriever.index(chunks());
        }
        log.info("Ingested {} policy chunks from {} documents into {} retrievers",
                chunks.size(), resources.length, retrievers.size());
    }

    private void parse(String content) {
        Map<String, String> header = new LinkedHashMap<>();
        String body = content;
        if (content.startsWith("---")) {
            int end = content.indexOf("---", 3);
            for (String line : content.substring(3, end).split("\n")) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    header.put(line.substring(0, colon).trim(),
                            line.substring(colon + 1).trim().replaceAll("^\"|\"$", ""));
                }
            }
            body = content.substring(end + 3);
        }
        require(header, "documentId");
        require(header, "version");
        require(header, "effectiveDate");

        String section = "overview";
        StringBuilder text = new StringBuilder();
        int chunkIdx = 0;
        for (String line : body.split("\n")) {
            if (line.startsWith("## ")) {
                chunkIdx = flush(header, section, text, chunkIdx);
                section = line.substring(3).trim();
                text = new StringBuilder();
            } else {
                text.append(line).append('\n');
            }
        }
        flush(header, section, text, chunkIdx);
    }

    private int flush(Map<String, String> header, String section, StringBuilder text, int chunkIdx) {
        String cleaned = text.toString().trim();
        if (cleaned.isEmpty()) {
            return chunkIdx;
        }
        String chunkId = header.get("documentId").toLowerCase() + "-c" + chunkIdx;
        chunks.add(new PolicyChunk(
                header.get("documentId"),
                header.getOrDefault("title", header.get("documentId")),
                header.get("version"),
                header.get("effectiveDate"),
                header.getOrDefault("owner", "unknown"),
                section,
                chunkId,
                cleaned));
        return chunkIdx + 1;
    }

    private void require(Map<String, String> header, String key) {
        if (!header.containsKey(key) || header.get(key).isBlank()) {
            throw new IllegalStateException("policy document missing required metadata: " + key);
        }
    }
}
