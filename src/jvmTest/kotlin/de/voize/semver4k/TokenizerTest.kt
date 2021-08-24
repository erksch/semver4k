package de.voize.semver4k

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Assert
import org.junit.Test

@RunWith(JUnit4::class)
class TokenizerTest {
    @Test
    fun tokenize_NPM_tilde() {
        val requirement = "~ 1.2.7"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(2, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.TILDE, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1.2.7", tokens[1]?.value)
    }

    @Test
    fun tokenize_NPM_caret() {
        val requirement = "^ 1.2.7   "
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(2, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.CARET, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1.2.7", tokens[1]?.value)
    }

    @Test
    fun tokenize_NPM_lte() {
        val requirement = "<=1.2.7"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(2, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.LTE, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1.2.7", tokens[1]?.value)
    }

    @Test
    fun tokenize_NPM_lt() {
        val requirement = "<1.2.7"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(2, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.LT, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1.2.7", tokens[1]?.value)
    }

    @Test
    fun tokenize_NPM_gte() {
        val requirement = ">=1.2.7"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(2, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.GTE, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1.2.7", tokens[1]?.value)
    }

    @Test
    fun tokenize_NPM_gt() {
        val requirement = ">1.2.7"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(2, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.GT, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1.2.7", tokens[1]?.value)
    }

    @Test
    fun tokenize_NPM_eq() {
        val requirement = "=1.2.7"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(2, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.EQ, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1.2.7", tokens[1]?.value)
    }

    @Test
    fun tokenize_NPM_gte_major() {
        val requirement = ">=1"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(2, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.GTE, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1", tokens[1]?.value)
    }

    @Test
    fun tokenize_NPM_suffix() {
        val requirement = "1.2.7-rc.1"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)

        // in NPM, A range can only be considered when `-` is surrounded by spaces
        // Since the - is directly next to the version it's automatically
        // considered as part of the version
        Assert.assertEquals(1, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[0]?.type)
        Assert.assertEquals("1.2.7-rc.1", tokens[0]?.value)
    }

    @Test
    fun tokenize_NPM_or_suffix() {
        val requirement = "1.2.7-rc.1 || 1.2.7-rc.2"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(3, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[0]?.type)
        Assert.assertEquals("1.2.7-rc.1", tokens[0]?.value)
        Assert.assertEquals(Tokenizer.TokenType.OR, tokens[1]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[2]?.type)
        Assert.assertEquals("1.2.7-rc.2", tokens[2]?.value)
    }

    @Test
    fun tokenize_NPM_or_hyphen() {
        val requirement = "1.2.7 || 1.2.9 - 2.0.0"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(5, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[0]?.type)
        Assert.assertEquals("1.2.7", tokens[0]?.value)
        Assert.assertEquals(Tokenizer.TokenType.OR, tokens[1]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[2]?.type)
        Assert.assertEquals("1.2.9", tokens[2]?.value)
        Assert.assertEquals(Tokenizer.TokenType.HYPHEN, tokens[3]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[4]?.type)
        Assert.assertEquals("2.0.0", tokens[4]?.value)
    }

    @Test
    fun tokenize_NPM_or_lte_parenthesis() {
        val requirement = "1.2.7 || (<=1.2.9 || 2.0.0)"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(8, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[0]?.type)
        Assert.assertEquals("1.2.7", tokens[0]?.value)
        Assert.assertEquals(Tokenizer.TokenType.OR, tokens[1]?.type)
        Assert.assertEquals(Tokenizer.TokenType.OPENING, tokens[2]?.type)
        Assert.assertEquals(Tokenizer.TokenType.LTE, tokens[3]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[4]?.type)
        Assert.assertEquals("1.2.9", tokens[4]?.value)
        Assert.assertEquals(Tokenizer.TokenType.OR, tokens[5]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[6]?.type)
        Assert.assertEquals("2.0.0", tokens[6]?.value)
        Assert.assertEquals(Tokenizer.TokenType.CLOSING, tokens[7]?.type)
    }

    @Test
    fun tokenize_NPM_or_and() {
        val requirement = ">1.2.1 <1.2.8 || >2.0.0 <3.0.0"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.NPM)
        Assert.assertEquals(11, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.GT, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1.2.1", tokens[1]?.value)
        Assert.assertEquals(Tokenizer.TokenType.AND, tokens[2]?.type)
        Assert.assertEquals(Tokenizer.TokenType.LT, tokens[3]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[4]?.type)
        Assert.assertEquals("1.2.8", tokens[4]?.value)
        Assert.assertEquals(Tokenizer.TokenType.OR, tokens[5]?.type)
        Assert.assertEquals(Tokenizer.TokenType.GT, tokens[6]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[7]?.type)
        Assert.assertEquals("2.0.0", tokens[7]?.value)
        Assert.assertEquals(Tokenizer.TokenType.AND, tokens[8]?.type)
        Assert.assertEquals(Tokenizer.TokenType.LT, tokens[9]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[10]?.type)
        Assert.assertEquals("3.0.0", tokens[10]?.value)
    }

    @Test
    fun tokenize_Cocoapods_tilde() {
        val requirement = "~> 1.2.7"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.COCOAPODS)
        Assert.assertEquals(2, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.TILDE, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1.2.7", tokens[1]?.value)
    }

    @Test
    fun tokenize_Cocoapods_lte() {
        val requirement = "<=1.2.7"
        val tokens = Tokenizer.tokenize(requirement, Semver.SemverType.COCOAPODS)
        Assert.assertEquals(2, tokens.size.toLong())
        Assert.assertEquals(Tokenizer.TokenType.LTE, tokens[0]?.type)
        Assert.assertEquals(Tokenizer.TokenType.VERSION, tokens[1]?.type)
        Assert.assertEquals("1.2.7", tokens[1]?.value)
    }
}