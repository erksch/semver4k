package de.voize.semver4k

import kotlin.jvm.JvmOverloads

/**
 * Semver is a tool that provides useful methods to manipulate versions that follow the "semantic versioning" specification
 * (see http://semver.org)
 */
class Semver @JvmOverloads constructor(val originalValue: String, val type: SemverType = SemverType.STRICT) : Comparable<Semver> {
    /**
     * Get the original value as a string
     *
     * @return the original string passed in the constructor
     */
    val value: String

    /**
     * Returns the major part of the version.
     * Example: for "1.2.3" = 1
     *
     * @return the major part of the version
     */
    var major: Int? = null

    /**
     * Returns the minor part of the version.
     * Example: for "1.2.3" = 2
     *
     * @return the minor part of the version
     */
    var minor: Int? = null

    /**
     * Returns the patch part of the version.
     * Example: for "1.2.3" = 3
     *
     * @return the patch part of the version
     */
    var patch: Int? = null

    /**
     * Returns the build of the version.
     * Example: for "1.2.3-beta.4+sha98450956" = "sha98450956"
     *
     * @return the build of the version
     */
    val build: String?

    /**
     * Returns the suffix of the version.
     * Example: for "1.2.3-beta.4+sha98450956" = {"beta", "4"}
     *
     * @return the suffix of the version
     */
    val suffixTokens: Array<String?>?

    init {
        var tempValue = originalValue.trim { it <= ' ' }
        if (type == SemverType.NPM && (tempValue.startsWith("v") || tempValue.startsWith("V"))) {
            tempValue = tempValue.substring(1).trim { it <= ' ' }
        }
        value = tempValue

        val tokens: Array<String> = if (hasPreRelease(value)) {
            value.split("-", limit=2).toTypedArray()
        } else {
            arrayOf(value)
        }

        var tempBuild: String? = null
        var tempMinor: Int? = null
        var tempPatch: Int? = null

        try {
            val mainTokens: Array<String>
            if (tokens.size == 1) {
                // The build version may be in the main tokens
                if (tokens[0].endsWith("+")) {
                    throw SemverException("The build cannot be empty.")
                }
                val tmp = tokens[0].split("+").toTypedArray()
                mainTokens = tmp[0].split(".").toTypedArray()
                if (tmp.size == 2) {
                    tempBuild = tmp[1]
                }
            } else {
                mainTokens = tokens[0].split(".").toTypedArray()
            }
            try {
                major = mainTokens[0].toInt()
            } catch (e: NumberFormatException) {
                throw SemverException("Invalid version (no major version): $value")
            } catch (e: IndexOutOfBoundsException) {
                throw SemverException("Invalid version (no major version): $value")
            }
            try {
                tempMinor = mainTokens[1].toInt()
            } catch (e: IndexOutOfBoundsException) {
                if (type == SemverType.STRICT) {
                    throw SemverException("Invalid version (no minor version): $value")
                }
            } catch (e: NumberFormatException) {
                if (type != SemverType.NPM || !"x".equals(mainTokens[1], ignoreCase = true) && "*" != mainTokens[1]) {
                    throw SemverException("Invalid version (no minor version): $value")
                }
            }
            try {
                tempPatch = mainTokens[2].toInt()
            } catch (e: IndexOutOfBoundsException) {
                if (type == SemverType.STRICT) {
                    throw SemverException("Invalid version (no patch version): $value")
                }
            } catch (e: NumberFormatException) {
                if (type != SemverType.NPM || !"x".equals(mainTokens[2], ignoreCase = true) && "*" != mainTokens[2]) {
                    throw SemverException("Invalid version (no patch version): $value")
                }
            }
        } catch (e: NumberFormatException) {
            throw SemverException("The version is invalid: $value")
        } catch (e: IndexOutOfBoundsException) {
            throw SemverException("The version is invalid: $value")
        }

        minor = tempMinor
        patch = tempPatch

        var suffix = arrayOfNulls<String>(0)
        try {
            // The build version may be in the suffix tokens
            if (tokens[1].endsWith("+")) {
                throw SemverException("The build cannot be empty.")
            }
            val tmp = tokens[1].split("+").toTypedArray()
            if (tmp.size == 2) {
                suffix = tmp[0].split(".").toTypedArray()
                tempBuild = tmp[1]
            } else {
                suffix = tokens[1].split(".").toTypedArray()
            }
        } catch (ignored: IndexOutOfBoundsException) {
        }

        suffixTokens = suffix
        build = tempBuild
        validate(type)
    }


