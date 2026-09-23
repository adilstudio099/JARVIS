package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResponse {
    data class TextResponse(val text: String) : GeminiResponse()
    data class FunctionCallResponse(
        val functionName: String,
        val arguments: Map<String, Any?>,
        val rawJson: String
    ) : GeminiResponse()
    data class ErrorResponse(val error: String) : GeminiResponse()
}

class GeminiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "GeminiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }

    suspend fun generateWithTools(
        prompt: String,
        history: List<Pair<String, String>>, // role ("user" or "model") to text
        customApiKey: String? = null
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            return@withContext GeminiResponse.ErrorResponse("API_KEY_MISSING")
        }

        try {
            val requestJson = JSONObject()

            // System Instruction
            val systemInstruction = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are JARVIS, an advanced AI personal assistant inspired by Tony Stark's J.A.R.V.I.S. You are articulate, sophisticated, helpful, and concise. " +
                                "You have access to tools for reminders, notes, to-dos, calculations, weather, and date/time. " +
                                "Whenever the user gives a command matching one of your tools (e.g., remind me, note this, add to-do, calculate, weather, time), you MUST call the matching function tool. " +
                                "For conversational or general knowledge questions, answer directly with intelligence and precision.")
                    })
                })
            }
            requestJson.put("systemInstruction", systemInstruction)

            // Contents array with conversation memory
            val contentsArray = JSONArray()
            for ((role, text) in history.takeLast(6)) {
                val turnObj = JSONObject().apply {
                    put("role", if (role == "user") "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                }
                contentsArray.put(turnObj)
            }

            // Current prompt
            val currentTurn = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            }
            contentsArray.put(currentTurn)
            requestJson.put("contents", contentsArray)

            // Tools definitions
            val toolsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("functionDeclarations", buildFunctionDeclarations())
                })
            }
            requestJson.put("tools", toolsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topK", 40)
            }
            requestJson.put("generationConfig", genConfig)

            val endpoint = "$BASE_URL$DEFAULT_MODEL:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error: ${response.code} $responseString")
                return@withContext GeminiResponse.ErrorResponse("API Error (${response.code}): $responseString")
            }

            parseResponse(responseString)
        } catch (e: Exception) {
            Log.e(TAG, "Request exception", e)
            GeminiResponse.ErrorResponse(e.message ?: "Unknown communication failure")
        }
    }

    private fun parseResponse(rawJson: String): GeminiResponse {
        try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return GeminiResponse.ErrorResponse("No candidates returned from Gemini.")
            if (candidates.length() == 0) return GeminiResponse.ErrorResponse("Empty candidate list.")

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return GeminiResponse.ErrorResponse("No content in candidate.")
            val parts = content.optJSONArray("parts") ?: return GeminiResponse.ErrorResponse("No parts in candidate content.")

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)

                // Check for function call
                if (part.has("functionCall")) {
                    val fc = part.getJSONObject("functionCall")
                    val name = fc.optString("name")
                    val argsObj = fc.optJSONObject("args") ?: JSONObject()
                    val argsMap = mutableMapOf<String, Any?>()
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.opt(key)
                    }
                    return GeminiResponse.FunctionCallResponse(
                        functionName = name,
                        arguments = argsMap,
                        rawJson = argsObj.toString()
                    )
                }

                // Check for text
                if (part.has("text")) {
                    val text = part.optString("text")
                    if (text.isNotBlank()) {
                        return GeminiResponse.TextResponse(text)
                    }
                }
            }

            return GeminiResponse.TextResponse("Action completed as requested, sir.")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
            return GeminiResponse.ErrorResponse("Parsing error: ${e.message}")
        }
    }

    private fun buildFunctionDeclarations(): JSONArray {
        val array = JSONArray()

        // 1. set_reminder
        array.put(JSONObject().apply {
            put("name", "set_reminder")
            put("description", "Set a reminder or alarm for a specific task and time (e.g. in 10 minutes, at 4 PM, tomorrow morning).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("task", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The task or action to be reminded of")
                    })
                    put("time", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The time string or relative duration, e.g. '15 minutes', '5:00 PM', 'tomorrow 8:00 AM'")
                    })
                })
                put("required", JSONArray().apply {
                    put("task")
                    put("time")
                })
            })
        })

        // 2. create_note
        array.put(JSONObject().apply {
            put("name", "create_note")
            put("description", "Create and save a new note with a title, body content, and optional category.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Short title or topic for the note")
                    })
                    put("content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The full content or details of the note")
                    })
                    put("category", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Category: Work, Personal, Ideas, Project, Tech")
                    })
                })
                put("required", JSONArray().apply {
                    put("title")
                    put("content")
                })
            })
        })

        // 3. manage_todo
        array.put(JSONObject().apply {
            put("name", "manage_todo")
            put("description", "Add, complete, or list items in the to-do list.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Action to take: 'add', 'complete', or 'list'")
                    })
                    put("title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The to-do item title or description")
                    })
                    put("priority", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Priority: 'High', 'Normal', or 'Low'")
                    })
                })
                put("required", JSONArray().apply {
                    put("action")
                    put("title")
                })
            })
        })

        // 4. calculate
        array.put(JSONObject().apply {
            put("name", "calculate")
            put("description", "Perform a mathematical calculation, equation, currency/unit conversion, or percentage calculation.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("expression", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The math expression, e.g. '15% tip on 85 dollars', '2^16', '345 * 82'")
                    })
                    put("result", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The exact numerical or converted result")
                    })
                    put("explanation", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Short step-by-step breakdown or context")
                    })
                })
                put("required", JSONArray().apply {
                    put("expression")
                    put("result")
                })
            })
        })

        // 5. get_date_time
        array.put(JSONObject().apply {
            put("name", "get_date_time")
            put("description", "Check current date, current time, day of week, or current year.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("format", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Requested format: 'time', 'date', or 'both'")
                    })
                })
            })
        })

        // 6. get_weather
        array.put(JSONObject().apply {
            put("name", "get_weather")
            put("description", "Get the current weather and forecast for any city or location.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("location", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The city or territory name, e.g., 'San Francisco', 'London', 'Tokyo'")
                    })
                })
                put("required", JSONArray().apply {
                    put("location")
                })
            })
        })

        return array
    }
}
