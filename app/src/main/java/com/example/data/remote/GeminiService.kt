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
    data class TextResponse(
        val text: String,
        val audioBase64: String? = null,
        val audioMimeType: String? = null,
        val sources: List<String> = emptyList()
    ) : GeminiResponse()

    data class FunctionCallResponse(
        val functionName: String,
        val arguments: Map<String, Any?>,
        val rawJson: String
    ) : GeminiResponse()

    object QuotaExceededResponse : GeminiResponse()
    data class ErrorResponse(val error: String, val technicalDetail: String? = null) : GeminiResponse()
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

        // Confirmed Google AI Studio Free Tier limits for gemini-3.5-flash:
        // - Requests Per Minute (RPM): 15 RPM
        // - Requests Per Day (RPD): 1,500 RPD
        // - Tokens Per Minute (TPM): 1,000,000 TPM
        // - Context Window: 1,000,000 tokens
        const val DEFAULT_MODEL = "gemini-3.5-flash"
    }

    fun resolveApiKey(customApiKey: String?): String {
        return when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY.trim()
            System.getenv("GEMINI_API_KEY")?.isNotBlank() == true -> (System.getenv("GEMINI_API_KEY") ?: "").trim()
            else -> ""
        }
    }

    /**
     * Diagnostic connection test that sends a lightweight prompt to Gemini to verify API key validity.
     */
    suspend fun testConnection(customApiKey: String? = null): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext Pair(false, "API key نہیں ملی۔ براہ کرم اپنی Gemini API Key درج کریں۔ (No API Key found)")
        }

        try {
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Respond in one short sentence in Urdu: 'کنکشن کامیابی سے فعال ہو چکا ہے۔'")
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 50)
                })
            }

            val endpoint = "$BASE_URL$DEFAULT_MODEL:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(endpoint).post(requestBody).build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.code == 429 || responseString.contains("RESOURCE_EXHAUSTED", ignoreCase = true)) {
                return@withContext Pair(false, "روزانہ کی حد ختم ہو گئی ہے، براہ کرم تھوڑی دیر بعد دوبارہ کوشش کریں۔ (Daily Quota Exceeded)")
            }

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(responseString).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseString"
                }
                return@withContext Pair(false, "رابطہ ناکام ہو گیا: $errorMsg (Code: ${response.code})")
            }

            val text = try {
                JSONObject(responseString).getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0)
                    .getString("text")
            } catch (e: Exception) {
                "کنکشن فعال ہے۔"
            }

            Pair(true, "✅ رابطہ کامیاب رہا! (Connected): $text")
        } catch (e: Exception) {
            Log.e(TAG, "testConnection failed", e)
            Pair(false, "نیٹ ورک خرابی: ${e.message ?: "Server Unreachable"}")
        }
    }

    /**
     * Executes general-purpose command analysis and execution for text queries.
     */
    suspend fun generateWithTools(
        prompt: String,
        history: List<Pair<String, String>>,
        customApiKey: String? = null
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext GeminiResponse.ErrorResponse("API_KEY_MISSING", "Gemini API key is not configured.")
        }

        try {
            val requestJson = JSONObject()
            requestJson.put("systemInstruction", buildSystemInstruction())

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

            // Current turn
            val currentTurn = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            }
            contentsArray.put(currentTurn)
            requestJson.put("contents", contentsArray)

            // Tools declaration
            requestJson.put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("functionDeclarations", buildGeneralCapabilityTools())
                })
            })

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.6)
                put("topK", 40)
            }
            requestJson.put("generationConfig", genConfig)

            executeGenerateCall(apiKey, requestJson)
        } catch (e: Exception) {
            Log.e(TAG, "generateWithTools failed", e)
            GeminiResponse.ErrorResponse("NETWORK_ERROR", e.message)
        }
    }

    /**
     * Voice Input Understanding (Replacing Android SpeechRecognizer):
     * Sends raw recorded audio WAV as base64 inlineData directly into Gemini 2.5 Flash.
     * Gemini transcribes, understands the Urdu/English speech, and returns the response or function call.
     */
    suspend fun generateWithAudioInput(
        base64WavAudio: String,
        history: List<Pair<String, String>>,
        customApiKey: String? = null
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext GeminiResponse.ErrorResponse("API_KEY_MISSING", "Gemini API key is not configured.")
        }

        try {
            val requestJson = JSONObject()
            requestJson.put("systemInstruction", buildSystemInstruction())

            val contentsArray = JSONArray()
            for ((role, text) in history.takeLast(4)) {
                val turnObj = JSONObject().apply {
                    put("role", if (role == "user") "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                }
                contentsArray.put(turnObj)
            }

            // Audio turn
            val audioTurn = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    // Inline Audio part
                    put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "audio/wav")
                            put("data", base64WavAudio)
                        })
                    })
                    // Guiding text part
                    put(JSONObject().apply {
                        put("text", "Carefully listen to this spoken audio directive. Transcribe and execute whatever was commanded (whether in Urdu or English). Respond strictly in natural, grammatically correct Urdu script (RTL) or execute the appropriate capability tool.")
                    })
                })
            }
            contentsArray.put(audioTurn)
            requestJson.put("contents", contentsArray)

            // Tools declaration
            requestJson.put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("functionDeclarations", buildGeneralCapabilityTools())
                })
            })

            // Attempt to generate spoken response audio if available
            val genConfig = JSONObject().apply {
                put("temperature", 0.6)
                put("topK", 40)
            }
            requestJson.put("generationConfig", genConfig)

            executeGenerateCall(apiKey, requestJson)
        } catch (e: Exception) {
            Log.e(TAG, "generateWithAudioInput failed", e)
            GeminiResponse.ErrorResponse("NETWORK_ERROR", e.message)
        }
    }

    /**
     * Executes real-time web search and information retrieval using Gemini's native Google Search Grounding.
     */
    suspend fun searchWithGrounding(
        query: String,
        customApiKey: String? = null
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext GeminiResponse.ErrorResponse("API_KEY_MISSING", "Gemini API key missing.")
        }

        try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", """