    private fun validate(type: SemverType) {
        if (minor == null && type == SemverType.STRICT) {
            throw SemverException("Invalid version (no minor version): $value")
        }
        if (patch == null && type == SemverType.STRICT) {
            throw SemverException("Invalid version (no patch version): $value")
        }
    }

    private fun hasPreRelease(version: String): Boolean {
        val firstIndexOfPlus = value.indexOf("+")
        val firstIndexOfHyphen = value.indexOf("-")
        return if (firstIndexOfHyphen == -1) {
            false
        } else firstIndexOfPlus == -1 || firstIndexOfHyphen < firstIndexOfPlus
    }

    /**
     * Check if the version satisfies a requirement
     *
     * @param requirement the requirement
     *
     * @return true if the version satisfies the requirement
     */
    fun satisfies(requirement: Requirement): Boolean {
        return requirement.isSatisfiedBy(this)
    }

    /**
     * Check if the version satisfies a requirement
     *
     * @param requirement the requirement
     *
     * @return true if the version satisfies the requirement
     */
    fun satisfies(requirement: String): Boolean {
        val req: Requirement = when (type) {
            SemverType.STRICT -> Requirement.buildStrict(requirement)
            SemverType.LOOSE -> Requirement.buildLoose(requirement)
            SemverType.NPM -> Requirement.buildNPM(requirement)
            SemverType.COCOAPODS -> Requirement.buildCocoapods(requirement)
            SemverType.IVY -> Requirement.buildIvy(requirement)
            else -> throw SemverException("Invalid requirement type: $type")
        }
        return this.satisfies(req)
    }

    /**
     * @see .isGreaterThan
     * @param version the version to compare
     *
     * @return true if the current version is greater than the provided version
     */
    fun isGreaterThan(version: String): Boolean {
        return this.isGreaterThan(Semver(version, type))
    }

    /**
     * Checks if the version is greater than another version
     *
     * @param version the version to compare
     *
     * @return true if the current version is greater than the provided version
     */
    fun isGreaterThan(version: Semver): Boolean {
        // Compare the main part
        if (major!! > version.major!!) return true
        if (major!! < version.major!!) return false
        if (type == SemverType.NPM && version.minor == null) return false
        val otherMinor = version.minor ?: 0
        if (minor != null && minor!! > otherMinor) return true
        if (minor != null && minor!! < otherMinor) return false
        if (type == SemverType.NPM && version.patch == null) return false
        val otherPatch = version.patch ?: 0
        if (patch != null && patch!! > otherPatch) return true
        if (patch != null && patch!! < otherPatch) return false

        // Let's take a look at the suffix
        val tokens1 = suffixTokens
        val tokens2 = version.suffixTokens

        // If one of the versions has no suffix, it's greater!
        if (tokens1!!.size == 0 && tokens2!!.size > 0) return true
        if (tokens2!!.size == 0 && tokens1.size > 0) return false

        // Let's see if one of suffixes is greater than the other
        var i = 0
        while (i < tokens1.size && i < tokens2.size) {
            var cmp: Int
            cmp = try {
                // Trying to resolve the suffix part with an integer
                val t1 = tokens1[i]!!.toInt()
                val t2 = tokens2[i]!!.toInt()
                t1 - t2
            } catch (e: NumberFormatException) {
                // Else, do a string comparison
                tokens1[i]!!.compareTo(tokens2[i]!!, ignoreCase = true)
            }
            if (cmp < 0) return false else if (cmp > 0) return true
            i++
        }

        // If one of the versions has some remaining suffixes, it's greater
        return tokens1.size > tokens2.size
    }

    /**
     * @see .isGreaterThanOrEqualTo
     * @param version the version to compare
     *
     * @return true if the current version is greater than or equal to the provided version
     */
    fun isGreaterThanOrEqualTo(version: String): Boolean {
        return this.isGreaterThanOrEqualTo(Semver(version, type))
    }

    /**
     * Checks if the version is greater than or equal to another version
     *
     * @param version the version to compare
     *
     * @return true if the current version is greater than or equal to the provided version
     */
    fun isGreaterThanOrEqualTo(version: Semver): Boolean {
        return this.isGreaterThan(version) || this.isEquivalentTo(version)
    }

