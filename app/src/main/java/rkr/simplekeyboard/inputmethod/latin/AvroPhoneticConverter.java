/*
 * Copyright (C) 2025 Raimondas Rimkus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rkr.simplekeyboard.inputmethod.latin;

/**
 * Avro Phonetic transliteration converter.
 * Converts Roman/Banglish input to Bengali Unicode text.
 * Based on the Avro Phonetic keyboard layout rules.
 */
public final class AvroPhoneticConverter {

    private AvroPhoneticConverter() {
        // Utility class
    }

    /**
     * Check if the given character is a Bengali Unicode character.
     */
    public static boolean isBengaliChar(char c) {
        return c >= '\u0980' && c <= '\u09FF';
    }

    /**
     * Check if the given character is a Bengali vowel sign (kar).
     */
    private static boolean isVowelSign(char c) {
        return (c == '\u09BE' || c == '\u09BF' || c == '\u09C0' ||
                c == '\u09C1' || c == '\u09C2' || c == '\u09C3' ||
                c == '\u09C4' || c == '\u09C7' || c == '\u09C8' ||
                c == '\u09CB' || c == '\u09CC' || c == '\u09CD');
    }

    /**
     * Get the vowel sign for a vowel character.
     */
    private static String getVowelSign(String vowel) {
        switch (vowel) {
            case "a": return "\u09BE";   // া
            case "i": return "\u09BF";   // ি
            case "I": return "\u09C0";   // ী
            case "u": return "\u09C1";   // ু
            case "U": return "\u09C2";   // ূ
            case "e": return "\u09C7";   // ে
            case "o": return "\u09CB";   // ো
            case "r": return "\u09C3";   // ৃ (vocalic r)
            default: return "";
        }
    }

    /**
     * Get the full vowel form (when at start of word or after vowel).
     */
    private static String getFullVowel(String vowel) {
        switch (vowel) {
            case "a": return "\u0986";   // আ
            case "i": return "\u0988";   // ই
            case "I": return "\u098A";   // ঈ
            case "u": return "\u098C";   // ঌ
            case "U": return "\u098E";   // ঌ (unused in standard)
            case "e": return "\u098F";   // এ
            case "o": return "\u0993";   // ও
            case "r": return "\u098B";   // ঋ
            case "ai": return "\u0990";  // ঐ
            case "au": return "\u0994";  // ঔ
            default: return "";
        }
    }

    /**
     * Convert a single character or pair to Bengali.
     * Returns null if no conversion found.
     */
    private static String convertChar(String input, int pos) {
        if (pos >= input.length()) return null;

        char c = input.charAt(pos);

        // Check for two-character combinations first
        if (pos + 1 < input.length()) {
            String two = input.substring(pos, pos + 2);
            String result = convertTwoChar(two);
            if (result != null) return result;
        }

        // Single character conversion
        return convertSingleChar(c);
    }

    /**
     * Convert two-character combinations.
     */
    private static String convertTwoChar(String two) {
        switch (two) {
            // Consonant + halant combinations (conjuncts)
            case "kh": return "\u0996";  // খ
            case "gh": return "\u0998";  // ঘ
            case "ch": return "\u099A";  // চ
            case "Ch": return "\u099B";  // ছ (Ch = chh)
            case "sh": return "\u09B6";  // শ
            case "Sh": return "\u09B7";  // ষ (Sh = shh)
            case "th": return "\u09A4";  // ত (aspirated)
            case "Th": return "\u09A5";  // থ
            case "dh": return "\u09A6";  // দ
            case "Dh": return "\u09A7";  // ধ
            case "ph": return "\u09AB";  // ফ
            case "bh": return "\u09AD";  // ভ
            case "rh": return "\u09DC";  // ড়
            case "Rh": return "\u09DD";  // ঢ়
            case "ng": return "\u0999";  // ঙ
            case "ny": return "\u099E";  // ঞ

            // Vowel combinations
            case "ai": return "\u0990";  // ঐ
            case "au": return "\u0994";  // ঔ

            // Special combinations
            case "ng": return "\u0999";  // ঙ
            case "nk": return "\u0999\u09CD\u0995"; // ঙ্ক
            case "nt": return "\u09A8\u09CD\u09A4"; // ন্ত
            case "nd": return "\u09A8\u09CD\u09A6"; // ন্দ
            case "shh": return "\u09B7"; // ষ
            default: return null;
        }
    }

