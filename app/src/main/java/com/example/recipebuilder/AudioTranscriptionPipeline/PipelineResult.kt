package com.example.recipebuilder.AudioTranscriptionPipeline


data class PipelineResult(
    val words: List<String>,
    val m1Labels: List<String>,
    val m2Labels: List<String>,
    val sentenceIndex: Int
)
