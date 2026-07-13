/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2025 Shunnuo
 * Copyright (C) 2025 GoodOldAbe
 * Copyright (C) 2025 Camille019
 * Copyright (C) 2023 Md. Rifat Hasan Jihan
 * Copyright (C) 2022 Md Rasel Hossain
 * Copyright (C) 2021 HanefiAcar
 * Copyright (C) 2021 wittmane
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

package rkr.simplekeyboard.inputmethod.latin.utils

import android.content.res.Resources
import rkr.simplekeyboard.inputmethod.R
import rkr.simplekeyboard.inputmethod.latin.Subtype
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils
import java.util.Locale

object SubtypeLocaleUtils {

    private const val LOCALE_AFRIKAANS = "af"
    private const val LOCALE_ARABIC = "ar"
    private const val LOCALE_AZERBAIJANI_AZERBAIJAN = "az_AZ"
    private const val LOCALE_BELARUSIAN_BELARUS = "be_BY"
    private const val LOCALE_BULGARIAN = "bg"
    private const val LOCALE_BENGALI_BANGLADESH = "bn_BD"
    private const val LOCALE_BENGALI_INDIA = "bn_IN"
    private const val LOCALE_CATALAN = "ca"
    private const val LOCALE_CZECH = "cs"
    private const val LOCALE_DANISH = "da"
    private const val LOCALE_GERMAN = "de"
    private const val LOCALE_GERMAN_SWITZERLAND = "de_CH"
    private const val LOCALE_GREEK = "el"
    private const val LOCALE_ENGLISH_INDIA = "en_IN"
    private const val LOCALE_ENGLISH_GREAT_BRITAIN = "en_GB"
    private const val LOCALE_ENGLISH_UNITED_STATES = "en_US"
    private const val LOCALE_ESPERANTO = "eo"
    private const val LOCALE_SPANISH = "es"
    private const val LOCALE_SPANISH_UNITED_STATES = "es_US"
    private const val LOCALE_SPANISH_LATIN_AMERICA = "es_419"
    private const val LOCALE_ESTONIAN_ESTONIA = "et_EE"
    private const val LOCALE_BASQUE_SPAIN = "eu_ES"
    private const val LOCALE_PERSIAN = "fa"
    private const val LOCALE_FINNISH = "fi"
    private const val LOCALE_FRENCH = "fr"
    private const val LOCALE_FRENCH_CANADA = "fr_CA"
    private const val LOCALE_FRENCH_SWITZERLAND = "fr_CH"
    private const val LOCALE_GALICIAN_SPAIN = "gl_ES"
    private const val LOCALE_HINDI = "hi"
    private const val LOCALE_CROATIAN = "hr"
    private const val LOCALE_HUNGARIAN = "hu"
    private const val LOCALE_ARMENIAN_ARMENIA = "hy_AM"
    private const val LOCALE_INDONESIAN_1 = "in"
    private const val LOCALE_INDONESIAN_2 = "id"
    private const val LOCALE_ICELANDIC = "is"
    private const val LOCALE_ITALIAN = "it"
    private const val LOCALE_ITALIAN_SWITZERLAND = "it_CH"
    private const val LOCALE_HEBREW_1 = "iw"
    private const val LOCALE_HEBREW_2 = "he"
    private const val LOCALE_GEORGIAN_GEORGIA = "ka_GE"
    private const val LOCALE_KAZAKH = "kk"
    private const val LOCALE_KHMER_CAMBODIA = "km_KH"
    private const val LOCALE_KANNADA_INDIA = "kn_IN"
    private const val LOCALE_KYRGYZ = "ky"
    private const val LOCALE_LAO_LAOS = "lo_LA"
    private const val LOCALE_LITHUANIAN = "lt"
    private const val LOCALE_LATVIAN = "lv"
    private const val LOCALE_MACEDONIAN = "mk"
    private const val LOCALE_MALAYALAM_INDIA = "ml_IN"
    private const val LOCALE_MONGOLIAN_MONGOLIA = "mn_MN"
    private const val LOCALE_MARATHI_INDIA = "mr_IN"
    private const val LOCALE_MALAY_MALAYSIA = "ms_MY"
    private const val LOCALE_NORWEGIAN_BOKMAL = "nb"
    private const val LOCALE_NEPALI_NEPAL = "ne_NP"
    private const val LOCALE_DUTCH = "nl"
    private const val LOCALE_DUTCH_BELGIUM = "nl_BE"
    private const val LOCALE_POLISH = "pl"
    private const val LOCALE_PORTUGUESE_BRAZIL = "pt_BR"
    private const val LOCALE_PORTUGUESE_PORTUGAL = "pt_PT"
    private const val LOCALE_ROMANIAN = "ro"
    private const val LOCALE_RUSSIAN = "ru"
    private const val LOCALE_SLOVAK = "sk"
    private const val LOCALE_SAKHA = "sah"
    private const val LOCALE_SLOVENIAN = "sl"
    private const val LOCALE_SERBIAN = "sr"
    private const val LOCALE_SERBIAN_LATIN = "sr_ZZ"
    private const val LOCALE_SWEDISH = "sv"
    private const val LOCALE_SWAHILI = "sw"
    private const val LOCALE_TAMIL_INDIA = "ta_IN"
    private const val LOCALE_TAMIL_SINGAPORE = "ta_SG"
    private const val LOCALE_TELUGU_INDIA = "te_IN"
    private const val LOCALE_THAI = "th"
    private const val LOCALE_TAGALOG = "tl"
    private const val LOCALE_TURKISH = "tr"
    private const val LOCALE_UKRAINIAN = "uk"
    private const val LOCALE_URDU = "ur"
    private const val LOCALE_UZBEK_UZBEKISTAN = "uz_UZ"
    private const val LOCALE_VIETNAMESE = "vi"
    private const val LOCALE_ZULU = "zu"

