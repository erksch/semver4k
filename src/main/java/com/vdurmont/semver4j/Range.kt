package com.vdurmont.semver4j

import java.lang.RuntimeException
import java.util.Objects

// TODO doc
class Range(val version: Semver, val op: RangeOperator) {
    constructor(version: String?, op: RangeOperator) : this(Semver(version!!, Semver.SemverType.LOOSE), op) {}

    fun isSatisfiedBy(version: String?): Boolean {
        return this.isSatisfiedBy(Semver(version!!, this.version.type))
    }

    fun isSatisfiedBy(version: Semver): Boolean {
        return when (op) {
            RangeOperator.EQ -> version.isEquivalentTo(this.version)
            RangeOperator.LT -> version.isLowerThan(this.version)
            RangeOperator.LTE -> version.isLowerThan(this.version) || version.isEquivalentTo(this.version)
            RangeOperator.GT -> version.isGreaterThan(this.version)
            RangeOperator.GTE -> version.isGreaterThan(this.version) || version.isEquivalentTo(this.version)
        }
        throw RuntimeException("Code error. Unknown RangeOperator: " + op) // Should never happen
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Range) return false
        val range = o
        return version == range.version &&
                op == range.op
    }

    override fun hashCode(): Int {
        return Objects.hash(version, op)
    }

    override fun toString(): String {
        return op.asString() + version
    }

    enum class RangeOperator(private val s: String) {
        /**
         * The version and the requirement are equivalent
         */
        EQ("="),

        /**
         * The version is lower than the requirent
         */
        LT("<"),

        /**
         * The version is lower than or equivalent to the requirement
         */
        LTE("<="),

        /**
         * The version is greater than the requirement
         */
        GT(">"),

        /**
         * The version is greater than or equivalent to the requirement
         */
        GTE(">=");

        fun asString(): String {
            return s
        }
    }
}