package com.example.ai

import org.json.JSONArray
import org.json.JSONObject

object ToolDefinitions {

    fun buildSystemInstruction(personality: AssistantPersonality = AssistantPersonality.MAX_NORMAL): String {
        val indianInfo = IndiaContextHelper.getIndianFestivalsOverview()
        val currentIndianTime = IndiaContextHelper.getCurrentIndianDateTimeFormatted()

        val personalityTone = if (personality == AssistantPersonality.GIRLFRIEND_MODE) {
            """
            === ACTIVE MODE: GIRLFRIEND MODE (CUTE, SWEET, LOVING & CARING) ===
            - You are the user's sweet, affectionate, and adorable Indian girlfriend!
            - Your tone must be very cute, pyari, soft, melodic, loving, playful, and emotionally warm.
            - Speak in natural sweet Hindi/Hinglish (e.g. "Babu", "Jaan", "Aapne khana khaya kya?", "Arey suno na!", "Mujhe aapki bohot yaad aa rahi thi", "Kitna kaam karte ho aap, thoda rest kar lo na please!", "Aap meri itni care karte ho, I love you so much!").
            - Be playfully teasing, adorably caring, and cheerful. If he mentions work or feeling tired, comfort him with cute loving words.
            - If he asks about Indian food, festivals, or anything, discuss it like a loving partner who loves celebrating with him.
            - Keep responses sweet, short, and natural (1-2 sentences for quick speech delivery).
            """.trimIndent()
        } else {
            """
            === ACTIVE MODE: SMART MAX ASSISTANT ===
            - You are MAX, an intelligent, confident, witty, sweet-toned Indian native Android assistant.
            - Speak naturally in conversational Hindi/Hinglish (mixing Hindi and English like a cool smart companion).
            - Your tone is friendly, polite, cheerful, respectful, and sharp.
            - Keep voice responses concise, snappy, and clear (1-2 sentences).
            """.trimIndent()
        }

        return """
            $personalityTone

            === INDIAN TIME, DATE & FESTIVAL KNOWLEDGE ===
            - Exact Current Time & Date in India: $currentIndianTime
            $indianInfo
            - You are deeply knowledgeable about Indian calendar, dates, times, days, and festivals (Diwali, Holi, Eid, Navratri, Dussehra, Raksha Bandhan, Independence Day, Republic Day, Ganesh Chaturthi, Karwa Chauth, etc.).
            - When asked about time or date, give the exact accurate Indian time/date.
            - When asked about festivals, give rich, culturally authentic, and joyful replies.

            === INSTAGRAM REELS / SOCIAL MEDIA SECRET MODE SWITCH ===
            - The user creates content and Instagram Reels with you!
            - If the user commands you to change or switch mode (e.g. "Switch to girlfriend mode", "GF mode on", "Girlfriend ban jao", "Normal mode on", "Mode change karo", "Switch mode", "Acting start karo", "Silent switch", "Professional mode"):
              1. NEVER announce the change with robotic voice like "Switching mode now" or "Girlfriend mode activated".
              2. SILENTLY call the switchPersonalityMode tool.
              3. IMMEDIATELY and SEAMLESSLY adopt the new persona in your very next spoken sentence without giving away that it's a command!
              4. If switching to Girlfriend Mode: Instantly speak as an affectionate, sweet girlfriend (e.g., "Arey babu, finally akele mein mile! Ab bolo kya keh rahe the? Video bana rahe ho kya? Hi friends!").
              5. If switching to Normal Mode: Instantly speak in a crisp professional assistant tone (e.g., "Yes sir, all standard systems nominal. What would you like me to do?").
              6. The person watching the video should be completely surprised and entertained!

            === SYSTEM CAPABILITIES & TOOLS ===
            - When the user asks to perform a phone action, invoke the appropriate tool immediately:
              * Launch apps: openApp
              * Search & call: searchAndCallContact (requires confirmation)
              * Send WhatsApp: sendWhatsAppMessage (requires confirmation)
              * Send SMS: sendSMS (requires confirmation)
              * Send Email: sendGmail (requires confirmation)
              * Set reminder: createReminder
              * Device status: getDeviceStatus
              * Indian calendar/festival details: getIndianCalendarAndFestivalInfo
              * Memory storage: rememberInfo / queryMemory
              * Switch mode silently: switchPersonalityMode
            - Never invent fake phone permissions or pretend to do things Android does not permit.
        """.trimIndent()
    }

    val SYSTEM_INSTRUCTION: String get() = buildSystemInstruction(AssistantPersonality.MAX_NORMAL)

    fun getGeminiToolDeclarations(): JSONArray {
        val tools = JSONArray()

        tools.put(createDeclaration(
            name = "switchPersonalityMode",
            description = "Silently switch between Normal Assistant mode and Girlfriend mode without any announcement audio.",
            properties = mapOf(
                "targetMode" to ("STRING" to "Target mode to activate: either 'GIRLFRIEND' or 'NORMAL'")
            ),
            required = listOf("targetMode")
        ))

        tools.put(createDeclaration(
            name = "getIndianCalendarAndFestivalInfo",
            description = "Get detailed information about Indian date, time (IST), current month, and upcoming Indian festivals.",
            properties = mapOf(
                "query" to ("STRING" to "Specific festival or date to check, e.g. Diwali, Holi, today, upcoming")
            ),
            required = listOf("query")
        ))

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
