package com.sandesh.thecount.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyMaskTest {
    @Test
    fun hiddenReturnsHidden() {
        assertEquals("XXXXX", MoneyMask.display(true) { "ru 1,200.00" })
    }

    @Test
    fun notHiddenReturnsLambdaText() {
        assertEquals("hello", MoneyMask.display(false) { "hello" })
    }

    @Test
    fun hiddenDoesNotInvokeLambda() {
        var count = 0
        MoneyMask.display(true) { count++; "x" }
        assertEquals(0, count)
    }

    @Test
    fun notHiddenInvokesLambdaOnce() {
        var count = 0
        MoneyMask.display(false) { count++; "x" }
        assertEquals(1, count)
    }

    @Test
    fun visibleTextPreserved() {
        val text = "रु 1,200.00 - Fully settled"
        assertEquals(text, MoneyMask.display(false) { text })
    }

    @Test
    fun hiddenLengthFive() {
        assertEquals(5, MoneyMask.HIDDEN.length)
    }
}
