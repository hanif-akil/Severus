package rkr.simplekeyboard.inputmethod.keyboard.internal

import java.util.Locale

object KeyboardTextsTable {
    private val sNameToIndexesMap = HashMap<String, Int>()
    private val sLocaleToTextsTableMap = HashMap<String, Array<String>>()

    fun getText(name: String, textsTable: Array<String>): String {
        val index = sNameToIndexesMap[name] ?: throw RuntimeException("Unknown text name=$name")
        val text = if (index < textsTable.size) textsTable[index] else null
        if (text != null) return text
        if (index in TEXTS_DEFAULT.indices) return TEXTS_DEFAULT[index]
        throw RuntimeException("Illegal index=$index for name=$name")
    }

    fun getTextsTable(locale: Locale): Array<String> {
        val localeKey = locale.toString()
        sLocaleToTextsTableMap[localeKey]?.let { return it }
        val languageKey = locale.language
        sLocaleToTextsTableMap[languageKey]?.let { return it }
        return TEXTS_DEFAULT
    }

    private val NAMES = arrayOf(
        "name", "morekeys_a", "morekeys_o", "morekeys_e", "morekeys_u",
        "keylabel_to_alpha", "morekeys_i", "morekeys_n", "morekeys_c",
        "double_quotes", "morekeys_s", "single_quotes", "keyspec_currency",
        "morekeys_y", "morekeys_z", "morekeys_d", "morekeys_t", "morekeys_l",
        "morekeys_g", "single_angle_quotes", "double_angle_quotes",
        "morekeys_r", "morekeys_k", "morekeys_cyrillic_ie",
        "keyspec_nordic_row1_11", "keyspec_nordic_row2_10", "keyspec_nordic_row2_11",
        "morekeys_nordic_row2_10", "keyspec_east_slavic_row1_9",
        "keyspec_east_slavic_row2_2", "keyspec_east_slavic_row2_11",
        "keyspec_east_slavic_row3_5", "morekeys_cyrillic_soft_sign",
        "keyspec_symbols_1", "keyspec_symbols_2", "keyspec_symbols_3",
        "keyspec_symbols_4", "keyspec_symbols_5", "keyspec_symbols_6",
        "keyspec_symbols_7", "keyspec_symbols_8", "keyspec_symbols_9",
        "keyspec_symbols_0", "keylabel_to_symbol",
        "additional_morekeys_symbols_1", "additional_morekeys_symbols_2",
        "additional_morekeys_symbols_3", "additional_morekeys_symbols_4",
        "additional_morekeys_symbols_5", "additional_morekeys_symbols_6",
        "additional_morekeys_symbols_7", "additional_morekeys_symbols_8",
        "additional_morekeys_symbols_9", "additional_morekeys_symbols_0",
        "morekeys_tablet_period", "morekeys_punctuation",
        "keyspec_tablet_comma", "keyspec_period", "morekeys_period"
    )

    private val TEXTS_DEFAULT = arrayOf(
        "", "", "", "", "", "ABC", "", "", "",
        "!text/double_lqm_rqm", "", "!text/single_lqm_rqm", "$",
        "", "", "", "", "", "",
        "!text/single_laqm_raqm", "!text/double_laqm_raqm",
        "", "", "",
        "", "", "", "",
        "", "", "", "",
        "", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
        "?123",
        "", "", "", "", "", "", "", "", "", "",
        "!text/morekeys_tablet_punctuation", "",
        "!autoColumnOrder!8,\\,,?,!,#,!text/keyspec_right_parenthesis,!text/keyspec_left_parenthesis,/,;,',@,:,-,\\,+,\\%,&",
        ",", ".", "!text/morekeys_punctuation"
    )

