package dev.sharanggupta.account.config;

/**
 * Constants used for entity auditing across the application.
 */
public final class AuditConstants {

    /**
     * Default auditor name when current user context is not available.
     * Used for system-initiated operations.
     */
    public static final String SYSTEM_AUDITOR = "SYSTEM";

    /**
     * Default auditor name when user context is not available and fallback is needed.
     */
    public static final String ANONYMOUS_AUDITOR = "ANONYMOUS";

    private AuditConstants() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
