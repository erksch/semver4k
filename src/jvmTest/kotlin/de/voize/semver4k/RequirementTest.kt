package de.voize.semver4k

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import de.voize.semver4k.Range.RangeOperator
import de.voize.semver4k.Semver.SemverType
import io.mockk.*
import org.junit.Assert
import org.junit.Test

@RunWith(JUnit4::class)
class RequirementTest {
    @Test
    fun buildStrict() {
        val version = "1.2.3"
        val requirement = Requirement.buildStrict(version)
        assertIsRange(requirement, version, RangeOperator.EQ)
    }

    @Test
    fun buildLoose() {
        val version = "0.27"
        val requirement = Requirement.buildLoose(version)
        assertIsRange(requirement, version, RangeOperator.EQ)
    }

    @Test
    fun buildNPM_with_a_full_version() {
        val version = "1.2.3"
        val requirement = Requirement.buildNPM(version)
        assertIsRange(requirement, version, RangeOperator.EQ)
    }

    @Test
    fun buildNPM_with_a_version_with_a_leading_v() {
        assertIsRange(Requirement.buildNPM("v1.2.3"), "1.2.3", RangeOperator.EQ)
        assertIsRange(Requirement.buildNPM("v 1.2.3"), "1.2.3", RangeOperator.EQ)
    }

    @Test
    fun buildNPM_with_a_version_with_a_leading_equal() {
        assertIsRange(Requirement.buildNPM("=1.2.3"), "1.2.3", RangeOperator.EQ)
        assertIsRange(Requirement.buildNPM("= 1.2.3"), "1.2.3", RangeOperator.EQ)
    }

    @Test
    fun buildNPM_with_a_range() {
        val req = Requirement.buildNPM(">=1.2.3 <4.5.6")
        rangeTest(req, "1.2.3", "4.5.6", true)
    }

    @Test
    fun buildNPM_with_a_OR_operator() {
        val req = Requirement.buildNPM(">=1.2.3 || >4.5.6")
        Assert.assertNull(req.range)
        Assert.assertEquals(Requirement.RequirementOperator.OR, req.op)
        val req1 = req.req1
        Assert.assertEquals(RangeOperator.GTE, req1?.range?.op)
        Assert.assertEquals("1.2.3", req1?.range?.version?.value)
        val req2 = req.req2
        Assert.assertEquals(RangeOperator.GT, req2?.range?.op)
        Assert.assertEquals("4.5.6", req2?.range?.version?.value)
    }

    @Test
    fun buildNPM_with_OR_and_AND_operators() {
        val req = Requirement.buildNPM(">1.2.1 <1.2.8 || >2.0.0 <3.0.0")
        Assert.assertNull(req.range)
        Assert.assertEquals(Requirement.RequirementOperator.OR, req.op)

        // >1.2.1 <1.2.8
        val req1 = req.req1
        Assert.assertNull(req1?.range)
        Assert.assertEquals(Requirement.RequirementOperator.AND, req1?.op)
        val req1_1 = req1?.req1
        Assert.assertNull(req1_1?.op)
        Assert.assertEquals(RangeOperator.GT, req1_1?.range?.op)
        Assert.assertEquals("1.2.1", req1_1?.range?.version?.value)
        val req1_2 = req1?.req2
        Assert.assertNull(req1_2?.op)
        Assert.assertEquals(RangeOperator.LT, req1_2?.range?.op)
        Assert.assertEquals("1.2.8", req1_2?.range?.version?.value)

        // >2.0.0 < 3.0.0
        val req2 = req.req2
        Assert.assertNull(req2?.range)
        Assert.assertEquals(Requirement.RequirementOperator.AND, req2?.op)
        val req2_1 = req2?.req1
        Assert.assertNull(req2_1?.op)
        Assert.assertEquals(RangeOperator.GT, req2_1?.range?.op)
        Assert.assertEquals("2.0.0", req2_1?.range?.version?.value)
        val req2_2 = req2?.req2
        Assert.assertNull(req2_2?.op)
        Assert.assertEquals(RangeOperator.LT, req2_2?.range?.op)
        Assert.assertEquals("3.0.0", req2_2?.range?.version?.value)
    }