You are J.A.R.V.I.S. Use Google Search grounding to retrieve real-time, accurate facts, weather, news, prices, sports scores, or answers to this query:
$query

Formatting requirements:
- If the query is in Urdu or asks in Urdu, respond in elegant, fluent Urdu script (اردو رسم الخط).
- If in English, respond in articulate, concise English.
- Provide a clean summary followed by key bullet points.
                                """.trimIndent())
                            })
                        })
                    })
                }
                put("contents", contents)
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("google_search", JSONObject())
                    })
                })
            }

            val endpoint = "$BASE_URL$DEFAULT_MODEL:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(endpoint).post(requestBody).build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.code == 429 || responseString.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || responseString.contains("quota", ignoreCase = true)) {
                return@withContext GeminiResponse.QuotaExceededResponse
            }

            if (!response.isSuccessful) {
                return@withContext GeminiResponse.ErrorResponse("API_ERROR", "HTTP ${response.code}: $responseString")
            }

            parseResponse(responseString)
        } catch (e: Exception) {
            Log.e(TAG, "searchWithGrounding failed", e)
            GeminiResponse.ErrorResponse("NETWORK_ERROR", e.message)
        }
    }

    private fun executeGenerateCall(apiKey: String, requestJson: JSONObject): GeminiResponse {
        val endpoint = "$BASE_URL$DEFAULT_MODEL:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseString = response.body?.string() ?: ""

        if (response.code == 429 || responseString.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || responseString.contains("quota", ignoreCase = true)) {
            Log.w(TAG, "Gemini free daily quota exceeded: ${response.code}")
            return GeminiResponse.QuotaExceededResponse
        }

        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini API error: ${response.code} $responseString")
            return GeminiResponse.ErrorResponse("API_ERROR", "HTTP ${response.code}: $responseString")
        }

        return parseResponse(responseString)
    }

    private fun buildSystemInstruction(): JSONObject {
        return JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", """
You are J.A.R.V.I.S., an advanced, general-purpose command executor and executive AI assistant.
Your core behavior is to analyze whatever directive the user gives you (in Urdu or English, typed or spoken), determine the best way to accomplish it dynamically, and execute it using your general capability tools.
The user should never need to know or care what features are "supported" — you simply understand their intent and handle it.