    private val sSupportedLocales = arrayOf(
        LOCALE_ENGLISH_UNITED_STATES,
        LOCALE_ENGLISH_GREAT_BRITAIN,
        LOCALE_AFRIKAANS,
        LOCALE_ARABIC,
        LOCALE_AZERBAIJANI_AZERBAIJAN,
        LOCALE_BELARUSIAN_BELARUS,
        LOCALE_BULGARIAN,
        LOCALE_BENGALI_BANGLADESH,
        LOCALE_BENGALI_INDIA,
        LOCALE_CATALAN,
        LOCALE_CZECH,
        LOCALE_DANISH,
        LOCALE_GERMAN,
        LOCALE_GERMAN_SWITZERLAND,
        LOCALE_GREEK,
        LOCALE_ENGLISH_INDIA,
        LOCALE_ESPERANTO,
        LOCALE_SPANISH,
        LOCALE_SPANISH_UNITED_STATES,
        LOCALE_SPANISH_LATIN_AMERICA,
        LOCALE_ESTONIAN_ESTONIA,
        LOCALE_BASQUE_SPAIN,
        LOCALE_PERSIAN,
        LOCALE_FINNISH,
        LOCALE_FRENCH,
        LOCALE_FRENCH_CANADA,
        LOCALE_FRENCH_SWITZERLAND,
        LOCALE_GALICIAN_SPAIN,
        LOCALE_HINDI,
        LOCALE_CROATIAN,
        LOCALE_HUNGARIAN,
        LOCALE_ARMENIAN_ARMENIA,
        LOCALE_INDONESIAN_1,
        LOCALE_INDONESIAN_2,
        LOCALE_ICELANDIC,
        LOCALE_ITALIAN,
        LOCALE_ITALIAN_SWITZERLAND,
        LOCALE_HEBREW_1,
        LOCALE_HEBREW_2,
        LOCALE_GEORGIAN_GEORGIA,
        LOCALE_KAZAKH,
        LOCALE_KHMER_CAMBODIA,
        LOCALE_KANNADA_INDIA,
        LOCALE_KYRGYZ,
        LOCALE_LAO_LAOS,
        LOCALE_LITHUANIAN,
        LOCALE_LATVIAN,
        LOCALE_MACEDONIAN,
        LOCALE_MALAYALAM_INDIA,
        LOCALE_MONGOLIAN_MONGOLIA,
        LOCALE_MARATHI_INDIA,
        LOCALE_MALAY_MALAYSIA,
        LOCALE_NORWEGIAN_BOKMAL,
        LOCALE_NEPALI_NEPAL,
        LOCALE_DUTCH,
        LOCALE_DUTCH_BELGIUM,
        LOCALE_POLISH,
        LOCALE_PORTUGUESE_BRAZIL,
        LOCALE_PORTUGUESE_PORTUGAL,
        LOCALE_ROMANIAN,
        LOCALE_RUSSIAN,
        LOCALE_SAKHA,
        LOCALE_SLOVAK,
        LOCALE_SLOVENIAN,
        LOCALE_SERBIAN,
        LOCALE_SERBIAN_LATIN,
        LOCALE_SWEDISH,
        LOCALE_SWAHILI,
        LOCALE_TAMIL_INDIA,
        LOCALE_TAMIL_SINGAPORE,
        LOCALE_TELUGU_INDIA,
        LOCALE_THAI,
        LOCALE_TAGALOG,
        LOCALE_TURKISH,
        LOCALE_UKRAINIAN,
        LOCALE_URDU,
        LOCALE_UZBEK_UZBEKISTAN,
        LOCALE_VIETNAMESE,
        LOCALE_ZULU
    )

