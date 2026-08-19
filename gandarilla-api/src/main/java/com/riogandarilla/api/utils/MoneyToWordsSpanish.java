package com.riogandarilla.api.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class MoneyToWordsSpanish {

    private MoneyToWordsSpanish() {
    }

    public static String pesos(BigDecimal amount) {
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        long integerPart = normalized.longValue();
        int cents = normalized.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        String words = apocope(number(integerPart));
        String currency = integerPart == 1 ? "peso" : "pesos";
        return String.format(Locale.ROOT, "%s %s %02d/100 M.N.", words, currency, cents);
    }

    static String number(long value) {
        if (value < 0 || value > 999_999_999L) {
            throw new IllegalArgumentException("El monto debe estar entre 0 y 999,999,999");
        }
        if (value == 0) {
            return "cero";
        }
        if (value < 1000) {
            return underThousand((int) value);
        }
        if (value < 1_000_000) {
            int thousands = (int) (value / 1000);
            int remainder = (int) (value % 1000);
            String prefix = thousands == 1 ? "mil" : apocope(underThousand(thousands)) + " mil";
            return remainder == 0 ? prefix : prefix + " " + underThousand(remainder);
        }
        int millions = (int) (value / 1_000_000);
        int remainder = (int) (value % 1_000_000);
        String prefix = millions == 1 ? "un millón" : apocope(number(millions)) + " millones";
        return remainder == 0 ? prefix : prefix + " " + number(remainder);
    }

    private static String underThousand(int value) {
        if (value == 0) {
            return "";
        }
        if (value == 100) {
            return "cien";
        }
        if (value > 100) {
            int hundreds = value / 100;
            int remainder = value % 100;
            String prefix = switch (hundreds) {
                case 1 -> "ciento";
                case 2 -> "doscientos";
                case 3 -> "trescientos";
                case 4 -> "cuatrocientos";
                case 5 -> "quinientos";
                case 6 -> "seiscientos";
                case 7 -> "setecientos";
                case 8 -> "ochocientos";
                case 9 -> "novecientos";
                default -> "";
            };
            return remainder == 0 ? prefix : prefix + " " + underHundred(remainder);
        }
        return underHundred(value);
    }

    private static String underHundred(int value) {
        if (value < 10) {
            return switch (value) {
                case 1 -> "uno";
                case 2 -> "dos";
                case 3 -> "tres";
                case 4 -> "cuatro";
                case 5 -> "cinco";
                case 6 -> "seis";
                case 7 -> "siete";
                case 8 -> "ocho";
                case 9 -> "nueve";
                default -> "cero";
            };
        }
        if (value <= 29) {
            return switch (value) {
                case 10 -> "diez";
                case 11 -> "once";
                case 12 -> "doce";
                case 13 -> "trece";
                case 14 -> "catorce";
                case 15 -> "quince";
                case 16 -> "dieciséis";
                case 17 -> "diecisiete";
                case 18 -> "dieciocho";
                case 19 -> "diecinueve";
                case 20 -> "veinte";
                case 21 -> "veintiuno";
                case 22 -> "veintidós";
                case 23 -> "veintitrés";
                case 24 -> "veinticuatro";
                case 25 -> "veinticinco";
                case 26 -> "veintiséis";
                case 27 -> "veintisiete";
                case 28 -> "veintiocho";
                case 29 -> "veintinueve";
                default -> throw new IllegalStateException("Valor inesperado");
            };
        }
        int tens = value / 10;
        int units = value % 10;
        String prefix = switch (tens) {
            case 3 -> "treinta";
            case 4 -> "cuarenta";
            case 5 -> "cincuenta";
            case 6 -> "sesenta";
            case 7 -> "setenta";
            case 8 -> "ochenta";
            case 9 -> "noventa";
            default -> "";
        };
        return units == 0 ? prefix : prefix + " y " + underHundred(units);
    }

    private static String apocope(String value) {
        if (value.endsWith("veintiuno")) {
            return value.substring(0, value.length() - "veintiuno".length()) + "veintiún";
        }
        if (value.endsWith(" y uno")) {
            return value.substring(0, value.length() - " y uno".length()) + " y un";
        }
        if (value.endsWith("uno")) {
            return value.substring(0, value.length() - 3) + "un";
        }
        return value;
    }
}