    private val TEXTS_af = arrayOf<String>()
    private val TEXTS_ar = arrayOf<String>()
    private val TEXTS_az_AZ = arrayOf<String>()
    private val TEXTS_be_BY = arrayOf<String>()
    private val TEXTS_bg = arrayOf<String>()
    private val TEXTS_bn_BD = arrayOf<String>()
    private val TEXTS_bn_IN = arrayOf<String>()
    private val TEXTS_ca = arrayOf<String>()
    private val TEXTS_cs = arrayOf<String>()
    private val TEXTS_da = arrayOf<String>()
    private val TEXTS_de = arrayOf<String>()
    private val TEXTS_el = arrayOf<String>()
    private val TEXTS_en = arrayOf<String>()
    private val TEXTS_eo = arrayOf<String>()
    private val TEXTS_es = arrayOf<String>()
    private val TEXTS_et_EE = arrayOf<String>()
    private val TEXTS_eu_ES = arrayOf<String>()
    private val TEXTS_fa = arrayOf<String>()
    private val TEXTS_fi = arrayOf<String>()
    private val TEXTS_fr = arrayOf<String>()
    private val TEXTS_gl_ES = arrayOf<String>()
    private val TEXTS_hi = arrayOf<String>()
    private val TEXTS_hi_ZZ = arrayOf<String>()
    private val TEXTS_hr = arrayOf<String>()
    private val TEXTS_hu = arrayOf<String>()
    private val TEXTS_hy_AM = arrayOf<String>()
    private val TEXTS_id = arrayOf<String>()
    private val TEXTS_is = arrayOf<String>()
    private val TEXTS_it = arrayOf<String>()
    private val TEXTS_iw = arrayOf<String>()
    private val TEXTS_ka_GE = arrayOf<String>()
    private val TEXTS_kk = arrayOf<String>()
    private val TEXTS_km_KH = arrayOf<String>()
    private val TEXTS_kn_IN = arrayOf<String>()
    private val TEXTS_ky = arrayOf<String>()
    private val TEXTS_lo_LA = arrayOf<String>()
    private val TEXTS_lt = arrayOf<String>()
    private val TEXTS_lv = arrayOf<String>()
    private val TEXTS_mk = arrayOf<String>()
    private val TEXTS_ml_IN = arrayOf<String>()
    private val TEXTS_mn_MN = arrayOf<String>()
    private val TEXTS_mr_IN = arrayOf<String>()
    private val TEXTS_ms_MY = arrayOf<String>()
    private val TEXTS_nb = arrayOf<String>()
    private val TEXTS_ne_NP = arrayOf<String>()
    private val TEXTS_nl = arrayOf<String>()
    private val TEXTS_pl = arrayOf<String>()
    private val TEXTS_pt_BR = arrayOf<String>()
    private val TEXTS_pt_PT = arrayOf<String>()
    private val TEXTS_rm = arrayOf<String>()
    private val TEXTS_ro = arrayOf<String>()
    private val TEXTS_ru = arrayOf<String>()
    private val TEXTS_sah = arrayOf<String>()
    private val TEXTS_si_LK = arrayOf<String>()
    private val TEXTS_sk = arrayOf<String>()
    private val TEXTS_sl = arrayOf<String>()
    private val TEXTS_sr = arrayOf<String>()
    private val TEXTS_sr_ZZ = arrayOf<String>()
    private val TEXTS_sv = arrayOf<String>()
    private val TEXTS_sw = arrayOf<String>()
    private val TEXTS_ta_IN = arrayOf<String>()
    private val TEXTS_ta_LK = arrayOf<String>()
    private val TEXTS_ta_SG = arrayOf<String>()
    private val TEXTS_te_IN = arrayOf<String>()
    private val TEXTS_th = arrayOf<String>()
    private val TEXTS_tl = arrayOf<String>()
    private val TEXTS_tr = arrayOf<String>()
    private val TEXTS_uk = arrayOf<String>()
    private val TEXTS_ur = arrayOf<String>()
    private val TEXTS_uz_UZ = arrayOf<String>()
    private val TEXTS_vi = arrayOf<String>()
    private val TEXTS_zu = arrayOf<String>()
    private val TEXTS_zz = arrayOf<String>()

