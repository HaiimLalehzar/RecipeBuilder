package com.example.recipebuilder.AudioTranscriptionPipeline.NewTokenizerBundle

import android.content.Context
import org.json.JSONObject

class MobileBertTokenizerFactory(
    private val context: Context,
    private val vocabAsset: String = "vocab_m1.txt",
    private val configAsset: String = "tokenization_config.json",
    private val stripPunctuation: Boolean = true
) {

    fun loadTokenizer(): AndroidTokenizer {
        val vocab = loadVocab(vocabAsset)
        val config = loadConfig(configAsset)

        val doLowerCase = config.optBoolean("do_lower_case", true)
        val stripAccents = config.optBoolean("strip_accents", false)
        val neverSplit = config.optJSONArray("never_split")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } ?: emptySet()

        return AndroidTokenizer(
            vocab = vocab,
            doLowerCase = doLowerCase,
            neverSplit = neverSplit,
            stripAccents = stripAccents,
            stripPunctuation = stripPunctuation
        )
    }

    private fun loadVocab(asset: String): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        context.assets.open(asset).bufferedReader().useLines { lines ->
            lines.forEachIndexed { idx, tok -> map[tok.trim()] = idx }
        }
        return map
    }

    private fun loadConfig(asset: String): JSONObject {
        return try {
            val txt = context.assets.open(asset).bufferedReader().readText()
            JSONObject(txt)
        } catch (e: Exception) {
            JSONObject() // fallback
        }
    }
}
