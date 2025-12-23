package com.example.recipebuilder.AudioTranscriptionPipeline

import android.util.Log

// ================================================================
// Clause Types
// ================================================================
enum class ClauseType {
    PREP,
    STEP,
    CONDITION,
    PURPOSE,
    CORRECTION,
    TIP,
    SERVING,
    META,
    O
}

enum class RelationToStep {
    BEFORE,
    AFTER
}

// ================================================================
// Data classes
// ================================================================
data class Clause(
    val type: ClauseType,
    val tokens: List<String>,
    val m1Labels: List<String>,
    val sentenceIndex: Int,
    val relationToStep: RelationToStep? = null,
    val attachments: ClauseAttachments = ClauseAttachments()
)

data class ClauseAttachments(
    val condition: List<Clause> = emptyList(),
    val purpose: List<Clause> = emptyList(),
    val correction: List<Clause> = emptyList()
)

data class Instruction(
    val stepTextRaw: String,
    val preConditionsRaw: List<String>,
    val postConditionsRaw: List<String>,
    val purposeRaw: List<String>,
    val correctionsRaw: List<String>,
    val followupStepRaw: String?,
    val followupPreConditionsRaw: List<String>,
    val followupPostConditionsRaw: List<String>,
    val sentenceIndexes: List<Int>
)

// ================================================================
// BIO → CLAUSES (with lookahead + I→B recovery)
// ================================================================
fun bioToClauses(
    tokens: List<String>,
    groupLabels: List<String>,
    m1Labels: List<String>,
    sentenceIndex: Int
): List<Clause> {

    val LOOKAHEAD = 2

    Log.d("ParserBIO", "---- BIO → CLAUSES for Sentence $sentenceIndex ----")

    val clauses = mutableListOf<Clause>()
    var currentType: ClauseType? = null
    var currentTokens = mutableListOf<String>()
    var currentM1 = mutableListOf<String>()

    fun flush() {
        if (currentType != null && currentTokens.isNotEmpty()) {
            clauses += Clause(
                type = currentType!!,
                tokens = currentTokens.toList(),
                m1Labels = currentM1.toList(),
                sentenceIndex = sentenceIndex
            )
        }
        currentType = null
        currentTokens = mutableListOf()
        currentM1 = mutableListOf()
    }

    for (i in tokens.indices) {
        val tok = tokens[i]
        val g = groupLabels[i]

        if (g == "O") {
            flush()
            clauses += Clause(
                type = ClauseType.O,
                tokens = listOf(tok),
                m1Labels = listOf(m1Labels[i]),
                sentenceIndex = sentenceIndex
            )
            continue
        }

        val isB = g.startsWith("B-")
        val isI = g.startsWith("I-")
        val typeName = g.substring(2)
        val type = ClauseType.valueOf(typeName)

        // ------------------------------------------------------------
        // LOOKAHEAD: If an upcoming B-X exists, skip this I-X
        // ------------------------------------------------------------
        if (isI) {
            var upcomingB = false
            for (k in 1..LOOKAHEAD) {
                if (i + k < groupLabels.size && groupLabels[i + k] == "B-$typeName") {
                    upcomingB = true
                    break
                }
            }
            if (upcomingB) {
                Log.w("ParserBIO", "SKIP I-$typeName at $i (B-$typeName ahead)")
                continue
            }
        }

        // ------------------------------------------------------------
        // I-X starting a clause without B-X (implicit B)
        // ------------------------------------------------------------
        if (isI && currentType == null) {
            Log.w("ParserBIO", "I-$typeName promoted to B-$typeName at $i")
            flush()
            currentType = type
            currentTokens.add(tok)
            currentM1.add(m1Labels[i])
            continue
        }

        // ------------------------------------------------------------
        // NORMAL BIO behavior
        // ------------------------------------------------------------
        if (isB) {
            flush()
            currentType = type
            currentTokens.add(tok)
            currentM1.add(m1Labels[i])
        } else if (isI) {
            if (currentType == type) {
                currentTokens.add(tok)
                currentM1.add(m1Labels[i])
            } else {
                // invalid sequence recover
                flush()
                currentType = type
                currentTokens.add(tok)
                currentM1.add(m1Labels[i])
            }
        }
    }

    flush()
    return clauses
}

// ================================================================
// ABSORB O CLAUSES (punctuation merges)
// ================================================================
fun absorbOClauses(input: List<Clause>): List<Clause> {
    val out = mutableListOf<Clause>()
    for (c in input) {
        if (c.type == ClauseType.O && c.tokens.size == 1) {
            val t = c.tokens.first()
            if (t.matches(Regex("[,.!?]")) && out.isNotEmpty()) {
                val prev = out.removeAt(out.lastIndex)
                out += prev.copy(
                    tokens = prev.tokens + t,
                    m1Labels = prev.m1Labels + c.m1Labels
                )
                continue
            }
        }
        out += c
    }
    return out
}

