package com.gorman.ourmemoryapp.domain.logic

import com.gorman.ourmemoryapp.domain.models.Veteran
import com.gorman.ourmemoryapp.domain.models.allNames

object VeteranNameMatcher {
    private const val SURNAME_WEIGHT = 3
    private const val GIVEN_NAME_WEIGHT = 2
    private const val YEAR_WEIGHT = 1
    private const val MIN_SCORE = 3
    private const val MAX_CANDIDATES = 3
    private const val MIN_WORD_LENGTH = 2
    private const val YEAR_LENGTH = 4
    private const val EXACT_MATCH_LENGTH_LIMIT = 4
    private const val SINGLE_TYPO_LENGTH_LIMIT = 7
    private const val SINGLE_TYPO = 1
    private const val DOUBLE_TYPO = 2
    private const val EDIT_COST = 1
    private val wordSeparator = Regex("[^\\p{L}\\p{N}]+")
    private val lookalikes = mapOf(
        'a' to 'а', 'b' to 'в', 'c' to 'с', 'e' to 'е', 'h' to 'н', 'k' to 'к',
        'm' to 'м', 'o' to 'о', 'p' to 'р', 't' to 'т', 'x' to 'х', 'y' to 'у', 'ё' to 'е'
    )

    fun candidates(recognizedText: String, veterans: List<Veteran>): List<Veteran> {
        val words = normalizedWords(recognizedText)
        val recognizedYears = words.filter(::isYear).toSet()
        val recognizedNames = words.filter { !isYear(it) && it.length >= MIN_WORD_LENGTH }
        if (recognizedNames.isEmpty()) return emptyList()
        return veterans
            .map { veteran -> veteran to nameScore(veteran, recognizedNames) + yearScore(veteran, recognizedYears) }
            .filter { (_, score) -> score >= MIN_SCORE }
            .sortedByDescending { (_, score) -> score }
            .take(MAX_CANDIDATES)
            .map { (veteran, _) -> veteran }
    }

    private fun nameScore(veteran: Veteran, recognizedNames: List<String>): Int =
        veteran.allNames().maxOfOrNull { name ->
            normalizedWords(name)
                .filter { it.length >= MIN_WORD_LENGTH }
                .withIndex()
                .sumOf { (index, part) ->
                    when {
                        recognizedNames.none { isSimilar(it, part) } -> 0
                        index == 0 -> SURNAME_WEIGHT
                        else -> GIVEN_NAME_WEIGHT
                    }
                }
        } ?: 0

    private fun yearScore(veteran: Veteran, recognizedYears: Set<String>): Int {
        val veteranYears = listOf(veteran.birthDate, veteran.deathDate, veteran.years)
            .flatMap(::normalizedWords)
            .filter(::isYear)
            .toSet()
        return veteranYears.intersect(recognizedYears).size * YEAR_WEIGHT
    }

    private fun normalizedWords(text: String): List<String> = text.lowercase()
        .map { lookalikes[it] ?: it }
        .joinToString("")
        .split(wordSeparator)
        .filter { it.isNotEmpty() }

    private fun isYear(word: String) = word.length == YEAR_LENGTH && word.all(Char::isDigit)

    private fun isSimilar(word: String, namePart: String): Boolean {
        val allowedTypos = when {
            namePart.length <= EXACT_MATCH_LENGTH_LIMIT -> 0
            namePart.length <= SINGLE_TYPO_LENGTH_LIMIT -> SINGLE_TYPO
            else -> DOUBLE_TYPO
        }
        if (kotlin.math.abs(word.length - namePart.length) > allowedTypos) return false
        return editDistance(word, namePart) <= allowedTypos
    }

    private fun editDistance(source: String, target: String): Int {
        if (source.isEmpty()) return target.length
        if (target.isEmpty()) return source.length
        var previous = IntArray(target.length + EDIT_COST) { it }
        for (sourceIndex in source.indices) {
            val current = IntArray(target.length + EDIT_COST)
            current[0] = sourceIndex + EDIT_COST
            for (targetIndex in target.indices) {
                val substitution = if (source[sourceIndex] == target[targetIndex]) 0 else EDIT_COST
                current[targetIndex + EDIT_COST] = minOf(
                    previous[targetIndex + EDIT_COST] + EDIT_COST,
                    current[targetIndex] + EDIT_COST,
                    previous[targetIndex] + substitution
                )
            }
            previous = current
        }
        return previous[target.length]
    }
}
