package de.voize.semver4k

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import de.voize.semver4k.Semver.SemverType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

@RunWith(JUnit4::class)
class SemverTest {
    @Test(expected = SemverException::class)
    fun constructor_with_empty_build_fails() {
        Semver("1.0.0+")
    }

    @Test
    fun default_constructor_test_full_version() {
        val version = "1.2.3-beta.11+sha.0nsfgkjkjsdf"
        val semver = Semver(version)
        assertIsSemver(semver, version, 1, 2, 3, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
    }

    @Test(expected = SemverException::class)
    fun default_constructor_test_only_major_and_minor() {
        val version = "1.2-beta.11+sha.0nsfgkjkjsdf"
        Semver(version)
    }

    @Test(expected = SemverException::class)
    fun default_constructor_test_only_major() {
        val version = "1-beta.11+sha.0nsfgkjkjsdf"
        Semver(version)
    }

    @Test
    fun npm_constructor_test_full_version() {
        val version = "1.2.3-beta.11+sha.0nsfgkjkjsdf"
        val semver = Semver(version, SemverType.NPM)
        assertIsSemver(semver, version, 1, 2, 3, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
    }

    @Test
    fun npm_constructor_test_only_major_and_minor() {
        val version = "1.2-beta.11+sha.0nsfgkjkjsdf"
        val semver = Semver(version, SemverType.NPM)
        assertIsSemver(semver, version, 1, 2, null, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
    }

    @Test
    fun npm_constructor_test_only_major() {
        val version = "1-beta.11+sha.0nsfgkjkjsdf"
        val semver = Semver(version, SemverType.NPM)
        assertIsSemver(semver, version, 1, null, null, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
    }

    @Test
    fun npm_constructor_with_leading_v() {
        val version = "v1.2.3-beta.11+sha.0nsfgkjkjsdf"
        val semver = Semver(version, SemverType.NPM)
        assertIsSemver(semver, "1.2.3-beta.11+sha.0nsfgkjkjsdf", 1, 2, 3, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
        val versionWithSpace = "v 1.2.3-beta.11+sha.0nsfgkjkjsdf"
        val semverWithSpace = Semver(versionWithSpace, SemverType.NPM)
        assertIsSemver(semverWithSpace, "1.2.3-beta.11+sha.0nsfgkjkjsdf", 1, 2, 3, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
    }

    @Test
    fun cocoapods_constructor_test_full_version() {
        val version = "1.2.3-beta.11+sha.0nsfgkjkjsdf"
        val semver = Semver(version, SemverType.COCOAPODS)
        assertIsSemver(semver, version, 1, 2, 3, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
    }

    @Test
    fun cocoapods_constructor_test_only_major_and_minor() {
        val version = "1.2-beta.11+sha.0nsfgkjkjsdf"
        val semver = Semver(version, SemverType.COCOAPODS)
        assertIsSemver(semver, version, 1, 2, null, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
    }

    @Test
    fun cocoapods_constructor_test_only_major() {
        val version = "1-beta.11+sha.0nsfgkjkjsdf"
        val semver = Semver(version, SemverType.COCOAPODS)
        assertIsSemver(semver, version, 1, null, null, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
    }

    @Test
    fun loose_constructor_test_only_major_and_minor() {
        val version = "1.2-beta.11+sha.0nsfgkjkjsdf"
        val semver = Semver(version, SemverType.LOOSE)
        assertIsSemver(semver, version, 1, 2, null, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
    }

    @Test
    fun loose_constructor_test_only_major() {
        val version = "1-beta.11+sha.0nsfgkjkjsdf"
        val semver = Semver(version, SemverType.LOOSE)
        assertIsSemver(semver, version, 1, null, null, arrayOf("beta", "11"), "sha.0nsfgkjkjsdf")
    }

    @Test
    fun default_constructor_test_myltiple_hyphen_signs() {
        val version = "1.2.3-beta.1-1.ab-c+sha.0nsfgkjkjs-df"
        val semver = Semver(version)
        assertIsSemver(semver, version, 1, 2, 3, arrayOf("beta", "1-1", "ab-c"), "sha.0nsfgkjkjs-df")
    }

    @Test
    fun statisfies_works_will_all_the_types() {
        // Used to prevent bugs when we add a new type
        for (type in SemverType.values()) {
            val version = "1.2.3"
            val semver = Semver(version, type)
            Assert.assertTrue(semver.satisfies("1.2.3"))
            Assert.assertFalse(semver.satisfies("4.5.6"))
        }
    }

    // 1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-alpha.beta < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0-rc.1 < 1.0.0
    @Test
    fun isGreaterThan_test() {
        // 1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-alpha.beta < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0-rc.1 < 1.0.0
        Assert.assertTrue(Semver("1.0.0-alpha.1").isGreaterThan("1.0.0-alpha"))
        Assert.assertTrue(Semver("1.0.0-alpha.beta").isGreaterThan("1.0.0-alpha.1"))
        Assert.assertTrue(Semver("1.0.0-beta").isGreaterThan("1.0.0-alpha.beta"))
        Assert.assertTrue(Semver("1.0.0-beta.2").isGreaterThan("1.0.0-beta"))
        Assert.assertTrue(Semver("1.0.0-beta.11").isGreaterThan("1.0.0-beta.2"))
        Assert.assertTrue(Semver("1.0.0-rc.1").isGreaterThan("1.0.0-beta.11"))
        Assert.assertTrue(Semver("1.0.0").isGreaterThan("1.0.0-rc.1"))
        Assert.assertFalse(Semver("1.0.0-alpha").isGreaterThan("1.0.0-alpha.1"))
        Assert.assertFalse(Semver("1.0.0-alpha.1").isGreaterThan("1.0.0-alpha.beta"))
        Assert.assertFalse(Semver("1.0.0-alpha.beta").isGreaterThan("1.0.0-beta"))
        Assert.assertFalse(Semver("1.0.0-beta").isGreaterThan("1.0.0-beta.2"))
        Assert.assertFalse(Semver("1.0.0-beta.2").isGreaterThan("1.0.0-beta.11"))
        Assert.assertFalse(Semver("1.0.0-beta.11").isGreaterThan("1.0.0-rc.1"))
        Assert.assertFalse(Semver("1.0.0-rc.1").isGreaterThan("1.0.0"))
        Assert.assertFalse(Semver("1.0.0").isGreaterThan("1.0.0"))
        Assert.assertFalse(Semver("1.0.0-alpha.12").isGreaterThan("1.0.0-alpha.12"))
        Assert.assertFalse(Semver("0.0.1").isGreaterThan("5.0.0"))
        Assert.assertFalse(Semver("1.0.0-alpha.12.ab-c").isGreaterThan("1.0.0-alpha.12.ab-c"))
    }

    // 1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-alpha.beta < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0-rc.1 < 1.0.0
    @Test
    fun isLowerThan_test() {
        // 1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-alpha.beta < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0-rc.1 < 1.0.0
        Assert.assertFalse(Semver("1.0.0-alpha.1").isLowerThan("1.0.0-alpha"))
        Assert.assertFalse(Semver("1.0.0-alpha.beta").isLowerThan("1.0.0-alpha.1"))
        Assert.assertFalse(Semver("1.0.0-beta").isLowerThan("1.0.0-alpha.beta"))
        Assert.assertFalse(Semver("1.0.0-beta.2").isLowerThan("1.0.0-beta"))
        Assert.assertFalse(Semver("1.0.0-beta.11").isLowerThan("1.0.0-beta.2"))
        Assert.assertFalse(Semver("1.0.0-rc.1").isLowerThan("1.0.0-beta.11"))
        Assert.assertFalse(Semver("1.0.0").isLowerThan("1.0.0-rc.1"))
        Assert.assertTrue(Semver("1.0.0-alpha").isLowerThan("1.0.0-alpha.1"))
        Assert.assertTrue(Semver("1.0.0-alpha.1").isLowerThan("1.0.0-alpha.beta"))
        Assert.assertTrue(Semver("1.0.0-alpha.beta").isLowerThan("1.0.0-beta"))
        Assert.assertTrue(Semver("1.0.0-beta").isLowerThan("1.0.0-beta.2"))
        Assert.assertTrue(Semver("1.0.0-beta.2").isLowerThan("1.0.0-beta.11"))
        Assert.assertTrue(Semver("1.0.0-beta.11").isLowerThan("1.0.0-rc.1"))
        Assert.assertTrue(Semver("1.0.0-rc.1").isLowerThan("1.0.0"))
        Assert.assertFalse(Semver("1.0.0").isLowerThan("1.0.0"))
        Assert.assertFalse(Semver("1.0.0-alpha.12").isLowerThan("1.0.0-alpha.12"))
        Assert.assertFalse(Semver("1.0.0-alpha.12.x-yz").isLowerThan("1.0.0-alpha.12.x-yz"))
    }

    @Test
    fun isEquivalentTo_isEqualTo_and_build() {
        val semver = Semver("1.0.0+ksadhjgksdhgksdhgfj")
        val version2 = "1.0.0+sdgfsdgsdhsdfgdsfgf"
        Assert.assertFalse(semver.isEqualTo(version2))
        Assert.assertTrue(semver.isEquivalentTo(version2))
    }

    @Test
    fun statisfies_calls_the_requirement() {
        val semver = Semver("1.2.2")
        val req = mockk<Requirement>()
        every { req.isSatisfiedBy(semver) } returns true
        semver.satisfies(req)
        verify { req.isSatisfiedBy(semver) }
    }

    @Test
    fun withIncMajor_test() {
        val semver = Semver("1.2.3-Beta.4+SHA123456789")
        semver.withIncMajor(2).isEqualTo("3.2.3-Beta.4+SHA123456789")
    }

    @Test
    fun withIncMinor_test() {
        val semver = Semver("1.2.3-Beta.4+SHA123456789")
        semver.withIncMinor(2).isEqualTo("1.4.3-Beta.4+SHA123456789")
    }

    @Test
    fun withIncPatch_test() {
        val semver = Semver("1.2.3-Beta.4+SHA123456789")
        semver.withIncPatch(2).isEqualTo("1.2.5-Beta.4+SHA123456789")
    }

    @Test
    fun withClearedSuffix_test() {
        val semver = Semver("1.2.3-Beta.4+SHA123456789")
        semver.withClearedSuffix().isEqualTo("1.2.3+SHA123456789")
    }

    @Test
    fun withClearedBuild_test() {
        val semver = Semver("1.2.3-Beta.4+sha123456789")
        semver.withClearedBuild().isEqualTo("1.2.3-Beta.4")
    }

    @Test
    fun withClearedBuild_test_multiple_hyphen_signs() {
        val semver = Semver("1.2.3-Beta.4-test+sha12345-6789")
        semver.withClearedBuild().isEqualTo("1.2.3-Beta.4-test")
    }

    @Test
    fun withClearedSuffixAndBuild_test() {
        val semver = Semver("1.2.3-Beta.4+SHA123456789")
        semver.withClearedSuffixAndBuild().isEqualTo("1.2.3")
    }

    @Test
    fun withSuffix_test_change_suffix() {
        val semver = Semver("1.2.3-Alpha.4+SHA123456789")
        val result = semver.withSuffix("Beta.1")
        Assert.assertEquals("1.2.3-Beta.1+SHA123456789", result.toString())
        Assert.assertArrayEquals(arrayOf("Beta", "1"), result.suffixTokens)
    }

    @Test
    fun withSuffix_test_add_suffix() {
        val semver = Semver("1.2.3+SHA123456789")
        val result = semver.withSuffix("Beta.1")
        Assert.assertEquals("1.2.3-Beta.1+SHA123456789", result.toString())
        Assert.assertArrayEquals(arrayOf("Beta", "1"), result.suffixTokens)
    }

    @Test
    fun withBuild_test_change_build() {
        val semver = Semver("1.2.3-Alpha.4+SHA123456789")
        val result = semver.withBuild("SHA987654321")
        Assert.assertEquals("1.2.3-Alpha.4+SHA987654321", result.toString())
        Assert.assertEquals("SHA987654321", result.build)
    }

    @Test
    fun withBuild_test_add_build() {
        val semver = Semver("1.2.3-Alpha.4")
        val result = semver.withBuild("SHA987654321")
        Assert.assertEquals("1.2.3-Alpha.4+SHA987654321", result.toString())
        Assert.assertEquals("SHA987654321", result.build)
    }

    @Test
    fun nextMajor_test() {
        val semver = Semver("1.2.3-beta.4+sha123456789")
        semver.nextMajor().isEqualTo("2.0.0")
    }

    @Test
    fun nextMinor_test() {
        val semver = Semver("1.2.3-beta.4+sha123456789")
        semver.nextMinor().isEqualTo("1.3.0")
    }

    @Test
    fun nextPatch_test() {
        val semver = Semver("1.2.3-beta.4+sha123456789")
        semver.nextPatch().isEqualTo("1.2.4")
    }

    @Test
    fun toStrict_test() {
        val versionGroups = arrayOf(arrayOf("3.0.0-beta.4+sha123456789", "3.0-beta.4+sha123456789", "3-beta.4+sha123456789"), arrayOf("3.0.0+sha123456789", "3.0+sha123456789", "3+sha123456789"), arrayOf("3.0.0-beta.4", "3.0-beta.4", "3-beta.4"), arrayOf("3.0.0", "3.0", "3"))
        val types = arrayOf(
                SemverType.NPM,
                SemverType.IVY,
                SemverType.LOOSE,
                SemverType.COCOAPODS)
        for (versions in versionGroups) {
            val strict = Semver(versions[0])
            Assert.assertEquals(strict, strict.toStrict())
            for (type in types) {
                for (version in versions) {
                    val sem = Semver(version, type)
                    Assert.assertEquals(strict, sem.toStrict())
                }
            }
        }
    }

    @Test
    fun diff() {
        val sem = Semver("1.2.3-beta.4+sha899d8g79f87")
        Assert.assertEquals(Semver.VersionDiff.NONE, sem.diff("1.2.3-beta.4+sha899d8g79f87"))
        Assert.assertEquals(Semver.VersionDiff.MAJOR, sem.diff("2.3.4-alpha.5+sha32iddfu987"))
        Assert.assertEquals(Semver.VersionDiff.MINOR, sem.diff("1.3.4-alpha.5+sha32iddfu987"))
        Assert.assertEquals(Semver.VersionDiff.PATCH, sem.diff("1.2.4-alpha.5+sha32iddfu987"))
        Assert.assertEquals(Semver.VersionDiff.SUFFIX, sem.diff("1.2.3-alpha.4+sha32iddfu987"))
        Assert.assertEquals(Semver.VersionDiff.SUFFIX, sem.diff("1.2.3-beta.5+sha32iddfu987"))
        Assert.assertEquals(Semver.VersionDiff.BUILD, sem.diff("1.2.3-beta.4+sha32iddfu987"))
        Assert.assertEquals(Semver.VersionDiff.BUILD, sem.diff("1.2.3-beta.4+sha899-d8g79f87"))
    }

    @Test
    fun compareTo_test() {
        // GIVEN
        val array = listOf(
                Semver("1.2.3"),
                Semver("1.2.3-rc3"),
                Semver("1.2.3-rc2"),
                Semver("1.2.3-rc1"),
                Semver("1.2.2"),
                Semver("1.2.2-rc2"),
                Semver("1.2.2-rc1"),
                Semver("1.2.0")
        )


        // WHEN
        val sortedArray = array.toList().sorted()

        // THEN
        Assert.assertEquals(array.reversed(), sortedArray)
    }

    @Test
    fun compareTo_without_path_or_minor() {
        Assert.assertTrue(Semver("1.2.3", SemverType.LOOSE).isGreaterThan("1.2"))
        Assert.assertTrue(Semver("1.3", SemverType.LOOSE).isGreaterThan("1.2.3"))
        Assert.assertTrue(Semver("1.2.3", SemverType.LOOSE).isGreaterThan("1"))
        Assert.assertTrue(Semver("2", SemverType.LOOSE).isGreaterThan("1.2.3"))
    }

    @Test
    fun value_returns_the_original_value_trimmed_and_with_the_same_case() {
        val version = "  1.2.3-BETA.11+sHa.0nSFGKjkjsdf  "
        val semver = Semver(version)
        Assert.assertEquals("1.2.3-BETA.11+sHa.0nSFGKjkjsdf", semver.value)
    }

    @Test
    fun compareTo_with_buildNumber() {
        val v3 = Semver("1.24.1-rc3+903423.234")
        val v4 = Semver("1.24.1-rc3+903423.235")
        Assert.assertEquals(0, v3.compareTo(v4).toLong())
    }

    @Test
    fun isStable_test() {
        Assert.assertTrue(Semver("1.2.3+sHa.0nSFGKjkjsdf").isStable)
        Assert.assertTrue(Semver("1.2.3").isStable)
        Assert.assertFalse(Semver("1.2.3-BETA.11+sHa.0nSFGKjkjsdf").isStable)
        Assert.assertFalse(Semver("0.1.2+sHa.0nSFGKjkjsdf").isStable)
        Assert.assertFalse(Semver("0.1.2").isStable)
    }

    @Test
    fun findsNextIncrements() {
        Assert.assertEquals(Semver("0.1.1"), Semver("0.1.0").nextIncrement())
        Assert.assertEquals(Semver("1.0.1"), Semver("1.0.0").nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.0"), Semver("1.0.0-beta").nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.5"), Semver("1.0.0-beta.4").nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.5+sha899-d8g79f87"), Semver("1.0.0-beta.4+sha899-d8g79f87").nextIncrement())
    }

    @Test
    fun findsNextIncrements_loose() {
        Assert.assertEquals(Semver("2", SemverType.LOOSE), Semver("1", SemverType.LOOSE).nextIncrement())
        Assert.assertEquals(Semver("1.2", SemverType.LOOSE), Semver("1.1", SemverType.LOOSE).nextIncrement())
        Assert.assertEquals(Semver("0.1.1", SemverType.LOOSE), Semver("0.1.0", SemverType.LOOSE).nextIncrement())
        Assert.assertEquals(Semver("1.0.1", SemverType.LOOSE), Semver("1.0.0", SemverType.LOOSE).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.0", SemverType.LOOSE), Semver("1.0.0-beta", SemverType.LOOSE).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.5", SemverType.LOOSE), Semver("1.0.0-beta.4", SemverType.LOOSE).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.5+sha899-d8g79f87", SemverType.LOOSE), Semver("1.0.0-beta.4+sha899-d8g79f87", SemverType.LOOSE).nextIncrement())
    }

    @Test
    fun findsNextIncrements_npm() {
        Assert.assertEquals(Semver("2", SemverType.NPM), Semver("1", SemverType.NPM).nextIncrement())
        Assert.assertEquals(Semver("1.2", SemverType.NPM), Semver("1.1", SemverType.NPM).nextIncrement())
        Assert.assertEquals(Semver("0.1.1", SemverType.NPM), Semver("0.1.0", SemverType.NPM).nextIncrement())
        Assert.assertEquals(Semver("1.0.1", SemverType.NPM), Semver("1.0.0", SemverType.NPM).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.0", SemverType.NPM), Semver("1.0.0-beta", SemverType.NPM).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.5", SemverType.NPM), Semver("1.0.0-beta.4", SemverType.NPM).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.5+sha899-d8g79f87", SemverType.NPM), Semver("1.0.0-beta.4+sha899-d8g79f87", SemverType.NPM).nextIncrement())
    }

    @Test
    fun findsNextIncrements_ivy() {
        Assert.assertEquals(Semver("2", SemverType.IVY), Semver("1", SemverType.IVY).nextIncrement())
        Assert.assertEquals(Semver("1.2", SemverType.IVY), Semver("1.1", SemverType.IVY).nextIncrement())
        Assert.assertEquals(Semver("0.1.1", SemverType.IVY), Semver("0.1.0", SemverType.IVY).nextIncrement())
        Assert.assertEquals(Semver("1.0.1", SemverType.IVY), Semver("1.0.0", SemverType.IVY).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.0", SemverType.IVY), Semver("1.0.0-beta", SemverType.IVY).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.5", SemverType.IVY), Semver("1.0.0-beta.4", SemverType.IVY).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.5+sha899-d8g79f87", SemverType.IVY), Semver("1.0.0-beta.4+sha899-d8g79f87", SemverType.IVY).nextIncrement())
    }

    @Test
    fun findsNextIncrements_cocoapods() {
        Assert.assertEquals(Semver("2", SemverType.COCOAPODS), Semver("1", SemverType.COCOAPODS).nextIncrement())
        Assert.assertEquals(Semver("1.2", SemverType.COCOAPODS), Semver("1.1", SemverType.COCOAPODS).nextIncrement())
        Assert.assertEquals(Semver("0.1.1", SemverType.COCOAPODS), Semver("0.1.0", SemverType.COCOAPODS).nextIncrement())
        Assert.assertEquals(Semver("1.0.1", SemverType.COCOAPODS), Semver("1.0.0", SemverType.COCOAPODS).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.0", SemverType.COCOAPODS), Semver("1.0.0-beta", SemverType.COCOAPODS).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.5", SemverType.COCOAPODS), Semver("1.0.0-beta.4", SemverType.COCOAPODS).nextIncrement())
        Assert.assertEquals(Semver("1.0.0-beta.5+sha899-d8g79f87", SemverType.COCOAPODS), Semver("1.0.0-beta.4+sha899-d8g79f87", SemverType.COCOAPODS).nextIncrement())
    }

    companion object {
        private fun assertIsSemver(semver: Semver, value: String, major: Int, minor: Int?, patch: Int?, suffixTokens: Array<String>, build: String) {
            Assert.assertEquals(value, semver.value)
            Assert.assertEquals(major, semver.major)
            Assert.assertEquals(minor, semver.minor)
            Assert.assertEquals(patch, semver.patch)
            Assert.assertEquals(suffixTokens.size.toLong(), semver.suffixTokens?.size?.toLong())
            for (i in suffixTokens.indices) {
                Assert.assertEquals(suffixTokens[i], semver.suffixTokens?.get(i))
            }
            Assert.assertEquals(build, semver.build)
        }
    }
}