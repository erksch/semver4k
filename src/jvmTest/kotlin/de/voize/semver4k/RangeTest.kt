package de.voize.semver4k

import de.voize.semver4k.Range.RangeOperator
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RangeTest {
    // SAME VERSION

    // GREATER
    // major
    // minor
    // patch

    // LOWER
    // major
    // minor
    // patch
    @Test
    fun isSatisfiedBy_EQ() // null suffix // non null suffix
    {
        val range = Range("1.2.3", RangeOperator.EQ)

        // SAME VERSION
        Assert.assertTrue(range.isSatisfiedBy("1.2.3"))

        // GREATER
        Assert.assertFalse(range.isSatisfiedBy("2.2.3")) // major
        Assert.assertFalse(range.isSatisfiedBy("1.3.3")) // minor
        Assert.assertFalse(range.isSatisfiedBy("1.2.4")) // patch

        // LOWER
        Assert.assertFalse(range.isSatisfiedBy("0.2.3")) // major
        Assert.assertFalse(range.isSatisfiedBy("1.1.3")) // minor
        Assert.assertFalse(range.isSatisfiedBy("1.2.2")) // patch
        val rangeWithSuffix = Range("1.2.3-alpha", RangeOperator.EQ)
        Assert.assertFalse(rangeWithSuffix.isSatisfiedBy("1.2.3")) // null suffix
        Assert.assertFalse(rangeWithSuffix.isSatisfiedBy("1.2.3-beta")) // non null suffix
    }

    @Test
    fun isSatisfiedBy_LT() {
        val range = Range("1.2.3", RangeOperator.LT)
        Assert.assertFalse(range.isSatisfiedBy("1.2.3"))
        Assert.assertFalse(range.isSatisfiedBy("1.2.4"))
        Assert.assertTrue(range.isSatisfiedBy("1.2.2"))
    }

    @Test
    fun isSatisfiedBy_LTE() {
        val range = Range("1.2.3", RangeOperator.LTE)
        Assert.assertTrue(range.isSatisfiedBy("1.2.3"))
        Assert.assertFalse(range.isSatisfiedBy("1.2.4"))
        Assert.assertTrue(range.isSatisfiedBy("1.2.2"))
    }

    @Test
    fun isSatisfiedBy_GT() {
        val range = Range("1.2.3", RangeOperator.GT)
        Assert.assertFalse(range.isSatisfiedBy("1.2.3"))
        Assert.assertFalse(range.isSatisfiedBy("1.2.2"))
        Assert.assertTrue(range.isSatisfiedBy("1.2.4"))
    }

    @Test
    fun isSatisfiedBy_GTE() {
        val range = Range("1.2.3", RangeOperator.GTE)
        Assert.assertTrue(range.isSatisfiedBy("1.2.3"))
        Assert.assertFalse(range.isSatisfiedBy("1.2.2"))
        Assert.assertTrue(range.isSatisfiedBy("1.2.4"))
    }

    @Test
    fun prettyString() {
        Assert.assertEquals("=1.2.3", Range("1.2.3", RangeOperator.EQ).toString())
        Assert.assertEquals("<1.2.3", Range("1.2.3", RangeOperator.LT).toString())
        Assert.assertEquals("<=1.2.3", Range("1.2.3", RangeOperator.LTE).toString())
        Assert.assertEquals(">1.2.3", Range("1.2.3", RangeOperator.GT).toString())
        Assert.assertEquals(">=1.2.3", Range("1.2.3", RangeOperator.GTE).toString())
    }

    @Test
    fun testEquals() {
        val range = Range("1.2.3", RangeOperator.EQ)
        Assert.assertEquals(range, range)
        Assert.assertNotEquals(range, null)
        Assert.assertNotEquals(range, "string")
        Assert.assertNotEquals(range, Range("1.2.3", RangeOperator.GTE))
        Assert.assertNotEquals(range, Range("1.2.4", RangeOperator.EQ))
    }

    @Test
    fun testHashCode() {
        val range = Range("1.2.3", RangeOperator.EQ)
        Assert.assertEquals(range.hashCode().toLong(), range.hashCode().toLong())
        Assert.assertNotEquals(range.hashCode().toLong(), Range("1.2.3", RangeOperator.GTE).hashCode().toLong())
        Assert.assertNotEquals(range.hashCode().toLong(), Range("1.2.4", RangeOperator.EQ).hashCode().toLong())
    }
}