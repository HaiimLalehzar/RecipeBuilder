package com.example.recipebuilder.AudioTranscriptionPipeline.NewTokenizerBundle

class AndroidWordPieceTokenizer(
    private val vocab: Map<String, Int>,
    private val unkId: Int
) {

    fun tokenizeWord(word: String): List<Int> {
        // Exact match
        vocab[word]?.let { return listOf(it) }

        val tokens = mutableListOf<Int>()
        var start = 0

        while (start < word.length) {
            var end = word.length
            var found: String? = null

            while (end > start) {
                val sub =
                    if (start == 0) word.substring(start, end)
                    else "##" + word.substring(start, end)

                if (vocab.containsKey(sub)) {
                    found = sub
                    break
                }
                end--
            }

            if (found == null) {
                tokens += unkId
                return tokens
            }

            tokens += vocab[found]!!
            start = if (found.startsWith("##"))
                start + (found.length - 2)
            else
                end
        }

        return tokens
    }
}