    @JvmStatic
    fun getSupportedLocales(): List<String> {
        return sSupportedLocales.toList()
    }

    const val LAYOUT_ARABIC = "arabic"
    const val LAYOUT_ARMENIAN_PHONETIC = "armenian_phonetic"
    const val LAYOUT_AZERTY = "azerty"
    const val LAYOUT_BENGALI = "bengali"
    const val LAYOUT_BENGALI_AKKHOR = "bengali_akkhor"
    const val LAYOUT_BENGALI_AVRO = "bengali_avro"
    const val LAYOUT_BENGALI_UNIJOY = "bengali_unijoy"
    const val LAYOUT_BEPO = "bepo"
    const val LAYOUT_BULGARIAN = "bulgarian"
    const val LAYOUT_BULGARIAN_BDS = "bulgarian_bds"
    const val LAYOUT_EAST_SLAVIC = "east_slavic"
    const val LAYOUT_ERGOL = "ergol"
    const val LAYOUT_FARSI = "farsi"
    const val LAYOUT_GEORGIAN = "georgian"
    const val LAYOUT_GREEK = "greek"
    const val LAYOUT_HCESAR = "hcesar"
    const val LAYOUT_HEBREW = "hebrew"
    const val LAYOUT_HINDI = "hindi"
    const val LAYOUT_HINDI_COMPACT = "hindi_compact"
    const val LAYOUT_KANNADA = "kannada"
    const val LAYOUT_KHMER = "khmer"
    const val LAYOUT_LAO = "lao"
    const val LAYOUT_MACEDONIAN = "macedonian"
    const val LAYOUT_MALAYALAM = "malayalam"
    const val LAYOUT_MARATHI = "marathi"
    const val LAYOUT_MONGOLIAN = "mongolian"
    const val LAYOUT_NEPALI_ROMANIZED = "nepali_romanized"
    const val LAYOUT_NEPALI_TRADITIONAL = "nepali_traditional"
    const val LAYOUT_NORDIC = "nordic"
    const val LAYOUT_QWERTY = "qwerty"
    const val LAYOUT_QWERTZ = "qwertz"
    const val LAYOUT_SAKHA = "sakha"
    const val LAYOUT_SERBIAN = "serbian"
    const val LAYOUT_SERBIAN_QWERTZ = "serbian_qwertz"
    const val LAYOUT_SPANISH = "spanish"
    const val LAYOUT_SWISS = "swiss"
    const val LAYOUT_TAMIL = "tamil"
    const val LAYOUT_TELUGU = "telugu"
    const val LAYOUT_THAI = "thai"
    const val LAYOUT_TURKISH_Q = "turkish_q"
    const val LAYOUT_TURKISH_F = "turkish_f"
    const val LAYOUT_URDU = "urdu"
    const val LAYOUT_UZBEK = "uzbek"

    @JvmStatic
    fun getSubtypes(locale: String, resources: Resources): List<Subtype> {
        return SubtypeBuilder(locale, true, resources).subtypes
    }

    @JvmStatic
    fun getDefaultSubtype(locale: String, resources: Resources): Subtype? {
        val subtypes = SubtypeBuilder(locale, true, resources).subtypes
        return if (subtypes.isEmpty()) null else subtypes[0]
    }

