package com.vdurmont.semver4j

import com.vdurmont.semver4j.Semver.SemverType
import kotlin.jvm.JvmOverloads

/**
 * Utility class to convert a NPM requirement string into a list of tokens.
 */
object Tokenizer {
    private val SPECIAL_CHARS: MutableMap<SemverType, MutableMap<Char, Token>> = mutableMapOf()

    /**
     * Takes a NPM requirement string and creates a list of tokens by performing 3 operations:
     * - If the token is a version, it will add the version string
     * - If the token is an operator, it will add the operator
     * - It will insert missing "AND" operators for ranges
     *
     * @param requirement the requirement string
     * @param type the version system used when tokenizing the requirement
     *
     * @return the list of tokens
     */
    @JvmStatic
    fun tokenize(requirement: String, type: SemverType): List<Token?> {
        var requirement = requirement
        val specialChars: Map<Char, Token> = SPECIAL_CHARS[type]!!

        // Replace the tokens made of 2 chars
        if (type == SemverType.COCOAPODS) {
            requirement = requirement.replace("~>", "~")
        } else if (type == SemverType.NPM) {
            requirement = requirement.replace("||", "|")
        }
        requirement = requirement.replace("<=", "≤").replace(">=", "≥")
        val tokens = mutableListOf<Token?>()
        var previousToken: Token? = null
        val chars = requirement.toCharArray()
        var token: Token? = null
        for (c in chars) {
            if (c == ' ') continue
            if (specialChars.containsKey(c)) {
                if (token != null) {
                    tokens.add(token)
                    previousToken = token
                    token = null
                }
                val current = specialChars[c]
                if (current!!.type.isUnary && previousToken != null && previousToken.type == TokenType.VERSION) {
                    // Handling the ranges like "≥1.2.3 <4.5.6" by inserting a "AND" binary operator
                    tokens.add(Token(TokenType.AND))
                }
                tokens.add(current)
                previousToken = current
            } else {
                if (token == null) {
                    token = Token(TokenType.VERSION)
                }
                token.append(c)
            }
        }
        if (token != null) {
            tokens.add(token)
        }
        return tokens
    }

    /**
     * A token in a requirement string. Has a type and a value if it is of type VERSION
     */
    class Token @JvmOverloads constructor(val type: TokenType, var value: String? = null) {
        fun append(c: Char) {
            if (value == null) value = ""
            value += c
        }
    }

    /**
     * The different types of tokens (unary operators, binary operators, delimiters and versions)
     */
    enum class TokenType(val character: Char?, val isUnary: Boolean, vararg supportedTypes: SemverType) {
        // Unary operators: ~ ^ = < <= > >=
        TILDE('~', true, SemverType.COCOAPODS, SemverType.NPM), CARET('^', true, SemverType.NPM), EQ('=', true, SemverType.NPM), LT('<', true, SemverType.COCOAPODS, SemverType.NPM), LTE('≤', true, SemverType.COCOAPODS, SemverType.NPM), GT('>', true, SemverType.COCOAPODS, SemverType.NPM), GTE('≥', true, SemverType.COCOAPODS, SemverType.NPM),  // Binary operators: - ||
        HYPHEN('-', false, SemverType.NPM), OR('|', false, SemverType.NPM), AND(null, false),  // Delimiters: ( )
        OPENING('(', false, SemverType.NPM), CLOSING(')', false, SemverType.NPM),  // Special
        VERSION(null, false);

        private val supportedTypes: List<SemverType> = supportedTypes.toList()
        fun supports(type: SemverType): Boolean {
            for (t in supportedTypes) {
                if (t == type) {
                    return true
                }
            }
            return false
        }

    }

    init {
        for (type in SemverType.values()) {
            SPECIAL_CHARS[type] = mutableMapOf()
        }
        for (tokenType in TokenType.values()) {
            if (tokenType.character != null) {
                for (type in SemverType.values()) {
                    if (tokenType.supports(type)) {
                        SPECIAL_CHARS[type]?.set(tokenType.character, Token(tokenType))
                    }
                }
            }
        }
    }
}