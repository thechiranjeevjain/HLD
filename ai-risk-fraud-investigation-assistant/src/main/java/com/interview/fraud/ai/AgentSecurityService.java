package com.interview.fraud.ai;

import com.interview.fraud.platform.AuditService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Deterministic security boundary around the non-deterministic model. */
@Service
public class AgentSecurityService {
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore (all |any )?(previous|prior|system) instructions?"),
            Pattern.compile("(?i)(reveal|print|return|send|upload).{0,40}(secret|password|token|api.?key|system prompt)"),
            Pattern.compile("(?i)(act as|you are now|developer message|system message)"),
            Pattern.compile("(?i)(curl|wget|https?://|webhook).{0,80}(secret|token|data|customer)"));
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(sk-[a-z0-9_-]{12,}|bearer\\s+[a-z0-9._-]{12,}|api[_-]?key\\s*[:=]\\s*[^\\s,}]+|password\\s*[:=]\\s*[^\\s,}]+)");

    private final AuditService audit;

    public AgentSecurityService(AuditService audit) {
        this.audit = audit;
    }

    public Inspection inspect(UUID caseId, String actor, InvestigationRequest request) {
        String userPrompt = request == null ? "" : safe(request.userPrompt());
        String document = request == null ? "" : safe(request.untrustedDocument());
        boolean injection = INJECTION_PATTERNS.stream().anyMatch(p -> p.matcher(userPrompt).find())
                || INJECTION_PATTERNS.stream().anyMatch(p -> p.matcher(document).find());
        if (injection) {
            audit.log(actor, "PROMPT_INJECTION_BLOCKED", "CASE", caseId.toString(),
                    "Untrusted instructions detected; content omitted from model context");
        }
        return new Inspection(injection, injection ? "BLOCKED_UNTRUSTED_INSTRUCTIONS" : "ALLOWED",
                injection ? "Potential instructions in user or retrieved content" : "No attack signature detected");
    }

    public String assertSafeOutput(UUID caseId, String actor, String output) {
        if (SECRET.matcher(safe(output)).find()) {
            audit.log(actor, "DATA_EXFILTRATION_BLOCKED", "CASE", caseId.toString(),
                    "Secret-like content removed at output boundary");
            throw new SecurityException("Model output blocked by data-loss-prevention policy");
        }
        return output;
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record Inspection(boolean blocked, String decision, String reason) {}
    public record InvestigationRequest(String userPrompt, String untrustedDocument) {}
}
