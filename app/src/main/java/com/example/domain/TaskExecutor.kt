package com.example.domain

import com.example.data.JarvisRepository
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TodoEntity
import com.example.data.remote.WeatherService
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
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
    private val weatherService: WeatherService = WeatherService()
) {
    suspend fun executeTool(functionName: String, arguments: Map<String, Any?>): TaskExecutionResult {
        return when (functionName) {
            "set_reminder" -> {
                val task = arguments["task"]?.toString() ?: "Reminder"
                val timeStr = arguments["time"]?.toString() ?: "Soon"

                // Estimate target epoch if possible
                val targetEpoch = parseTimeToEpoch(timeStr)
                repository.insertReminder(
                    ReminderEntity(
                        task = task,
                        timeText = timeStr,
                        targetEpochMs = targetEpoch
                    )
                )

                val spoken = "Reminder set for $task at $timeStr, sir."
                val display = "⏰ Reminder Scheduled\n• Task: $task\n• Time: $timeStr"
                val json = JSONObject().apply {
                    put("type", "reminder")
                    put("task", task)
                    put("time", timeStr)
                }.toString()

                TaskExecutionResult("REMINDER", spoken, display, json)
            }

            "create_note" -> {
                val title = arguments["title"]?.toString() ?: "Note"
                val content = arguments["content"]?.toString() ?: ""
                val category = arguments["category"]?.toString() ?: "General"

                repository.insertNote(
                    NoteEntity(
                        title = title,
                        content = content,
                        category = category
                    )
                )

                val spoken = "I have noted that down under $title."
                val display = "📝 Note Saved\n• Title: $title\n• Category: $category\n• Content: $content"
                val json = JSONObject().apply {
                    put("type", "note")
                    put("title", title)
                    put("content", content)
                    put("category", category)
                }.toString()

                TaskExecutionResult("NOTE", spoken, display, json)
            }

            "manage_todo" -> {
                val action = arguments["action"]?.toString()?.lowercase() ?: "add"
                val title = arguments["title"]?.toString() ?: "Task"
                val priority = arguments["priority"]?.toString() ?: "Normal"

                when (action) {
                    "add" -> {
                        repository.insertTodo(
                            TodoEntity(
                                title = title,
                                priority = priority
                            )
                        )
                        val spoken = "Added '$title' to your to-do list."
                        val display = "✅ To-Do Added\n• Task: $title\n• Priority: $priority"
                        val json = JSONObject().apply {
                            put("type", "todo")
                            put("action", "add")
                            put("title", title)
                            put("priority", priority)
                        }.toString()
                        TaskExecutionResult("TODO", spoken, display, json)
                    }
                    else -> {
                        repository.insertTodo(
                            TodoEntity(title = title, priority = priority)
                        )
                        val spoken = "Updated your to-do list with '$title'."
                        val display = "✅ To-Do Updated\n• Task: $title"
                        TaskExecutionResult("TODO", spoken, display)
                    }
                }
            }

            "calculate" -> {
                val expression = arguments["expression"]?.toString() ?: ""
                val result = arguments["result"]?.toString() ?: evaluateExpression(expression)
                val explanation = arguments["explanation"]?.toString() ?: ""

                val spoken = "The result of $expression is $result."
                val display = if (explanation.isNotBlank()) {
                    "🔢 Calculation\n• Equation: $expression\n• Result: $result\n• Detail: $explanation"
                } else {
                    "🔢 Calculation\n• Equation: $expression\n• Result: = $result"
                }
                val json = JSONObject().apply {
                    put("type", "calculator")
                    put("expression", expression)
                    put("result", result)
                }.toString()

                TaskExecutionResult("CALCULATOR", spoken, display, json)
            }

            "get_date_time" -> {
                val now = Date()
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                val currentTime = timeFormat.format(now)
                val currentDate = dateFormat.format(now)

                val spoken = "It is currently $currentTime on $currentDate."
                val display = "⏱ Temporal Telemetry\n• Current Time: $currentTime\n• Date: $currentDate"
                val json = JSONObject().apply {
                    put("type", "date_time")
                    put("time", currentTime)
                    put("date", currentDate)
                }.toString()

                TaskExecutionResult("DATE_TIME", spoken, display, json)
            }

            "get_weather" -> {
                val location = arguments["location"]?.toString() ?: "Current Location"
                val weather = weatherService.fetchWeather(location)

                val spoken = weather.rawSummary
                val display = "🌤 Weather Analysis for ${weather.location}\n" +
                        "• Condition: ${weather.condition}\n" +
                        "• Temperature: ${"%.1f".format(weather.temperatureC)}°C (${"%.1f".format(weather.temperatureF)}°F)\n" +
                        "• Humidity: ${weather.humidity}%\n" +
                        "• Wind: ${weather.windSpeedKmh} km/h"
                val json = JSONObject().apply {
                    put("type", "weather")
                    put("location", weather.location)
                    put("tempC", weather.temperatureC)
                    put("condition", weather.condition)
                    put("humidity", weather.humidity)
                }.toString()

                TaskExecutionResult("WEATHER", spoken, display, json)
            }

            else -> {
                TaskExecutionResult(
                    "SYSTEM",
                    "Task executed successfully, sir.",
                    "Function '$functionName' processed with parameters: $arguments"
                )
            }
        }
    }

    private fun parseTimeToEpoch(timeStr: String): Long {
        val now = System.currentTimeMillis()
        val lower = timeStr.lowercase()
        return try {
            if (lower.contains("min")) {
                val numbers = Regex("\\d+").find(lower)?.value?.toLongOrNull() ?: 10
                now + numbers * 60 * 1000
            } else if (lower.contains("hour")) {
                val numbers = Regex("\\d+").find(lower)?.value?.toLongOrNull() ?: 1
                now + numbers * 60 * 60 * 1000
            } else {
                now + 30 * 60 * 1000 // default 30 mins
            }
        } catch (e: Exception) {
            now + 15 * 60 * 1000
        }
    }

    fun evaluateExpression(expr: String): String {
        return try {
            val clean = expr.replace("x", "*").replace("×", "*").replace("÷", "/")
            if (clean.contains("% of")) {
                val parts = clean.split("% of")
                val pct = parts[0].filter { it.isDigit() || it == '.' }.toDouble()
                val total = parts[1].filter { it.isDigit() || it == '.' }.toDouble()
                return "%.2f".format((pct / 100.0) * total)
            }
            // Simple arithmetic evaluator
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

    // Local offline command parser fallback
    suspend fun executeLocalFallback(query: String): TaskExecutionResult? {
        val q = query.trim().lowercase()

        // 1. Time / Date
        if (q.contains("time") || q.contains("what time") || q.contains("current time") || q.contains("date") || q.contains("what day")) {
            return executeTool("get_date_time", emptyMap())
        }

        // 2. Weather
        if (q.startsWith("weather") || q.contains("weather in") || q.contains("temperature")) {
            val loc = if (q.contains(" in ")) {
                q.substringAfter(" in ").trim()
            } else {
                q.removePrefix("weather").removePrefix("temperature").trim().ifBlank { "New York" }
            }
            return executeTool("get_weather", mapOf("location" to loc))
        }

        // 3. Reminders
        if (q.startsWith("remind me to") || q.startsWith("set reminder") || q.startsWith("reminder")) {
            val taskPart = q.removePrefix("remind me to").removePrefix("set reminder").removePrefix("reminder").trim()
            val timePart = if (taskPart.contains(" in ")) {
                taskPart.substringAfter(" in ").trim()
            } else if (taskPart.contains(" at ")) {
                taskPart.substringAfter(" at ").trim()
            } else {
                "in 15 minutes"
            }
            val cleanTask = taskPart.substringBefore(" in ").substringBefore(" at ").trim()
            return executeTool("set_reminder", mapOf("task" to cleanTask.ifBlank { "Task" }, "time" to timePart))
        }

        // 4. Notes
        if (q.startsWith("note down") || q.startsWith("take a note") || q.startsWith("note:") || q.startsWith("create note") || q.startsWith("save note")) {
            val body = q.removePrefix("note down").removePrefix("take a note").removePrefix("note:").removePrefix("create note").removePrefix("save note").trim()
            val title = if (body.contains(":")) body.substringBefore(":").trim() else "Quick Note"
            val content = if (body.contains(":")) body.substringAfter(":").trim() else body
            return executeTool("create_note", mapOf("title" to title.replaceFirstChar { it.uppercase() }, "content" to content, "category" to "Personal"))
        }

        // 5. Todo
        if (q.startsWith("todo") || q.startsWith("add to todo") || q.startsWith("add to-do") || q.contains("to my todo list")) {
            val task = q.removePrefix("add to todo").removePrefix("add to-do").removePrefix("todo").removeSuffix("to my todo list").trim()
            return executeTool("manage_todo", mapOf("action" to "add", "title" to task.ifBlank { "New Task" }, "priority" to "Normal"))
        }

        // 6. Calculator
        if (q.startsWith("calculate") || q.startsWith("what is ") && (q.contains("+") || q.contains("-") || q.contains("*") || q.contains("/") || q.contains("%"))) {
            val expr = q.removePrefix("calculate").removePrefix("what is ").removeSuffix("?").trim()
            val res = evaluateExpression(expr)
            return executeTool("calculate", mapOf("expression" to expr, "result" to res))
        }

        return null
    }
}
