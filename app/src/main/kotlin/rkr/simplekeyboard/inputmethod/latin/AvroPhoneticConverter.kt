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

package rkr.simplekeyboard.inputmethod.latin

/**
 * Avro Phonetic transliteration converter.
 * Converts Roman/Banglish input to Bengali Unicode text.
 * Based on the Avro Phonetic keyboard layout rules.
 */
object AvroPhoneticConverter {

    @JvmStatic
    fun isBengaliChar(c: Char): Boolean {
        return c in '\u0980'..'\u09FF'
    }

    private fun isVowelSign(c: Char): Boolean {
        return c == '\u09BE' || c == '\u09BF' || c == '\u09C0' ||
                c == '\u09C1' || c == '\u09C2' || c == '\u09C3' ||
                c == '\u09C4' || c == '\u09C7' || c == '\u09C8' ||
                c == '\u09CB' || c == '\u09CC' || c == '\u09CD'
    }

    private fun getVowelSign(vowel: String): String {
        return when (vowel) {
            "a" -> "\u09BE"   // া
            "i" -> "\u09BF"   // ি
            "I" -> "\u09C0"   // ী
            "u" -> "\u09C1"   // ু
            "U" -> "\u09C2"   // ূ
            "e" -> "\u09C7"   // ে
            "o" -> "\u09CB"   // ো
            "r" -> "\u09C3"   // ৃ (vocalic r)
            else -> ""
        }
    }

    private fun getFullVowel(vowel: String): String {
        return when (vowel) {
            "a" -> "\u0986"   // আ
            "i" -> "\u0988"   // ই
            "I" -> "\u098A"   // ঈ
            "u" -> "\u098C"   // ঌ
            "U" -> "\u098E"   // ঌ (unused in standard)
            "e" -> "\u098F"   // এ
            "o" -> "\u0993"   // ও
            "r" -> "\u098B"   // ঋ
            "ai" -> "\u0990"  // ঐ
            "au" -> "\u0994"  // ঔ
            else -> ""
        }
    }

    private fun convertChar(input: String, pos: Int): String? {
        if (pos >= input.length) return null

        val c = input[pos]

        if (pos + 1 < input.length) {
            val two = input.substring(pos, pos + 2)
            val result = convertTwoChar(two)
            if (result != null) return result
        }

        return convertSingleChar(c)
    }

    private fun convertTwoChar(two: String): String? {
        return when (two) {
            "kh" -> "\u0996"  // খ
            "gh" -> "\u0998"  // ঘ
            "ch" -> "\u099A"  // চ
            "Ch" -> "\u099B"  // ছ (Ch = chh)
            "sh" -> "\u09B6"  // শ
            "Sh" -> "\u09B7"  // ষ (Sh = shh)
            "th" -> "\u09A4"  // ত (aspirated)
            "Th" -> "\u09A5"  // থ
            "dh" -> "\u09A6"  // দ
            "Dh" -> "\u09A7"  // ধ
            "ph" -> "\u09AB"  // ফ
            "bh" -> "\u09AD"  // ভ
            "rh" -> "\u09DC"  // ড়
            "Rh" -> "\u09DD"  // ঢ়
            "ng" -> "\u0999"  // ঙ
            "ny" -> "\u099E"  // ঞ
            "ai" -> "\u0990"  // ঐ
            "au" -> "\u0994"  // ঔ
            "nk" -> "\u0999\u09CD\u0995" // ঙ্ক
            "nt" -> "\u09A8\u09CD\u09A4" // ন্ত
            "nd" -> "\u09A8\u09CD\u09A6" // ন্দ
            else -> null
        }
    }

    private fun convertSingleChar(c: Char): String? {
        return when (c) {
            'a' -> "\u0985"   // অ
            'i' -> "\u0987"   // ই
            'I' -> "\u0989"   // ঈ
            'u' -> "\u0989"   // উ
            'U' -> "\u098B"   // ঊ
            'e' -> "\u098F"   // এ
            'o' -> "\u0993"   // ও
            'r' -> "\u09B0"   // র (as consonant by default)
            'R' -> "\u098B"   // ঋ
            'k' -> "\u0995"   // ক
            'g' -> "\u0997"   // গ
            'G' -> "\u0998"   // ঘ (alternate)
            'c' -> "\u099A"   // চ
            'C' -> "\u099B"   // ছ (alternate)
            'j' -> "\u099C"   // জ
            'J' -> "\u099D"   // ঝ
            'T' -> "\u099F"   // ট
            'D' -> "\u09A1"   // ড
            'N' -> "\u09A3"   // ণ
            't' -> "\u09A4"   // ত
            'd' -> "\u09A6"   // দ
            'n' -> "\u09A8"   // ন
            'p' -> "\u09AA"   // প
            'f' -> "\u09AB"   // ফ (f = ph)
            'b' -> "\u09AC"   // ব
            'm' -> "\u09AE"   // ম
            'y' -> "\u09AF"   // য
            'l' -> "\u09B2"   // ল
            'v' -> "\u09AC"   // ব (v = b in Bengali)
            'w' -> "\u09AC"   // ব (w = b in Bengali)
            's' -> "\u09B8"   // স
            'S' -> "\u09B7"   // ষ (S = shh)
            'h' -> "\u09B9"   // হ
            ':' -> "\u0983"   // ঃ (visarga)
            '.' -> "\u0964"   // । (danda)
            '0' -> "\u09E6"   // ০
            '1' -> "\u09E7"   // ১
            '2' -> "\u09E8"   // ২
            '3' -> "\u09E9"   // ৩
            '4' -> "\u09EA"   // ৪
            '5' -> "\u09EB"   // ৫
            '6' -> "\u09EC"   // ৬
            '7' -> "\u09ED"   // ৭
            '8' -> "\u09EE"   // ৮
            '9' -> "\u09EF"   // ৯
            'Z' -> "\u09CD"   // ্
            else -> null
        }
    }

    @JvmStatic
    fun convert(input: String?): String? {
        if (input == null || input.isEmpty()) {
            return input
        }

        val result = StringBuilder()
        var i = 0

        while (i < input.length) {
            val c = input[i]

            if (!c.isLetter() && c != ':' && c != '.' && c != 'Z') {
                result.append(c)
                i++
                continue
            }

            var matched: String? = null
            var matchLen = 0

            if (i + 2 < input.length) {
                val three = input.substring(i, i + 3)
                matched = convertThreeChar(three)
                if (matched != null) matchLen = 3
            }

            if (matched == null && i + 1 < input.length) {
                val two = input.substring(i, i + 2)
                matched = convertTwoChar(two)
                if (matched != null) matchLen = 2
            }

            if (matched == null) {
                matched = convertSingleChar(c)
                if (matched != null) matchLen = 1
            }

            if (matched != null) {
                result.append(matched)
                i += matchLen
            } else {
                result.append(c)
                i++
            }
        }

        return result.toString()
    }

    private fun convertThreeChar(three: String): String? {
        return when (three) {
            "shh" -> "\u09B7"  // ষ
            "ngk" -> "\u0999\u09CD\u0995" // ঙ্ক
            "nth" -> "\u09A8\u09CD\u09A5" // ন্থ
            "nkh" -> "\u09A8\u09CD\u0996" // ন্খ
            else -> null
        }
    }
}
