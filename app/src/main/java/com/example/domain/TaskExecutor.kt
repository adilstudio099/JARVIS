package com.example.domain

import com.example.data.JarvisRepository
import com.example.data.local.DirectiveItemEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TodoEntity
import com.example.data.remote.GeminiResponse
import com.example.data.remote.GeminiService
import com.example.device.DeviceActionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TaskExecutionResult(
    val moduleName: String,
    val spokenResponse: String,
    val displayContent: String,
    val rawJson: String? = null
)

class TaskExecutor(
    private val repository: JarvisRepository,
    private val geminiService: GeminiService = GeminiService(),
    private val deviceActionHandler: DeviceActionHandler? = null
) {

    private fun isUrdu(text: String): Boolean {
        return text.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }
    }

    suspend fun executeTool(
        functionName: String,
        arguments: Map<String, Any?>,
        userPrompt: String = "",
        customApiKey: String? = null
    ): TaskExecutionResult = withContext(Dispatchers.IO) {
        val userIsUrdu = isUrdu(userPrompt) || isUrdu(arguments.values.joinToString(" ") { it?.toString() ?: "" })

        when (functionName) {
            // 1. WEB SEARCH GROUNDING
            "web_search" -> {
                val query = arguments["query"]?.toString() ?: userPrompt
                val groundingResponse = geminiService.searchWithGrounding(query, customApiKey)

                when (groundingResponse) {
                    is GeminiResponse.TextResponse -> {
                        val text = groundingResponse.text
                        val spoken = text.take(240).replace(Regex("[*#`_~]"), "")
                        val display = if (userIsUrdu) {
                            "🌐 لائیو گوگل سرچ معلومات:\n\n$text"
                        } else {
                            "🌐 Live Google Search Grounding:\n\n$text"
                        }
                        TaskExecutionResult("SEARCH", spoken, display)
                    }
                    is GeminiResponse.QuotaExceededResponse -> {
                        TaskExecutionResult(
                            "QUOTA",
                            "معذرت، آج کی مفت یومیہ حد مکمل ہو گئی ہے۔ برائے مہربانی کچھ دیر بعد کوشش فرمائیں۔",
                            "⚠️ روزانہ مفت حد (Free Daily Quota) ختم ہو گئی ہے۔ آپ سیٹنگز میں ذاتی API Key درج کر سکتے ہیں۔"
                        )
                    }
                    else -> {
                        val fallback = if (userIsUrdu) {
                            "معذرت، انٹرنیٹ پر اس وقت مطلوبہ معلومات حاصل نہیں ہو سکیں۔ برائے مہربانی دوبارہ کوشش کریں۔"
                        } else {
                            "Unable to retrieve search results at this moment. Please check your internet connection."
                        }
                        TaskExecutionResult("SEARCH", fallback, fallback)
                    }
                }
            }

            // 2. SAVE DIRECTIVE DATA (General persistence for any task, reminder, note, memory, list)
            "save_directive_data" -> {
                val type = arguments["type"]?.toString()?.lowercase() ?: "general"
                val title = arguments["title"]?.toString()?.trim() ?: "Directive"
                val content = arguments["content"]?.toString()?.trim() ?: ""
                val tags = arguments["tags"]?.toString()?.trim() ?: ""

                // Persist into dynamic generic directive store
                repository.insertDirective(
                    DirectiveItemEntity(
                        type = type,
                        title = title,
                        content = content,
                        tags = tags
                    )
                )

                // Also sync with specialized tables for cross-compatibility
                when (type) {
                    "reminder", "alarm" -> {
                        val targetEpoch = parseTimeToEpoch(content)
                        repository.insertReminder(
                            ReminderEntity(
                                task = title,
                                timeText = content.ifBlank { "Scheduled" },
                                targetEpochMs = targetEpoch
                            )
                        )
                    }
                    "task", "todo" -> {
                        repository.insertTodo(
                            TodoEntity(
                                title = title,
                                priority = if (tags.contains("urgent", ignoreCase = true)) "High" else "Normal"
                            )
                        )
                    }
                    "note", "memo", "memory" -> {
                        repository.insertNote(
                            NoteEntity(
                                title = title,
                                content = content.ifBlank { title },
                                category = if (tags.isNotBlank()) tags else "General"
                            )
                        )
                    }
                }

                val typeLabelUrdu = when (type) {
                    "reminder" -> "یاد دہانی"
                    "task", "todo" -> "کام (ٹاسک)"
                    "note", "memo" -> "نوٹ"
                    else -> "ریکارڈ"
                }

                val spoken = if (userIsUrdu) {
                    "$typeLabelUrdu کامیابی سے والٹ میں محفوظ کر دیا گیا ہے۔"
                } else {
                    "Directive saved securely to the Stark database."
                }

                val display = if (userIsUrdu) {
                    "💾 والٹ میں محفوظ ہو گیا:\n• عنوان: $title" +
                            (if (content.isNotBlank()) "\n• تفصیل: $content" else "") +
                            "\n• زمرہ: $typeLabelUrdu"
                } else {
                    "💾 Saved to Vault:\n• Title: $title" +
                            (if (content.isNotBlank()) "\n• Detail: $content" else "") +
                            "\n• Category: $type"
                }

                val rawJson = JSONObject().apply {
                    put("type", type)
                    put("title", title)
                    put("content", content)
                    put("tags", tags)
                }.toString()

                TaskExecutionResult("VAULT", spoken, display, rawJson)
            }

            // 3. QUERY DIRECTIVE DATA
            "query_directive_data" -> {
                val query = arguments["query"]?.toString()?.trim() ?: ""
                val type = arguments["type"]?.toString()?.trim()?.lowercase() ?: "all"

                val results = if (query.isNotBlank()) {
                    repository.searchDirectives(query)
                } else {
                    repository.getDirectivesByType(if (type == "all") "" else type)
                }

                if (results.isEmpty()) {
                    val spoken = if (userIsUrdu) "آپ کے پاس فی الحال کوئی محفوظ ریکارڈ موجود نہیں ہے۔" else "No matching directives found in storage."
                    TaskExecutionResult("VAULT", spoken, "📭 $spoken")
                } else {
                    val count = results.size
                    val spoken = if (userIsUrdu) "آپ کے $count محفوظ ریکارڈز حاصل کر لیے گئے ہیں۔" else "Retrieved $count stored directives, sir."

                    val sb = StringBuilder()
                    sb.append(if (userIsUrdu) "📋 محفوظ ریکارڈز کی فہرست ($count):\n" else "📋 Stored Vault Directives ($count):\n")
                    results.take(8).forEachIndexed { idx, item ->
                        val check = if (item.isCompleted) "✅" else "📌"
                        sb.append("\n$check ${idx + 1}. [${item.type.uppercase()}] ${item.title}")
                        if (item.content.isNotBlank()) sb.append("\n   ↳ ${item.content}")
                    }

                    TaskExecutionResult("VAULT", spoken, sb.toString())
                }
            }

            // 4. UPDATE OR REMOVE DIRECTIVE DATA
            "update_or_remove_data" -> {
                val action = arguments["action"]?.toString()?.lowercase() ?: "complete"
                val identifier = arguments["identifier"]?.toString()?.trim() ?: ""

                val matches = repository.searchDirectives(identifier)
                if (matches.isNotEmpty()) {
                    val target = matches.first()
                    if (action == "delete" || action == "remove") {
                        repository.deleteDirectiveById(target.id)
                        val spoken = if (userIsUrdu) "آئٹم کامیابی سے ہٹا دیا گیا ہے۔" else "Item removed from vault."
                        TaskExecutionResult("VAULT", spoken, "🗑 Removed: ${target.title}")
                    } else {
                        repository.toggleDirectiveCompletion(target.id, true)
                        val spoken = if (userIsUrdu) "آئٹم مکمل قرار دے دیا گیا ہے۔" else "Directive marked as completed."
                        TaskExecutionResult("VAULT", spoken, "✅ Completed: ${target.title}")
                    }
                } else {
                    val spoken = if (userIsUrdu) "مطلوبہ آئٹم نہیں ملا۔" else "No matching directive found."
                    TaskExecutionResult("VAULT", spoken, spoken)
                }
            }

            // 5. COMPUTE CALCULATION
            "compute_calculation", "calculate" -> {
                val expression = arguments["expression"]?.toString() ?: ""
                val result = arguments["result"]?.toString() ?: evaluateExpression(expression)
                val explanation = arguments["explanation"]?.toString() ?: ""

                val spoken = if (userIsUrdu) {
                    "$expression کا نتیجہ $result ہے۔"
                } else {
                    "The calculation result is $result."
                }

                val display = if (userIsUrdu) {
                    "🔢 حسابی تخمینہ\n• مساوات: $expression\n• نتیجہ: = $result" + (if (explanation.isNotBlank()) "\n• تفصیل: $explanation" else "")
                } else {
                    "🔢 Numerical Calculation\n• Equation: $expression\n• Result: = $result" + (if (explanation.isNotBlank()) "\n• Detail: $explanation" else "")
                }

                val json = JSONObject().apply {
                    put("type", "calculator")
                    put("expression", expression)
                    put("result", result)
                }.toString()

                TaskExecutionResult("CALCULATOR", spoken, display, json)
            }

            // 6. GET DEVICE TELEMETRY
            "get_device_telemetry", "get_date_time" -> {
                val now = Date()
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                val currentTime = timeFormat.format(now)
                val currentDate = dateFormat.format(now)

                val spoken = if (userIsUrdu) {
                    "اس وقت $currentTime بجے ہیں، اور آج $currentDate ہے۔"
                } else {
                    "Current time is $currentTime on $currentDate."
                }

                val display = if (userIsUrdu) {
                    "⏱ سسٹم وقت و تاریخ\n• موجودہ وقت: $currentTime\n• تاریخ: $currentDate"
                } else {
                    "⏱ System Temporal Telemetry\n• Time: $currentTime\n• Date: $currentDate"
                }

                TaskExecutionResult("TELEMETRY", spoken, display)
            }

            // 7. REAL NATIVE DEVICE ACTION (Android Intents)
            "device_action" -> {
                val action = arguments["action"]?.toString()?.lowercase() ?: ""
                val target = arguments["target"]?.toString()?.trim() ?: ""
                val extraData = arguments["extraData"]?.toString()?.trim() ?: ""

                if (deviceActionHandler != null) {
                    val result = when (action) {
                        "call" -> deviceActionHandler.dialPhone(target, userIsUrdu)
                        "sms" -> deviceActionHandler.sendSms(target, extraData, userIsUrdu)
                        "maps" -> deviceActionHandler.openMaps(target, userIsUrdu)
                        "alarm" -> {
                            val parts = target.split(":")
                            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
                            val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            deviceActionHandler.setAlarm(hour, min, extraData.ifBlank { "Jarvis Alarm" }, userIsUrdu)
                        }
                        "open_url" -> deviceActionHandler.openUrl(target, userIsUrdu)
                        "open_app" -> deviceActionHandler.launchApp(target, userIsUrdu)
                        else -> {
                            val err = if (userIsUrdu) "نامعلوم ڈیوائس ایکشن: $action" else "Unknown device action: $action"
                            com.example.device.DeviceActionResult(false, err, "⚠️ $err")
                        }
                    }
                    TaskExecutionResult("DEVICE_ACTION", result.spokenResponse, result.displayResponse)
                } else {
                    val msg = if (userIsUrdu) "ڈیوائس ایکشنز ہینڈلر دستیاب نہیں ہے۔" else "Device action handler not initialized."
                    TaskExecutionResult("DEVICE_ACTION", msg, "⚠️ $msg")
                }
            }

            else -> {
                val spoken = if (userIsUrdu) "حکم موصول ہو گیا ہے اور لاگ کر دیا گیا ہے۔" else "Directive logged, sir."
                TaskExecutionResult("UNKNOWN", spoken, spoken)
            }
        }
    }

    private fun parseTimeToEpoch(timeStr: String): Long {
        val now = System.currentTimeMillis()
        return try {
            if (timeStr.contains("شام") || timeStr.contains("pm") || timeStr.contains("PM")) {
                val digits = Regex("\\d+").find(timeStr)?.value?.toIntOrNull() ?: 5
                val hour = if (digits < 12) digits + 12 else digits
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.timeInMillis
            } else if (timeStr.contains("صبح") || timeStr.contains("am") || timeStr.contains("AM")) {
                val digits = Regex("\\d+").find(timeStr)?.value?.toIntOrNull() ?: 9
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, digits)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.timeInMillis
            } else {
                now + 30 * 60 * 1000
            }
        } catch (e: Exception) {
            now + 15 * 60 * 1000
        }
    }

    fun evaluateExpression(expr: String): String {
        return try {
            val clean = expr.replace("x", "*").replace("×", "*").replace("÷", "/")
            if (clean.contains("% of") || clean.contains("%")) {
                val pctMatch = Regex("([\\d.]+)\\s*%").find(clean)
                val totalMatch = Regex("(?:of|پر|کا)\\s*([\\d.]+)").find(clean)
                if (pctMatch != null && totalMatch != null) {
                    val pct = pctMatch.groupValues[1].toDouble()
                    val total = totalMatch.groupValues[1].toDouble()
                    return "%.2f".format((pct / 100.0) * total)
                }
            }
            when {
                clean.contains("+") -> {
                    val p = clean.split("+")
                    (p[0].trim().toDouble() + p[1].trim().toDouble()).toString()
                }
                clean.contains("-") -> {
                    val p = clean.split("-")
                    (p[0].trim().toDouble() - p[1].trim().toDouble()).toString()
                }
                clean.contains("*") -> {
                    val p = clean.split("*")
                    (p[0].trim().toDouble() * p[1].trim().toDouble()).toString()
                }
                clean.contains("/") -> {
                    val p = clean.split("/")
                    (p[0].trim().toDouble() / p[1].trim().toDouble()).toString()
                }
                else -> clean
            }
        } catch (e: Exception) {
            "Calculated"
        }
    }

    /**
     * Checks if a user command asks for an action that genuinely cannot be performed
     * due to mobile platform sandbox / security restrictions (e.g. money transfers, restarting phone, hacking).
     */
    fun checkImpossibleAction(query: String): TaskExecutionResult? {
        val q = query.lowercase()
        val impossibleKeywords = listOf(
            "restart phone", "reboot", "ری اسٹارٹ",
            "bank transfer", "send money", "پیسے بھیجو",
            "hack", "ہیک",
            "download youtube", "یوٹیوب ویڈیو ڈاؤنلوڈ"
        )

        val matches = impossibleKeywords.any { q.contains(it) }
        if (!matches) return null

        val isUrduQuery = isUrdu(query) || q.contains("karo") || q.contains("bhejo") || q.contains("karen")

        val spoken = if (isUrduQuery) {
            "معذرت، ڈیوائس کی سیکیورٹی پابندیوں اور پالیسی کی وجہ سے میں سسٹم کو ریبوٹ یا مالیاتی ٹرانزیکشن نہیں کر سکتا۔"
        } else {
            "Sir, due to mobile security sandbox restrictions, I cannot directly reboot the device or execute financial transfers."
        }

        val display = if (isUrduQuery) {
            "⚠️ سیکیورٹی پابندی (Security Boundary):\n" +
                    "اینڈرائیڈ آپریٹنگ سسٹم کی سیکیورٹی پابندیوں کے باعث میں سسٹم سطح کے کنٹرول (جیسے ریبوٹ) یا غیر محفوظ مالیاتی ٹرانزیکشن نہیں کر سکتا۔\n\n" +
                    "💡 تاہم میں اس سے متعلق کوئی بھی یاد دہانی، ایجنڈا، حساب یا نوٹ آپ کے لیے محفوظ کر سکتا ہوں۔"
        } else {
            "⚠️ Mobile Platform Security Boundary:\n" +
                    "Due to Android sandbox protections, rebooting the system or executing financial transactions is prohibited.\n\n" +
                    "💡 I can securely store reminders, memos, notes, or search for relevant instructions instead."
        }

        return TaskExecutionResult("RESTRICTED", spoken, display)
    }

    /**
     * Offline local fallback command parser supporting both Urdu and English natural language.
     */
    suspend fun executeLocalFallback(query: String, customApiKey: String? = null): TaskExecutionResult? {
        val q = query.trim().lowercase()

        // Check if impossible / OS restricted action
        checkImpossibleAction(query)?.let { return it }

        // 1. Time / Date
        if (q.contains("time") || q.contains("وقت") || q.contains("date") || q.contains("تاریخ") || q.contains("ٹائم")) {
            return executeTool("get_device_telemetry", emptyMap(), query, customApiKey)
        }

        // 2. Call / Phone Dial
        if (q.contains("call") || q.contains("کال") || q.contains("فون ملاؤ") || q.contains("ڈائل")) {
            val digits = Regex("[0-9+]{4,}").find(query)?.value ?: "0300"
            return executeTool("device_action", mapOf("action" to "call", "target" to digits), query, customApiKey)
        }

        // 3. SMS Message
        if (q.contains("sms") || q.contains("میسج") || q.contains("پیغام")) {
            val digits = Regex("[0-9+]{4,}").find(query)?.value ?: ""
            return executeTool("device_action", mapOf("action" to "sms", "target" to digits, "extraData" to query), query, customApiKey)
        }

        // 4. Maps / Navigation
        if (q.contains("map") || q.contains("نقشہ") || q.contains("راستہ") || q.contains("navigation")) {
            val loc = query.replace(Regex("(?i)map|maps|نقشہ|راستہ|دکھاؤ"), "").trim()
            return executeTool("device_action", mapOf("action" to "maps", "target" to loc.ifBlank { "Lahore" }), query, customApiKey)
        }

        // 5. App Launch
        if (q.contains("open ") || q.contains("کھولو")) {
            val app = query.replace(Regex("(?i)open|کھولو"), "").trim()
            if (app.isNotBlank()) {
                return executeTool("device_action", mapOf("action" to "open_app", "target" to app), query, customApiKey)
            }
        }

        // 6. Reminders
        if (q.contains("remind") || q.contains("یاد دلاؤ") || q.contains("یاد دہانی")) {
            val task = q.removePrefix("remind me to").removePrefix("مجھے یاد دلاؤ").trim()
            return executeTool("save_directive_data", mapOf("type" to "reminder", "title" to task.ifBlank { "Reminder" }, "content" to "Scheduled"), query, customApiKey)
        }

        // 7. Notes & Memory
        if (q.contains("note") || q.contains("نوٹ") || q.contains("یاد رکھو") || q.contains("محفوظ کرو")) {
            val body = q.removePrefix("note down").removePrefix("take a note").removePrefix("نوٹ کرو").removePrefix("یاد رکھو").trim()
            return executeTool("save_directive_data", mapOf("type" to "note", "title" to body.take(30).ifBlank { "Quick Note" }, "content" to body), query, customApiKey)
        }

        // 8. Query / List saved items
        if (q.contains("show notes") || q.contains("my tasks") || q.contains("دکھاؤ") || q.contains("والٹ") || q.contains("لسٹ")) {
            return executeTool("query_directive_data", emptyMap(), query, customApiKey)
        }

        // 9. Math calculation
        if (q.contains("calculate") || q.contains("حساب") || q.contains("کتنے ہوتے ہیں") || (q.contains("+") || q.contains("-") || q.contains("*") || q.contains("/"))) {
            val expr = q.removePrefix("calculate").removePrefix("what is ").removePrefix("حساب لگاؤ").removeSuffix("?").trim()
            val res = evaluateExpression(expr)
            return executeTool("compute_calculation", mapOf("expression" to expr, "result" to res), query, customApiKey)
        }

        // 10. Weather / Live Search (if API key available)
        if (q.contains("weather") || q.contains("موسم") || q.contains("temperature") || q.contains("درجہ حرارت")) {
            val loc = if (q.contains("in ")) {
                q.substringAfter("in ").trim()
            } else if (q.contains("میں ")) {
                q.substringBefore("میں ").trim().split(" ").lastOrNull() ?: "Lahore"
            } else {
                "Lahore"
            }
            return executeTool("web_search", mapOf("query" to "weather in $loc"), query, customApiKey)
        }

        return null
    }
}
