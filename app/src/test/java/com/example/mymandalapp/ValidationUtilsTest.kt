package com.example.mymandalapp

import com.example.mymandalapp.core.utils.ValidationUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun `test mandal id validation`() {
        assertTrue(ValidationUtils.isValidMandalId("BGMV2026"))
        assertFalse(ValidationUtils.isValidMandalId(""))
        assertFalse(ValidationUtils.isValidMandalId("ABC")) // Too short
        assertFalse(ValidationUtils.isValidMandalId("ABC 123")) // No spaces
    }

    @Test
    fun `test mobile validation`() {
        assertTrue(ValidationUtils.isValidMobile("9876543210"))
        assertTrue(ValidationUtils.isValidMobile("")) // Optional is valid if blank
        assertFalse(ValidationUtils.isValidMobile("12345")) // Too short
        assertFalse(ValidationUtils.isValidMobile("98765432101")) // Too long
        assertFalse(ValidationUtils.isValidMobile("98765abc10")) // Not numeric
    }

    @Test
    fun `test amount validation`() {
        assertTrue(ValidationUtils.isValidAmount("100"))
        assertTrue(ValidationUtils.isValidAmount("1250.50"))
        assertFalse(ValidationUtils.isValidAmount("0"))
        assertFalse(ValidationUtils.isValidAmount("-100"))
        assertFalse(ValidationUtils.isValidAmount("abc"))
    }
}
