package com.matrix.background

/**
 * Matrix glyph characters - Unicode characters that resemble the Matrix digital rain
 *
 * The original glmatrix.c uses a texture atlas with characters arranged in a 16x13 grid.
 * Here we map the glyph indices to Unicode characters that create a similar effect.
 *
 * The character set includes:
 * - Half-width Katakana (the distinctive Matrix look)
 * - Numbers and symbols
 * - Latin characters (mirrored in the original)
 */
object MatrixGlyphs {

    /**
     * Glyph characters arranged to match the texture atlas layout from glmatrix.c
     * Index corresponds to glyph number in the encoding arrays
     *
     * Row 0 (0-15): Symbols and punctuation
     * Row 1 (16-31): Numbers 0-9 and more symbols
     * Row 2 (32-47): Uppercase letters A-O and @
     * Row 3 (48-63): Uppercase letters P-Z and more symbols
     * Row 4 (64-79): Lowercase letters a-o and `
     * Row 5 (80-95): Lowercase letters p-z and more symbols
     * Row 6 (96): Blank/space character
     * Rows 7-12 (97-175): Half-width Katakana and special Matrix glyphs
     */
    private val glyphChars = arrayOf(
        // Row 0 (0-15): Symbols
        ' ', '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
        // Row 1 (16-31): Numbers and symbols
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?',
        // Row 2 (32-47): @ and uppercase A-O
        '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
        // Row 3 (48-63): Uppercase P-Z and symbols
        'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', '\\', ']', '^', '_',
        // Row 4 (64-79): ` and lowercase a-o
        '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
        // Row 5 (80-95): Lowercase p-z and symbols
        'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', ' ',
        // Row 6 (96): Blank
        ' ',
        // Row 7+ (97-175): Half-width Katakana - the distinctive Matrix characters
        // These are the characters that give Matrix its iconic look
        '\uFF66', // ヲ (97)
        '\uFF67', // ァ (98)
        '\uFF68', // ィ (99)
        '\uFF69', // ゥ (100)
        '\uFF6A', // ェ (101)
        '\uFF6B', // ォ (102)
        '\uFF6C', // ャ (103)
        '\uFF6D', // ュ (104)
        '\uFF6E', // ョ (105)
        '\uFF6F', // ッ (106)
        '\uFF70', // ー (107)
        '\uFF71', // ア (108)
        '\uFF72', // イ (109)
        '\uFF73', // ウ (110)
        '\uFF74', // エ (111)
        '\uFF75', // オ (112)
        '\uFF76', // カ (113)
        '\uFF77', // キ (114)
        '\uFF78', // ク (115)
        '\uFF79', // ケ (116)
        '\uFF7A', // コ (117)
        '\uFF7B', // サ (118)
        '\uFF7C', // シ (119)
        '\uFF7D', // ス (120)
        '\uFF7E', // セ (121)
        '\uFF7F', // ソ (122)
        '\uFF80', // タ (123)
        '\uFF81', // チ (124)
        '\uFF82', // ツ (125)
        '\uFF83', // テ (126)
        '\uFF84', // ト (127)
        '\uFF85', // ナ (128)
        '\uFF86', // ニ (129)
        '\uFF87', // ヌ (130)
        '\uFF88', // ネ (131)
        '\uFF89', // ノ (132)
        '\uFF8A', // ハ (133)
        '\uFF8B', // ヒ (134)
        '\uFF8C', // フ (135)
        '\uFF8D', // ヘ (136)
        '\uFF8E', // ホ (137)
        '\uFF8F', // マ (138)
        '\uFF90', // ミ (139)
        '\uFF91', // ム (140)
        '\uFF92', // メ (141)
        '\uFF93', // モ (142)
        '\uFF94', // ヤ (143)
        '\uFF95', // ユ (144)
        '\uFF96', // ヨ (145)
        '\uFF97', // ラ (146)
        '\uFF98', // リ (147)
        '\uFF99', // ル (148)
        '\uFF9A', // レ (149)
        '\uFF9B', // ロ (150)
        '\uFF9C', // ワ (151)
        '\uFF9D', // ン (152)
        '\uFF9E', // ゙ (153)
        '\uFF9F', // ゚ (154)
        // Additional characters (155-175) - using more katakana variants and symbols
        '\u30A2', // ア full-width (155)
        '\u30A4', // イ (156)
        '\u30A6', // ウ (157)
        '\u30A8', // エ (158)
        '\u30AA', // オ (159)
        '\u30AB', // カ (160)
        '\u30AD', // キ (161)
        '\u30AF', // ク (162)
        '\u30B1', // ケ (163)
        '\u30B3', // コ (164)
        '\u30B5', // サ (165)
        '\u30B7', // シ (166)
        '\u30B9', // ス (167)
        '\u30BB', // セ (168)
        '\u30BD', // ソ (169)
        '\u30BF', // タ (170)
        '\u30C1', // チ (171)
        '\u30C4', // ツ (172)
        '\u30C6', // テ (173)
        '\u30C8', // ト (174)
        '\u30CA', // ナ (175)
    )

    /**
     * Get the character for a given glyph index.
     * Handles the +1 offset used in the glmatrix.c encoding (where 0 means empty)
     *
     * @param glyphIndex The glyph index (1-based from the encoding, or absolute value if spinner)
     * @return The Unicode character to display
     */
    fun getChar(glyphIndex: Int): Char {
        // glyphIndex is 1-based in the glmatrix code (0 means no glyph)
        // Convert to 0-based index
        val index = glyphIndex - 1
        return if (index >= 0 && index < glyphChars.size) {
            glyphChars[index]
        } else {
            ' '
        }
    }

    /**
     * Get a random Matrix-style character
     */
    fun getRandomMatrixChar(): Char {
        // Prefer katakana characters (indices 97-175) for authentic Matrix look
        val katakanaStart = 97
        val katakanaEnd = 175
        val index = (katakanaStart..katakanaEnd).random()
        return if (index < glyphChars.size) glyphChars[index] else '\uFF71'
    }
}
