package com.apex.wallet.application.risk;

import com.apex.wallet.domain.risk.RiskAssessmentResult;
import com.apex.wallet.domain.risk.RiskContext;
import com.apex.wallet.domain.risk.RiskRule;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SanctionsCheckRule implements RiskRule {

    private static final Set<String> SANCTIONED_KEYS = Set.of(
            "blocked@fraud.com",
            "sanctioned@ofac.gov",
            "golpe@phishing.net",
            "hacker@darknet.ru"
    );

    @Override
    public String getRuleName() {
        return "SANCTIONS_AND_BLACKLIST_RULE";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public RiskAssessmentResult evaluate(RiskContext context) {
        // 1. Debtor / Sender Sanctions Screening
        if (context.senderAccount() != null) {
            String senderEmail = context.senderAccount().getEmail();
            String senderPix = context.senderAccount().getPixKey();
            if ((senderEmail != null && SANCTIONED_KEYS.contains(senderEmail.toLowerCase())) ||
                (senderPix != null && SANCTIONED_KEYS.contains(senderPix.toLowerCase()))) {
                return RiskAssessmentResult.rejected(
                        getRuleName(),
                        "Conta de origem consta na lista restritiva de sanções e bloqueios cautelares.",
                        1.0
                );
            }
        }

        // 2. Creditor / Recipient Sanctions Screening
        if (context.targetPixKey() != null && SANCTIONED_KEYS.contains(context.targetPixKey().toLowerCase())) {
            return RiskAssessmentResult.rejected(
                    getRuleName(),
                    "Chave destinatária consta na lista restritiva de sanções e fraudes catalogadas.",
                    1.0
            );
        }
        return RiskAssessmentResult.approved(getRuleName());
    }
}
