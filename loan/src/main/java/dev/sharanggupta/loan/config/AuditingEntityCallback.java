package dev.sharanggupta.loan.config;

import dev.sharanggupta.loan.entity.BaseEntity;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class AuditingEntityCallback implements BeforeConvertCallback<Object> {

    private final ReactiveAuditorAware<String> auditorAware;

    public AuditingEntityCallback(ReactiveAuditorAware<String> auditorAware) {
        this.auditorAware = auditorAware;
    }

    @Override
    public Publisher<Object> onBeforeConvert(Object entity, SqlIdentifier table) {
        if (entity instanceof BaseEntity baseEntity) {
            return auditorAware.getCurrentAuditor()
                    .defaultIfEmpty(AuditConstants.ANONYMOUS_AUDITOR)
                    .map(auditor -> {
                        if (baseEntity.getCreatedAt() == null) {
                            baseEntity.setCreatedAt(LocalDateTime.now());
                            baseEntity.setCreatedBy(auditor);
                        }

                        baseEntity.setUpdatedAt(LocalDateTime.now());
                        baseEntity.setUpdatedBy(auditor);

                        return entity;
                    });
        }
        return Mono.just(entity);
    }
}
