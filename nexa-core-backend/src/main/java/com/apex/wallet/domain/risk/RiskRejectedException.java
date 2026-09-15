package com.apex.wallet.domain.risk;

public class RiskRejectedException extends RuntimeException {
    private final RiskAssessmentResult riskResult;

    public RiskRejectedException(RiskAssessmentResult riskResult) {
        super("Transação bloqueada pelo Motor Antifraude/AML [" + riskResult.ruleName() + "]: " + riskResult.reason());
        this.riskResult = riskResult;
    }

    public RiskAssessmentResult getRiskResult() {
        return riskResult;
    }
}
