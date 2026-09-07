package com.example.ai

import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.data.repository.ContactRepository
import com.example.data.repository.DeviceRepository
import com.example.domain.model.ConfirmationRequest
import com.example.domain.model.RiskLevel
import com.example.domain.model.ToolCall
import com.example.domain.model.ToolResult
import com.example.memory.MemoryManager
import com.example.permissions.PermissionManager
import com.example.reminders.ReminderManager
import com.example.security.ConfirmationManager
import com.example.security.SecurityManager

class ToolExecutionEngine(
    private val deviceRepository: DeviceRepository,
    private val contactRepository: ContactRepository,
    private val reminderManager: ReminderManager,
    private val memoryManager: MemoryManager,
    private val permissionManager: PermissionManager,
    private val confirmationManager: ConfirmationManager,
    private val database: AppDatabase
) {

    suspend fun executeTool(toolCall: ToolCall, userAlreadyConfirmed: Boolean = false): ToolResult {
        val name = toolCall.name
        val params = toolCall.arguments

        // 1. Schema / Malicious command validation
        if (SecurityManager.isDangerousExecution(name, params)) {
            logAudit(name, "Rejected dangerous execution attempt", "HIGH", "BLOCKED")
            return ToolResult(toolCall.callId, name, false, "Security violation: disallowed execution attempted.")
        }

        // 2. Risk Classification & Confirmation Gate
        val risk = confirmationManager.evaluateRisk(name, params)
        if (risk == RiskLevel.HIGH && !userAlreadyConfirmed) {
            val title = when (name) {
                "searchAndCallContact" -> "Call Contact"
                "sendSMS" -> "Send SMS"
                "sendWhatsAppMessage" -> "Send WhatsApp"
                "sendGmail" -> "Send Email"
                "clearAllMemory" -> "Clear Memory"
                else -> "High-Risk Action"
            }
            val desc = when (name) {
                "searchAndCallContact" -> "Kya aap sach me ${params["contactName"]} ko call karna chahte hain?"
                "sendSMS" -> "SMS bhejne ke liye confirm karein: '${params["message"]}'"
                "sendWhatsAppMessage" -> "WhatsApp par message bhejne ke liye confirm karein: '${params["message"]}'"
                "sendGmail" -> "Email bhejne ke liye confirm karein to ${params["recipientEmail"]}"
                else -> "Aap ye action confirm karte hain?"
            }

            val request = ConfirmationRequest(
                toolName = name,
                title = title,
                description = desc,
                riskLevel = risk,
                payload = params.mapValues { it.value?.toString() ?: "" }
            )
            confirmationManager.requestConfirmation(request)

            return ToolResult(
                callId = toolCall.callId,
                name = name,
                success = false,
                message = "Confirmation required: $desc",
                outputData = mapOf("pendingConfirmation" to true, "prompt" to desc)
            )
        }

        // 3. Execution based on tool
        return try {
            val result = when (name) {
                "openApp" -> {
                    val app = params["appName"]?.toString() ?: ""
                    val res = deviceRepository.openApp(app)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "searchAndCallContact" -> {
                    val contactName = params["contactName"]?.toString() ?: ""
                    if (!permissionManager.hasReadContacts()) {
                        ToolResult(toolCall.callId, name, false, "Contacts permission granted nahi hai.")
                    } else {
                        val matches = contactRepository.searchContacts(contactName)
                        when {
                            matches.isEmpty() -> {
                                ToolResult(toolCall.callId, name, false, "'$contactName' naam ka koi contact nahi mila.")
                            }
                            matches.size > 1 -> {
                                val listDesc = matches.take(3).joinToString(", ") { "${it.displayName} (${it.phoneNumber})" }
                                ToolResult(
                                    toolCall.callId,
                                    name,
                                    false,
                                    "Multiple contacts mile: $listDesc. Kisko call lagau?",
                                    mapOf("multiple" to true)
                                )
                            }
                            else -> {
                                val contact = matches.first()
                                val callRes = deviceRepository.makePhoneCall(contact.phoneNumber)
                                ToolResult(toolCall.callId, name, callRes.first, callRes.second)
                            }
                        }
                    }
                }

                "sendWhatsAppMessage" -> {
                    val contactName = params["contactName"]?.toString() ?: ""
                    val message = params["message"]?.toString() ?: ""
                    // Try to resolve phone number from contact name if needed
                    val matches = contactRepository.searchContacts(contactName)
                    val phone = if (matches.isNotEmpty()) matches.first().phoneNumber else contactName
                    val res = deviceRepository.sendWhatsAppMessage(phone, message)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "sendSMS" -> {
                    val contactName = params["contactName"]?.toString() ?: ""
                    val message = params["message"]?.toString() ?: ""
                    val matches = contactRepository.searchContacts(contactName)
                    val phone = if (matches.isNotEmpty()) matches.first().phoneNumber else contactName
                    val res = deviceRepository.sendSMS(phone, message)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "sendGmail" -> {
                    val email = params["recipientEmail"]?.toString() ?: ""
                    val subject = params["subject"]?.toString() ?: "MAX Message"
                    val body = params["body"]?.toString() ?: ""
                    val res = deviceRepository.sendGmail(email, subject, body)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "createReminder" -> {
                    val title = params["title"]?.toString() ?: "Reminder"
                    val minutes = (params["minutesFromNow"] as? Number)?.toLong() ?: 10L
                    val repeatRule = params["repeatRule"]?.toString() ?: "NONE"
                    val triggerMs = System.currentTimeMillis() + (minutes * 60 * 1000L)
                    reminderManager.createReminder(title, triggerMs, repeatRule)
                    ToolResult(toolCall.callId, name, true, "Done! $minutes minute baad ka reminder set kar diya: '$title'.")
                }

                "setAlarm" -> {
                    val hour = (params["hour"] as? Number)?.toInt() ?: 8
                    val minutes = (params["minutes"] as? Number)?.toInt() ?: 0
                    val label = params["label"]?.toString() ?: "MAX Alarm"
                    val res = deviceRepository.setAlarm(hour, minutes, label)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "createCalendarEvent" -> {
                    val title = params["title"]?.toString() ?: "New Event"
                    val offsetMin = (params["minutesFromNow"] as? Number)?.toLong() ?: 60L
                    val durMin = (params["durationMinutes"] as? Number)?.toLong() ?: 30L
                    val desc = params["description"]?.toString() ?: ""
                    val startMs = System.currentTimeMillis() + (offsetMin * 60 * 1000L)
                    val endMs = startMs + (durMin * 60 * 1000L)
                    val res = deviceRepository.addCalendarEvent(title, startMs, endMs, desc)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "getDeviceStatus" -> {
                    val status = deviceRepository.getDeviceStatus()
                    val chargingStr = if (status.isCharging) "(Charging)" else "(Battery mode)"
                    val msg = "Battery: ${status.batteryPercentage}% $chargingStr, Storage: ${status.freeStorageGb}GB free of ${status.totalStorageGb}GB, Network: ${status.networkType}."
                    ToolResult(toolCall.callId, name, true, msg, mapOf(
                        "battery" to status.batteryPercentage,
                        "charging" to status.isCharging,
                        "freeStorage" to status.freeStorageGb,
                        "network" to status.networkType
                    ))
                }

                "openSettings" -> {
                    val settingType = params["settingType"]?.toString() ?: "general"
                    val res = deviceRepository.openSetting(settingType)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "flashlight" -> {
                    val enabled = params["enabled"]?.toString()?.toBooleanStrictOrNull() ?: true
                    val res = deviceRepository.setFlashlight(enabled)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "mediaControl" -> {
                    val action = params["action"]?.toString() ?: "play"
                    val res = deviceRepository.controlMedia(action)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "openCamera" -> {
                    val res = deviceRepository.openCamera()
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "openGallery" -> {
                    val res = deviceRepository.openGallery()
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "searchMaps" -> {
                    val q = params["query"]?.toString() ?: ""
                    val res = deviceRepository.openMaps(q)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "webSearch" -> {
                    val q = params["query"]?.toString() ?: ""
                    val res = deviceRepository.webSearch(q)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "searchYouTube" -> {
                    val q = params["query"]?.toString() ?: ""
                    val res = deviceRepository.searchYouTube(q)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "rememberInfo" -> {
                    val topic = params["topic"]?.toString() ?: "Note"
                    val content = params["content"]?.toString() ?: ""
                    val res = memoryManager.saveMemory(topic, content)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "forgetInfo" -> {
                    val q = params["query"]?.toString() ?: ""
                    val res = memoryManager.forgetMemory(q)
                    ToolResult(toolCall.callId, name, res.first, res.second)
                }

                "queryMemory" -> {
                    val q = params["query"]?.toString() ?: ""
                    val matches = memoryManager.searchMemory(q)
                    if (matches.isEmpty()) {
                        ToolResult(toolCall.callId, name, true, "Is baare me koi saved memory nahi hai.")
                    } else {
                        val summary = matches.joinToString("\n") { "- ${it.topic}: ${it.content}" }
                        ToolResult(toolCall.callId, name, true, "Memory me ye mila:\n$summary")
                    }
                }

                else -> {
                    ToolResult(toolCall.callId, name, false, "Unknown tool: $name")
                }
            }

            logAudit(name, result.message, risk.name, if (result.success) "SUCCESS" else "FAILED")
            result
        } catch (e: Exception) {
            val errMsg = "Action execute karte waqt error aaya: ${e.localizedMessage ?: "Unknown"}"
            logAudit(name, errMsg, risk.name, "ERROR")
            ToolResult(toolCall.callId, name, false, errMsg)
        }
    }

    private suspend fun logAudit(action: String, details: String, risk: String, status: String) {
        try {
            database.auditLogDao().insertLog(
                AuditLogEntity(
                    actionType = action,
                    details = details,
                    riskLevel = risk,
                    status = status
                )
            )
        } catch (e: Exception) {
            // Non-blocking log failure
        }
    }
}
