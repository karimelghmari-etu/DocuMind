package org.example.documind.service.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.*;
import org.example.documind.domain.enums.MessageRole;
import org.example.documind.service.interfaces.LLMService;
import org.example.documind.web.exception.LLMException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "claude")
public class ClaudeLLMService implements LLMService {
    private final AnthropicClient client;
    private final String model;

    public ClaudeLLMService(@Value("${app.llm.claude.model}") String model, @Value("${app.llm.claude.api-key}") String apiKey){
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = model;
    }

    @Override
    public String generate(String systemPrompt, List<ChatTurn> conversation) {
        try {
            MessageCreateParams.Builder message = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(4096L)
                    .system(systemPrompt)
                    .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build());

            for (ChatTurn chatTurn : conversation) {
                if (chatTurn.role().equals(MessageRole.USER)) {
                    message.addUserMessage(chatTurn.content());
                }
                if (chatTurn.role().equals(MessageRole.ASSISTANT)) {
                    message.addAssistantMessage(chatTurn.content());
                }
            }
            Message response = client.messages().create(message.build());

            if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
                throw new LLMException("Claude refused to answer");
            }
            if (response.stopReason().map(StopReason.MAX_TOKENS::equals).orElse(false)) {
                throw new LLMException("Claude answer was cut off");
            }

            String result = response.content().stream().flatMap(b -> b.text().stream()).map(TextBlock::text).collect(Collectors.joining());
            if (result.isBlank()) {
                throw new LLMException("Claude returned no answer");
            }
            return result;
        }catch (RateLimitException e){
            throw new LLMException("Claude rate limit reached", e);
        }catch (AnthropicServiceException | AnthropicIoException e){
            throw new LLMException("Claude request failed", e);
        }
    }
}
