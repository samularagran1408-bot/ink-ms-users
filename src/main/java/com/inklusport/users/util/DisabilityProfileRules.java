package com.inklusport.users.util;

import java.util.Locale;
import java.util.Set;

/**
 * Reglas de perfil inclusivo según tipo de discapacidad:
 * <ul>
 *   <li>VISUAL, INTELECTUAL, COGNITIVA, MULTIPLE: acompañante obligatorio.</li>
 *   <li>MOTRIZ: acompañante opcional.</li>
 *   <li>AUDITIVA y sin discapacidad: no se exige acompañante.</li>
 * </ul>
 */
public final class DisabilityProfileRules {

    private static final Set<String> REQUIRES_COMPANION = Set.of(
            "VISUAL", "VISION",
            "INTELECTUAL", "INTELLECTUAL",
            "COGNITIVA", "COGNITIVE",
            "MULTIPLE", "MULTIPLE_DISABILITY"
    );

    private DisabilityProfileRules() {
    }

    public static boolean requiresCompanion(String disability) {
        String normalized = normalize(disability);
        return normalized != null && REQUIRES_COMPANION.contains(normalized);
    }

    public static void assertCompanionPresent(String disability,
                                              String companionFullName,
                                              String companionPhone) {
        if (!requiresCompanion(disability)) {
            return;
        }
        if (isBlank(companionFullName) || isBlank(companionPhone)) {
            throw new RuntimeException(
                    "Para discapacidad " + disability.trim().toUpperCase(Locale.ROOT)
                            + " el acompañante es obligatorio. "
                            + "Indique al menos nombre completo y teléfono de contacto.");
        }
    }

    private static String normalize(String disability) {
        if (disability == null || disability.isBlank()) {
            return null;
        }
        return disability.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
