package org.example.documind.service.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.documind.service.interfaces.LLMService;
import org.example.documind.web.exception.LLMException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

public abstract class OpenAiCompatibleLLMService implements LLMService {
    private final String providerName;
    private final String url;
    private final String model;
    private final RestClient restClient;


    protected OpenAiCompatibleLLMService(String providerName, String url, String model, String apiKey) {
        this.providerName = providerName;
        this.url = url;
        this.model = model;
        RestClient.Builder builder = RestClient.builder();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.restClient = builder.build();
    }
    record Message(String role, String content) {}
    record CompletionRequest(String model, List<Message> messages, double temperature,
                       @JsonProperty("max_tokens") int maxTokens) {}
    record Response(List<Choice> choices) {}
    record Choice(Message message) {}


    @Override
    public String generate(String systemPrompt, List<ChatTurn> conversation) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        for (ChatTurn turn : conversation) {
            messages.add(new Message(turn.role().name().toLowerCase(), turn.content()));
        }

        CompletionRequest request = new CompletionRequest(model, messages, 0.2, 1024);

        try {
            Response response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Response.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new LLMException(providerName + " returned no answer");
            }
            return response.choices().get(0).message().content();
        }catch (HttpClientErrorException.TooManyRequests e){
            throw new LLMException(providerName + " rate limit reached", e);
        }catch (RestClientException e){
            throw new LLMException(providerName + " request failed", e);
        }
    }
}
