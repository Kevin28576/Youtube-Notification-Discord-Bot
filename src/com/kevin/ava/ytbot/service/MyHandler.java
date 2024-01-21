package service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MyHandler implements HttpHandler {
    @Override

    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            boolean apiEnabled = com.kevin.ava.ytbot.config.BotConfig.getEnableAPI();

            String mergedJsonContent = readJsonFiles("last_youtube_videos");

            System.out.println(mergedJsonContent);
            String response = apiEnabled ? mergedJsonContent : "API服務未啟用\n";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");

            int contentLength = response.getBytes(StandardCharsets.UTF_8).length;

            exchange.sendResponseHeaders(200, contentLength);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            exchange.sendResponseHeaders(500, 0);
        }
    }


    private String readJsonFiles(String directoryPath) throws IOException {
        Path directory = Paths.get(directoryPath);
        Map<String, String> jsonContentsMap = new LinkedHashMap<>();

        List<Map.Entry<String, String>> jsonContents = Files.list(directory)
                .filter(path -> path.toString().endsWith(".json"))
                .map(path -> {
                    String fileName = path.getFileName().toString();
                    fileName = fileName.substring(0, fileName.length() - 5);
                    return Map.entry(fileName, readFileContent(path));
                })
                .peek(entry -> jsonContentsMap.put(entry.getKey(), entry.getValue()))
                .toList();

        return formatJsonContents(jsonContentsMap);
    }

    private String readFileContent(Path filePath) {
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            String content = new String(fileBytes);
            //System.out.println("Read JSON content: " + content);
            return content;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String formatJsonContents(Map<String, String> jsonContentsMap) {
        StringBuilder formattedJsonContent = new StringBuilder();

        for (Map.Entry<String, String> entry : jsonContentsMap.entrySet()) {
            formattedJsonContent.append("\"").append(entry.getKey()).append("\": ").append(entry.getValue()).append(",\n");
        }

        if (formattedJsonContent.length() > 2) {
            formattedJsonContent.setLength(formattedJsonContent.length() - 2);
        }

        return "{\n" + formattedJsonContent.toString() + "\n}";
    }
}
