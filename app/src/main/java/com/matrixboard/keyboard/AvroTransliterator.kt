package com.matrixboard.keyboard

/**
 * Clean-room Roman -> Bangla phonetic (Avro-style) transliteration.
 * Type "ami" -> "আমি", "bangla" -> "বাংলা". See README for limits.
 */
object AvroTransliterator {

    // Exact overrides for everyday words where generic rules alone give a
    // slightly different spelling. Key = lowercase roman, value = Bangla.
    private val commonWords = mapOf(
        "ami" to "আমি", "tumi" to "তুমি", "amar" to "আমার", "tomar" to "তোমার",
        "kemon" to "কেমন", "acho" to "আছো", "achen" to "আছেন",
        "bhalo" to "ভালো", "bangla" to "বাংলা", "bangladesh" to "বাংলাদেশ",
        "dhonnobad" to "ধন্যবাদ", "dhonyobad" to "ধন্যবাদ",
        "onek" to "অনেক", "ki" to "কি", "kothay" to "কোথায়",
        "kobe" to "কবে", "keno" to "কেন", "kivabe" to "কিভাবে",
        "ajke" to "আজকে", "kalke" to "কালকে", "ekhon" to "এখন",
        "bari" to "বাড়ি", "bhai" to "ভাই", "bon" to "বোন",
        "bondhu" to "বন্ধু", "baba" to "বাবা", "ma" to "মা",
        "khabar" to "খাবার", "pani" to "পানি", "valo" to "ভালো",
        "apni" to "আপনি", "tara" to "তারা",
        "amra" to "আমরা", "tomra" to "তোমরা", "sobai" to "সবাই",
        "hobe" to "হবে", "ache" to "আছে", "nai" to "নাই",
        "bhalobashi" to "ভালোবাসি", "shundor" to "সুন্দর",
        "aschi" to "আসছি", "jacchi" to "যাচ্ছি", "korchi" to "করছি",
        "korsi" to "করছি", "bolchi" to "বলছি", "shokal" to "সকাল",
        "rat" to "রাত", "din" to "দিন", "mash" to "মাস"
    )

    private data class Phoneme(val roman: String, val bangla: String, val isVowel: Boolean, val vowelSign: String?)

    // Longest roman first at lookup time; list order does not matter because
    // transliterate() tries longest match first.
    private val consonants = mapOf(
        "kh" to "খ", "gh" to "ঘ", "ch" to "ছ", "jh" to "ঝ", "Th" to "ঠ",
        "Dh" to "ঢ", "ph" to "ফ", "bh" to "ভ", "sh" to "শ", "ng" to "ঙ",
        "k" to "ক", "g" to "গ", "c" to "চ", "j" to "জ", "T" to "ট",
        "D" to "ড", "N" to "ণ", "t" to "ত", "d" to "দ", "n" to "ন",
        "p" to "প", "b" to "ব", "m" to "ম", "y" to "য", "r" to "র",
        "l" to "ল", "S" to "ষ", "s" to "স", "h" to "হ",
        "R" to "ড়", "Rh" to "ঢ়", "z" to "জ", "w" to "ও", "f" to "ফ",
        "v" to "ভ", "q" to "ক"
    )

    // roman -> (independent vowel, vowel sign)
    private val vowels = mapOf(
        "rri" to ("ঋ" to "ৃ"), "ou" to ("ঔ" to "ৌ"), "oi" to ("ঐ" to "ৈ"),
        "aa" to ("আ" to "া"), "ii" to ("ঈ" to "ী"), "uu" to ("ঊ" to "ূ"),
        "a" to ("অ" to ""), "i" to ("ই" to "ি"), "u" to ("উ" to "ু"),
        "e" to ("এ" to "ে"), "o" to ("ও" to "ো")
    )

    private val hasant = "্"

    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""
        // Preserve a trailing capital run exactly? No - Avro is case sensitive,
        // so only the common-word override is case-insensitive.
        commonWords[input.lowercase()]?.let { return it }
        if (commonWords[input]?.isNotEmpty() == true) return commonWords[input]!!

        // Tokenise into phonemes, longest match first.
        val tokens = mutableListOf<Phoneme>()
        var i = 0
        while (i < input.length) {
            var matched: Phoneme? = null
            var len = 0
            // Try 3, 2, 1 length matches.
            for (l in 3 downTo 1) {
                if (i + l > input.length) continue
                val piece = input.substring(i, i + l)
                vowels[piece]?.let { (ind, sign) ->
                    matched = Phoneme(piece, ind, true, sign); len = l; break
                }
                consonants[piece]?.let { b ->
                    matched = Phoneme(piece, b, false, null); len = l; break
                }
            }
            if (matched == null) {
                // Unknown char (digit / punctuation): pass through.
                tokens.add(Phoneme(input[i].toString(), input[i].toString(), false, null))
                i += 1
            } else {
                tokens.add(matched!!); i += len
            }
        }

        val out = StringBuilder()
        var pendingConsonant = false // last output char is a consonant with no vowel yet
        var firstToken = true
        for ((idx, tok) in tokens.withIndex()) {
            // ng before a consonant becomes anusvara, e.g. bangla -> বাংলা, not বঙ্লা
            if (tok.roman == "ng" && idx + 1 < tokens.size && !tokens[idx+1].isVowel && consonants.containsKey(tokens[idx+1].roman)) {
                if (pendingConsonant) out.append(hasant)
                out.append("ং")
                pendingConsonant = false
                firstToken = false
                continue
            }
            if (tok.isVowel) {
                val (ind, sign) = vowels[tok.roman] ?: ("অ" to "")
                if (pendingConsonant) {
                    // Vowel sign on the previous consonant. 'a' adds nothing.
                    // 'o' sign is "ো", 'ou' is "ৌ" - both single codepoints in the table above.
                    out.append(sign)
                    pendingConsonant = false
                } else {
                    // Word-start / after a vowel: independent vowel.
                    // Special Avro-like rule: a word-initial 'a' before a consonant
                    // is pronounced long in everyday words (ami -> আমি, amar -> আমার).
                    if (firstToken && tok.roman == "a") out.append("আ") else out.append(ind)
                    pendingConsonant = false
                }
            } else {
                if (pendingConsonant) {
                    // Two consonants in a row with no vowel: conjunct with hasant.
                    out.append(hasant)
                }
                out.append(tok.bangla)
                // Only real Bangla consonants stay pending; passed-through chars do not.
                pendingConsonant = consonants.containsKey(tok.roman)
            }
            firstToken = false
        }
        return out.toString()
    }
}