    @Test
    fun tildeRequirement_npm_full_version() {
        // ~1.2.3 := >=1.2.3 <1.(2+1).0 := >=1.2.3 <1.3.0
        tildeTest("1.2.3", "1.2.3", "1.3.0", SemverType.NPM)
    }

    @Test
    fun tildeRequirement_npm_only_major_and_minor() {
        // ~1.2 := >=1.2.0 <1.(2+1).0 := >=1.2.0 <1.3.0
        tildeTest("1.2", "1.2.0", "1.3.0", SemverType.NPM)
        tildeTest("1.2.x", "1.2.0", "1.3.0", SemverType.NPM)
        tildeTest("1.2.*", "1.2.0", "1.3.0", SemverType.NPM)
    }

    @Test
    fun tildeRequirement_npm_only_major() {
        // ~1 := >=1.0.0 <(1+1).0.0 := >=1.0.0 <2.0.0
        tildeTest("1", "1.0.0", "2.0.0", SemverType.NPM)
        tildeTest("1.x", "1.0.0", "2.0.0", SemverType.NPM)
        tildeTest("1.x.x", "1.0.0", "2.0.0", SemverType.NPM)
        tildeTest("1.*", "1.0.0", "2.0.0", SemverType.NPM)
        tildeTest("1.*.*", "1.0.0", "2.0.0", SemverType.NPM)
    }

    @Test
    fun tildeRequirement_npm_full_version_major_0() {
        // ~0.2.3 := >=0.2.3 <0.(2+1).0 := >=0.2.3 <0.3.0
        tildeTest("0.2.3", "0.2.3", "0.3.0", SemverType.NPM)
    }

    @Test
    fun tildeRequirement_npm_only_major_and_minor_with_major_0() {
        // ~0.2 := >=0.2.0 <0.(2+1).0 := >=0.2.0 <0.3.0
        tildeTest("0.2", "0.2.0", "0.3.0", SemverType.NPM)
        tildeTest("0.2.x", "0.2.0", "0.3.0", SemverType.NPM)
        tildeTest("0.2.*", "0.2.0", "0.3.0", SemverType.NPM)
    }

    @Test
    fun tildeRequirement_npm_only_major_with_major_0() {
        // ~0 := >=0.0.0 <(0+1).0.0 := >=0.0.0 <1.0.0
        tildeTest("0", "0.0.0", "1.0.0", SemverType.NPM)
        tildeTest("0.x", "0.0.0", "1.0.0", SemverType.NPM)
        tildeTest("0.x.x", "0.0.0", "1.0.0", SemverType.NPM)
        tildeTest("0.*", "0.0.0", "1.0.0", SemverType.NPM)
        tildeTest("0.*.*", "0.0.0", "1.0.0", SemverType.NPM)
    }

    @Test
    fun tildeRequirement_npm_with_suffix() {
        // ~1.2.3-beta.2 := >=1.2.3-beta.2 <1.3.0
        tildeTest("1.2.3-beta.2", "1.2.3-beta.2", "1.3.0", SemverType.NPM)
    }

    @Test
    fun caretRequirement_npm_full_version() {
        // ^1.2.3 := >=1.2.3 <2.0.0
        caretTest("1.2.3", "1.2.3", "2.0.0")
    }

    @Test
    fun caretRequirement_npm_full_version_with_major_0() {
        // ^0.2.3 := >=0.2.3 <0.3.0
        caretTest("0.2.3", "0.2.3", "0.3.0")
    }

    @Test
    fun caretRequirement_npm_full_version_with_major_and_minor_0() {
        // ^0.0.3 := >=0.0.3 <0.0.4
        caretTest("0.0.3", "0.0.3", "0.0.4")
    }

    @Test
    fun caretRequirement_npm_with_suffix() {
        // ^1.2.3-beta.2 := >=1.2.3-beta.2 <2.0.0
        caretTest("1.2.3-beta.2", "1.2.3-beta.2", "2.0.0")
    }

    @Test
    fun caretRequirement_npm_with_major_and_minor_0_and_suffix() {
        // ^0.0.3-beta := >=0.0.3-beta <0.0.4
        caretTest("0.0.3-beta", "0.0.3-beta", "0.0.4")
    }

