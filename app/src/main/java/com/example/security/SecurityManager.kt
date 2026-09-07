package com.example.security

object SecurityManager {

    private val SENSITIVE_PATTERNS = listOf(
        Regex("(?i)\\b(password|passcode|pin|otp|cvv|secret|token|api[_-]?key)\\b"),
        Regex("(?i)\\b(credit\\s*card|debit\\s*card|bank\\s*account)\\b"),
        Regex("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b"), // 16 digit card
        Regex("\\b\\d{4,6}\\b") // potential standalone 4-6 digit OTPs if paired with keywords
    )

    private val DISALLOWED_COMMAND_KEYWORDS = listOf(
        "rm -rf", "su", "chmod", "eval", "exec", "sh", "bash", "reboot", "format"
    )

    /**
     * Checks if memory content contains sensitive credentials that should never be saved.
     */
    fun containsSensitiveData(text: String): Boolean {
        return SENSITIVE_PATTERNS.any { it.containsMatchIn(text) }
    }

    /**
     * Validate against malicious code or arbitrary commands.
     */
    fun isDangerousExecution(toolName: String, params: Map<String, Any?>): Boolean {
        for ((_, value) in params) {
            val str = value?.toString() ?: continue
            if (DISALLOWED_COMMAND_KEYWORDS.any { str.contains(it, ignoreCase = true) }) {
                return true
            }
        }
        return false
    }

    /**
     * Sanitizes package names to valid Android package conventions.
     */
    fun isValidPackageName(packageName: String): Boolean {
        val packageRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
        return packageRegex.matches(packageName)
    }
}
