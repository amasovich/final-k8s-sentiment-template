import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SentimentApplication {

    private static final int PORT = 8080;
    private static volatile long requestCount = 0;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // REST endpoint: /api/sentiment?text=...
        server.createContext("/api/sentiment", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Only GET is supported\"}");
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            String text = params.getOrDefault("text", "");
            String sentiment = analyzeSentiment(text);

            requestCount++;

            String json = String.format(
                    "{\"text\":\"%s\",\"sentiment\":\"%s\",\"score\":0.85}",
                    escapeJson(text), sentiment
            );

            sendJson(exchange, 200, json);
        });

        // Health endpoint: /health
        server.createContext("/health", exchange -> sendJson(exchange, 200, "{\"status\":\"UP\"}"));

        // Metrics endpoint: /metrics (Prometheus text format)
        server.createContext("/metrics", exchange -> {
            String bodyStr =
                    "# HELP sentiment_requests_total Total number of sentiment requests\n" +
                    "# TYPE sentiment_requests_total counter\n" +
                    "sentiment_requests_total " + requestCount + "\n";

            byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("SentimentApplication started on port " + PORT);
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;

        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = urlDecode(kv[0]);
            String value = kv.length > 1 ? urlDecode(kv[1]) : "";
            result.put(key, value);
        }
        return result;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String analyzeSentiment(String text) {
        String lower = text.toLowerCase();

        String[] positiveWords = {"good", "great", "love", "like", "awesome", "excellent", "happy"};
        String[] negativeWords = {"bad", "hate", "terrible", "awful", "sad", "angry"};

        int score = 0;
        for (String w : positiveWords) if (lower.contains(w)) score++;
        for (String w : negativeWords) if (lower.contains(w)) score--;

        if (score > 0) return "positive";
        if (score < 0) return "negative";
        return "neutral";
    }

    private static String escapeJson(String value) {
        return value.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