    @Test
    fun caretRequirement_npm_without_patch() {
        // ^1.2.x := >=1.2.0 <2.0.0
        caretTest("1.2", "1.2.0", "2.0.0")
        caretTest("1.2.x", "1.2.0", "2.0.0")
        caretTest("1.2.*", "1.2.0", "2.0.0")
    }

    @Test
    fun caretRequirement_npm_with_only_major() {
        // ^1.x.x := >=1.0.0 <2.0.0
        caretTest("1", "1.0.0", "2.0.0")
        caretTest("1.x", "1.0.0", "2.0.0")
        caretTest("1.x.x", "1.0.0", "2.0.0")
        caretTest("1.*", "1.0.0", "2.0.0")
        caretTest("1.*.*", "1.0.0", "2.0.0")
    }

    @Test
    fun caretRequirement_npm_with_only_major_0() {
        // ^0.x := >=0.0.0 <1.0.0
        caretTest("0", "0.0.0", "1.0.0")
        caretTest("0.x", "0.0.0", "1.0.0")
        caretTest("0.x.x", "0.0.0", "1.0.0")
        caretTest("0.*", "0.0.0", "1.0.0")
        caretTest("0.*.*", "0.0.0", "1.0.0")
    }

    @Test
    fun caretRequirement_npm_without_patch_with_major_and_minor_0() {
        // ^0.0.x := >=0.0.0 <0.1.0
        caretTest("0.0", "0.0.0", "0.1.0")
        caretTest("0.0.x", "0.0.0", "0.1.0")
        caretTest("0.0.*", "0.0.0", "0.1.0")
    }

    @Test
    fun hyphenRequirement() {
        // 1.2.3 - 2.3.4 := >=1.2.3 <=2.3.4
        hyphenTest("1.2.3", "2.3.4", "1.2.3", "2.3.4", false)
    }

    @Test
    fun hyphenRequirement_with_partial_lower_bound() {
        // 1.2 - 2.3.4 := >=1.2.0 <=2.3.4
        hyphenTest("1.2", "2.3.4", "1.2.0", "2.3.4", false)
        hyphenTest("1.2.x", "2.3.4", "1.2.0", "2.3.4", false)
        hyphenTest("1.2.*", "2.3.4", "1.2.0", "2.3.4", false)

        // 1 - 2.3.4 := >=1.0.0 <=2.3.4
        hyphenTest("1", "2.3.4", "1.0.0", "2.3.4", false)
        hyphenTest("1.x", "2.3.4", "1.0.0", "2.3.4", false)
        hyphenTest("1.x.x", "2.3.4", "1.0.0", "2.3.4", false)
        hyphenTest("1.*", "2.3.4", "1.0.0", "2.3.4", false)
        hyphenTest("1.*.*", "2.3.4", "1.0.0", "2.3.4", false)
    }

    @Test
    fun hyphenRequirement_with_partial_upper_bound() {
        // 1.2.3 - 2.3 := >=1.2.3 <2.4.0
        hyphenTest("1.2.3", "2.3", "1.2.3", "2.4.0", true)
        hyphenTest("1.2.3", "2.3.x", "1.2.3", "2.4.0", true)
        hyphenTest("1.2.3", "2.3.*", "1.2.3", "2.4.0", true)

        // 1.2.3 - 2 := >=1.2.3 <3.0.0
        hyphenTest("1.2.3", "2", "1.2.3", "3.0.0", true)
        hyphenTest("1.2.3", "2.x", "1.2.3", "3.0.0", true)
        hyphenTest("1.2.3", "2.x.x", "1.2.3", "3.0.0", true)
        hyphenTest("1.2.3", "2.*", "1.2.3", "3.0.0", true)
        hyphenTest("1.2.3", "2.*.*", "1.2.3", "3.0.0", true)
    }

    @Test
    fun buildNPM_with_hyphen() {
        val reqs = arrayOf(
                // Hyphens must have a space before and after to be valid npm requirements
                //Requirement.buildNPM("1.2.3-2.3.4"),
                //Requirement.buildNPM("1.2.3 -2.3.4"),
                //Requirement.buildNPM("1.2.3- 2.3.4"),
                Requirement.buildNPM("1.2.3 - 2.3.4")
        )
        for (req in reqs) {
            rangeTest(req, "1.2.3", "2.3.4", false)
        }
    }

