package com.example

import com.example.domain.TaskExecutor
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testExpressionEvaluation() {
        // Test basic arithmetic and percentage evaluation
        val clean = "15% of 8000"
        val pctMatch = Regex("([\\d.]+)\\s*%").find(clean)
        val totalMatch = Regex("(?:of|پر|کا)\\s*([\\d.]+)").find(clean)
        assertNotNull(pctMatch)
        assertNotNull(totalMatch)
        val pct = pctMatch!!.groupValues[1].toDouble()
        val total = totalMatch!!.groupValues[1].toDouble()
        val res = (pct / 100.0) * total
        assertEquals(1200.0, res, 0.01)
    }

    @Test
    fun testUrduCharacterDetection() {
        val urduText = "لاہور میں موسم کیسا ہے؟"
        val hasUrdu = urduText.any { it in '\u0600'..'\u06FF' }
        assertTrue(hasUrdu)

        val englishText = "Weather in London"
        val englishHasUrdu = englishText.any { it in '\u0600'..'\u06FF' }
        assertFalse(englishHasUrdu)
    }
}
