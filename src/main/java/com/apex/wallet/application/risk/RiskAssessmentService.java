package com.apex.wallet.application.risk;

import com.apex.wallet.domain.risk.RiskAssessmentResult;
import com.apex.wallet.domain.risk.RiskContext;
import com.apex.wallet.domain.risk.RiskDecision;
import com.apex.wallet.domain.risk.RiskRejectedException;
import com.apex.wallet.domain.risk.RiskRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class RiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);

    private final List<RiskRule> rules;

    public RiskAssessmentService(List<RiskRule> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(RiskRule::getOrder))
                .toList();
    }

    public RiskAssessmentResult assessTransaction(RiskContext context) {
        log.info("Iniciando avaliação de risco Antifraude/AML para conta={} valor={}",
                context.senderAccount().getAccountNumber(), context.amount());

        for (RiskRule rule : rules) {
            RiskAssessmentResult result = rule.evaluate(context);
            if (result.decision() == RiskDecision.REJECTED) {
                log.warn("Transação REJEITADA pela regra [{}] para conta={}. Motivo: {}",
                        rule.getRuleName(), context.senderAccount().getAccountNumber(), result.reason());
                throw new RiskRejectedException(result);
            }
            if (result.decision() == RiskDecision.FLAGGED_REVIEW) {
                log.info("Transação SINALIZADA para auditoria pela regra [{}]", rule.getRuleName());
            }
        }

        log.info("Transação APROVADA em todas as regras de risco.");
        return new RiskAssessmentResult(RiskDecision.APPROVED, "Todas as regras de risco foram aprovadas", "CHAIN_APPROVED", 0.0, Instant.now());
    }
}
