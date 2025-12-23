package com.example.recipebuilder.domain.transcription

import android.app.Application
import com.example.recipebuilder.AudioTranscriptionPipeline.NewTokenizerBundle.AndroidTokenizer
import com.example.recipebuilder.AudioTranscriptionPipeline.OnnxModel1
import com.example.recipebuilder.AudioTranscriptionPipeline.OnnxModel2
import com.example.recipebuilder.AudioTranscriptionPipeline.PipelineResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnnxTranscriptionModelRunner(
    private val app: Application,
    private val tokenizerM1: AndroidTokenizer,
    private val tokenizerM2: AndroidTokenizer,
    private val model1File: String,
    private val model2File: String,
    private val id2labelM1: Map<Int, String>,
    private val id2labelM2: Map<Int, String>,
    private val m1LabelToId: Map<String, Int>
) : TranscriptionModelRunner {

    override suspend fun run(sentences: List<String>): List<PipelineResult> = withContext(Dispatchers.IO) {

        val model1 = OnnxModel1(
            modelPath = model1File,
            tokenizer = tokenizerM1,
            id2label = id2labelM1
        )

        val model2 = OnnxModel2(
            modelPath = model2File,
            tokenizer = tokenizerM2,
            id2labelM2 = id2labelM2,
            m1LabelToId = m1LabelToId
        )

        try {
            sentences.mapIndexed { index, sent ->

                val words = tokenizerM1.splitWords(sent)

                val m1Labels = model1.predict(words)

                val m2Labels = model2.predict(words, m1Labels)

                PipelineResult(
                    words = words,
                    m1Labels = m1Labels,
                    m2Labels = m2Labels,
                    sentenceIndex = index
                )
            }

        } finally {
            model1.close()
            model2.close()
        }
    }
}