// ================================================================
// STEP MERGING HEURISTICS
// ================================================================
private fun shouldMergeSteps(a: Clause, b: Clause): Boolean {

    // MUST both be step
    if (a.type != ClauseType.STEP || b.type != ClauseType.STEP) return false

    // Same sentence = strong merge
    if (a.sentenceIndex == b.sentenceIndex) return true

    // If next step is tiny and contains no ingredient, merge
    val small = b.tokens.size <= 4
    val hasIngredient = b.m1Labels.any { it.contains("INGREDIENT") }

    if (small && !hasIngredient) return true

    return false
}

fun mergeAdjacentSteps(clauses: List<Clause>): List<Clause> {
    if (clauses.isEmpty()) return clauses

    val out = mutableListOf<Clause>()
    var buffer: Clause? = null

    fun pushBuffer() {
        buffer?.let { out += it }
        buffer = null
    }

    for (c in clauses) {
        if (buffer == null) {
            buffer = c
            continue
        }

        if (buffer!!.type == ClauseType.STEP && c.type == ClauseType.STEP && shouldMergeSteps(buffer!!, c)) {
            // merge tokens + m1 labels
            buffer = buffer!!.copy(
                tokens = buffer!!.tokens + c.tokens,
                m1Labels = buffer!!.m1Labels + c.m1Labels
            )
        } else {
            pushBuffer()
            buffer = c
        }
    }

    pushBuffer()
    return out
}

// ================================================================
// ATTACH CLAUSES TO STEPS
// ================================================================
fun attachClauses(all: List<Clause>): List<Clause> {
    val steps = all.mapIndexedNotNull { idx, c -> if (c.type == ClauseType.STEP) idx else null }
    if (steps.isEmpty()) return all

    val mutable = all.map { it.copy(attachments = it.attachments.copy()) }.toMutableList()

    for (i in all.indices) {
        val c = mutable[i]
        when (c.type) {
            ClauseType.CONDITION,
            ClauseType.PURPOSE,
            ClauseType.CORRECTION -> {

                val target =
                    steps.firstOrNull { it > i && mutable[it].sentenceIndex == c.sentenceIndex }
                        ?: steps.lastOrNull { it < i }
                        ?: steps.first()

                val relation =
                    if (c.sentenceIndex == mutable[target].sentenceIndex && i < target)
                        RelationToStep.BEFORE else RelationToStep.AFTER

                val updated = c.copy(relationToStep = relation)
                val att = mutable[target].attachments

                mutable[target] = when (c.type) {
                    ClauseType.CONDITION -> mutable[target].copy(attachments = att.copy(condition = att.condition + updated))
                    ClauseType.PURPOSE -> mutable[target].copy(attachments = att.copy(purpose = att.purpose + updated))
                    ClauseType.CORRECTION -> mutable[target].copy(attachments = att.copy(correction = att.correction + updated))
                    else -> mutable[target]
                }
            }
            else -> {}
        }
    }

    return mutable
}

// ================================================================
// BUILD INSTRUCTIONS
// ================================================================
fun buildInstructionsFromClauses(all: List<Clause>): List<Instruction> {

    val steps = all.filter { it.type == ClauseType.STEP }
    if (steps.isEmpty()) return emptyList()

    val result = mutableListOf<Instruction>()

    for (s in steps) {
        val att = s.attachments
        result += Instruction(
            stepTextRaw = s.tokens.joinToString(" "),
            preConditionsRaw = att.condition.filter { it.relationToStep == RelationToStep.BEFORE }.map { it.tokens.joinToString(" ") },
            postConditionsRaw = att.condition.filter { it.relationToStep == RelationToStep.AFTER }.map { it.tokens.joinToString(" ") },
            purposeRaw = att.purpose.map { it.tokens.joinToString(" ") },
            correctionsRaw = att.correction.map { it.tokens.joinToString(" ") },
            followupStepRaw = null,
            followupPreConditionsRaw = emptyList(),
            followupPostConditionsRaw = emptyList(),
            sentenceIndexes = listOf(s.sentenceIndex)
        )
    }

    return result
}

// ================================================================
// MASTER PIPELINE
// ================================================================
fun buildInstructionsFromPipelineResults(results: List<PipelineResult>): List<Instruction> {

    val clauses = buildClausesFromPipelineResults(results)

    val mergedSteps = mergeAdjacentSteps(clauses)

    val attached = attachClauses(mergedSteps)

    return buildInstructionsFromClauses(attached)
}

fun buildClausesFromPipelineResults(results: List<PipelineResult>): List<Clause> {
    val out = mutableListOf<Clause>()
    results.sortedBy { it.sentenceIndex }.forEach { r ->
        out += absorbOClauses(
            bioToClauses(
                tokens = r.words,
                m1Labels = r.m1Labels,
                groupLabels = r.m2Labels,
                sentenceIndex = r.sentenceIndex
            )
        )
    }
    return out
}