    init {
        for (i in NAMES.indices) {
            sNameToIndexesMap[NAMES[i]] = i
        }
        sLocaleToTextsTableMap["af"] = TEXTS_af
        sLocaleToTextsTableMap["ar"] = TEXTS_ar
        sLocaleToTextsTableMap["az_AZ"] = TEXTS_az_AZ
        sLocaleToTextsTableMap["be_BY"] = TEXTS_be_BY
        sLocaleToTextsTableMap["bg"] = TEXTS_bg
        sLocaleToTextsTableMap["bn_BD"] = TEXTS_bn_BD
        sLocaleToTextsTableMap["bn_IN"] = TEXTS_bn_IN
        sLocaleToTextsTableMap["ca"] = TEXTS_ca
        sLocaleToTextsTableMap["cs"] = TEXTS_cs
        sLocaleToTextsTableMap["da"] = TEXTS_da
        sLocaleToTextsTableMap["de"] = TEXTS_de
        sLocaleToTextsTableMap["el"] = TEXTS_el
        sLocaleToTextsTableMap["en"] = TEXTS_en
        sLocaleToTextsTableMap["eo"] = TEXTS_eo
        sLocaleToTextsTableMap["es"] = TEXTS_es
        sLocaleToTextsTableMap["et_EE"] = TEXTS_et_EE
        sLocaleToTextsTableMap["eu_ES"] = TEXTS_eu_ES
        sLocaleToTextsTableMap["fa"] = TEXTS_fa
        sLocaleToTextsTableMap["fi"] = TEXTS_fi
        sLocaleToTextsTableMap["fr"] = TEXTS_fr
        sLocaleToTextsTableMap["gl_ES"] = TEXTS_gl_ES
        sLocaleToTextsTableMap["hi"] = TEXTS_hi
        sLocaleToTextsTableMap["hr"] = TEXTS_hr
        sLocaleToTextsTableMap["hu"] = TEXTS_hu
        sLocaleToTextsTableMap["hy_AM"] = TEXTS_hy_AM
        sLocaleToTextsTableMap["id"] = TEXTS_id
        sLocaleToTextsTableMap["is"] = TEXTS_is
        sLocaleToTextsTableMap["it"] = TEXTS_it
        sLocaleToTextsTableMap["ka_GE"] = TEXTS_ka_GE
        sLocaleToTextsTableMap["kk"] = TEXTS_kk
        sLocaleToTextsTableMap["km_KH"] = TEXTS_km_KH
        sLocaleToTextsTableMap["kn_IN"] = TEXTS_kn_IN
        sLocaleToTextsTableMap["lo_LA"] = TEXTS_lo_LA
        sLocaleToTextsTableMap["lt"] = TEXTS_lt
        sLocaleToTextsTableMap["lv"] = TEXTS_lv
        sLocaleToTextsTableMap["mk"] = TEXTS_mk
        sLocaleToTextsTableMap["ml_IN"] = TEXTS_ml_IN
        sLocaleToTextsTableMap["mr_IN"] = TEXTS_mr_IN
        sLocaleToTextsTableMap["ms_MY"] = TEXTS_ms_MY
        sLocaleToTextsTableMap["nb"] = TEXTS_nb
        sLocaleToTextsTableMap["nl"] = TEXTS_nl
        sLocaleToTextsTableMap["pl"] = TEXTS_pl
        sLocaleToTextsTableMap["pt_BR"] = TEXTS_pt_BR
        sLocaleToTextsTableMap["pt_PT"] = TEXTS_pt_PT
        sLocaleToTextsTableMap["ro"] = TEXTS_ro
        sLocaleToTextsTableMap["ru"] = TEXTS_ru
        sLocaleToTextsTableMap["sah"] = TEXTS_sah
        sLocaleToTextsTableMap["sk"] = TEXTS_sk
        sLocaleToTextsTableMap["sl"] = TEXTS_sl
        sLocaleToTextsTableMap["sr"] = TEXTS_sr
        sLocaleToTextsTableMap["sr_ZZ"] = TEXTS_sr_ZZ
        sLocaleToTextsTableMap["sv"] = TEXTS_sv
        sLocaleToTextsTableMap["sw"] = TEXTS_sw
        sLocaleToTextsTableMap["ta_IN"] = TEXTS_ta_IN
        sLocaleToTextsTableMap["te_IN"] = TEXTS_te_IN
        sLocaleToTextsTableMap["th"] = TEXTS_th
        sLocaleToTextsTableMap["tl"] = TEXTS_tl
        sLocaleToTextsTableMap["tr"] = TEXTS_tr
        sLocaleToTextsTableMap["uk"] = TEXTS_uk
        sLocaleToTextsTableMap["ur"] = TEXTS_ur
        sLocaleToTextsTableMap["vi"] = TEXTS_vi
    }
}
