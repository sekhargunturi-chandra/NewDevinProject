package com.capstone.fraudcase.tools;

import com.capstone.fraudcase.domain.dto.ToolEvidence;
import com.capstone.fraudcase.persistence.ToolTraceEntity;
import com.capstone.fraudcase.persistence.ToolTraceRepository;
import com.capstone.fraudcase.util.Hashes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Executes allow-listed tools inside a bounded timeout and records an
 * observable, redacted trace row for every invocation.
 */
@Component
public class ToolExecutor {

    private final ToolTraceRepository traceRepository;
    private final ObjectMapper objectMapper;
    private final long timeoutMs;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ToolExecutor(ToolTraceRepository traceRepository,
                        ObjectMapper objectMapper,
                        @org.springframework.beans.factory.annotation.Value("${capstone.tools.timeout-ms:2500}") long timeoutMs) {
        this.traceRepository = traceRepository;
        this.objectMapper = objectMapper;
        this.timeoutMs = timeoutMs;
    }

    public ToolEvidence execute(String caseId, String toolName, Map<String, Object> args,
                                Supplier<Map<String, Object>> tool) {
        String traceId = Hashes.nextId("TR");
        String evidenceId = Hashes.nextId("EV");
        String requestHash = Hashes.shortHash(toolName + "|" + args);
        long start = System.nanoTime();
        String status;
        Map<String, Object> result;
        try {
            Callable<Map<String, Object>> task = tool::get;
            Future<Map<String, Object>> future = executor.submit(task);
            result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            status = Boolean.FALSE.equals(result.get("found")) ? "NOT_FOUND" : "OK";
        } catch (TimeoutException e) {
            status = "TIMEOUT";
            result = Map.of("error", "tool timed out after " + timeoutMs + "ms");
        } catch (ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            status = "UNAVAILABLE";
            result = Map.of("error", "tool unavailable");
        }
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        ToolTraceEntity trace = new ToolTraceEntity();
        trace.setTraceId(traceId);
        trace.setCaseId(caseId);
        trace.setToolName(toolName);
        trace.setRequestHash(requestHash);
        trace.setStatus(status);
        trace.setDurationMs(durationMs);
        trace.setResultSummary(summarise(result));
        traceRepository.save(trace);

        return new ToolEvidence(evidenceId, traceId, toolName, status, durationMs, result, Instant.now());
    }

    private String summarise(Map<String, Object> result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            return json.length() > 1800 ? json.substring(0, 1800) : json;
        } catch (Exception e) {
            return String.valueOf(result);
        }
    }
}
