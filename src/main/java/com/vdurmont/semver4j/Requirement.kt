package com.vdurmont.semver4j

import com.vdurmont.semver4j.Tokenizer.tokenize
import com.vdurmont.semver4j.Semver.SemverType
import com.vdurmont.semver4j.Range.RangeOperator
import java.util.*
import java.util.regex.Pattern

/**
 * A requirement will provide an easy way to check if a version is satisfying.
 * There are 2 types of requirements:
 * - Strict: checks if a version is equivalent to another
 * - NPM: follows the rules of NPM
 */
class Requirement
/**
 * Builds a requirement. (private use only)
 *
 * A requirement has to be a range or a combination of an operator and 2 other requirements.
 *
 * @param range the range that will be used for the requirement (optional if all other params are provided)
 * @param req1 the requirement used as a left operand (requires the `op` and `req2` params to be provided)
 * @param op the operator used between the requirements (requires the `req1` and `req2` params to be provided)
 * @param req2 the requirement used as a right operand (requires the `req1` and `op` params to be provided)
 */ constructor(val range: Range?, val req1: Requirement?, val op: RequirementOperator?, val req2: Requirement?) {
    /**
     * @see .isSatisfiedBy
     * @param version the version that will be checked
     *
     * @return true if the version satisfies the requirement
     */
    fun isSatisfiedBy(version: String?): Boolean {
        return if (range != null) {
            this.isSatisfiedBy(Semver(version!!, range.version.type))
        } else {
            this.isSatisfiedBy(Semver(version!!))
        }
    }

    /**
     * Checks if the requirement is satisfied by a version.
     *
     * @param version the version that will be checked
     *
     * @return true if the version satisfies the requirement
     */
    fun isSatisfiedBy(version: Semver): Boolean {
        return if (range != null) {
            // We are on a leaf
            range.isSatisfiedBy(version)
        } else {
            // We have several sub-requirements
            when (op) {
                RequirementOperator.AND -> {
                    return try {
                        val set = getAllRanges(this, ArrayList())
                        for (range in set) {
                            if (!range!!.isSatisfiedBy(version)) {
                                return false
                            }
                        }
                        if (version.suffixTokens?.isNotEmpty() == true) {
                            // Find the set of versions that are allowed to have prereleases
                            // For example, ^1.2.3-pr.1 desugars to >=1.2.3-pr.1 <2.0.0
                            // That should allow `1.2.3-pr.2` to pass.
                            // However, `1.2.4-alpha.notready` should NOT be allowed,
                            // even though it's within the range set by the comparators.
                            for (range in set) {
                                if (range!!.version == null) {
                                    continue
                                }
                                if (range.version.suffixTokens?.isNotEmpty() == true) {
                                    val allowed = range.version
                                    if (version.major == allowed.major &&
                                            version.minor == allowed.minor &&
                                            version.patch == allowed.patch) {
                                        return true
                                    }
                                }
                            }
                            // Version has a -pre, but it's not one of the ones we like.
                            return false
                        }
                        true
                    } catch (e: Exception) {
                        // Could be that we have a OR in AND - fallback to default test
                        req1!!.isSatisfiedBy(version) && req2!!.isSatisfiedBy(version)
                    }
                    return req1!!.isSatisfiedBy(version) || req2!!.isSatisfiedBy(version)
                }
                RequirementOperator.OR -> return req1!!.isSatisfiedBy(version) || req2!!.isSatisfiedBy(version)
            }
            throw RuntimeException("Code error. Unknown RequirementOperator: " + op) // Should never happen
        }
    }

    private fun getAllRanges(requirement: Requirement?, res: MutableList<Range?>): List<Range?> {
        if (requirement!!.range != null) {
            res.add(requirement.range)
        } else if (requirement.op == RequirementOperator.AND) {
            getAllRanges(requirement.req1, res)
            getAllRanges(requirement.req2, res)
        } else {
            throw RuntimeException("OR in AND not allowed")
        }
        return res
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Requirement) return false
        val that = o
        return range == that.range &&
                req1 == that.req1 && op == that.op &&
                req2 == that.req2
    }

    override fun hashCode(): Int {
        return Objects.hash(range, req1, op, req2)
    }

    override fun toString(): String {
        return if (range != null) {
            range.toString()
        } else req1.toString() + " " + (if (op == RequirementOperator.OR) op.asString() + " " else "") + req2
    }

    /**
     * The operators that can be used in a requirement.
     */
    enum class RequirementOperator(private val s: String) {
        AND(""), OR("||");

        fun asString(): String {
            return s
        }
    }

    companion object {
        private val IVY_DYNAMIC_PATCH_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.\\+")
        private val IVY_DYNAMIC_MINOR_PATTERN = Pattern.compile("(\\d+)\\.\\+")
        private val IVY_LATEST_PATTERN = Pattern.compile("latest\\.\\w+")
        private val IVY_MATH_BOUNDED_PATTERN = Pattern.compile(
                "(\\[|\\])" +  // 1ST GROUP: a square bracket
                        "([\\d\\.]+)" +  // 2ND GROUP: a version
                        "," +  // a comma separator
                        "([\\d\\.]+)" +  // 3RD GROUP: a version
                        "(\\[|\\])" // 4TH GROUP: a square bracket
        )
        private val IVY_MATH_LOWER_UNBOUNDED_PATTERN = Pattern.compile(
                "\\(," +  // a parenthesis and a comma separator
                        "([\\d\\.]+)" +  // 1ST GROUP: a version
                        "(\\[|\\])" // 2ND GROUP: a square bracket
        )
        private val IVY_MATH_UPPER_UNBOUNDED_PATTERN = Pattern.compile(
                "(\\[|\\])" +  // 1ST GROUP: a square bracket
                        "([\\d\\.]+)" +  // 2ND GROUP: a version
                        ",\\)" // a comma separator and a parenthesis
        )

        /**
         * Builds a requirement (will test that the version is equivalent to the requirement)
         *
         * @param requirement the version of the requirement
         *
         * @return the generated requirement
         */
        fun build(requirement: Semver?): Requirement {
            return Requirement(Range(requirement!!, RangeOperator.EQ), null, null, null)
        }

        /**
         * Builds a strict requirement (will test that the version is equivalent to the requirement)
         *
         * @param requirement the version of the requirement
         *
         * @return the generated requirement
         */
        fun buildStrict(requirement: String?): Requirement {
            return build(Semver(requirement!!, SemverType.STRICT))
        }

        /**
         * Builds a loose requirement (will test that the version is equivalent to the requirement)
         *
         * @param requirement the version of the requirement
         *
         * @return the generated requirement
         */
        fun buildLoose(requirement: String?): Requirement {
            return build(Semver(requirement!!, SemverType.LOOSE))
        }

        /**
         * Builds a requirement following the rules of NPM.
         *
         * @param requirement the requirement as a string
         *
         * @return the generated requirement
         */
        fun buildNPM(requirement: String): Requirement {
            var requirement = requirement
            if (requirement.isEmpty()) {
                requirement = "*"
            }
            return buildWithTokenizer(requirement, SemverType.NPM)
        }

        /**
         * Builds a requirement following the rules of Cocoapods.
         *
         * @param requirement the requirement as a string
         *
         * @return the generated requirement
         */
        fun buildCocoapods(requirement: String): Requirement {
            return buildWithTokenizer(requirement, SemverType.COCOAPODS)
        }

        private fun buildWithTokenizer(requirement: String, type: SemverType): Requirement {
            // Tokenize the string
            var tokens = tokenize(requirement, type)
            tokens = removeFalsePositiveVersionRanges(tokens)
            tokens = addParentheses(tokens)

            // Tranform the tokens list to a reverse polish notation list
            val rpn = toReversePolishNotation(tokens)

            // Create the requirement tree by evaluating the rpn list
            return evaluateReversePolishNotation(rpn.iterator(), type)
        }

        /**
         * Builds a requirement following the rules of Ivy.
         *
         * @param requirement the requirement as a string
         *
         * @return the generated requirement
         */
        fun buildIvy(requirement: String?): Requirement {
            try {
                return buildLoose(requirement)
            } catch (ignored: SemverException) {
            }
            var matcher = IVY_DYNAMIC_PATCH_PATTERN.matcher(requirement)
            if (matcher.find()) {
                val major = Integer.valueOf(matcher.group(1))
                val minor = Integer.valueOf(matcher.group(2))
                val lower = Requirement(Range("$major.$minor.0", RangeOperator.GTE), null, null, null)
                val upper = Requirement(Range(major.toString() + "." + (minor + 1) + ".0", RangeOperator.LT), null, null, null)
                return Requirement(null, lower, RequirementOperator.AND, upper)
            }
            matcher = IVY_DYNAMIC_MINOR_PATTERN.matcher(requirement)
            if (matcher.find()) {
                val major = Integer.valueOf(matcher.group(1))
                val lower = Requirement(Range("$major.0.0", RangeOperator.GTE), null, null, null)
                val upper = Requirement(Range((major + 1).toString() + ".0.0", RangeOperator.LT), null, null, null)
                return Requirement(null, lower, RequirementOperator.AND, upper)
            }
            matcher = IVY_LATEST_PATTERN.matcher(requirement)
            if (matcher.find()) {
                return Requirement(Range("0.0.0", RangeOperator.GTE), null, null, null)
            }
            matcher = IVY_MATH_BOUNDED_PATTERN.matcher(requirement)
            if (matcher.find()) {
                val lowerOp = if ("[" == matcher.group(1)) RangeOperator.GTE else RangeOperator.GT
                val lowerVersion = Semver(matcher.group(2), SemverType.LOOSE)
                val upperVersion = Semver(matcher.group(3), SemverType.LOOSE)
                val upperOp = if ("]" == matcher.group(4)) RangeOperator.LTE else RangeOperator.LT
                val lower = Requirement(Range(extrapolateVersion(lowerVersion), lowerOp), null, null, null)
                val upper = Requirement(Range(extrapolateVersion(upperVersion), upperOp), null, null, null)
                return Requirement(null, lower, RequirementOperator.AND, upper)
            }
            matcher = IVY_MATH_LOWER_UNBOUNDED_PATTERN.matcher(requirement)
            if (matcher.find()) {
                val version = Semver(matcher.group(1), SemverType.LOOSE)
                val op = if ("]" == matcher.group(2)) RangeOperator.LTE else RangeOperator.LT
                return Requirement(Range(extrapolateVersion(version), op), null, null, null)
            }
            matcher = IVY_MATH_UPPER_UNBOUNDED_PATTERN.matcher(requirement)
            if (matcher.find()) {
                val op = if ("[" == matcher.group(1)) RangeOperator.GTE else RangeOperator.GT
                val version = Semver(matcher.group(2), SemverType.LOOSE)
                return Requirement(Range(extrapolateVersion(version), op), null, null, null)
            }
            throw SemverException("Invalid requirement")
        }

        /**
         * Return parenthesized expression, giving lowest priority to OR operator
         *
         * @param tokens the tokens contained in the requirement string
         *
         * @return the tokens with parenthesis
         */
        private fun addParentheses(tokens: List<Tokenizer.Token?>): List<Tokenizer.Token?> {
            val result: MutableList<Tokenizer.Token?> = ArrayList()
            result.add(Tokenizer.Token(Tokenizer.TokenType.OPENING, "("))
            for (token in tokens) {
                if (token!!.type === Tokenizer.TokenType.OR) {
                    result.add(Tokenizer.Token(Tokenizer.TokenType.CLOSING, ")"))
                    result.add(token)
                    result.add(Tokenizer.Token(Tokenizer.TokenType.OPENING, "("))
                } else {
                    result.add(token)
                }
            }
            result.add(Tokenizer.Token(Tokenizer.TokenType.CLOSING, ")"))
            return result
        }

        /**
         * Some requirements may contain versions that look like version ranges. For example ' 0.0.1-SNASHOT ' could be
         * interpreted incorrectly as a version range from 0.0.1 to SNAPSHOT. This method parses all tokens and looks for
         * groups of three tokens that are respectively of type [VERSION, HYPHEN, VERSION] and validates that the token
         * after the hyphen is a valid version string. If it isn't the, three tokens are merged into one (thus creating a
         * single version token, in which the third token is the build information).
         *
         * @param tokens the tokens contained in the requirement string
         *
         * @return the tokens with any false positive version ranges replaced with version strings
         */
        private fun removeFalsePositiveVersionRanges(tokens: List<Tokenizer.Token?>): List<Tokenizer.Token?> {
            val result: MutableList<Tokenizer.Token?> = ArrayList()
            var i = 0
            while (i < tokens.size) {
                var token = tokens[i]
                if (thereIsFalsePositiveVersionRange(tokens, i)) {
                    token = Tokenizer.Token(Tokenizer.TokenType.VERSION, token!!.value + '-' + tokens[i + 2]!!.value)
                    i += 2
                }
                result.add(token)
                i++
            }
            return result
        }

        private fun thereIsFalsePositiveVersionRange(tokens: List<Tokenizer.Token?>, i: Int): Boolean {
            if (i + 2 >= tokens.size) {
                return false
            }
            val suspiciousTokens = arrayOf(tokens[i], tokens[i + 1], tokens[i + 2])
            if (suspiciousTokens[0]!!.type != Tokenizer.TokenType.VERSION) {
                return false
            }
            if (suspiciousTokens[2]!!.type != Tokenizer.TokenType.VERSION) {
                return false
            }
            return if (suspiciousTokens[1]!!.type != Tokenizer.TokenType.HYPHEN) {
                false
            } else attemptToParse(suspiciousTokens[2]!!.value) == null
        }

        private fun attemptToParse(value: String?): Semver? {
            try {
                return Semver(value!!, SemverType.NPM)
            } catch (e: SemverException) {
                // Ignore.
            }
            return null
        }

        /**
         * Adaptation of the shutting yard algorithm
         */
        private fun toReversePolishNotation(tokens: List<Tokenizer.Token?>): List<Tokenizer.Token?> {
            val queue = LinkedList<Tokenizer.Token?>()
            val stack = Stack<Tokenizer.Token?>()
            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]
                when (token!!.type) {
                    Tokenizer.TokenType.VERSION -> queue.push(token)
                    Tokenizer.TokenType.CLOSING -> {
                        while (stack.peek()!!.type !== Tokenizer.TokenType.OPENING) {
                            queue.push(stack.pop())
                        }
                        stack.pop()
                        if (stack.size > 0 && stack.peek()!!.type.isUnary) {
                            queue.push(stack.pop())
                        }
                    }
                    else -> if (token.type.isUnary) {
                        // Push the operand first
                        i++
                        queue.push(tokens[i])
                        // Then the operator
                        queue.push(token)
                    } else {
                        stack.push(token)
                    }
                }
                i++
            }
            while (!stack.isEmpty()) {
                queue.push(stack.pop())
            }
            return queue
        }

        /**
         * Evaluates a reverse polish notation token list
         */
        private fun evaluateReversePolishNotation(iterator: Iterator<Tokenizer.Token?>, type: SemverType): Requirement {
            return try {
                val token = iterator.next()
                if (token!!.type === Tokenizer.TokenType.VERSION) {
                    if ("*" == token!!.value || type === SemverType.NPM && "latest" == token.value) {
                        // Special case for "*" and "latest" in NPM
                        return Requirement(Range("0.0.0", RangeOperator.GTE), null, null, null)
                    }
                    val version = Semver(token.value!!, type)
                    if (version.minor != null && version.patch != null) {
                        val range = Range(version, RangeOperator.EQ)
                        Requirement(range, null, null, null)
                    } else {
                        // If we have a version with a wildcard char (like 1.2.x, 1.2.* or 1.2), we need a tilde requirement
                        tildeRequirement(version.value, type)
                    }
                } else if (token!!.type === Tokenizer.TokenType.HYPHEN) {
                    val token3 = iterator.next() // Note that token3 is before token2!
                    val token2 = iterator.next()
                    hyphenRequirement(token2!!.value, token3!!.value, type)
                } else if (token!!.type.isUnary) {
                    val token2 = iterator.next()
                    val rangeOp: RangeOperator
                    rangeOp = when (token.type) {
                        Tokenizer.TokenType.EQ -> RangeOperator.EQ
                        Tokenizer.TokenType.LT -> RangeOperator.LT
                        Tokenizer.TokenType.LTE -> RangeOperator.LTE
                        Tokenizer.TokenType.GT -> RangeOperator.GT
                        Tokenizer.TokenType.GTE -> RangeOperator.GTE
                        Tokenizer.TokenType.TILDE -> return tildeRequirement(token2!!.value, type)
                        Tokenizer.TokenType.CARET -> return caretRequirement(token2!!.value, type)
                        else -> throw SemverException("Invalid requirement")
                    }
                    val range = Range(token2!!.value, rangeOp)
                    Requirement(range, null, null, null)
                } else {
                    // They don't call it "reverse" for nothing
                    val req2 = evaluateReversePolishNotation(iterator, type)
                    val req1 = evaluateReversePolishNotation(iterator, type)
                    val requirementOp: RequirementOperator
                    requirementOp = when (token.type) {
                        Tokenizer.TokenType.OR -> RequirementOperator.OR
                        Tokenizer.TokenType.AND -> RequirementOperator.AND
                        else -> throw SemverException("Invalid requirement")
                    }
                    Requirement(null, req1, requirementOp, req2)
                }
            } catch (e: NoSuchElementException) {
                throw SemverException("Invalid requirement")
            }
        }

        /**
         * Allows patch-level changes if a minor version is specified on the comparator. Allows minor-level changes if not.
         *
         * @param version the version of the requirement
         * @param type the version system used for this requirement
         *
         * @return the generated requirement
         */
        fun tildeRequirement(version: String?, type: SemverType): Requirement {
            if (type !== SemverType.NPM && type !== SemverType.COCOAPODS) {
                throw SemverException("The tilde requirements are only compatible with NPM and Cocoapods.")
            }
            val semver = Semver(version!!, type)
            val req1 = Requirement(Range(extrapolateVersion(semver), RangeOperator.GTE), null, null, null)
            val next: String
            next = when (type) {
                SemverType.COCOAPODS -> {
                    if (semver.patch != null) {
                        semver.major.toString() + "." + (semver.minor!! + 1) + ".0"
                    } else if (semver.minor != null) {
                        (semver.major!! + 1).toString() + ".0.0"
                    } else {
                        return req1
                    }
                }
                SemverType.NPM -> {
                    if (semver.minor != null) {
                        semver.major.toString() + "." + (semver.minor!! + 1) + ".0"
                    } else {
                        (semver.major!! + 1).toString() + ".0.0"
                    }
                }
                else -> throw SemverException("The tilde requirements are only compatible with NPM and Cocoapods.")
            }
            val req2 = Requirement(Range(next, RangeOperator.LT), null, null, null)
            return Requirement(null, req1, RequirementOperator.AND, req2)
        }

        /**
         * Allows changes that do not modify the left-most non-zero digit in the [major, minor, patch] tuple.
         *
         * @param version the version of the requirement
         * @param type the version system used for this requirement
         *
         * @return the generated requirement
         */
        fun caretRequirement(version: String?, type: SemverType): Requirement {
            if (type !== SemverType.NPM) {
                throw SemverException("The caret requirements are only compatible with NPM.")
            }
            val semver = Semver(version!!, type)
            val req1 = Requirement(Range(extrapolateVersion(semver), RangeOperator.GTE), null, null, null)
            val next: String
            next = if (semver.major == 0) {
                if (semver.minor == null) {
                    "1.0.0"
                } else if (semver.minor == 0) {
                    if (semver.patch == null) {
                        "0.1.0"
                    } else {
                        "0.0." + (semver.patch!! + 1)
                    }
                } else {
                    "0." + (semver.minor!! + 1) + ".0"
                }
            } else {
                (semver.major!! + 1).toString() + ".0.0"
            }
            val req2 = Requirement(Range(next, RangeOperator.LT), null, null, null)
            return Requirement(null, req1, RequirementOperator.AND, req2)
        }

        /**
         * Creates a requirement that satisfies "x1.y1.z1 - x2.y2.z2".
         *
         * @param lowerVersion the version of the lower bound of the requirement
         * @param upperVersion the version of the upper bound of the requirement
         * @param type the version system used for this requirement
         *
         * @return the generated requirement
         */
        fun hyphenRequirement(lowerVersion: String?, upperVersion: String?, type: SemverType): Requirement {
            if (type !== SemverType.NPM) {
                throw SemverException("The hyphen requirements are only compatible with NPM.")
            }
            val lower = extrapolateVersion(Semver(lowerVersion!!, type))
            var upper = Semver(upperVersion!!, type)
            var upperOperator = RangeOperator.LTE
            if (upper.minor == null || upper.patch == null) {
                upperOperator = RangeOperator.LT
                upper = if (upper.minor == null) {
                    extrapolateVersion(upper).withIncMajor()
                } else {
                    extrapolateVersion(upper).withIncMinor()
                }
            }
            val req1 = Requirement(Range(lower, RangeOperator.GTE), null, null, null)
            val req2 = Requirement(Range(upper, upperOperator), null, null, null)
            return Requirement(null, req1, RequirementOperator.AND, req2)
        }

        /**
         * Extrapolates the optional minor and patch numbers.
         * - 1 = 1.0.0
         * - 1.2 = 1.2.0
         * - 1.2.3 = 1.2.3
         *
         * @param semver the original semver
         *
         * @return a semver with the extrapolated minor and patch numbers
         */
        private fun extrapolateVersion(semver: Semver): Semver {
            val sb = StringBuilder()
                    .append(semver.major)
                    .append(".")
                    .append(if (semver.minor == null) 0 else semver.minor)
                    .append(".")
                    .append(if (semver.patch == null) 0 else semver.patch)
            var first = true
            for (i in 0 until (semver.suffixTokens?.size ?: 0)) {
                if (first) {
                    sb.append("-")
                    first = false
                } else {
                    sb.append(".")
                }
                sb.append(semver.suffixTokens!![i])
            }
            if (semver.build != null) {
                sb.append("+").append(semver.build)
            }
            return Semver(sb.toString(), semver.type)
        }
    }
}