    @Test
    fun buildNPM_with_a_wildcard() {
        val req = Requirement.buildNPM("*")
        Assert.assertNull(req.op)
        Assert.assertNull(req.req1)
        Assert.assertNull(req.req2)
        Assert.assertEquals(RangeOperator.GTE, req.range?.op)
        Assert.assertEquals(Semver("0.0.0"), req.range?.version)
    }

    @Test
    fun buildCocoapods_with_a_tilde() {
        val reqs = arrayOf(
                Requirement.buildCocoapods(" ~> 1.2.3 "),
                Requirement.buildCocoapods(" ~> 1.2.3"),
                Requirement.buildCocoapods("~> 1.2.3 "),
                Requirement.buildCocoapods(" ~>1.2.3 "),
                Requirement.buildCocoapods("~>1.2.3 "),
                Requirement.buildCocoapods("~> 1.2.3"),
                Requirement.buildCocoapods("~>1.2.3"))
        for (req in reqs) {
            rangeTest(req, "1.2.3", "1.3.0", true)
        }
    }

    @Test
    fun buildCocoapods_with_a_wildcard() {
        val req = Requirement.buildCocoapods("*")
        Assert.assertNull(req.op)
        Assert.assertNull(req.req1)
        Assert.assertNull(req.req2)
        Assert.assertEquals(RangeOperator.GTE, req.range?.op)
        Assert.assertEquals(Semver("0.0.0"), req.range?.version)
    }

    @Test
    fun buildIvy_with_a_dynamic_patch() {
        val req = Requirement.buildIvy("1.2.+")
        Assert.assertEquals(Requirement.RequirementOperator.AND, req.op)
        Assert.assertNull(req.range)
        assertIsRange(req.req1!!, "1.2.0", RangeOperator.GTE)
        assertIsRange(req.req2!!, "1.3.0", RangeOperator.LT)
    }

    @Test
    fun buildIvy_with_a_dynamic_minor() {
        val req = Requirement.buildIvy("1.+")
        Assert.assertEquals(Requirement.RequirementOperator.AND, req.op)
        Assert.assertNull(req.range)
        assertIsRange(req.req1!!, "1.0.0", RangeOperator.GTE)
        assertIsRange(req.req2!!, "2.0.0", RangeOperator.LT)
    }

    @Test
    fun buildIvy_with_latest() {
        val req = Requirement.buildIvy("latest.integration")
        assertIsRange(req, "0.0.0", RangeOperator.GTE)
    }

    @Test
    fun buildIvy_with_mathematical_bounded_ranges() {
        rangeTest(Requirement.buildIvy("[1.0,2.0]"), "1.0.0", false, "2.0.0", false)
        rangeTest(Requirement.buildIvy("[1.0,2.0["), "1.0.0", false, "2.0.0", true)
        rangeTest(Requirement.buildIvy("]1.0,2.0]"), "1.0.0", true, "2.0.0", false)
        rangeTest(Requirement.buildIvy("]1.0,2.0["), "1.0.0", true, "2.0.0", true)
    }

    @Test
    fun buildIvy_with_mathematical_unbounded_ranges() {
        assertIsRange(Requirement.buildIvy("[1.0,)"), "1.0.0", RangeOperator.GTE)
        assertIsRange(Requirement.buildIvy("]1.0,)"), "1.0.0", RangeOperator.GT)
        assertIsRange(Requirement.buildIvy("(,2.0]"), "2.0.0", RangeOperator.LTE)
        assertIsRange(Requirement.buildIvy("(,2.0["), "2.0.0", RangeOperator.LT)
    }

    @Test
    fun isSatisfiedBy_with_a_loose_type() {
        val req = Requirement.buildLoose("1.3.2")
        Assert.assertFalse(req.isSatisfiedBy("0.27"))
        Assert.assertTrue(req.isSatisfiedBy("1.3.2"))
        Assert.assertFalse(req.isSatisfiedBy("1.5"))
    }

