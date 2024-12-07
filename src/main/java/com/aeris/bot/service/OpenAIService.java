package com.aeris.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OpenAIService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.endpoint}")
    private String endpoint;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getResponse(String userMessage) {
        RequestBody body = createRequestBody(userMessage);

        Request request = new Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.body().string());
                return jsonResponse.at("/choices/0/message/content").asText();
            } else {
                return "Ошибка получения ответа от ChatGPT.";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Ошибка соединения с OpenAI.";
        }
    }

    private RequestBody createRequestBody(String userMessage) {
        String json = String.format(
                "{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                userMessage
        );
        return RequestBody.create(json, MediaType.parse("application/json"));
    }
}