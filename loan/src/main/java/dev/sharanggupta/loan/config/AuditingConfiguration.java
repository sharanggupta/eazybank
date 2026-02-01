package dev.sharanggupta.loan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@Configuration
@EnableR2dbcAuditing
public class AuditingConfiguration {
    // AuditingEntityCallback and AuditorAwareImpl are auto-discovered via @Component
    // No explicit bean definitions needed
}
