package dev.sharanggupta.card.config;

import jakarta.annotation.Nonnull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String AUDITOR_HEADER = "X-User-ID";
    private static final String DEFAULT_AUDITOR = "ANONYMOUS";

    @Override
    @Nonnull
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader(AUDITOR_HEADER))
                .filter(header -> !header.isBlank())
                .or(() -> Optional.of(DEFAULT_AUDITOR));
    }
}