GENERAL CAPABILITY TOOLS:
1. 'web_search': Use this for ANY query requiring external or real-time information (weather in any city, latest news, facts, current prices, sports scores, exchange rates, guides, internet research). Never fabricate live information.
2. 'save_directive_data': Use this to save and remember ANY item, task, to-do, reminder, note, memory, idea, fact, user preference, or custom record into local device storage.
3. 'query_directive_data': Use this to retrieve or list previously stored items, tasks, notes, or memories when requested.
4. 'update_or_remove_data': Use this to mark tasks as completed, update items, or delete stored directives.
5. 'compute_calculation': Use this to calculate math, conversions, percentages, discounts, or formulas.
6. 'get_device_telemetry': Use this to report current time, date, day of week, and system status.
7. 'device_action': Use this when the user asks to perform an action on the phone:
   - 'call': Dial a phone number (e.g. action='call', target='03001234567')
   - 'sms': Send SMS to a contact or number (e.g. action='sms', target='03001234567', extraData='message text')
   - 'maps': Open Google Maps navigation for a location (e.g. action='maps', target='Liberty Market Lahore')
   - 'alarm': Set an alarm or timer (e.g. action='alarm', target='07:30', extraData='Morning wake up')
   - 'open_url': Open a website link (e.g. action='open_url', target='https://google.com')
   - 'open_app': Open an app like YouTube, WhatsApp, Camera, Settings, Chrome (e.g. action='open_app', target='youtube')

URDU UNDERSTANDING & RTL OUTPUT:
- When the user communicates in Urdu (Urdu Nastaliq script or Roman Urdu), ALWAYS formulate your response in natural, dignified, fluent Urdu written in proper Urdu script (اردو رسم الخط).
- Never answer in Roman Urdu.
- When the user communicates in English, respond in polished, concise English with a refined Jarvis persona.