    /**
     * @see .isLowerThan
     * @param version the version to compare
     *
     * @return true if the current version is lower than the provided version
     */
    fun isLowerThan(version: String): Boolean {
        return this.isLowerThan(Semver(version, type))
    }

    /**
     * Checks if the version is lower than another version
     *
     * @param version the version to compare
     *
     * @return true if the current version is lower than the provided version
     */
    fun isLowerThan(version: Semver): Boolean {
        return !this.isGreaterThan(version) && !this.isEquivalentTo(version)
    }

    /**
     * @see .isLowerThanOrEqualTo
     * @param version the version to compare
     *
     * @return true if the current version is lower than or equal to the provided version
     */
    fun isLowerThanOrEqualTo(version: String): Boolean {
        return this.isLowerThanOrEqualTo(Semver(version, type))
    }

    /**
     * Checks if the version is lower than or equal to another version
     *
     * @param version the version to compare
     *
     * @return true if the current version is lower than or equal to the provided version
     */
    fun isLowerThanOrEqualTo(version: Semver): Boolean {
        return !this.isGreaterThan(version)
    }

    /**
     * @see .isEquivalentTo
     * @param version the version to compare
     *
     * @return true if the current version equals the provided version (build excluded)
     */
    fun isEquivalentTo(version: String): Boolean {
        return this.isEquivalentTo(Semver(version, type))
    }

    /**
     * Checks if the version equals another version, without taking the build into account.
     *
     * @param version the version to compare
     *
     * @return true if the current version equals the provided version (build excluded)
     */
    fun isEquivalentTo(version: Semver): Boolean {
        // Get versions without build
        val sem1 = if (build == null) this else Semver(value.replace("+" + build, ""))
        val sem2 = if (version.build == null) version else Semver(version.value.replace("+" + version.build, ""))
        // Compare those new versions
        return sem1.isEqualTo(sem2)
    }

    /**
     * @see .isEqualTo
     * @param version the version to compare
     *
     * @return true if the current version equals the provided version
     */
    fun isEqualTo(version: String): Boolean {
        return this.isEqualTo(Semver(version, type))
    }

    /**
     * Checks if the version equals another version
     *
     * @param version the version to compare
     *
     * @return true if the current version equals the provided version
     */
    fun isEqualTo(version: Semver): Boolean {
        if (type == SemverType.NPM) {
            if (major !== version.major) return false
            if (version.minor == null) return true
            if (version.patch == null) return true
        }
        return this == version
    }

    /**
     * Determines if the current version is stable or not.
     * Stable version have a major version number strictly positive and no suffix tokens.
     *
     * @return true if the current version is stable
     */
    val isStable: Boolean
        get() = major != null && major!! > 0 &&
                (suffixTokens == null || suffixTokens.size == 0)

    /**
     * @see .diff
     * @param version the version to compare
     *
     * @return the greatest difference
     */
    fun diff(version: String): VersionDiff {
        return this.diff(Semver(version, type))
    }

    /**
     * Returns the greatest difference between 2 versions.
     * For example, if the current version is "1.2.3" and compared version is "1.3.0", the biggest difference
     * is the 'MINOR' number.
     *
     * @param version the version to compare
     *
     * @return the greatest difference
     */
    fun diff(version: Semver): VersionDiff {
        if (major != version.major) return VersionDiff.MAJOR
        if (minor != version.minor) return VersionDiff.MINOR
        if (patch != version.patch) return VersionDiff.PATCH
        if (!areSameSuffixes(version.suffixTokens)) return VersionDiff.SUFFIX
        return if (build != version.build) VersionDiff.BUILD else VersionDiff.NONE
    }

    private fun areSameSuffixes(suffixTokens: Array<String?>?): Boolean {
        if (this.suffixTokens == null && suffixTokens == null) return true else if (this.suffixTokens == null || suffixTokens == null) return false else if (this.suffixTokens.size != suffixTokens.size) return false
        for (i in this.suffixTokens.indices) {
            if (this.suffixTokens[i] != suffixTokens[i]) return false
        }
        return true
    }

    fun toStrict(): Semver {
        val minor = minor ?: 0
        val patch = patch ?: 0
        return create(SemverType.STRICT, major!!, minor, patch, suffixTokens, build)
    }

