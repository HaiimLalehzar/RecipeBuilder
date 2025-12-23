package com.example.recipebuilder.AudioTranscriptionPipeline.Whisper

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class SentenceChunk(
    val index: Int,
    val text: String
)

class WhisperSentenceAssembler {

    private val _sentences = MutableSharedFlow<SentenceChunk>()
    val sentences: SharedFlow<SentenceChunk> = _sentences

    private val _isCompleted = MutableSharedFlow<Unit>(replay = 1)
    val isCompleted: SharedFlow<Unit> = _isCompleted


    private var currentBuffer = StringBuilder()
    private var sentenceCounter = 0

    // WhisperKit timestamps: [00:00:00.000 --> 00:00:03.200]
    private val whisperTimestamp = Regex(
        "\\[\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s*-->\\s*\\d{2}:\\d{2}:\\d{2}\\.\\d{3}]"
    )

    // OpenAI timestamps: <|0.00|>  or <|23.56|>
    private val openAITimestamp = Regex("<\\|\\d+\\.\\d+\\|>")

    private val initToken = Regex("\\[INIT]\\s*")

    suspend fun processRawChunk(raw: String) {
        Log.d("Assembler", "RAW_IN: $raw")

        // ----------------------------------------------------
        // 1. Remove *all* timestamps BEFORE anything else
        // ----------------------------------------------------
        var cleaned = raw
            .replace(whisperTimestamp, ". ")     // treat as hard sentence end
            .replace(openAITimestamp, " ")       // treat as soft boundary
            .replace(initToken, " ")
            .replace("<|startoftranscript|>", " ")
            .replace("<|endoftext|>", ". ")      // force boundary
            .trim()

        if (cleaned.isBlank()) return

        Log.d("Assembler", "CLEANED_RAW: '$cleaned'")

        // append to working buffer
        currentBuffer.append(" ").append(cleaned)

        // try emitting sentences
        maybeEmitSentences()

        // if explicit end-of-text → flush remainder
        if (raw.contains("<|endoftext|>")) {
            flushAllRemaining()
        }
    }

    /**
     * Extract complete sentences from buffer.
     */
    private suspend fun maybeEmitSentences() {
        val text = currentBuffer.toString().trim()
        if (text.isEmpty()) return

        // Split on punctuation boundaries ONLY
        val parts = text.split(Regex("(?<=[.!?])\\s+"))

        Log.d("Assembler", "PUNCT_SPLIT(${parts.size}): '$text'")

        // emit all complete sentences except last one (incomplete)
        for (i in 0 until parts.size - 1) {
            val cleaned = cleanSentence(parts[i])
            if (cleaned.isNotBlank()) emitSentence(cleaned)
        }

        // keep last fragment for future merge
        currentBuffer.clear()
        currentBuffer.append(parts.last())
        Log.d("Assembler", "Remaining buffer='${currentBuffer.toString()}'")
    }

    /**
     * Emit whatever is left at EOF.
     */
     suspend fun flushAllRemaining() {
        val final = cleanSentence(currentBuffer.toString())
        if (final.isNotBlank()) {
            Log.d("Assembler", "→ Emit FINAL: $final")
            emitSentence(final)
        }
        currentBuffer.clear()

        // 🚨 Emit completion signal
        Log.d("Assembler", "### ASSEMBLER COMPLETE ###")
        _isCompleted.emit(Unit)
    }
    suspend fun flushPartial() {
        val partial = cleanSentence(currentBuffer.toString())
        if (partial.isNotBlank()) {
            emitSentence(partial)
        }
        currentBuffer.clear()
    }



    /**
     * Final cleanup of a single sentence.
     */
    private fun cleanSentence(raw: String): String {
        var t = raw

        // remove any leftover timestamps
        t = t.replace(whisperTimestamp, " ")
            .replace(openAITimestamp, " ")
            .replace(initToken, " ")

        // normalize whitespace
        t = t.replace(Regex("\\s+"), " ").trim()

        if (t.isEmpty()) return ""

        // ensure punctuation
        if (!t.endsWith(".") && !t.endsWith("!") && !t.endsWith("?")) {
            t += "."
        }

        // safe uppercase
        t = capitalizeFirst(t)

        return t
    }

    private fun capitalizeFirst(t: String): String {
        if (t.isEmpty()) return t
        val ch = t[0]
        return if (ch.isLowerCase()) ch.uppercaseChar() + t.substring(1) else t
    }

    private suspend fun emitSentence(text: String) {
        Log.d("Assembler", "→ Emit (sentence#$sentenceCounter): $text")
        _sentences.emit(SentenceChunk(sentenceCounter++, text))
    }
}
