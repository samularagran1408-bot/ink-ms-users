package com.inklusport.users.util;

import java.util.Locale;
import java.util.Set;

/**
 * Reglas compartidas de perfil inclusivo: discapacidades graves requieren acompañante.
 */
public final class DisabilityProfileRules {

    private static final Set<String> REQUIRES_COMPANION = Set.of("MOTRIZ", "AUDITIVA");

    private DisabilityProfileRules() {
    }

    public static boolean requiresCompanion(String disability) {
        if (disability == null || disability.isBlank()) {
            return false;
        }
        String normalized = disability.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("FISICA".equals(normalized) || "FISICA_MOTORA".equals(normalized)
                || "MOTORA".equals(normalized) || "PHYSICAL".equals(normalized)) {
            return true;
        }
        return REQUIRES_COMPANION.contains(normalized);
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