    @Test
    fun isSatisfiedBy_with_a_complex_example() {
        val req = Requirement.buildNPM("1.x || >=2.5.0 || 5.0.0 - 7.2.3")
        Assert.assertTrue(req.isSatisfiedBy("1.2.3"))
        Assert.assertTrue(req.isSatisfiedBy("2.5.2"))
        Assert.assertFalse(req.isSatisfiedBy("0.2.3"))
    }

    @Test
    fun isSatisfiedBy_with_a_range() {
        val range = mockk<Range>()
        val version = Semver("1.2.3")
        every { range.isSatisfiedBy(version) } returns true
        val requirement = Requirement(range, null, null, null)
        requirement.isSatisfiedBy(version)
        verify { range.isSatisfiedBy(version) }
    }

    @Test
    fun isSatisfiedBy_with_subRequirements_AND_first_is_true() {
        val version = Semver("1.2.3")
        val req1 = mockk<Requirement>()
        every { req1.isSatisfiedBy(version) } returns true
        val req2 = mockk<Requirement>()
        every { req2.isSatisfiedBy(version) } returns true
        val requirement = Requirement(null, req1, Requirement.RequirementOperator.AND, req2)
        requirement.isSatisfiedBy(version)
        verify { req1.isSatisfiedBy(version) }
        verify { req2.isSatisfiedBy(version) }
    }

    @Test
    fun isSatisfiedBy_with_subRequirements_AND_first_is_false() {
        val version = Semver("1.2.3")
        val req1 = mockk<Requirement>()
        every { req1.isSatisfiedBy(version) } returns false
        val req2 = mockk<Requirement>()
        every { req2.isSatisfiedBy(version) } returns true
        val requirement = Requirement(null, req1, Requirement.RequirementOperator.AND, req2)
        requirement.isSatisfiedBy(version)
        verify { req1.isSatisfiedBy(version) }
        verify { req2 wasNot Called }
    }

    @Test
    fun isSatisfiedBy_with_subRequirements_OR_first_is_true() {
        val version = Semver("1.2.3")
        val req1 = mockk<Requirement>()
        every { req1.isSatisfiedBy(version) } returns true
        val req2 = mockk<Requirement>()
        every { req2.isSatisfiedBy(version) } returns true
        val requirement = Requirement(null, req1, Requirement.RequirementOperator.OR, req2)
        requirement.isSatisfiedBy(version)
        verify { req1.isSatisfiedBy(version) }
        verify { req2 wasNot Called }
    }

    @Test
    fun isSatisfiedBy_with_subRequirements_OR_first_is_false() {
        val version = Semver("1.2.3")
        val req1 = mockk<Requirement>()
        every { req1.isSatisfiedBy(version) } returns false
        val req2 = mockk<Requirement>()
        every { req2.isSatisfiedBy(version) } returns true
        val requirement = Requirement(null, req1, Requirement.RequirementOperator.OR, req2)
        requirement.isSatisfiedBy(version)
        verifyAll {
            req1.isSatisfiedBy(version)
            req2.isSatisfiedBy(version)
        }
    }

    @Test
    fun npm_isSatisfiedBy_with_an_empty_string() {
        val req = Requirement.buildNPM("")
        Assert.assertTrue(req.isSatisfiedBy("1.2.3"))
        Assert.assertTrue(req.isSatisfiedBy("2.5.2"))
        Assert.assertTrue(req.isSatisfiedBy("0.2.3"))
    }

    @Test
    fun isSatisfiedBy_with_a_star() {
        val req = Requirement.buildNPM("*")
        Assert.assertTrue(req.isSatisfiedBy("1.2.3"))
        Assert.assertTrue(req.isSatisfiedBy("2.5.2"))
        Assert.assertTrue(req.isSatisfiedBy("0.2.3"))
    }

    @Test
    fun isSatisfiedBy_with_latest() {
        val req = Requirement.buildNPM("latest")
        Assert.assertTrue(req.isSatisfiedBy("1.2.3"))
        Assert.assertTrue(req.isSatisfiedBy("2.5.2"))
        Assert.assertTrue(req.isSatisfiedBy("0.2.3"))
    }

