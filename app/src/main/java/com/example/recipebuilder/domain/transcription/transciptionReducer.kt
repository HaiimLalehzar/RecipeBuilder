package com.example.recipebuilder.domain.transcription

fun transcriptionReducer(
    old: TranscriptionDomainState,
    action: TranscriptionDomainAction
): TranscriptionDomainState {

    return when (action) {

        is TranscriptionDomainAction.Start -> {
            // reset everything and mark as starting
            old.copy(
                stage = com.example.recipebuilder.AudioTranscriptionPipeline.PipelineStage.TRANSCRIBING,
                rawText = "",
                sentences = emptyList(),
                pipelineResults = emptyList(),
                instructions = emptyList(),
                error = null
            )
        }

        is TranscriptionDomainAction.SetStage ->
            old.copy(stage = action.stage)

        is TranscriptionDomainAction.WhisperText ->
            old.copy(
                rawText = if (old.rawText.isEmpty())
                    action.text
                else
                    old.rawText + "\n" + action.text
            )

        is TranscriptionDomainAction.Sentence ->
            old.copy(sentences = old.sentences + action.text)

        is TranscriptionDomainAction.ModelResults ->
            old.copy(pipelineResults = action.results)

        is TranscriptionDomainAction.Parsed ->
            old.copy(instructions = action.instructions)

        is TranscriptionDomainAction.Error ->
            old.copy(error = action.message)
    }
}