    /**
     * Convert a single character.
     */
    private static String convertSingleChar(char c) {
        switch (c) {
            // Vowels
            case 'a': return "\u0985";   // অ
            case 'i': return "\u0987";   // ই
            case 'I': return "\u0989";   // ঈ
            case 'u': return "\u0989";   // উ
            case 'U': return "\u098B";   // ঊ
            case 'e': return "\u098F";   // এ
            case 'o': return "\u0993";   // ও
            case 'r': return "\u09B0";   // র (as consonant by default)
            case 'R': return "\u098B";   // ঋ

            // Consonants
            case 'k': return "\u0995";   // ক
            case 'g': return "\u0997";   // গ
            case 'G': return "\u0998";   // ঘ (alternate)
            case 'c': return "\u099A";   // চ
            case 'C': return "\u099B";   // ছ (alternate)
            case 'j': return "\u099C";   // জ
            case 'J': return "\u099D";   // ঝ
            case 'T': return "\u099F";   // ট
            case 'D': return "\u09A1";   // ড
            case 'N': return "\u09A3";   // ণ
            case 't': return "\u09A4";   // ত
            case 'd': return "\u09A6";   // দ
            case 'n': return "\u09A8";   // ন
            case 'p': return "\u09AA";   // প
            case 'f': return "\u09AB";   // ফ (f = ph)
            case 'b': return "\u09AC";   // ব
            case 'm': return "\u09AE";   // ম
            case 'y': return "\u09AF";   // য
            case 'l': return "\u09B2";   // ল
            case 'v': return "\u09AC";   // ব (v = b in Bengali)
            case 'w': return "\u09AC";   // ব (w = b in Bengali)
            case 's': return "\u09B8";   // স
            case 'S': return "\u09B7";   // ষ (S = shh)
            case 'h': return "\u09B9";   // হ
            case 'k': return "\u0995";   // ক

            // Special characters
            case ':': return "\u0983";   // ঃ (visarga)
            case '.': return "\u0964";   // । (danda)
            case 'N': return "\u09A3";   // ণ

            // Digits
            case '0': return "\u09E6";   // ০
            case '1': return "\u09E7";   // ১
            case '2': return "\u09E8";   // ২
            case '3': return "\u09E9";   // ৩
            case '4': return "\u09EA";   // ৪
            case '5': return "\u09EB";   // ৫
            case '6': return "\u09EC";   // ৬
            case '7': return "\u09ED";   // ৭
            case '8': return "\u09EE";   // ৮
            case '9': return "\u09EF";   // ৯

            // Halant (virama)
            case 'Z': return "\u09CD";   // ্

            default: return null;
        }
    }

    /**
     * Convert a Roman/Banglish string to Bengali Unicode.
     * This is a simplified Avro phonetic converter.
     *
     * @param input The Roman/Banglish input string
     * @return The converted Bengali Unicode string
     */
    public static String convert(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < input.length()) {
            char c = input.charAt(i);

            // Skip non-letter characters (except special chars)
            if (!Character.isLetter(c) && c != ':' && c != '.' && c != 'Z') {
                result.append(c);
                i++;
                continue;
            }

            // Try to match the longest possible sequence
            String matched = null;
            int matchLen = 0;

            // Try 3-character match first
            if (i + 2 < input.length()) {
                String three = input.substring(i, i + 3);
                matched = convertThreeChar(three);
                if (matched != null) matchLen = 3;
            }

            // Try 2-character match
            if (matched == null && i + 1 < input.length()) {
                String two = input.substring(i, i + 2);
                matched = convertTwoChar(two);
                if (matched != null) matchLen = 2;
            }

            // Try single character match
            if (matched == null) {
                matched = convertSingleChar(c);
                if (matched != null) matchLen = 1;
            }

            if (matched != null) {
                result.append(matched);
                i += matchLen;
            } else {
                // No conversion found, pass through
                result.append(c);
                i++;
            }
        }

        return result.toString();
    }

    /**
     * Convert three-character combinations.
     */
    private static String convertThreeChar(String three) {
        switch (three) {
            case "shh": return "\u09B7";  // ষ
            case "kh": return "\u0996";   // খ
            case "gh": return "\u0998";   // ঘ
            case "ch": return "\u099A";   // চ
            case "Ch": return "\u099B";   // ছ
            case "sh": return "\u09B6";   // শ
            case "Sh": return "\u09B7";   // ষ
            case "th": return "\u09A4";   // ত
            case "Th": return "\u09A5";   // থ
            case "dh": return "\u09A6";   // দ
            case "Dh": return "\u09A7";   // ধ
            case "ph": return "\u09AB";   // ফ
            case "bh": return "\u09AD";   // ভ
            case "rh": return "\u09DC";   // ড়
            case "Rh": return "\u09DD";   // ঢ়
            default: return null;
        }
    }
}