    @Test
    fun tildeRequirement_cocoapods() {
        // '~> 0.1.2' Version 0.1.2 and the versions up to 0.2, not including 0.2 and higher
        tildeTest("0.1.2", "0.1.2", "0.2.0", SemverType.COCOAPODS)
        tildeTest("1.1.2", "1.1.2", "1.2.0", SemverType.COCOAPODS)

        // '~> 0.1' Version 0.1 and the versions up to 1.0, not including 1.0 and higher
        tildeTest("0.1", "0.1.0", "1.0.0", SemverType.COCOAPODS)
        tildeTest("1.1", "1.1.0", "2.0.0", SemverType.COCOAPODS)

        // '~> 0' Version 0 and higher, this is basically the same as not having it.
        var req = Requirement.tildeRequirement("0", SemverType.COCOAPODS)
        Assert.assertNull(req.op)
        Assert.assertEquals(RangeOperator.GTE, req.range?.op)
        Assert.assertTrue(req.range?.version?.isEquivalentTo("0.0.0") ?: false)
        req = Requirement.tildeRequirement("1", SemverType.COCOAPODS)
        Assert.assertNull(req.op)
        Assert.assertEquals(RangeOperator.GTE, req.range?.op)
        Assert.assertTrue(req.range?.version?.isEquivalentTo("1.0.0") ?: false)
    }

    @Test
    fun prettyString() {
        Assert.assertEquals(">=0.0.0", Requirement.buildNPM("latest").toString())
        Assert.assertEquals(">=0.0.0", Requirement.buildNPM("*").toString())
        Assert.assertEquals(">=1.0.0 <2.0.0", Requirement.buildNPM("1.*").toString())
        Assert.assertEquals(">=1.0.0 <2.0.0", Requirement.buildNPM("1.x").toString())
        Assert.assertEquals("=1.0.0", Requirement.buildNPM("1.0.0").toString())
        Assert.assertEquals("=1.0.0", Requirement.buildNPM("=1.0.0").toString())
        Assert.assertEquals("=1.0.0", Requirement.buildNPM("v1.0.0").toString())
        Assert.assertEquals("<1.0.0", Requirement.buildNPM("<1.0.0").toString())
        Assert.assertEquals("<=1.0.0", Requirement.buildNPM("<=1.0.0").toString())
        Assert.assertEquals(">1.0.0", Requirement.buildNPM(">1.0.0").toString())
        Assert.assertEquals(">=1.0.0", Requirement.buildNPM(">=1.0.0").toString())
        Assert.assertEquals(">=1.0.0 <1.1.0", Requirement.buildNPM("~1.0.0").toString())
        Assert.assertEquals(">=1.0.0 <2.0.0", Requirement.buildNPM("^1.0.0").toString())
        Assert.assertEquals(">=1.0.0 <2.0.0 || >=2.5.0 || >=5.0.0 <=7.2.3", Requirement.buildNPM("1.x || >=2.5.0 || 5.0.0 - 7.2.3").toString())
        Assert.assertEquals(">=1.2.0 <1.3.0", Requirement.buildCocoapods("~>1.2.0").toString())
        Assert.assertEquals(">=1.0.0 <=2.0.0", Requirement.buildIvy("[1.0,2.0]").toString())
        Assert.assertEquals(">=1.0.0 <2.0.0", Requirement.buildIvy("[1.0,2.0[").toString())
        Assert.assertEquals(">1.0.0 <=2.0.0", Requirement.buildIvy("]1.0,2.0]").toString())
        Assert.assertEquals(">1.0.0 <2.0.0", Requirement.buildIvy("]1.0,2.0[").toString())
        Assert.assertEquals(">=1.0.0", Requirement.buildIvy("[1.0,)").toString())
        Assert.assertEquals(">1.0.0", Requirement.buildIvy("]1.0,)").toString())
        Assert.assertEquals("<=2.0.0", Requirement.buildIvy("(,2.0]").toString())
        Assert.assertEquals("<2.0.0", Requirement.buildIvy("(,2.0[").toString())
    }