    @JvmStatic
    fun getSubtype(locale: String, layoutSet: String, resources: Resources): Subtype? {
        val subtypes = SubtypeBuilder(locale, layoutSet, resources).subtypes
        return if (subtypes.isEmpty()) null else subtypes[0]
    }

    @JvmStatic
    fun getDefaultSubtypes(resources: Resources): List<Subtype> {
        val supportedLocales = ArrayList<Locale>(sSupportedLocales.size)
        for (localeString in sSupportedLocales) {
            supportedLocales.add(LocaleUtils.constructLocaleFromString(localeString))
        }

        val systemLocales = LocaleUtils.getSystemLocales()

        val subtypes = ArrayList<Subtype>()
        val addedLocales = HashSet<Locale>()
        for (systemLocale in systemLocales) {
            val bestLocale = LocaleUtils.findBestLocale(systemLocale, supportedLocales)
            if (bestLocale != null && !addedLocales.contains(bestLocale)) {
                addedLocales.add(bestLocale)
                val bestLocaleString = LocaleUtils.getLocaleString(bestLocale)
                val bestSubtype = getDefaultSubtype(bestLocaleString, resources)
                if (bestSubtype != null) {
                    subtypes.add(bestSubtype)
                }
            }
        }
        if (subtypes.isEmpty()) {
            subtypes.add(getSubtypes(LOCALE_ENGLISH_UNITED_STATES, resources)[0])
        }
        return subtypes
    }

    private class SubtypeBuilder {
        private val mResources: Resources
        private val mAllowMultiple: Boolean
        private val mLocale: String
        private val mExpectedLayoutSet: String?
        private var mSubtypes: MutableList<Subtype>? = null

        constructor(locale: String, layoutSet: String, resources: Resources) {
            mLocale = locale
            mExpectedLayoutSet = layoutSet
            mAllowMultiple = false
            mResources = resources
        }

        constructor(locale: String, all: Boolean, resources: Resources) {
            mLocale = locale
            mExpectedLayoutSet = null
            mAllowMultiple = all
            mResources = resources
        }

