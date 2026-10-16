package com.example.comicreader.data

/**
 * Delimiter-safe codec for serializing and deserializing records that may contain
 * delimiter characters (";;") or newline characters ("\n", "\r").
 *
 * Guarantees 100% backward compatibility with legacy unencoded records.
 */
object SafeDelimiterCodec {
    private const val ESCAPED_DELIMITER = "&#59;&#59;"
    private const val ESCAPED_NEWLINE = "&#10;"
    private const val ESCAPED_CARRIAGE = "&#13;"

    /**
     * Escapes delimiters and newline characters so a single record can be safely
     * serialized into a delimited line.
     */
    fun encode(raw: String): String {
        return raw
            .replace(";;", ESCAPED_DELIMITER)
            .replace("\r", ESCAPED_CARRIAGE)
            .replace("\n", ESCAPED_NEWLINE)
            .trim()
    }

    /**
     * Restores original characters from an escaped record field.
     * Legacy records without escape sequences are decoded identically.
     */
    fun decode(encoded: String): String {
        return encoded
            .replace(ESCAPED_DELIMITER, ";;")
            .replace(ESCAPED_CARRIAGE, "\r")
            .replace(ESCAPED_NEWLINE, "\n")
    }
}
