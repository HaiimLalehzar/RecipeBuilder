package com.example.recipebuilder.AudioTranscriptionPipeline.NewTokenizerBundle

import java.text.Normalizer

class AndroidBasicTokenizer(
    private val doLowerCase: Boolean,
    private val neverSplit: Set<String>,
    private val stripAccents: Boolean,
    private val removePunctuation: Boolean
) {

    fun tokenize(text: String): List<String> {
        // 1) Clean basic whitespace + normalize curly quotes
        var t = clean(text)

        // 2) Put spaces around punctuation (HF-style), unless user asked to remove
        t = if (removePunctuation) {
            // Drop punctuation entirely
            t.replace(Regex("[.,!?;:()]"), " ")
        } else {
            // Split punctuation into separate tokens
            // This approximates HF's punctuation splitting
            t.replace(Regex("([.,!?;:()\"'])"), " $1 ")
        }

        // 3) Split on whitespace
        val rawTokens = t.split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        // 4) Apply lowercasing / accents / neverSplit logic
        val out = mutableListOf<String>()
        for (tok in rawTokens) {
            // If token is in neverSplit, we leave it exactly as-is
            if (neverSplit.contains(tok)) {
                out += tok
                continue
            }

            var cur = tok
            if (doLowerCase) cur = cur.lowercase()
            if (stripAccents) cur = stripAccentsFn(cur)

            out += cur
        }
        return out
    }

    private fun clean(s: String): String {
        return s
            .replace("\n", " ")
            // Normalize curly apostrophes to straight
            .replace("[\u2018\u2019]".toRegex(), "'")
            .trim()
    }

    private fun stripAccentsFn(s: String): String {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
    }
}
