package com.example.recipebuilder.domain.transcription


import com.example.recipebuilder.AudioTranscriptionPipeline.PipelineResult

interface TranscriptionModelRunner {
    suspend fun run(sentences: List<String>): List<PipelineResult>
}
