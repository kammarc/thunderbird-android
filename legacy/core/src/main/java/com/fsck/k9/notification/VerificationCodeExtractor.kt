package com.fsck.k9.notification

/**
 * Extracts a one-time verification code (e.g. a sign-up or login code) from the text of a message.
 *
 * A code is a single token of 4 to 10 upper case letters and digits, optionally split by one hyphen, that contains at
 * least one digit and appears close to a keyword like "code" or "verification".
 */
internal object VerificationCodeExtractor {
    private const val MAX_DISTANCE_TO_KEYWORD = 40
    private const val MIN_CODE_LENGTH = 4
    private const val MAX_CODE_LENGTH = 10

    private val keywordRegex = Regex(
        """code|c[oó]digo|codice|kod|koodi|verif|\b(?:otp|pin|tan)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val candidateRegex = Regex(
        """(?<![\w/@#.,-])[A-Z0-9]{2,8}(?:-[A-Z0-9]{2,8})?(?![\w/@-]|[.,]\w)""",
    )

    /**
     * Returns the verification code found in [text] or `null` if [text] doesn't seem to contain one.
     */
    fun extract(text: CharSequence): String? {
        val keywordRanges = keywordRegex.findAll(text).map { it.range }.toList()
        if (keywordRanges.isEmpty()) return null

        return candidateRegex.findAll(text)
            .filter { it.value.isPlausibleCode() }
            .map { candidate -> candidate.value to keywordRanges.minOf { distance(candidate.range, it) } }
            .filter { (_, distance) -> distance <= MAX_DISTANCE_TO_KEYWORD }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    private fun String.isPlausibleCode(): Boolean {
        return count { it != '-' } in MIN_CODE_LENGTH..MAX_CODE_LENGTH && any { it.isDigit() }
    }

    private fun distance(first: IntRange, second: IntRange): Int {
        return when {
            first.first > second.last -> first.first - second.last - 1
            second.first > first.last -> second.first - first.last - 1
            else -> 0
        }
    }
}
