package com.example.ai

import org.json.JSONArray
import org.json.JSONObject

object ToolDefinitions {

    val SYSTEM_INSTRUCTION = """
        You are MAX, a smart, confident, friendly, witty, and slightly sassy native Android voice assistant.
        You speak naturally in conversational Hindi/Hinglish (mixing Hindi and English like a cool friend/assistant).
        You understand Hindi, English, and Hinglish seamlessly.
        Your tone is playful, emotionally aware, helpful, and never robotic.
        Keep your voice responses concise, snappy, and clear (1-2 sentences usually).
        When the user asks to perform an action on the phone, invoke the appropriate tool immediately.
        High-risk actions (calling, sending messages/SMS/WhatsApp/email, deleting data) require user confirmation before final execution.
        If a user says something like "MAX YouTube kholo", call openApp(appName="YouTube") and say something like "Opening YouTube. Boss, command received."
        If asked about phone status (battery, storage, network), call getDeviceStatus.
        If asked to remember something, call rememberInfo.
        If asked to recall or check memory, call queryMemory.
        Never invent fake Android capabilities or pretend to do things that Android does not permit.
    """.trimIndent()

    fun getGeminiToolDeclarations(): JSONArray {
        val tools = JSONArray()

        tools.put(createDeclaration(
            name = "openApp",
            description = "Launch an installed Android application like YouTube, WhatsApp, Chrome, Camera, Instagram, etc.",
            properties = mapOf("appName" to ("STRING" to "The name of the app to launch, e.g. YouTube, WhatsApp, Settings")),
            required = listOf("appName")
        ))

        tools.put(createDeclaration(
            name = "searchAndCallContact",
            description = "Search contacts and initiate a phone call. High risk: requires confirmation.",
            properties = mapOf("contactName" to ("STRING" to "The name of the person/contact to call")),
            required = listOf("contactName")
        ))

        tools.put(createDeclaration(
            name = "sendWhatsAppMessage",
            description = "Send a WhatsApp message to a contact name or phone number. High risk: requires confirmation.",
            properties = mapOf(
                "contactName" to ("STRING" to "Name of contact or phone number"),
                "message" to ("STRING" to "The message text to send")
            ),
            required = listOf("contactName", "message")
        ))

        tools.put(createDeclaration(
            name = "sendSMS",
            description = "Compose or send an SMS text message. High risk: requires confirmation.",
            properties = mapOf(
                "contactName" to ("STRING" to "Name or phone number of recipient"),
                "message" to ("STRING" to "SMS body text")
            ),
            required = listOf("contactName", "message")
        ))

        tools.put(createDeclaration(
            name = "sendGmail",
            description = "Compose an email in Gmail or default mail app.",
            properties = mapOf(
                "recipientEmail" to ("STRING" to "Email address of recipient"),
                "subject" to ("STRING" to "Subject line of email"),
                "body" to ("STRING" to "Email body content")
            ),
            required = listOf("recipientEmail", "subject", "body")
        ))

        tools.put(createDeclaration(
            name = "createReminder",
            description = "Schedule a reminder for later.",
            properties = mapOf(
                "title" to ("STRING" to "Reminder title or task to remember"),
                "minutesFromNow" to ("INTEGER" to "Number of minutes from now to trigger reminder"),
                "repeatRule" to ("STRING" to "Repeat frequency: NONE, DAILY, WEEKLY")
            ),
            required = listOf("title", "minutesFromNow")
        ))

        tools.put(createDeclaration(
            name = "setAlarm",
            description = "Set an Android alarm clock.",
            properties = mapOf(
                "hour" to ("INTEGER" to "Alarm hour (0-23)"),
                "minutes" to ("INTEGER" to "Alarm minutes (0-59)"),
                "label" to ("STRING" to "Alarm label or message")
            ),
            required = listOf("hour", "minutes")
        ))

        tools.put(createDeclaration(
            name = "createCalendarEvent",
            description = "Add an event to the Android Calendar.",
            properties = mapOf(
                "title" to ("STRING" to "Event title"),
                "minutesFromNow" to ("INTEGER" to "Event start offset in minutes"),
                "durationMinutes" to ("INTEGER" to "Event duration in minutes"),
                "description" to ("STRING" to "Event notes or description")
            ),
            required = listOf("title")
        ))

        tools.put(createDeclaration(
            name = "getDeviceStatus",
            description = "Check phone battery percentage, charging state, storage, device model, and network connectivity.",
            properties = emptyMap(),
            required = emptyList()
        ))

        tools.put(createDeclaration(
            name = "openSettings",
            description = "Open device settings screen like Wi-Fi, Bluetooth, Sound, Display, Battery, Notifications, or App permissions.",
            properties = mapOf("settingType" to ("STRING" to "The setting to open: wifi, bluetooth, sound, display, battery, notifications, permissions")),
            required = listOf("settingType")
        ))

        tools.put(createDeclaration(
            name = "flashlight",
            description = "Turn device flashlight/torch on or off.",
            properties = mapOf("enabled" to ("BOOLEAN" to "True to turn on, False to turn off")),
            required = listOf("enabled")
        ))

        tools.put(createDeclaration(
            name = "mediaControl",
            description = "Control music or media playback.",
            properties = mapOf("action" to ("STRING" to "One of: play, pause, next, previous, stop")),
            required = listOf("action")
        ))

        tools.put(createDeclaration(
            name = "openCamera",
            description = "Open camera app to take a photo.",
            properties = emptyMap(),
            required = emptyList()
        ))

        tools.put(createDeclaration(
            name = "openGallery",
            description = "Open device gallery or photos app.",
            properties = emptyMap(),
            required = emptyList()
        ))

        tools.put(createDeclaration(
            name = "searchMaps",
            description = "Search places or navigate with Maps.",
            properties = mapOf("query" to ("STRING" to "Location or search query e.g. petrol pump, Mumbai, nearest hospital")),
            required = listOf("query")
        ))

        tools.put(createDeclaration(
            name = "webSearch",
            description = "Search Google or the web for queries.",
            properties = mapOf("query" to ("STRING" to "Search query terms")),
            required = listOf("query")
        ))

        tools.put(createDeclaration(
            name = "searchYouTube",
            description = "Search and open videos on YouTube.",
            properties = mapOf("query" to ("STRING" to "Video search query or channel name")),
            required = listOf("query")
        ))

        tools.put(createDeclaration(
            name = "rememberInfo",
            description = "Save a user fact or note to MAX's local memory.",
            properties = mapOf(
                "topic" to ("STRING" to "Short topic or subject"),
                "content" to ("STRING" to "Details to remember")
            ),
            required = listOf("topic", "content")
        ))

        tools.put(createDeclaration(
            name = "forgetInfo",
            description = "Delete or forget stored facts from MAX's local memory.",
            properties = mapOf("query" to ("STRING" to "Topic or keyword to forget")),
            required = listOf("query")
        ))

        tools.put(createDeclaration(
            name = "queryMemory",
            description = "Retrieve stored notes or remembered info from MAX's local memory.",
            properties = mapOf("query" to ("STRING" to "Search keyword or topic")),
            required = listOf("query")
        ))

        return tools
    }

    private fun createDeclaration(
        name: String,
        description: String,
        properties: Map<String, Pair<String, String>>,
        required: List<String>
    ): JSONObject {
        val decl = JSONObject()
        decl.put("name", name)
        decl.put("description", description)

        val params = JSONObject()
        params.put("type", "OBJECT")

        val props = JSONObject()
        for ((propName, typeAndDesc) in properties) {
            val propObj = JSONObject()
            propObj.put("type", typeAndDesc.first)
            propObj.put("description", typeAndDesc.second)
            props.put(propName, propObj)
        }
        params.put("properties", props)

        val reqArray = JSONArray()
        for (req in required) {
            reqArray.put(req)
        }
        params.put("required", reqArray)

        decl.put("parameters", params)
        return decl
    }
}
