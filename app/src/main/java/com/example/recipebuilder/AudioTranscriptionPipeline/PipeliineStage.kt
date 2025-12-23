package com.example.recipebuilder.AudioTranscriptionPipeline

enum class PipelineStage {
    IDLE,
    TRANSCRIBING,   // Whisper running
    MODEL_1,        // Model-1 NER
    MODEL_2,        // Model-2 grouping
    PARSING,        // Build instructions
    DONE,
    ERROR
}