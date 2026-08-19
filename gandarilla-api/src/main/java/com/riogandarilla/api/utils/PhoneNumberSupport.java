package com.riogandarilla.api.utils;

public final class PhoneNumberSupport {

    private PhoneNumberSupport() {
    }

    public static boolean isValidInternational(String phone) {
        try {
            normalizeInternational(phone);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static String normalizeInternational(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("El teléfono es obligatorio");
        }
        String normalized = phone.trim().replaceAll("[\\s+()\\-]", "");
        if (!normalized.matches("^[1-9][0-9]{9,14}$")) {
            throw new IllegalArgumentException("El teléfono no tiene un formato internacional válido");
        }
        return normalized;
    }

    public static String mask(String phone) {
        if (phone == null || phone.isBlank()) {
            return "sin teléfono";
        }
        String normalized = phone.replaceAll("[\\s+()\\-]", "");
        int visible = Math.min(4, normalized.length());
        return "*".repeat(Math.max(0, normalized.length() - visible))
                + normalized.substring(normalized.length() - visible);
    }
}
