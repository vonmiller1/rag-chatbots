package com.example.rag.llm;

import com.example.rag.config.OpenRouterProperties;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Service
public class OpenRouterChatClient implements ChatClient {

    private final WebClient webClient;
    private final OpenRouterProperties properties;

    public OpenRouterChatClient(OpenRouterProperties properties) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String complete(String prompt) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("OPENROUTER_API_KEY is not configured.");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.model(),
                properties.temperature(),
                List.of(new Message("user", prompt))
        );

        ChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenRouter returned an empty response.");
        }

        return response.choices().getFirst().message().content().trim();
    }

    private record ChatCompletionRequest(
            String model,
            double temperature,
            List<Message> messages
    ) {
    }

    private record Message(String role, String content) {
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }
}
