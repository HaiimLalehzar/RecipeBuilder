package com.example.recipebuilder.AudioTranscriptionPipeline

object LabelMaps {

    val m1Labels = listOf(
        "O",
        "B-INGREDIENT","I-INGREDIENT",
        "B-DESCRIPTOR","I-DESCRIPTOR",
        "B-ACTION","I-ACTION",
        "B-UNIT","I-UNIT",
        "B-TIMEUNIT","I-TIMEUNIT",
        "B-QUANTITY","I-QUANTITY",
        "B-TOOL","I-TOOL",
        "B-CONNECTOR","I-CONNECTOR",
        "B-MODIFIER","I-MODIFIER",
        "B-FILLER","I-FILLER"
    )

    val m2Labels = listOf(
        "O",
        "B-STEP","I-STEP",
        "B-CONDITION","I-CONDITION",
        "B-PURPOSE","I-PURPOSE",
        "B-CORRECTION","I-CORRECTION",
        "B-PREP","I-PREP"
    )
}
