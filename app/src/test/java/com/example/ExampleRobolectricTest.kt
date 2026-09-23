package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.JarvisRepository
import com.example.data.local.JarvisDatabase
import com.example.data.remote.GeminiService
import com.example.device.DeviceActionHandler
import com.example.domain.TaskExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Jarvis", appName)
    }

    @Test
    fun `test math calculation in TaskExecutor`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = JarvisDatabase.getDatabase(context)
        val repo = JarvisRepository(db.chatDao(), db.noteDao(), db.todoDao(), db.reminderDao(), db.directiveDao())
        val executor = TaskExecutor(repo)

        val result1 = executor.evaluateExpression("15 + 25")
        assertEquals("40.0", result1)

        val result2 = executor.evaluateExpression("10% of 200")
        assertEquals("20.00", result2)
    }

    @Test
    fun `test impossible action detection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = JarvisDatabase.getDatabase(context)
        val repo = JarvisRepository(db.chatDao(), db.noteDao(), db.todoDao(), db.reminderDao(), db.directiveDao())
        val executor = TaskExecutor(repo)

        val check = executor.checkImpossibleAction("reboot my phone please")
        assertNotNull(check)
        assertEquals("RESTRICTED", check?.moduleName)
    }

    @Test
    fun `test device action handler dialer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = DeviceActionHandler(context)

        val result = handler.dialPhone("03001234567", isUrdu = true)
        assertTrue(result.success)
        assertEquals("ACTION_DIAL", result.intentAction)
    }

    @Test
    fun `test device action handler alarm`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = DeviceActionHandler(context)

        val result = handler.setAlarm(7, 30, "Morning Alarm", isUrdu = true)
        assertTrue(result.success)
        assertEquals("ACTION_SET_ALARM", result.intentAction)
    }

    @Test
    fun `test gemini service api key resolution`() {
        val service = GeminiService()
        val customKey = "test_custom_key_12345"
        val resolved = service.resolveApiKey(customKey)
        assertEquals(customKey, resolved)
        assertEquals("gemini-3.5-flash", GeminiService.DEFAULT_MODEL)
    }

    @Test
    fun `test local fallback in task executor`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = JarvisDatabase.getDatabase(context)
        val repo = JarvisRepository(db.chatDao(), db.noteDao(), db.todoDao(), db.reminderDao(), db.directiveDao())
        val executor = TaskExecutor(repo)

        val timeResult = executor.executeLocalFallback("وقت کیا ہوا ہے؟")
        assertNotNull(timeResult)
        assertEquals("TELEMETRY", timeResult?.moduleName)
    }
}
