package com.apex.wallet.application.risk;

import com.apex.wallet.domain.risk.RiskAssessmentResult;
import com.apex.wallet.domain.risk.RiskContext;
import com.apex.wallet.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HighValueThresholdRule implements RiskRule {

    private static final BigDecimal HARD_MAX_THRESHOLD = new BigDecimal("100000.00");
    private static final BigDecimal FLAGGED_THRESHOLD = new BigDecimal("30000.00");

    @Override
    public String getRuleName() {
        return "HIGH_VALUE_THRESHOLD_RULE";
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public RiskAssessmentResult evaluate(RiskContext context) {
        if (context.amount() != null && context.amount().compareTo(HARD_MAX_THRESHOLD) > 0) {
            return RiskAssessmentResult.rejected(
                    getRuleName(),
                    "Valor da transferência (R$ " + context.amount() + ") excede o limite regulatório instantâneo de R$ 100.000,00.",
                    0.99
            );
        }
        if (context.amount() != null && context.amount().compareTo(FLAGGED_THRESHOLD) > 0) {
            return RiskAssessmentResult.flagged(
                    getRuleName(),
                    "Transação de alto valor auditada para monitoramento secundário.",
                    0.4
            );
        }
        return RiskAssessmentResult.approved(getRuleName());
    }
}