GENUINELY IMPOSSIBLE ACTIONS:
- If a command requires an action the app genuinely cannot perform due to mobile OS sandbox or security restrictions (such as initiating unauthorized money transfers, restarting the phone, or modifying system hardware directly), DO NOT fail silently or give false confirmation.
- Clearly and politely explain the exact limitation in Urdu (or English if prompted in English):
"معذرت، ڈیوائس کی سیکیورٹی پابندیوں کے باعث میں سسٹم ہارڈویئر یا فنڈز کی براہ راست منتقلی سے قاصر ہوں۔ البتہ میں اس کا نوٹ یا یاد دہانی آپ کے لیے محفوظ کر سکتا ہوں۔"
                    """.trimIndent())
                })
            })
        }
    }

    private fun buildGeneralCapabilityTools(): JSONArray {
        val tools = JSONArray()

        // 1. Web Search Tool
        tools.put(JSONObject().apply {
            put("name", "web_search")
            put("description", "Search the live web for real-time data, weather in any location, news, sports scores, exchange rates, guides, or facts.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The search query to look up on Google Search.")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 2. Save Directive Data Tool
        tools.put(JSONObject().apply {
            put("name", "save_directive_data")
            put("description", "Save and remember any task, to-do, note, reminder, custom data, memory, or fact into local device storage.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("type", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The category of item: 'task', 'note', 'reminder', 'alarm', 'fact', or 'general'.")
                    })
                    put("title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Title or short descriptor of the item.")
                    })
                    put("content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Detailed contents, time specification, or memo text.")
                    })
                    put("tags", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional comma-separated tags or priority (e.g. 'work, urgent').")
                    })
                })
                put("required", JSONArray().apply { put("title") })
            })
        })

        // 3. Query Directive Data Tool
        tools.put(JSONObject().apply {
            put("name", "query_directive_data")
            put("description", "Retrieve previously stored items, tasks, notes, or memories from the local database.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search keywords or empty string to list all stored records.")
                    })
                    put("type", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional filter by type ('all', 'task', 'note', 'reminder').")
                    })
                })
            })
        })

        // 4. Update or Remove Data Tool
        tools.put(JSONObject().apply {
            put("name", "update_or_remove_data")
            put("description", "Mark a directive as completed or delete an item from the vault.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'complete' or 'delete'")
                    })
                    put("identifier", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Title, keyword, or ID of the item to update.")
                    })
                })
                put("required", JSONArray().apply {
                    put("action")
                    put("identifier")
                })
            })
        })

        // 5. Compute Calculation Tool
        tools.put(JSONObject().apply {
            put("name", "compute_calculation")
            put("description", "Perform mathematical calculations, financial percentages, discounts, unit conversions, or formulas.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("expression", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The math expression (e.g. '15% of 12500' or '45 * 18').")
                    })
                    put("result", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Calculated numeric result.")
                    })
                    put("explanation", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Short step-by-step reasoning or formula used.")
                    })
                })
                put("required", JSONArray().apply { put("expression") })
            })
        })

        // 6. Device Telemetry Tool
        tools.put(JSONObject().apply {
            put("name", "get_device_telemetry")
            put("description", "Get the current system time, date, day of week, or local device status.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("metric", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'time', 'date', 'full_telemetry'")
                    })
                })
            })
        })

        // 7. Native Device Action Tool
        tools.put(JSONObject().apply {
            put("name", "device_action")
            put("description", "Perform a real device action via Android Intents: make a call, send an SMS, open Maps, set an alarm, open a website, or launch an app.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The action to perform: 'call', 'sms', 'maps', 'alarm', 'open_url', 'open_app'.")
                    })
                    put("target", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Target phone number, map query, alarm time (HH:MM), URL, or app name.")
                    })
                    put("extraData", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional extra info like SMS text message body or alarm label.")
                    })
                })
                put("required", JSONArray().apply {
                    put("action")
                    put("target")
                })
            })
        })

        return tools
    }

    private fun parseResponse(responseString: String): GeminiResponse {
        try {
            val root = JSONObject(responseString)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return GeminiResponse.ErrorResponse("EMPTY_RESPONSE", "No response candidates returned.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return GeminiResponse.ErrorResponse("NO_CONTENT")
            val parts = content.optJSONArray("parts") ?: return GeminiResponse.ErrorResponse("NO_PARTS")

            var collectedText = ""
            var audioBase64: String? = null
            var audioMimeType: String? = null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)

                // Check for function call
                if (part.has("functionCall")) {
                    val fnCall = part.getJSONObject("functionCall")
                    val fnName = fnCall.getString("name")
                    val argsObj = fnCall.optJSONObject("args") ?: JSONObject()
                    val argsMap = mutableMapOf<String, Any?>()
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.get(key)
                    }
                    return GeminiResponse.FunctionCallResponse(fnName, argsMap, fnCall.toString())
                }

                // Check for inline audio
                if (part.has("inlineData")) {
                    val inlineData = part.getJSONObject("inlineData")
                    val mime = inlineData.optString("mimeType", "")
                    if (mime.startsWith("audio/")) {
                        audioBase64 = inlineData.optString("data", "")
                        audioMimeType = mime
                    }
                }

                // Check for text
                if (part.has("text")) {
                    collectedText += part.getString("text")
                }
            }

            // Extract Google search grounding sources if present
            val sources = mutableListOf<String>()
            val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
            if (groundingMetadata != null) {
                val searchChunks = groundingMetadata.optJSONArray("groundingChunks")
                if (searchChunks != null) {
                    for (i in 0 until searchChunks.length()) {
                        val chunk = searchChunks.optJSONObject(i)
                        val web = chunk?.optJSONObject("web")
                        val uri = web?.optString("uri")
                        if (!uri.isNullOrBlank()) {
                            sources.add(uri)
                        }
                    }
                }
            }

            return GeminiResponse.TextResponse(
                text = collectedText.ifBlank { "ہدایت موصول ہو گئی۔" },
                audioBase64 = audioBase64,
                audioMimeType = audioMimeType,
                sources = sources
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response JSON", e)
            return GeminiResponse.ErrorResponse("PARSE_ERROR", e.message)
        }
    }
}
