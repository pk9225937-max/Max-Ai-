package com.example

import com.example.ai.ToolDefinitions
import com.example.domain.model.RiskLevel
import com.example.security.ConfirmationManager
import com.example.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AssistantUnitTests {

    private val confirmationManager = ConfirmationManager()

    @Test
    fun `risk classification properly categorizes actions`() {
        assertEquals(
            RiskLevel.HIGH,
            confirmationManager.evaluateRisk("searchAndCallContact", mapOf("contactName" to "John"))
        )
        assertEquals(
            RiskLevel.HIGH,
            confirmationManager.evaluateRisk("sendSMS", mapOf("contactName" to "John", "message" to "Hi"))
        )
        assertEquals(
            RiskLevel.HIGH,
            confirmationManager.evaluateRisk("sendWhatsAppMessage", mapOf("contactName" to "John", "message" to "Hi"))
        )
        assertEquals(
            RiskLevel.HIGH,
            confirmationManager.evaluateRisk("clearAllMemory", emptyMap())
        )

        assertEquals(
            RiskLevel.MEDIUM,
            confirmationManager.evaluateRisk("createReminder", mapOf("title" to "Buy milk"))
        )
        assertEquals(
            RiskLevel.MEDIUM,
            confirmationManager.evaluateRisk("createCalendarEvent", mapOf("title" to "Meeting"))
        )

        assertEquals(
            RiskLevel.LOW,
            confirmationManager.evaluateRisk("openApp", mapOf("appName" to "YouTube"))
        )
        assertEquals(
            RiskLevel.LOW,
            confirmationManager.evaluateRisk("getDeviceStatus", emptyMap())
        )
        assertEquals(
            RiskLevel.LOW,
            confirmationManager.evaluateRisk("flashlight", mapOf("enabled" to true))
        )
    }

    @Test
    fun `confirmation engine recognizes affirmative and negative tokens strictly`() {
        assertTrue(confirmationManager.isAffirmativeConfirmation("haan"))
        assertTrue(confirmationManager.isAffirmativeConfirmation("yes"))
        assertTrue(confirmationManager.isAffirmativeConfirmation("kar do"))
        assertTrue(confirmationManager.isAffirmativeConfirmation("confirm"))

        assertTrue(confirmationManager.isNegativeConfirmation("nahi"))
        assertTrue(confirmationManager.isNegativeConfirmation("no"))
        assertTrue(confirmationManager.isNegativeConfirmation("mat karo"))
        assertTrue(confirmationManager.isNegativeConfirmation("cancel"))

        assertFalse(confirmationManager.isAffirmativeConfirmation("maybe later"))
        assertFalse(confirmationManager.isAffirmativeConfirmation("kya karu"))
    }

    @Test
    fun `security manager blocks sensitive information in memory`() {
        assertTrue(SecurityManager.containsSensitiveData("My password is secret123"))
        assertTrue(SecurityManager.containsSensitiveData("OTP 482910 received"))
        assertTrue(SecurityManager.containsSensitiveData("My credit card pin is 1234"))
        assertTrue(SecurityManager.containsSensitiveData("Save api_key AIzaSyABC123"))

        assertFalse(SecurityManager.containsSensitiveData("My mother's birthday is June 5th"))
        assertFalse(SecurityManager.containsSensitiveData("Meeting with boss at 4 PM"))
    }

    @Test
    fun `security manager blocks dangerous command executions`() {
        assertTrue(SecurityManager.isDangerousExecution("openApp", mapOf("cmd" to "rm -rf /data")))
        assertTrue(SecurityManager.isDangerousExecution("openApp", mapOf("arg" to "su root")))
        assertTrue(SecurityManager.isDangerousExecution("openApp", mapOf("cmd" to "sh reboot")))

        assertFalse(SecurityManager.isDangerousExecution("openApp", mapOf("appName" to "YouTube")))
        assertFalse(SecurityManager.isDangerousExecution("webSearch", mapOf("query" to "best pizza nearby")))
    }

    @Test
    fun `personality prompt contains MAX directives and conversational style`() {
        val prompt = ToolDefinitions.SYSTEM_INSTRUCTION
        assertTrue(prompt.contains("MAX"))
        assertTrue(prompt.contains("Hindi/Hinglish"))
        assertTrue(prompt.contains("witty"))
        assertTrue(prompt.contains("confident"))
    }

    @Test
    fun `tool declarations provide all expected tools`() {
        val tools = ToolDefinitions.getGeminiToolDeclarations()
        val declaredNames = mutableSetOf<String>()
        for (i in 0 until tools.length()) {
            val obj = tools.getJSONObject(i)
            declaredNames.add(obj.getString("name"))
        }

        assertTrue(declaredNames.contains("openApp"))
        assertTrue(declaredNames.contains("searchAndCallContact"))
        assertTrue(declaredNames.contains("sendWhatsAppMessage"))
        assertTrue(declaredNames.contains("sendSMS"))
        assertTrue(declaredNames.contains("sendGmail"))
        assertTrue(declaredNames.contains("createReminder"))
        assertTrue(declaredNames.contains("setAlarm"))
        assertTrue(declaredNames.contains("getDeviceStatus"))
        assertTrue(declaredNames.contains("flashlight"))
        assertTrue(declaredNames.contains("mediaControl"))
        assertTrue(declaredNames.contains("rememberInfo"))
        assertTrue(declaredNames.contains("queryMemory"))
    }
}