    @JvmOverloads
    fun withIncMajor(increment: Int = 1): Semver {
        return withInc(increment, 0, 0)
    }

    @JvmOverloads
    fun withIncMinor(increment: Int = 1): Semver {
        return withInc(0, increment, 0)
    }

    @JvmOverloads
    fun withIncPatch(increment: Int = 1): Semver {
        return withInc(0, 0, increment)
    }

    private fun withInc(majorInc: Int, minorInc: Int, patchInc: Int): Semver {
        val minor = minor?.plus(minorInc)
        val patch = patch?.plus(patchInc)
        return with(major!! + majorInc, minor!!, patch!!, true, true)
    }

    fun withClearedSuffix(): Semver {
        return with(major!!, minor!!, patch!!, false, true)
    }

    fun withClearedBuild(): Semver {
        return with(major!!, minor!!, patch!!, true, false)
    }

    fun withClearedSuffixAndBuild(): Semver {
        return with(major!!, minor!!, patch!!, false, false)
    }

    fun withSuffix(suffix: String): Semver {
        return with(major!!, minor!!, patch!!, suffix.split(".").toTypedArray(), build)
    }

    fun withBuild(build: String?): Semver {
        return with(major!!, minor!!, patch!!, suffixTokens, build)
    }

    fun nextMajor(): Semver {
        return with(major!! + 1, 0, 0, false, false)
    }

    fun nextMinor(): Semver {
        return with(major!!, minor!! + 1, 0, false, false)
    }

    fun nextPatch(): Semver {
        return with(major!!, minor!!, patch!! + 1, false, false)
    }

    private fun with(major: Int, minor: Int, patch: Int, suffix: Boolean, build: Boolean): Semver {
        val tempMinor: Int? = if (this.minor != null) minor else null
        val tempPatch: Int? = if (this.patch != null) patch else null
        val buildStr = if (build) this.build else null
        val suffixTokens = if (suffix) suffixTokens else null
        return create(type, major, tempMinor, tempPatch, suffixTokens, buildStr)
    }

    private fun with(major: Int, minor: Int, patch: Int, suffixTokens: Array<String?>?, build: String?): Semver {
        val tempMinor: Int? = if (this.minor != null) minor else null
        val tempPatch: Int? = if (this.patch != null) patch else null
        return create(type, major, tempMinor, tempPatch, suffixTokens, build)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Semver) return false
        return value == o.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun compareTo(version: Semver): Int {
        if (this.isGreaterThan(version)) return 1 else if (this.isLowerThan(version)) return -1
        return 0
    }

    override fun toString(): String {
        return value
    }

    /**
     * The types of diffs between two versions.
     */
    enum class VersionDiff {
        NONE, MAJOR, MINOR, PATCH, SUFFIX, BUILD
    }

    /**
     * The different types of supported version systems.
     */
    enum class SemverType {
        /**
         * The default type of version.
         * Major, minor and patch parts are required.
         * Suffixes and build are optional.
         */
        STRICT,

        /**
         * Major part is required.
         * Minor, patch, suffixes and build are optional.
         */
        LOOSE,

        /**
         * Follows the rules of NPM.
         * Supports ^, x, *, ~, and more.
         * See https://github.com/npm/node-semver
         */
        NPM,

        /**
         * Follows the rules of Cocoapods.
         * Supports optimistic and comparison operators
         * See https://guides.cocoapods.org/using/the-podfile.html
         */
        COCOAPODS,

        /**
         * Follows the rules of ivy.
         * Supports dynamic parts (eg: 4.2.+) and ranges
         * See http://ant.apache.org/ivy/history/latest-milestone/ivyfile/dependency.html
         */
        IVY
    }

    companion object {
        private fun create(type: SemverType, major: Int, minor: Int?, patch: Int?, suffix: Array<String?>?, build: String?): Semver {
            val sb = StringBuilder()
                    .append(major)
            if (minor != null) {
                sb.append(".").append(minor)
            }
            if (patch != null) {
                sb.append(".").append(patch)
            }
            if (suffix != null) {
                var first = true
                for (suffixToken in suffix) {
                    if (first) {
                        sb.append("-")
                        first = false
                    } else {
                        sb.append(".")
                    }
                    sb.append(suffixToken)
                }
            }
            if (build != null) {
                sb.append("+").append(build)
            }
            return Semver(sb.toString(), type)
        }
    }
}