package com.apex.wallet.domain.risk;

public interface RiskRule {
    String getRuleName();
    int getOrder();
    RiskAssessmentResult evaluate(RiskContext context);
}
