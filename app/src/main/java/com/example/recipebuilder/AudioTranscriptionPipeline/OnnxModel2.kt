// OnnxModel2.kt
package com.example.recipebuilder.AudioTranscriptionPipeline

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.util.Log
import com.example.recipebuilder.AudioTranscriptionPipeline.NewTokenizerBundle.AndroidTokenizer

class OnnxModel2(
    modelPath: String,
    private val tokenizer: AndroidTokenizer,
    private val id2labelM2: Map<Int, String>,
    private val m1LabelToId: Map<String, Int>
) {

    private val env = OrtEnvironment.getEnvironment()
    private val session = env.createSession(modelPath)

    fun predict(words: List<String>, m1Labels: List<String>): List<String> {
        return try {
            if (words.isEmpty()) return emptyList()

            // -------------------------------------------------------
            // 1) TOKENIZE → WordPiece + wordIds
            // -------------------------------------------------------
            val tok = tokenizer.tokenizeWords(words)
            val seqLen = tok.inputIds.size

            Log.d("M2", "seqLen=$seqLen  words=${words.size}")

            // Build input arrays
            val ids = Array(1) { LongArray(seqLen) }
            val mask = Array(1) { LongArray(seqLen) }
            val m1  = Array(1) { LongArray(seqLen) }

            val padId = tokenizer.vocab["[PAD]"]!!
            val clsId = tokenizer.vocab["[CLS]"]!!
            val sepId = tokenizer.vocab["[SEP]"]!!

            // -------------------------------------------------------
            // 2) ALIGN M1 LABELS TO SUBWORDS (MATCH PYTORCH TRAINING)
            // -------------------------------------------------------
            for (i in 0 until seqLen) {

                val tokenId = tok.inputIds[i]
                val wid = tok.wordIds[i]

                ids[0][i] = tokenId.toLong()
                mask[0][i] = tok.attentionMask[i].toLong()

                val m1id: Long =
                    if (tokenId == padId || tokenId == clsId || tokenId == sepId) {
                        //// FIXED TO MATCH PYTORCH TRAINING (-100 for CLS/SEP/PAD)
                        -100L
                    } else if (wid >= 0 && wid < m1Labels.size) {
                        val real = m1Labels[wid]
                        m1LabelToId[real]?.toLong() ?: -100L
                    } else {
                        // Should not happen but safe fallback
                        -100L
                    }

                m1[0][i] = m1id

                Log.d(
                    "M2_ALIGN",
                    "i=$i tokenId=$tokenId wid=$wid → m1_id=$m1id (${if (wid >= 0 && wid < m1Labels.size) m1Labels[wid] else "NONE"})"
                )
            }

            // Create ONNX tensors
            val tIds = OnnxTensor.createTensor(env, ids)
            val tMask = OnnxTensor.createTensor(env, mask)
            val tM1 = OnnxTensor.createTensor(env, m1)

            val inputs = mapOf(
                "input_ids" to tIds,
                "attention_mask" to tMask,
                "m1_ids" to tM1
            )

            // -------------------------------------------------------
            // 3) RUN MODEL
            // -------------------------------------------------------
            session.run(inputs).use { out ->
                @Suppress("UNCHECKED_CAST")
                val logits = (out[0].value as Array<Array<FloatArray>>)[0]

                // ---------------------------------------------------
                // 4) WORD-LEVEL MAX POOLING OVER SUBWORDS
                // ---------------------------------------------------
                val wordCount = words.size
                val labelCount = id2labelM2.size
                val pooled = Array(wordCount) { FloatArray(labelCount) { Float.NEGATIVE_INFINITY } }

                for (i in logits.indices) {
                    val wid = tok.wordIds[i]
                    if (wid < 0 || wid >= wordCount) continue

                    for (j in 0 until labelCount) {
                        val v = logits[i][j]
                        if (v > pooled[wid][j]) pooled[wid][j] = v
                    }
                }

                // ---------------------------------------------------
                // 5) ARGMAX PER WORD
                // ---------------------------------------------------
                val outLabels = MutableList(wordCount) { "O" }

                for (w in 0 until wordCount) {
                    val vec = pooled[w]
                    val valid = vec.any { it > Float.NEGATIVE_INFINITY }
                    val idx = if (valid) argmax(vec) else 0
                    outLabels[w] = id2labelM2[idx] ?: "O"

                    Log.d("M2_PRED", "word=$w '${words[w]}' → ${outLabels[w]}")
                }

                return outLabels
            }

        } catch (e: Exception) {
            Log.e("Model2", "ERROR: ${e.message}", e)
            emptyList()
        }
    }

    private fun argmax(arr: FloatArray): Int {
        var max = arr[0]
        var ix = 0
        for (i in 1 until arr.size) {
            if (arr[i] > max) { max = arr[i]; ix = i }
        }
        return ix
    }

    fun close() {
        try { session.close() } catch (_: Exception) {}
    }
}
