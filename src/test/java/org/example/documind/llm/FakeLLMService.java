package org.example.documind.llm;

import org.example.documind.service.interfaces.LLMService;
import org.example.documind.service.llm.ChatTurn;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-only LLM, active when app.llm.provider=fake (set in the test application.properties).
 *
 * Tests can choose what it answers (willReply), make it fail (willFail),
 * and look at what it received (calls). Call reset() before each test, because Spring
 * shares this one bean between all tests.
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "fake")
public class FakeLLMService implements LLMService {

    /** What one generate(...) call received. */
    public record Call(String systemPrompt, List<ChatTurn> conversation) {}

    private String reply;
    private RuntimeException failure;
    private final List<Call> calls = new ArrayList<>();

    public FakeLLMService() {
        reset();
    }

    public void reset() {
        reply = "Fake answer [1].";
        failure = null;
        calls.clear();
    }

    public void willReply(String reply) {
        this.reply = reply;
        this.failure = null;
    }

    public void willFail(RuntimeException failure) {
        this.failure = failure;
    }

    public List<Call> calls() {
        return calls;
    }

    public Call lastCall() {
        return calls.get(calls.size() - 1);
    }

    @Override
    public String generate(String systemPrompt, List<ChatTurn> conversation) {
        calls.add(new Call(systemPrompt, List.copyOf(conversation)));
        if (failure != null) {
            throw failure;
        }
        return reply;
    }
}
