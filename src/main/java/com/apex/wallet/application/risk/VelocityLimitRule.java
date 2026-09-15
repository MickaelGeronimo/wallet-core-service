package com.apex.wallet.application.risk;

import com.apex.wallet.domain.risk.RiskAssessmentResult;
import com.apex.wallet.domain.risk.RiskContext;
import com.apex.wallet.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

@Component
public class VelocityLimitRule implements RiskRule {

    private static final int MAX_TX_PER_MINUTE = 5;

    @Override
    public String getRuleName() {
        return "VELOCITY_BURST_RULE";
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public RiskAssessmentResult evaluate(RiskContext context) {
        if (context.recentTransactionsInLastMinute() >= MAX_TX_PER_MINUTE) {
            return RiskAssessmentResult.rejected(
                    getRuleName(),
                    "Limite de frequência excedido: mais de " + MAX_TX_PER_MINUTE + " transações em menos de 60 segundos.",
                    0.95
            );
        }
        return RiskAssessmentResult.approved(getRuleName());
    }
}
