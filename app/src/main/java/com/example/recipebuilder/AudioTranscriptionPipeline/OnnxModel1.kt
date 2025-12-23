package com.example.recipebuilder.AudioTranscriptionPipeline

import ai.onnxruntime.*
import com.example.recipebuilder.AudioTranscriptionPipeline.NewTokenizerBundle.AndroidTokenizer

class OnnxModel1(
    modelPath: String,
    private val tokenizer: AndroidTokenizer,
    private val id2label: Map<Int, String>     // <-- pass label map
) {

    private val env = OrtEnvironment.getEnvironment()
    private val session = env.createSession(modelPath)

    fun predict(words: List<String>): List<String> {

        val tok = tokenizer.tokenizeWords(words)

        // Convert IntArray → LongArray
        val inputIds2D = arrayOf(tok.inputIds.map { it.toLong() }.toLongArray())
        val attention2D = arrayOf(tok.attentionMask.map { it.toLong() }.toLongArray())

        val inputs = mapOf(
            "input_ids" to OnnxTensor.createTensor(env, inputIds2D),
            "attention_mask" to OnnxTensor.createTensor(env, attention2D)
        )

        val output = session.run(inputs)
        val logits3D = output[0].value as Array<Array<FloatArray>> // shape [1][seq][labels]
        val logits = logits3D[0]                                   // shape [seq][labels]

        val labels = mutableListOf<String>()
        var lastWord = -1

        for (i in tok.wordIds.indices) {
            val wordId = tok.wordIds[i]

            // 🔥 FIX: skip CLS and SEP tokens (wordId < 0)
            if (wordId < 0) continue

            // Skip duplicate wordpieces
            if (wordId == lastWord) continue

            val predIdx = argmaxFloatArray(logits[i])
            val label = id2label[predIdx] ?: "O"
            labels.add(label)

            lastWord = wordId
        }

        return labels
    }

    private fun argmaxFloatArray(arr: FloatArray): Int {
        var max = arr[0]
        var idx = 0
        for (i in 1 until arr.size) {
            if (arr[i] > max) {
                max = arr[i]
                idx = i
            }
        }
        return idx
    }
    fun close() {
        try {
            session.close()
        } catch (_: Exception) {}

        // Don't close OrtEnvironment globally — it is a singleton.
        // ORT handles multiple sessions fine.
    }
}
