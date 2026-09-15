package com.apex.wallet.domain.risk;

import java.time.Instant;

public record RiskAssessmentResult(
        RiskDecision decision,
        String reason,
        String ruleName,
        double riskScore,
        Instant evaluatedAt
) {
    public static RiskAssessmentResult approved(String ruleName) {
        return new RiskAssessmentResult(RiskDecision.APPROVED, "Transaction cleared standard AML/Risk checks", ruleName, 0.0, Instant.now());
    }

    public static RiskAssessmentResult flagged(String ruleName, String reason, double score) {
        return new RiskAssessmentResult(RiskDecision.FLAGGED_REVIEW, reason, ruleName, score, Instant.now());
    }

    public static RiskAssessmentResult rejected(String ruleName, String reason, double score) {
        return new RiskAssessmentResult(RiskDecision.REJECTED, reason, ruleName, score, Instant.now());
    }
}
