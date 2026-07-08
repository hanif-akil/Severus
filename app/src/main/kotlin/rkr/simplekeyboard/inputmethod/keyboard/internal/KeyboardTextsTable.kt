package rkr.simplekeyboard.inputmethod.keyboard.internal

import java.util.Locale

object KeyboardTextsTable {
    private val sNameToIndexesMap = HashMap<String, Int>()
    private val sLocaleToTextsTableMap = HashMap<String, Array<String>>()

    fun getText(name: String, textsTable: Array<String>): String {
        val index = sNameToIndexesMap[name] ?: throw RuntimeException("Unknown text name=")
        val text = if (index < textsTable.size) textsTable[index] else null
        if (text != null) return text
        if (index in TEXTS_DEFAULT.indices) return TEXTS_DEFAULT[index]
        throw RuntimeException("Illegal index= for name=")
    }

    fun getTextsTable(locale: Locale): Array<String> {
        val localeKey = locale.toString()
        sLocaleToTextsTableMap[localeKey]?.let { return it }
        val languageKey = locale.language
        sLocaleToTextsTableMap[languageKey]?.let { return it }
        return TEXTS_DEFAULT
    }
    private val NAMES = arrayOf(" + (private static final String[] NAMES = {     //  /* index:histogram */ "name",         /*   0:33 */ "morekeys_a",         /*   1:33 */ "morekeys_o",         /*   2:32 */ "morekeys_e",         /*   3:31 */ "morekeys_u",         /*   4:31 */ "keylabel_to_alpha",         /*   5:30 */ "morekeys_i",         /*   6:25 */ "morekeys_n",         /*   7:25 */ "morekeys_c",         /*   8:23 */ "double_quotes",         /*   9:22 */ "morekeys_s",         /*  10:22 */ "single_quotes",         /*  11:19 */ "keyspec_currency",         /*  12:17 */ "morekeys_y",         /*  13:16 */ "morekeys_z",         /*  14:14 */ "morekeys_d",         /*  15:10 */ "morekeys_t",         /*  16:10 */ "morekeys_l",         /*  17:10 */ "morekeys_g",         /*  18: 9 */ "single_angle_quotes",         /*  19: 9 */ "double_angle_quotes",         /*  20: 8 */ "morekeys_r",         /*  21: 6 */ "morekeys_k",         /*  22: 6 */ "morekeys_cyrillic_ie",         /*  23: 5 */ "keyspec_nordic_row1_11",         /*  24: 5 */ "keyspec_nordic_row2_10",         /*  25: 5 */ "keyspec_nordic_row2_11",         /*  26: 5 */ "morekeys_nordic_row2_10",         /*  27: 5 */ "keyspec_east_slavic_row1_9",         /*  28: 5 */ "keyspec_east_slavic_row2_2",         /*  29: 5 */ "keyspec_east_slavic_row2_11",         /*  30: 5 */ "keyspec_east_slavic_row3_5",         /*  31: 5 */ "morekeys_cyrillic_soft_sign",         /*  32: 5 */ "keyspec_symbols_1",         /*  33: 5 */ "keyspec_symbols_2",         /*  34: 5 */ "keyspec_symbols_3",         /*  35: 5 */ "keyspec_symbols_4",         /*  36: 5 */ "keyspec_symbols_5",         /*  37: 5 */ "keyspec_symbols_6",         /*  38: 5 */ "keyspec_symbols_7",         /*  39: 5 */ "keyspec_symbols_8",         /*  40: 5 */ "keyspec_symbols_9",         /*  41: 5 */ "keyspec_symbols_0",         /*  42: 5 */ "keylabel_to_symbol",         /*  43: 5 */ "additional_morekeys_symbols_1",         /*  44: 5 */ "additional_morekeys_symbols_2",         /*  45: 5 */ "additional_morekeys_symbols_3",         /*  46: 5 */ "additional_morekeys_symbols_4",         /*  47: 5 */ "additional_morekeys_symbols_5",         /*  48: 5 */ "additional_morekeys_symbols_6",         /*  49: 5 */ "additional_morekeys_symbols_7",         /*  50: 5 */ "additional_morekeys_symbols_8",         /*  51: 5 */ "additional_morekeys_symbols_9",         /*  52: 5 */ "additional_morekeys_symbols_0",         /*  53: 5 */ "morekeys_tablet_period",         /*  54: 4 */ "morekeys_nordic_row2_11",         /*  55: 4 */ "morekeys_punctuation",         /*  56: 4 */ "keyspec_tablet_comma",         /*  57: 4 */ "keyspec_period",         /*  58: 4 */ "morekeys_period",         /*  59: 4 */ "keyspec_tablet_period",         /*  60: 3 */ "keyspec_swiss_row1_11",         /*  61: 3 */ "keyspec_swiss_row2_10",         /*  62: 3 */ "keyspec_swiss_row2_11",         /*  63: 3 */ "morekeys_swiss_row1_11",         /*  64: 3 */ "morekeys_swiss_row2_10",         /*  65: 3 */ "morekeys_swiss_row2_11",         /*  66: 3 */ "morekeys_star",         /*  67: 3 */ "keyspec_left_parenthesis",         /*  68: 3 */ "keyspec_right_parenthesis",         /*  69: 3 */ "keyspec_left_square_bracket",         /*  70: 3 */ "keyspec_right_square_bracket",         /*  71: 3 */ "keyspec_left_curly_bracket",         /*  72: 3 */ "keyspec_right_curly_bracket",         /*  73: 3 */ "keyspec_less_than",         /*  74: 3 */ "keyspec_greater_than",         /*  75: 3 */ "keyspec_less_than_equal",         /*  76: 3 */ "keyspec_greater_than_equal",         /*  77: 3 */ "keyspec_left_double_angle_quote",         /*  78: 3 */ "keyspec_right_double_angle_quote",         /*  79: 3 */ "keyspec_left_single_angle_quote",         /*  80: 3 */ "keyspec_right_single_angle_quote",         /*  81: 3 */ "keyspec_comma",         /*  82: 3 */ "morekeys_tablet_comma",         /*  83: 3 */ "keyhintlabel_period",         /*  84: 3 */ "morekeys_question",         /*  85: 2 */ "morekeys_h",         /*  86: 2 */ "morekeys_w",         /*  87: 2 */ "morekeys_east_slavic_row2_2",         /*  88: 2 */ "morekeys_cyrillic_u",         /*  89: 2 */ "morekeys_cyrillic_en",         /*  90: 2 */ "morekeys_cyrillic_ghe",         /*  91: 2 */ "morekeys_cyrillic_o",         /*  92: 2 */ "morekeys_cyrillic_i",         /*  93: 2 */ "keyspec_south_slavic_row1_6",         /*  94: 2 */ "keyspec_south_slavic_row2_11",         /*  95: 2 */ "keyspec_south_slavic_row3_8",         /*  96: 2 */ "morekeys_tablet_punctuation",         /*  97: 2 */ "keyspec_spanish_row2_10",         /*  98: 2 */ "morekeys_bullet",         /*  99: 2 */ "morekeys_left_parenthesis",         /* 100: 2 */ "morekeys_right_parenthesis",         /* 101: 2 */ "morekeys_arabic_diacritics",         /* 102: 2 */ "keyhintlabel_tablet_comma",         /* 103: 2 */ "keyhintlabel_tablet_period",         /* 104: 2 */ "keyspec_symbols_question",         /* 105: 2 */ "keyspec_symbols_semicolon",         /* 106: 2 */ "keyspec_symbols_percent",         /* 107: 2 */ "morekeys_symbols_semicolon",         /* 108: 2 */ "morekeys_symbols_percent",         /* 109: 2 */ "label_pause_key",         /* 110: 2 */ "label_wait_key",         /* 111: 1 */ "morekeys_v",         /* 112: 1 */ "morekeys_j",         /* 113: 1 */ "morekeys_q",         /* 114: 1 */ "morekeys_x",         /* 115: 1 */ "keyspec_q",         /* 116: 1 */ "keyspec_w",         /* 117: 1 */ "keyspec_y",         /* 118: 1 */ "keyspec_x",         /* 119: 1 */ "morekeys_east_slavic_row2_11",         /* 120: 1 */ "morekeys_cyrillic_ka",         /* 121: 1 */ "morekeys_cyrillic_a",         /* 122: 1 */ "morekeys_currency_dollar",         /* 123: 1 */ "morekeys_plus",         /* 124: 1 */ "morekeys_less_than",         /* 125: 1 */ "morekeys_greater_than",         /* 126: 1 */ "morekeys_exclamation",         /* 127: 0 */ "morekeys_currency_generic",         /* 128: 0 */ "morekeys_symbols_1",         /* 129: 0 */ "morekeys_symbols_2",         /* 130: 0 */ "morekeys_symbols_3",         /* 131: 0 */ "morekeys_symbols_4",         /* 132: 0 */ "morekeys_symbols_5",         /* 133: 0 */ "morekeys_symbols_6",         /* 134: 0 */ "morekeys_symbols_7",         /* 135: 0 */ "morekeys_symbols_8",         /* 136: 0 */ "morekeys_symbols_9",         /* 137: 0 */ "morekeys_symbols_0",         /* 138: 0 */ "morekeys_am_pm",         /* 139: 0 */ "keyspec_settings",         /* 140: 0 */ "keyspec_action_next",         /* 141: 0 */ "keyspec_action_previous",         /* 142: 0 */ "keylabel_to_more_symbol",         /* 143: 0 */ "keylabel_tablet_to_more_symbol",         /* 144: 0 */ "keylabel_to_phone_numeric",         /* 145: 0 */ "keylabel_to_phone_symbols",         /* 146: 0 */ "keylabel_time_am",         /* 147: 0 */ "keylabel_time_pm",         /* 148: 0 */ "keyspecs_left_parenthesis_more_keys",         /* 149: 0 */ "keyspecs_right_parenthesis_more_keys",         /* 150: 0 */ "single_laqm_raqm",         /* 151: 0 */ "single_raqm_laqm",         /* 152: 0 */ "double_laqm_raqm",         /* 153: 0 */ "double_raqm_laqm",         /* 154: 0 */ "single_lqm_rqm",         /* 155: 0 */ "single_9qm_lqm",         /* 156: 0 */ "single_9qm_rqm",         /* 157: 0 */ "single_rqm_9qm",         /* 158: 0 */ "double_lqm_rqm",         /* 159: 0 */ "double_9qm_lqm",         /* 160: 0 */ "double_9qm_rqm",         /* 161: 0 */ "double_rqm_9qm",         /* 162: 0 */ "morekeys_single_quote",         /* 163: 0 */ "morekeys_double_quote",         /* 164: 0 */ "morekeys_tablet_double_quote",         /* 165: 0 */ "morekeys_cyrillic_ya",         /* 166: 0 */ "morekeys_cyrillic_yu",     };.Groups[1].Value - '/*' - '*/').Trim() + @")

