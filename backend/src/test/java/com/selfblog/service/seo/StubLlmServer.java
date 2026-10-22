package com.selfblog.service.seo;

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
 * 测试用 OpenAI 兼容上游：真实 HTTP 服务，绑到 {@code 127.0.0.1:0} 的临时端口。
 *
 * <p><b>为什么不用 Mockito mock 掉 HttpClient：</b>本任务最容易出错的正是
 * 「真实 HTTP 语义」—— 401/429/5xx 的状态码分支、{@code response_format} 被 400 拒绝后
 * 的重试、连接超时、响应体结构。把这些都 mock 掉，测的就只是自己的 if/else。
 * 用 JDK 自带的 {@code com.sun.net.httpserver} 起一个真服务，既能覆盖这些分支，
 * 又完全不依赖外网（测试必须在无网络环境可跑）。</p>
 *
 * <p>服务同时记录每个请求的 body 与 {@code Authorization} 头，便于断言
 * 「重试时去掉了 response_format」以及「请求确实带了 Bearer」。</p>
 */
final class StubLlmServer implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 一次应答。{@code delayMillis > 0} 用于构造超时场景。 */
    record Reply(int status, String body, long delayMillis) {
        static Reply ok(String content) {
            return new Reply(200, completion(content), 0);
        }

        static Reply status(int status, String body) {
            return new Reply(status, body, 0);
        }

        static Reply slow(long delayMillis, String content) {
            return new Reply(200, completion(content), delayMillis);
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

    /**
     * {@code GET /v1/models} 的应答。默认 404 —— 这恰好是「很多 OpenAI 兼容网关没有这个端点」
     * 的真实情况，让不关心模型列表的测试天然走回落分支。
     */
    private volatile Reply modelsReply = Reply.status(404, "{\"error\":\"not found\"}");
    private final AtomicInteger modelCalls = new AtomicInteger();
    private final List<String> modelAuthorizations = new CopyOnWriteArrayList<>();

    StubLlmServer(Responder responder) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", exchange -> {
            modelAuthorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            modelCalls.incrementAndGet();
            Reply reply = modelsReply;
            byte[] bytes = reply.body() == null ? new byte[0] : reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            } catch (IOException ignored) {
                // 同上：客户端断开无需断言。
            }
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
            byte[] bytes = reply.body() == null ? new byte[0] : reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            } catch (IOException ignored) {
                // 客户端已超时断开：真实上游也会有这个分支，测试不需要断言。
            }
        });
        server.start();
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

    /** 让 {@code GET /v1/models} 返回指定的 JSON。 */
    void serveModels(String body) {
        this.modelsReply = Reply.status(200, body);
    }

    /** 让 {@code GET /v1/models} 返回指定状态码（测鉴权/不可用分支）。 */
    void rejectModels(int status, String body) {
        this.modelsReply = Reply.status(status, body);
    }

    int modelCallCount() {
        return modelCalls.get();
    }

    List<String> modelAuthorizations() {
        return modelAuthorizations;
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