    @Test
    fun testEquals() {
        val requirement = Requirement.buildStrict("1.2.3")
        Assert.assertEquals(requirement, requirement)
        Assert.assertEquals(requirement, Requirement.buildStrict("1.2.3"))
        Assert.assertEquals(requirement, Requirement.buildLoose("1.2.3"))
        Assert.assertEquals(requirement, Requirement.buildNPM("=1.2.3"))
        Assert.assertEquals(requirement, Requirement.buildIvy("1.2.3"))
        Assert.assertEquals(requirement, Requirement.buildCocoapods("1.2.3"))
        Assert.assertNotEquals(requirement, null)
        Assert.assertNotEquals(requirement, "string")
        Assert.assertNotEquals(requirement, Requirement.buildStrict("1.2.4"))
        Assert.assertNotEquals(requirement, Requirement.buildNPM(">1.2.3"))
    }

    @Test
    fun testHashCode() {
        val requirement = Requirement.buildStrict("1.2.3")
        Assert.assertEquals(requirement.hashCode().toLong(), requirement.hashCode().toLong())
        Assert.assertEquals(requirement.hashCode().toLong(), Requirement.buildStrict("1.2.3").hashCode().toLong())
        Assert.assertEquals(requirement.hashCode().toLong(), Requirement.buildLoose("1.2.3").hashCode().toLong())
        Assert.assertEquals(requirement.hashCode().toLong(), Requirement.buildNPM("=1.2.3").hashCode().toLong())
        Assert.assertEquals(requirement.hashCode().toLong(), Requirement.buildIvy("1.2.3").hashCode().toLong())
        Assert.assertEquals(requirement.hashCode().toLong(), Requirement.buildCocoapods("1.2.3").hashCode().toLong())
        Assert.assertNotEquals(requirement.hashCode().toLong(), Requirement.buildStrict("1.2.4").hashCode().toLong())
        Assert.assertNotEquals(requirement.hashCode().toLong(), Requirement.buildNPM(">1.2.3").hashCode().toLong())
    }