    private val EMPTY = ""

    private val TEXTS_DEFAULT = arrayOf(" + (private static final String[] TEXTS_DEFAULT = {         /* morekeys_a ~ */         EMPTY, EMPTY, EMPTY, EMPTY,         /* ~ morekeys_u */         // Label for "switch to alphabetic" key.         /* keylabel_to_alpha */ "ABC",         /* morekeys_i ~ */         EMPTY, EMPTY, EMPTY,         /* ~ morekeys_c */         /* double_quotes */ "!text/double_lqm_rqm",         /* morekeys_s */ EMPTY,         /* single_quotes */ "!text/single_lqm_rqm",         /* keyspec_currency */ "$",         /* morekeys_y ~ */         EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,         /* ~ morekeys_g */         /* single_angle_quotes */ "!text/single_laqm_raqm",         /* double_angle_quotes */ "!text/double_laqm_raqm",         /* morekeys_r ~ */         EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,         /* ~ morekeys_cyrillic_soft_sign */         /* keyspec_symbols_1 */ "1",         /* keyspec_symbols_2 */ "2",         /* keyspec_symbols_3 */ "3",         /* keyspec_symbols_4 */ "4",         /* keyspec_symbols_5 */ "5",         /* keyspec_symbols_6 */ "6",         /* keyspec_symbols_7 */ "7",         /* keyspec_symbols_8 */ "8",         /* keyspec_symbols_9 */ "9",         /* keyspec_symbols_0 */ "0",         // Label for "switch to symbols" key.         /* keylabel_to_symbol */ "?123",         /* additional_morekeys_symbols_1 ~ */         EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,         /* ~ additional_morekeys_symbols_0 */         /* morekeys_tablet_period */ "!text/morekeys_tablet_punctuation",         /* morekeys_nordic_row2_11 */ EMPTY,         /* morekeys_punctuation */ "!autoColumnOrder!8,\\,,?,!,#,!text/keyspec_right_parenthesis,!text/keyspec_left_parenthesis,/,;,',@,:,-,\",+,\\%,&",         /* keyspec_tablet_comma */ ",",         // Period key         /* keyspec_period */ ".",         /* morekeys_period */ "!text/morekeys_punctuation",         /* keyspec_tablet_period */ ".",         /* keyspec_swiss_row1_11 ~ */         EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,         /* ~ morekeys_swiss_row2_11 */         // U+2020: "†" DAGGER         // U+2021: "‡" DOUBLE DAGGER         // U+2605: "★" BLACK STAR         /* morekeys_star */ "\u2020,\u2021,\u2605",         // The all letters need to be mirrored are found at         // http://www.unicode.org/Public/6.1.0/ucd/BidiMirroring.txt         // U+2039: "‹" SINGLE LEFT-POINTING ANGLE QUOTATION MARK         // U+203A: "›" SINGLE RIGHT-POINTING ANGLE QUOTATION MARK         // U+2264: "≤" LESS-THAN OR EQUAL TO         // U+2265: "≥" GREATER-THAN EQUAL TO         // U+00AB: "«" LEFT-POINTING DOUBLE ANGLE QUOTATION MARK         // U+00BB: "»" RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK         /* keyspec_left_parenthesis */ "(",         /* keyspec_right_parenthesis */ ")",         /* keyspec_left_square_bracket */ "[",         /* keyspec_right_square_bracket */ "]",         /* keyspec_left_curly_bracket */ "{",         /* keyspec_right_curly_bracket */ "}",         /* keyspec_less_than */ "<",         /* keyspec_greater_than */ ">",         /* keyspec_less_than_equal */ "\u2264",         /* keyspec_greater_than_equal */ "\u2265",         /* keyspec_left_double_angle_quote */ "\u00AB",         /* keyspec_right_double_angle_quote */ "\u00BB",         /* keyspec_left_single_angle_quote */ "\u2039",         /* keyspec_right_single_angle_quote */ "\u203A",         // Comma key         /* keyspec_comma */ ",",         /* morekeys_tablet_comma */ EMPTY,         /* keyhintlabel_period */ EMPTY,         // U+00BF: "¿" INVERTED QUESTION MARK         /* morekeys_question */ "\u00BF",         /* morekeys_h ~ */         EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY,         /* ~ keyspec_south_slavic_row3_8 */         /* morekeys_tablet_punctuation */ "!autoColumnOrder!7,\\,,',#,!text/keyspec_right_parenthesis,!text/keyspec_left_parenthesis,/,;,@,:,-,\",+,\\%,&",         // U+00F1: "ñ" LATIN SMALL LETTER N WITH TILDE         /* keyspec_spanish_row2_10 */ "\u00F1",         // U+266A: "♪" EIGHTH NOTE         // U+2665: "♥" BLACK HEART SUIT         // U+2660: "♠" BLACK SPADE SUIT         // U+2666: "♦" BLACK DIAMOND SUIT         // U+2663: "♣" BLACK CLUB SUIT         /* morekeys_bullet */ "\u266A,\u2665,\u2660,\u2666,\u2663",         /* morekeys_left_parenthesis */ "!fixedColumnOrder!3,!text/keyspecs_left_parenthesis_more_keys",         /* morekeys_right_parenthesis */ "!fixedColumnOrder!3,!text/keyspecs_right_parenthesis_more_keys",         /* morekeys_arabic_diacritics ~ */         EMPTY, EMPTY, EMPTY,         /* ~ keyhintlabel_tablet_period */         /* keyspec_symbols_question */ "?",         /* keyspec_symbols_semicolon */ ";",         /* keyspec_symbols_percent */ "%",         /* morekeys_symbols_semicolon */ EMPTY,         // U+2030: "‰" PER MILLE SIGN         /* morekeys_symbols_percent */ "\u2030",         /* label_pause_key */ "!string/label_pause_key",         /* label_wait_key */ "!string/label_wait_key",         /* morekeys_v ~ */         EMPTY, EMPTY, EMPTY, EMPTY,         /* ~ morekeys_x */         /* keyspec_q */ "q",         /* keyspec_w */ "w",         /* keyspec_y */ "y",         /* keyspec_x */ "x",         /* morekeys_east_slavic_row2_11 ~ */         EMPTY, EMPTY, EMPTY,         /* ~ morekeys_cyrillic_a */         // U+00A2: "¢" CENT SIGN         // U+00A3: "£" POUND SIGN         // U+20AC: "€" EURO SIGN         // U+00A5: "¥" YEN SIGN         // U+20B1: "₱" PESO SIGN         /* morekeys_currency_dollar */ "\u00A2,\u00A3,\u20AC,\u00A5,\u20B1",         // U+00B1: "±" PLUS-MINUS SIGN         /* morekeys_plus */ "\u00B1",         /* morekeys_less_than */ "!fixedColumnOrder!3,!text/keyspec_left_single_angle_quote,!text/keyspec_less_than_equal,!text/keyspec_left_double_angle_quote",         /* morekeys_greater_than */ "!fixedColumnOrder!3,!text/keyspec_right_single_angle_quote,!text/keyspec_greater_than_equal,!text/keyspec_right_double_angle_quote",         // U+00A1: "¡" INVERTED EXCLAMATION MARK         /* morekeys_exclamation */ "\u00A1",         /* morekeys_currency_generic */ "$,\u00A2,\u20AC,\u00A3,\u00A5,\u20B1",         // U+00B9: "¹" SUPERSCRIPT ONE         // U+00BD: "½" VULGAR FRACTION ONE HALF         // U+2153: "⅓" VULGAR FRACTION ONE THIRD         // U+00BC: "¼" VULGAR FRACTION ONE QUARTER         // U+215B: "⅛" VULGAR FRACTION ONE EIGHTH         /* morekeys_symbols_1 */ "\u00B9,\u00BD,\u2153,\u00BC,\u215B",         // U+00B2: "²" SUPERSCRIPT TWO         // U+2154: "⅔" VULGAR FRACTION TWO THIRDS         /* morekeys_symbols_2 */ "\u00B2,\u2154",         // U+00B3: "³" SUPERSCRIPT THREE         // U+00BE: "¾" VULGAR FRACTION THREE QUARTERS         // U+215C: "⅜" VULGAR FRACTION THREE EIGHTHS         /* morekeys_symbols_3 */ "\u00B3,\u00BE,\u215C",         // U+2074: "⁴" SUPERSCRIPT FOUR         /* morekeys_symbols_4 */ "\u2074",         // U+215D: "⅝" VULGAR FRACTION FIVE EIGHTHS         /* morekeys_symbols_5 */ "\u215D",         /* morekeys_symbols_6 */ EMPTY,         // U+215E: "⅞" VULGAR FRACTION SEVEN EIGHTHS         /* morekeys_symbols_7 */ "\u215E",         /* morekeys_symbols_8 */ EMPTY,         /* morekeys_symbols_9 */ EMPTY,         // U+207F: "ⁿ" SUPERSCRIPT LATIN SMALL LETTER N         // U+2205: "∅" EMPTY SET         /* morekeys_symbols_0 */ "\u207F,\u2205",         /* morekeys_am_pm */ "!fixedColumnOrder!2,!hasLabels!,!text/keylabel_time_am,!text/keylabel_time_pm",         /* keyspec_settings */ "!icon/paste_key|!code/key_paste,!icon/settings_key|!code/key_settings",         /* keyspec_action_next */ "!code/key_action_next",         /* keyspec_action_previous */ "!code/key_action_previous",         // Label for "switch to more symbol" modifier key ("= \ <"). Must be short to fit on key!         /* keylabel_to_more_symbol */ "= \\\\ <",         // Label for "switch to more symbol" modifier key on tablets.  Must be short to fit on key!         /* keylabel_tablet_to_more_symbol */ "~ [ <",         // Label for "switch to phone numeric" key.  Must be short to fit on key!         /* keylabel_to_phone_numeric */ "123",         // Label for "switch to phone symbols" key.  Must be short to fit on key!         // U+FF0A: "＊" FULLWIDTH ASTERISK         // U+FF03: "＃" FULLWIDTH NUMBER SIGN         /* keylabel_to_phone_symbols */ "\uFF0A\uFF03",         // Key label for "ante meridiem"         /* keylabel_time_am */ "AM",         // Key label for "post meridiem"         /* keylabel_time_pm */ "PM",         /* keyspecs_left_parenthesis_more_keys */ "!text/keyspec_less_than,!text/keyspec_left_curly_bracket,!text/keyspec_left_square_bracket",         /* keyspecs_right_parenthesis_more_keys */ "!text/keyspec_greater_than,!text/keyspec_right_curly_bracket,!text/keyspec_right_square_bracket",         // The following characters don't need BIDI mirroring.         // U+2018: "‘" LEFT SINGLE QUOTATION MARK         // U+2019: "’" RIGHT SINGLE QUOTATION MARK         // U+201A: "‚" SINGLE LOW-9 QUOTATION MARK         // U+201C: "“" LEFT DOUBLE QUOTATION MARK         // U+201D: "”" RIGHT DOUBLE QUOTATION MARK         // U+201E: "„" DOUBLE LOW-9 QUOTATION MARK         // Abbreviations are:         // laqm: LEFT-POINTING ANGLE QUOTATION MARK         // raqm: RIGHT-POINTING ANGLE QUOTATION MARK         // lqm: LEFT QUOTATION MARK         // rqm: RIGHT QUOTATION MARK         // 9qm: LOW-9 QUOTATION MARK         // The following each quotation mark pair consist of         // <opening quotation mark>, <closing quotation mark>         // and is named after (single|double)_<opening quotation mark>_<closing quotation mark>.         /* single_laqm_raqm */ "!text/keyspec_left_single_angle_quote,!text/keyspec_right_single_angle_quote",         /* single_raqm_laqm */ "!text/keyspec_right_single_angle_quote,!text/keyspec_left_single_angle_quote",         /* double_laqm_raqm */ "!text/keyspec_left_double_angle_quote,!text/keyspec_right_double_angle_quote",         /* double_raqm_laqm */ "!text/keyspec_right_double_angle_quote,!text/keyspec_left_double_angle_quote",         // The following each quotation mark triplet consists of         // <another quotation mark>, <opening quotation mark>, <closing quotation mark>         // and is named after (single|double)_<opening quotation mark>_<closing quotation mark>.         /* single_lqm_rqm */ "\u201A,\u2018,\u2019",         /* single_9qm_lqm */ "\u2019,\u201A,\u2018",         /* single_9qm_rqm */ "\u2018,\u201A,\u2019",         /* single_rqm_9qm */ "\u2018,\u2019,\u201A",         /* double_lqm_rqm */ "\u201E,\u201C,\u201D",         /* double_9qm_lqm */ "\u201D,\u201E,\u201C",         /* double_9qm_rqm */ "\u201C,\u201E,\u201D",         /* double_rqm_9qm */ "\u201C,\u201D,\u201E",         /* morekeys_single_quote */ "!fixedColumnOrder!5,!text/single_quotes,!text/single_angle_quotes",         /* morekeys_double_quote */ "!fixedColumnOrder!5,!text/double_quotes,!text/double_angle_quotes",         /* morekeys_tablet_double_quote */ "!fixedColumnOrder!6,!text/double_quotes,!text/single_quotes,!text/double_angle_quotes,!text/single_angle_quotes",         /* morekeys_cyrillic_ya */ EMPTY,         /* morekeys_cyrillic_yu */ EMPTY,     };.Groups[1].Value - '/*' - '*/').Trim() + @")
    private val TEXTS_DEFAULT = arrayOf()
    private val TEXTS_af = arrayOf()
    private val TEXTS_ar = arrayOf()
    private val TEXTS_az_AZ = arrayOf()
    private val TEXTS_be_BY = arrayOf()
    private val TEXTS_bg = arrayOf()
    private val TEXTS_bn_BD = arrayOf()
    private val TEXTS_bn_IN = arrayOf()
    private val TEXTS_ca = arrayOf()
    private val TEXTS_cs = arrayOf()
    private val TEXTS_da = arrayOf()
    private val TEXTS_de = arrayOf()
    private val TEXTS_el = arrayOf()
    private val TEXTS_en = arrayOf()
    private val TEXTS_eo = arrayOf()
    private val TEXTS_es = arrayOf()
    private val TEXTS_et_EE = arrayOf()
    private val TEXTS_eu_ES = arrayOf()
    private val TEXTS_fa = arrayOf()
    private val TEXTS_fi = arrayOf()
    private val TEXTS_fr = arrayOf()
    private val TEXTS_gl_ES = arrayOf()
    private val TEXTS_hi = arrayOf()
    private val TEXTS_hi_ZZ = arrayOf()
    private val TEXTS_hr = arrayOf()
    private val TEXTS_hu = arrayOf()
    private val TEXTS_hy_AM = arrayOf()
    private val TEXTS_is = arrayOf()
    private val TEXTS_it = arrayOf()
    private val TEXTS_iw = arrayOf()
    private val TEXTS_ka_GE = arrayOf()
    private val TEXTS_kk = arrayOf()
    private val TEXTS_km_KH = arrayOf()
    private val TEXTS_kn_IN = arrayOf()
    private val TEXTS_ky = arrayOf()
    private val TEXTS_lo_LA = arrayOf()
    private val TEXTS_lt = arrayOf()
    private val TEXTS_lv = arrayOf()
    private val TEXTS_mk = arrayOf()
    private val TEXTS_ml_IN = arrayOf()
    private val TEXTS_mn_MN = arrayOf()
    private val TEXTS_mr_IN = arrayOf()
    private val TEXTS_nb = arrayOf()
    private val TEXTS_ne_NP = arrayOf()
    private val TEXTS_nl = arrayOf()
    private val TEXTS_pl = arrayOf()
    private val TEXTS_pt = arrayOf()
    private val TEXTS_rm = arrayOf()
    private val TEXTS_ro = arrayOf()
    private val TEXTS_ru = arrayOf()
    private val TEXTS_sah = arrayOf()
    private val TEXTS_si_LK = arrayOf()
    private val TEXTS_sk = arrayOf()
    private val TEXTS_sl = arrayOf()
    private val TEXTS_sr = arrayOf()
    private val TEXTS_sr_ZZ = arrayOf()
    private val TEXTS_sv = arrayOf()
    private val TEXTS_sw = arrayOf()
    private val TEXTS_ta_IN = arrayOf()
    private val TEXTS_ta_LK = arrayOf()
    private val TEXTS_ta_SG = arrayOf()
    private val TEXTS_te_IN = arrayOf()
    private val TEXTS_th = arrayOf()
    private val TEXTS_tl = arrayOf()
    private val TEXTS_tr = arrayOf()
    private val TEXTS_uk = arrayOf()
    private val TEXTS_ur = arrayOf()
    private val TEXTS_uz_UZ = arrayOf()
    private val TEXTS_vi = arrayOf()
    private val TEXTS_zu = arrayOf()
    private val TEXTS_zz = arrayOf()
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