        val subtypes: MutableList<Subtype>
            get() {
                if (mSubtypes != null) {
                    return mSubtypes!!
                }
                mSubtypes = ArrayList()
                when (mLocale) {
                    LOCALE_AFRIKAANS, LOCALE_AZERBAIJANI_AZERBAIJAN, LOCALE_ENGLISH_INDIA,
                    LOCALE_ENGLISH_GREAT_BRITAIN, LOCALE_ENGLISH_UNITED_STATES,
                    LOCALE_INDONESIAN_1, LOCALE_INDONESIAN_2, LOCALE_ICELANDIC,
                    LOCALE_ITALIAN, LOCALE_LITHUANIAN, LOCALE_LATVIAN,
                    LOCALE_MALAY_MALAYSIA, LOCALE_DUTCH, LOCALE_POLISH,
                    LOCALE_ROMANIAN, LOCALE_SLOVAK, LOCALE_SWAHILI,
                    LOCALE_VIETNAMESE, LOCALE_ZULU -> {
                        addLayout(LAYOUT_QWERTY)
                        addGenericLayouts()
                    }
                    LOCALE_CZECH, LOCALE_GERMAN, LOCALE_CROATIAN,
                    LOCALE_HUNGARIAN, LOCALE_SLOVENIAN -> {
                        addLayout(LAYOUT_QWERTZ)
                        addGenericLayouts()
                    }
                    LOCALE_FRENCH_CANADA -> {
                        addLayout(LAYOUT_QWERTY)
                        addLayout(LAYOUT_ERGOL, R.string.subtype_ergol)
                        addGenericLayouts()
                    }
                    LOCALE_FRENCH -> {
                        addLayout(LAYOUT_AZERTY)
                        addLayout(LAYOUT_BEPO)
                        addLayout(LAYOUT_ERGOL, R.string.subtype_ergol)
                        addGenericLayouts()
                    }
                    LOCALE_DUTCH_BELGIUM -> {
                        addLayout(LAYOUT_AZERTY)
                        addGenericLayouts()
                    }
                    LOCALE_CATALAN, LOCALE_SPANISH, LOCALE_SPANISH_UNITED_STATES,
                    LOCALE_SPANISH_LATIN_AMERICA, LOCALE_BASQUE_SPAIN,
                    LOCALE_GALICIAN_SPAIN, LOCALE_TAGALOG -> {
                        addLayout(LAYOUT_SPANISH)
                        addGenericLayouts()
                    }
                    LOCALE_ESPERANTO -> {
                        addLayout(LAYOUT_SPANISH)
                    }
                    LOCALE_DANISH, LOCALE_ESTONIAN_ESTONIA, LOCALE_FINNISH,
                    LOCALE_NORWEGIAN_BOKMAL, LOCALE_SWEDISH -> {
                        addLayout(LAYOUT_NORDIC)
                        addGenericLayouts()
                    }
                    LOCALE_FRENCH_SWITZERLAND -> {
                        addLayout(LAYOUT_SWISS)
                        addLayout(LAYOUT_BEPO)
                        addLayout(LAYOUT_ERGOL, R.string.subtype_ergol)
                        addGenericLayouts()
                    }
                    LOCALE_GERMAN_SWITZERLAND, LOCALE_ITALIAN_SWITZERLAND -> {
                        addLayout(LAYOUT_SWISS)
                        addGenericLayouts()
                    }
                    LOCALE_TURKISH -> {
                        addLayout(LAYOUT_QWERTY)
                        addLayout(LAYOUT_TURKISH_Q, R.string.subtype_q)
                        addLayout(LAYOUT_TURKISH_F, R.string.subtype_f)
                        addGenericLayouts()
                    }
                    LOCALE_UZBEK_UZBEKISTAN -> {
                        addLayout(LAYOUT_UZBEK)
                        addGenericLayouts()
                    }
                    LOCALE_ARABIC -> {
                        addLayout(LAYOUT_ARABIC)
                    }
                    LOCALE_BELARUSIAN_BELARUS, LOCALE_KAZAKH, LOCALE_KYRGYZ,
                    LOCALE_RUSSIAN, LOCALE_UKRAINIAN -> {
                        addLayout(LAYOUT_EAST_SLAVIC)
                    }
                    LOCALE_BULGARIAN -> {
                        addLayout(LAYOUT_BULGARIAN)
                        addLayout(LAYOUT_BULGARIAN_BDS, R.string.subtype_bds)
                    }
                    LOCALE_BENGALI_BANGLADESH -> {
                        addLayout(LAYOUT_BENGALI_UNIJOY)
                        addLayout(LAYOUT_BENGALI_AKKHOR, R.string.subtype_akkhor)
                    }
                    LOCALE_BENGALI_INDIA -> {
                        addLayout(LAYOUT_BENGALI)
                        addLayout(LAYOUT_BENGALI_AVRO, R.string.subtype_avro)
                    }
                    LOCALE_GREEK -> {
                        addLayout(LAYOUT_GREEK)
                    }
                    LOCALE_PERSIAN -> {
                        addLayout(LAYOUT_FARSI)
                    }
                    LOCALE_HINDI -> {
                        addLayout(LAYOUT_HINDI)
                        addLayout(LAYOUT_HINDI_COMPACT, R.string.subtype_compact)
                    }
                    LOCALE_ARMENIAN_ARMENIA -> {
                        addLayout(LAYOUT_ARMENIAN_PHONETIC)
                    }
                    LOCALE_HEBREW_1, LOCALE_HEBREW_2 -> {
                        addLayout(LAYOUT_HEBREW)
                    }
                    LOCALE_GEORGIAN_GEORGIA -> {
                        addLayout(LAYOUT_GEORGIAN)
                    }
                    LOCALE_KHMER_CAMBODIA -> {
                        addLayout(LAYOUT_KHMER)
                    }
                    LOCALE_KANNADA_INDIA -> {
                        addLayout(LAYOUT_KANNADA)
                    }
                    LOCALE_LAO_LAOS -> {
                        addLayout(LAYOUT_LAO)
                    }
                    LOCALE_MACEDONIAN -> {
                        addLayout(LAYOUT_MACEDONIAN)
                    }
                    LOCALE_MALAYALAM_INDIA -> {
                        addLayout(LAYOUT_MALAYALAM)
                    }
                    LOCALE_MONGOLIAN_MONGOLIA -> {
                        addLayout(LAYOUT_MONGOLIAN)
                    }
                    LOCALE_MARATHI_INDIA -> {
                        addLayout(LAYOUT_MARATHI)
                    }
                    LOCALE_NEPALI_NEPAL -> {
                        addLayout(LAYOUT_NEPALI_ROMANIZED)
                        addLayout(LAYOUT_NEPALI_TRADITIONAL, R.string.subtype_traditional)
                    }
                    LOCALE_SAKHA -> {
                        addLayout(LAYOUT_SAKHA)
                    }
                    LOCALE_SERBIAN -> {
                        addLayout(LAYOUT_SERBIAN)
                    }
                    LOCALE_SERBIAN_LATIN -> {
                        addLayout(LAYOUT_SERBIAN_QWERTZ)
                        addGenericLayouts()
                    }
                    LOCALE_TAMIL_INDIA, LOCALE_TAMIL_SINGAPORE -> {
                        addLayout(LAYOUT_TAMIL)
                    }
                    LOCALE_TELUGU_INDIA -> {
                        addLayout(LAYOUT_TELUGU)
                    }
                    LOCALE_THAI -> {
                        addLayout(LAYOUT_THAI)
                    }
                    LOCALE_URDU -> {
                        addLayout(LAYOUT_URDU)
                    }
                    LOCALE_PORTUGUESE_BRAZIL, LOCALE_PORTUGUESE_PORTUGAL -> {
                        addLayout(LAYOUT_QWERTY)
                        addLayout(LAYOUT_HCESAR, R.string.subtype_hcesar)
                        addGenericLayouts()
                    }
                }
                return mSubtypes!!
            }

