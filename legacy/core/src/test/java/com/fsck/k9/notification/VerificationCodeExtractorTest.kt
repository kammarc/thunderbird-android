package com.fsck.k9.notification

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test

class VerificationCodeExtractorTest {
    private val testSubject = VerificationCodeExtractor

    @Test
    fun `extract should return numeric code following a keyword`() {
        // Arrange
        val text = "Your verification code is 482913"

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isEqualTo("482913")
    }

    @Test
    fun `extract should return numeric code preceding a keyword`() {
        // Arrange
        val text = "482913 is your login code."

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isEqualTo("482913")
    }

    @Test
    fun `extract should return code when keyword is part of a compound word`() {
        // Arrange
        val text = "Ihr Bestätigungscode\nBitte geben Sie 739201 ein."

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isEqualTo("739201")
    }

    @Test
    fun `extract should return alphanumeric code`() {
        // Arrange
        val text = "Sign-up code: A1B2C3"

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isEqualTo("A1B2C3")
    }

    @Test
    fun `extract should return alphanumeric code containing a hyphen`() {
        // Arrange
        val text = "Use the one-time code K7P-2QX to sign in."

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isEqualTo("K7P-2QX")
    }

    @Test
    fun `extract should return code closest to a keyword`() {
        // Arrange
        val text = "Order 99887766 confirmed. Your security code is 123456"

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isEqualTo("123456")
    }

    @Test
    fun `extract should return null without keyword`() {
        // Arrange
        val text = "Your order 123456 has shipped"

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `extract should return null when number is far away from keyword`() {
        // Arrange
        val text = "Thanks for verifying your email address. We will ship your order soon. Order number 123456"

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `extract should return null for upper case words without digits`() {
        // Arrange
        val text = "ENTER THE CODE HERE"

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `extract should ignore dates, decimal numbers, URLs and email addresses`() {
        // Arrange
        val text = "Code valid until 2026-11-16 for 12.50 at https://example.com/v/AB12CD or AB12CD@example.com"

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `extract should ignore numbers that are too short or too long`() {
        // Arrange
        val text = "Your code: 123 or 12345678901"

        // Act
        val result = testSubject.extract(text)

        // Assert
        assertThat(result).isNull()
    }
}
