package com.example.recipebuilder.AudioTranscriptionPipeline.NewTokenizerBundle

class AndroidTokenizer(
    val vocab: Map<String, Int>,
    private val doLowerCase: Boolean = true,
    private val neverSplit: Set<String> = emptySet(),
    private val stripAccents: Boolean = false,
    private val stripPunctuation: Boolean = true,
    private val maxSeqLen: Int = 128    // ⚠️ MUST MATCH training max_length
) {

    private val basic = AndroidBasicTokenizer(
        doLowerCase = doLowerCase,
        neverSplit = neverSplit,
        stripAccents = stripAccents,
        removePunctuation = stripPunctuation
    )

    private val wordPiece = AndroidWordPieceTokenizer(
        vocab = vocab,
        unkId = vocab["[UNK]"]!!
    )

    private val clsId = vocab["[CLS]"]!!
    private val sepId = vocab["[SEP]"]!!
    private val padId = vocab["[PAD]"]!!   // MobileBERT vocab has this

    fun tokenizeText(text: String): TokenizedResult {
        val basicTokens = basic.tokenize(text)
        return tokenizeWords(basicTokens)
    }

    fun tokenizeWords(words: List<String>): TokenizedResult {
        val input = mutableListOf<Int>()
        val wordIds = mutableListOf<Int>()

        // [CLS]
        input += clsId
        wordIds += -1

        // WordPiece tokens
        words.forEachIndexed { index, word ->
            val pieces = wordPiece.tokenizeWord(word)
            for (p in pieces) {
                input += p
                wordIds += index
            }
        }

        // [SEP]
        input += sepId
        wordIds += -1

        // --- Truncate if too long ---
        if (input.size > maxSeqLen) {
            // keep first maxSeqLen-1 and force last token to [SEP]
            val truncated = input.subList(0, maxSeqLen).toMutableList()
            truncated[truncated.lastIndex] = sepId
            input.clear()
            input.addAll(truncated)

            val truncatedWordIds = wordIds.subList(0, maxSeqLen)
            wordIds.clear()
            wordIds.addAll(truncatedWordIds)
        }

        // --- Pad if too short ---
        if (input.size < maxSeqLen) {
            val padLen = maxSeqLen - input.size
            repeat(padLen) {
                input += padId
                wordIds += -1    // no associated word
            }
        }

        // --- Attention mask: 1 for non-PAD, 0 for PAD ---
        val att = IntArray(input.size) { i ->
            if (input[i] == padId) 0 else 1
        }

        return TokenizedResult(
            inputIds = input.toIntArray(),
            attentionMask = att,
            wordIds = wordIds
        )
    }

    fun splitWords(text: String): List<String> {
        return basic.tokenize(text)
    }
}

data class TokenizedResult(
    val inputIds: IntArray,
    val attentionMask: IntArray,
    val wordIds: List<Int>
)