        private fun shouldSkipLayout(keyboardLayoutSet: String): Boolean {
            if (mAllowMultiple) {
                return false
            }
            if (mSubtypes!!.size > 0) {
                return true
            }
            if (mExpectedLayoutSet != null) {
                return mExpectedLayoutSet != keyboardLayoutSet
            }
            return false
        }

        private fun addLayout(keyboardLayoutSet: String) {
            if (shouldSkipLayout(keyboardLayoutSet)) {
                return
            }
            val predefinedLayouts = mResources.getStringArray(R.array.predefined_layouts)
            val predefinedLayoutIndex = predefinedLayouts.indexOf(keyboardLayoutSet)
            val layoutNameStr: String? = if (predefinedLayoutIndex >= 0) {
                val predefinedLayoutDisplayNames = mResources.getStringArray(
                    R.array.predefined_layout_display_names
                )
                predefinedLayoutDisplayNames[predefinedLayoutIndex]
            } else {
                null
            }
            mSubtypes!!.add(
                Subtype(mLocale, keyboardLayoutSet, layoutNameStr, false, mResources)
            )
        }

        private fun addLayout(keyboardLayoutSet: String, layoutRes: Int) {
            if (shouldSkipLayout(keyboardLayoutSet)) {
                return
            }
            mSubtypes!!.add(
                Subtype(mLocale, keyboardLayoutSet, layoutRes, true, mResources)
            )
        }

        private fun addGenericLayouts() {
            if (mSubtypes!!.size > 0 && !mAllowMultiple) {
                return
            }
            val initialSize = mSubtypes!!.size
            val predefinedKeyboardLayoutSets = mResources.getStringArray(R.array.predefined_layouts)
            val predefinedKeyboardLayoutSetDisplayNames = mResources.getStringArray(
                R.array.predefined_layout_display_names
            )
            for (i in predefinedKeyboardLayoutSets.indices) {
                val predefinedLayout = predefinedKeyboardLayoutSets[i]
                if (shouldSkipLayout(predefinedLayout)) {
                    continue
                }
                var alreadyExists = false
                for (subtypeIndex in 0 until initialSize) {
                    val layoutSet = mSubtypes!![subtypeIndex].keyboardLayoutSet
                    if (layoutSet == predefinedLayout) {
                        alreadyExists = true
                        break
                    }
                }
                if (alreadyExists) {
                    continue
                }
                mSubtypes!!.add(
                    Subtype(
                        mLocale, predefinedLayout,
                        predefinedKeyboardLayoutSetDisplayNames[i], true, mResources
                    )
                )
            }
        }
    }
}