    @Test
    fun testMinVersion() {
        // Stars
        Assert.assertEquals(Semver("0.0.0"), Requirement.buildNPM("*").minVersion())
        Assert.assertEquals(Semver("0.0.0"), Requirement.buildNPM("* || >=2").minVersion())
        Assert.assertEquals(Semver("0.0.0"), Requirement.buildNPM(">=2 || *").minVersion())
        Assert.assertEquals(Semver("0.0.0"), Requirement.buildNPM(">2 || *").minVersion())

        // equal
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("1.0.0").minVersion())
        Assert.assertEquals(Semver("1.0", SemverType.LOOSE), Requirement.buildLoose("1.0").minVersion())
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("1.0.x").minVersion())
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("1.0.*").minVersion())
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("1").minVersion())
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("1.x.x").minVersion())
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("1.x.x").minVersion())
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("1.*.x").minVersion())
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("1.x.*").minVersion())
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("1.x").minVersion())
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("1.*").minVersion())
        Assert.assertEquals(Semver("1.0.0"), Requirement.buildNPM("=1.0.0").minVersion())

        // Tilde
        Assert.assertEquals(Semver("1.1.1"), Requirement.buildNPM("~1.1.1").minVersion())
        Assert.assertEquals(Semver("1.1.1-beta"), Requirement.buildNPM("~1.1.1-beta").minVersion())
        Assert.assertEquals(Semver("1.1.1"), Requirement.buildNPM("~1.1.1 || >=2").minVersion())

        // Caret
        Assert.assertEquals(Semver("1.1.1"), Requirement.buildNPM("^1.1.1").minVersion())
        Assert.assertEquals(Semver("1.1.1-beta"), Requirement.buildNPM("^1.1.1-beta").minVersion())
        Assert.assertEquals(Semver("1.1.1"), Requirement.buildNPM("^1.1.1 || >=2").minVersion())
        Assert.assertEquals(Semver("2.16.2"), Requirement.buildNPM("^2.16.2 ^2.16").minVersion())

        // '-' operator
        Assert.assertEquals(Semver("1.1.1"), Requirement.buildNPM("1.1.1 - 1.8.0").minVersion(), )
        Assert.assertEquals(Semver("1.1.0"), Requirement.buildNPM("1.1 - 1.8.0").minVersion())

        // Less / less or equal
        Assert.assertEquals(Semver("0.0.0"), Requirement.buildNPM("<2").minVersion(), )
        Assert.assertEquals(Semver("0.0.0-0"), Requirement.buildNPM("<0.0.0-beta").minVersion())
        Assert.assertEquals(Semver("0.0.0"), Requirement.buildNPM("<0.0.1-beta").minVersion())
        Assert.assertEquals(Semver("0.0.0"), Requirement.buildNPM("<2 || >4").minVersion())
        Assert.assertEquals(Semver("0.0.0"), Requirement.buildNPM(">4 || <2").minVersion())
        Assert.assertEquals(Semver("0.0.0"), Requirement.buildNPM("<=2 || >=4").minVersion())
        Assert.assertEquals(Semver("0.0.0"), Requirement.buildNPM(">=4 || <=2").minVersion())
        Assert.assertEquals(Semver("0.0.0-alpha.0"), Requirement.buildNPM("<0.0.0-beta >0.0.0-alpha").minVersion())
        Assert.assertEquals(Semver("0.0.0-alpha.0"), Requirement.buildNPM(">0.0.0-alpha <0.0.0-beta").minVersion())

        // Greater than or equal
        Assert.assertEquals(Semver("1.1.1"), Requirement.buildNPM(">=1.1.1 <2 || >=2.2.2 <2").minVersion())
        Assert.assertEquals(Semver("1.1.1"), Requirement.buildNPM(">=2.2.2 <2 || >=1.1.1 <2").minVersion())

        // Greater than but not equal
        Assert.assertEquals(Semver("1.0.1"), Requirement.buildNPM(">1.0.0").minVersion())
        Assert.assertEquals(Semver("1.0.0-0.0"), Requirement.buildNPM(">1.0.0-0").minVersion())
        Assert.assertEquals(Semver("1.0.0-beta.0"), Requirement.buildNPM(">1.0.0-beta").minVersion())
        Assert.assertEquals(Semver("1.0.1"), Requirement.buildNPM(">2 || >1.0.0").minVersion())
        Assert.assertEquals(Semver("1.0.0-0.0"), Requirement.buildNPM(">2 || >1.0.0-0").minVersion())
        Assert.assertEquals(Semver("1.0.0-beta.0"), Requirement.buildNPM(">2 || >1.0.0-beta").minVersion())
        Assert.assertEquals(Semver("3", SemverType.LOOSE), Requirement.buildNPM(">2").minVersion())
    }

    companion object {
        private fun assertIsRange(requirement: Requirement, version: String, operator: RangeOperator) {
            Assert.assertNull(requirement.req1)
            Assert.assertNull(requirement.op)
            Assert.assertNull(requirement.req2)
            val range = requirement.range
            Assert.assertTrue(range?.version?.isEquivalentTo(version) ?: false)
            Assert.assertEquals(operator, range?.op)
        }

        private fun tildeTest(requirement: String, lower: String, upper: String, type: SemverType) {
            val req = Requirement.tildeRequirement(requirement, type)
            rangeTest(req, lower, upper, true)
        }

        private fun caretTest(requirement: String, lower: String, upper: String) {
            val req = Requirement.caretRequirement(requirement, SemverType.NPM)
            rangeTest(req, lower, upper, true)
        }

        private fun hyphenTest(reqLower: String, reqUpper: String, lower: String, upper: String, upperStrict: Boolean) {
            val req = Requirement.hyphenRequirement(reqLower, reqUpper, SemverType.NPM)
            rangeTest(req, lower, upper, upperStrict)
        }

        private fun rangeTest(req: Requirement, lower: String, upper: String, upperStrict: Boolean) {
            rangeTest(req, lower, false, upper, upperStrict)
        }

        private fun rangeTest(req: Requirement, lower: String, lowerStrict: Boolean, upper: String, upperStrict: Boolean) {
            Assert.assertNull(req.range)
            Assert.assertEquals(Requirement.RequirementOperator.AND, req.op)
            val req1 = req.req1
            val lowOp = if (lowerStrict) RangeOperator.GT else RangeOperator.GTE
            Assert.assertEquals(lowOp, req1?.range?.op)
            Assert.assertEquals(lower, req1?.range?.version?.value)
            val req2 = req.req2
            val upOp = if (upperStrict) RangeOperator.LT else RangeOperator.LTE
            Assert.assertEquals(upOp, req2?.range?.op)
            Assert.assertEquals(upper, req2?.range?.version?.value)
        }
    }
}