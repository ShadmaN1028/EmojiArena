package com.shadman.emojiarena.data

import java.text.BreakIterator

/**
 * Approximate "is this text a single emoji" check: the trimmed text must be
 * exactly one grapheme cluster (so a compound emoji — a family, a flag, a
 * skin-toned gesture — still counts as one, but two emoji typed next to
 * each other don't), and that cluster can't be a plain letter or digit.
 * Not Unicode-perfect — a lone symbol like "→" would also pass — but the
 * spec calls for an approximation, not full emoji-property matching.
 */
fun isSingleEmoji(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return false

    val clusterBoundaries = BreakIterator.getCharacterInstance()
    clusterBoundaries.setText(trimmed)
    var clusterCount = 0
    while (clusterBoundaries.next() != BreakIterator.DONE) clusterCount++
    if (clusterCount != 1) return false

    return trimmed.codePoints().noneMatch(Character::isLetterOrDigit)
}
