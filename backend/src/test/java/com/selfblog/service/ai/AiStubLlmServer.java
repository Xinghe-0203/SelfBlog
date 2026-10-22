package com.selfblog.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试用 OpenAI 兼容上游（真实 HTTP 服务，绑 {@code 127.0.0.1:0} 临时端口）。
 *
 * <p>与 {@code com.selfblog.service.seo.StubLlmServer} 同一思路：把真实 HTTP 语义留在测试里，
 * 而不是 mock 掉 {@code HttpClient}——这里要覆盖的正是 400/422 重试、模型清单解析、
 * 多模态请求体形状这些只有真服务才暴露的分支。包级可见，所以 {@code service.ai} 包外的
 * 测试用不到它，也不依赖 {@code service.seo} 包里的同名工具。</p>
 */
final class AiStubLlmServer implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 一次应答。{@code delayMillis > 0} 用于构造超时场景。 */
    record Reply(int status, String body, long delayMillis) {
        static Reply ok(String content) {
            return new Reply(200, completion(content), 0);
        }

        static Reply status(int status, String body) {
            return new Reply(status, body, 0);
        }
    }

    /** 按调用序号决定应答（序号从 0 开始）。 */
    interface Responder {
        Reply respond(int callIndex, String requestBody) throws Exception;
    }

    private final HttpServer server;
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final List<String> authorizations = new CopyOnWriteArrayList<>();
    private final AtomicInteger calls = new AtomicInteger();

    /** {@code GET /v1/models} 的应答；默认 404，即「网关没实现这个端点」的真实常见情况。 */
    private volatile Reply modelsReply = Reply.status(404, "{\"error\":\"not found\"}");
    private final AtomicInteger modelCalls = new AtomicInteger();

    AiStubLlmServer(Responder responder) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", exchange -> {
            modelCalls.incrementAndGet();
            write(exchange, modelsReply);
        });
        server.createContext("/v1/chat/completions", exchange -> {
            String body;
            try (InputStream in = exchange.getRequestBody()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            bodies.add(body);
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            Reply reply;
            try {
                reply = responder.respond(calls.getAndIncrement(), body);
            } catch (Exception e) {
                reply = Reply.status(500, "{\"error\":\"stub failure\"}");
            }
            if (reply.delayMillis() > 0) {
                try {
                    Thread.sleep(reply.delayMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            write(exchange, reply);
        });
        server.start();
    }

    private static void write(com.sun.net.httpserver.HttpExchange exchange, Reply reply)
            throws IOException {
        byte[] bytes = reply.body() == null ? new byte[0] : reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(reply.status(), bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        } catch (IOException ignored) {
            // 客户端已断开（超时用例），真实上游同样会走到这里，无需断言。
        }
    }

    /** 可直接作为 {@code llmBaseUrl} 的地址（含 {@code /v1} 前缀）。 */
    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    int callCount() {
        return calls.get();
    }

    List<String> bodies() {
        return bodies;
    }

    List<String> authorizations() {
        return authorizations;
    }

    int modelCallCount() {
        return modelCalls.get();
    }

    void serveModels(String body) {
        this.modelsReply = Reply.status(200, body);
    }

    void rejectModels(int status, String body) {
        this.modelsReply = Reply.status(status, body);
    }

    /** 包一层 OpenAI 兼容的 chat completion 响应。 */
    static String completion(String content) {
        try {
            return JSON.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of("role", "assistant", "content", content)))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
