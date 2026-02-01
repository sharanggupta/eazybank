package dev.sharanggupta.card.config;

import jakarta.annotation.Nonnull;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuditorAwareImpl implements ReactiveAuditorAware<String> {

    @Override
    @Nonnull
    public Mono<String> getCurrentAuditor() {
        return Mono.just(AuditConstants.SYSTEM_AUDITOR);
    }
}
