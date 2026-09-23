package com.example.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.util.Log

data class DeviceActionResult(
    val success: Boolean,
    val spokenResponse: String,
    val displayResponse: String,
    val intentAction: String? = null
)

/**
 * Handles executing real native Android Intents for device commands:
 * - Phone calls / Dialing
 * - SMS messaging
 * - Maps / Navigation
 * - Alarms / Timers
 * - Web URLs
 * - Launching installed apps
 * If an action is truly restricted by Android security boundaries, provides courteous explanations in Urdu and English.
 */
class DeviceActionHandler(private val context: Context) {

    companion object {
        private const val TAG = "DeviceActionHandler"
    }

    /**
     * Dials a phone number using Android's native ACTION_DIAL.
     */
    fun dialPhone(phoneNumber: String, isUrdu: Boolean = true): DeviceActionResult {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) {
            val err = if (isUrdu) "براہ کرم درست فون نمبر فراہم کریں۔" else "Please provide a valid phone number."
            return DeviceActionResult(false, err, "⚠️ $err")
        }

        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            val spoken = if (isUrdu) "$cleanNumber پر کال ملائی جا رہی ہے۔" else "Dialing $cleanNumber now."
            val display = if (isUrdu) "📞 کال ڈائلر:\n• نمبر: $cleanNumber\n• اسٹیٹس: کال اسکرین کھولی جا رہی ہے" else "📞 Phone Dialer:\n• Number: $cleanNumber\n• Status: Opening Phone App"
            DeviceActionResult(true, spoken, display, "ACTION_DIAL")
        } catch (e: Exception) {
            Log.e(TAG, "Error dialing phone", e)
            val err = if (isUrdu) "فون ایپ کھولنے میں دشواری پیش آئی: ${e.message}" else "Unable to open phone dialer: ${e.message}"
            DeviceActionResult(false, err, "⚠️ $err")
        }
    }

    /**
     * Prepares an SMS using ACTION_SENDTO.
     */
    fun sendSms(phoneNumber: String, message: String, isUrdu: Boolean = true): DeviceActionResult {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        return try {
            val uri = if (cleanNumber.isNotBlank()) Uri.parse("smsto:$cleanNumber") else Uri.parse("smsto:")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            val spoken = if (isUrdu) "پیغام بھیجنے کے لیے میسجز ایپ کھولی جا رہی ہے۔" else "Opening messages app to send text."
            val display = if (isUrdu) "💬 ایس ایم ایس (SMS):\n• نمبر: ${cleanNumber.ifBlank { "منتخب کریں" }}\n• متن: $message" else "💬 SMS Compose:\n• Recipient: ${cleanNumber.ifBlank { "Select contact" }}\n• Message: $message"
            DeviceActionResult(true, spoken, display, "ACTION_SENDTO")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening SMS", e)
            val err = if (isUrdu) "پیغام ایپ کھولنے میں خرابی: ${e.message}" else "Failed to open SMS app: ${e.message}"
            DeviceActionResult(false, err, "⚠️ $err")
        }
    }

    /**
     * Opens location / navigation in Google Maps.
     */
    fun openMaps(locationQuery: String, isUrdu: Boolean = true): DeviceActionResult {
        return try {
            val encoded = Uri.encode(locationQuery)
            val geoUri = Uri.parse("geo:0,0?q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            val spoken = if (isUrdu) "نقشے پر $locationQuery تلاش کیا جا رہا ہے۔" else "Locating $locationQuery on maps."
            val display = if (isUrdu) "🗺 نقشہ جات (Maps):\n• مقام: $locationQuery\n• نیویگیشن شروع کی جا رہی ہے" else "🗺 Google Maps:\n• Location: $locationQuery\n• Opening map navigation"
            DeviceActionResult(true, spoken, display, "ACTION_VIEW_MAPS")
        } catch (e: Exception) {
            // Fallback to web maps URL
            try {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(locationQuery)}")
                val intent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                val spoken = if (isUrdu) "نقشے کا ویب لنک کھولا جا رہا ہے۔" else "Opening web map link."
                DeviceActionResult(true, spoken, "🗺 Web Maps: $locationQuery", "ACTION_VIEW_MAPS")
            } catch (ex: Exception) {
                Log.e(TAG, "Error opening maps", ex)
                val err = if (isUrdu) "نقشہ کھولنے میں دشواری پیش آئی۔" else "Could not open maps application."
                DeviceActionResult(false, err, "⚠️ $err")
            }
        }
    }

    /**
     * Sets an Android system alarm or timer via AlarmClock Intent.
     */
    fun setAlarm(hour: Int, minute: Int, message: String = "Jarvis Directive", isUrdu: Boolean = true): DeviceActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            val timeDisplay = "%02d:%02d".format(hour, minute)
            val spoken = if (isUrdu) "$timeDisplay بجے کا الارم کامیابی سے لگا دیا گیا ہے۔" else "Alarm set for $timeDisplay."
            val display = if (isUrdu) "⏰ الارم مقرر ہو گیا:\n• وقت: $timeDisplay\n• عنوان: $message" else "⏰ Alarm Configured:\n• Time: $timeDisplay\n• Label: $message"
            DeviceActionResult(true, spoken, display, "ACTION_SET_ALARM")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm", e)
            val err = if (isUrdu) "الارم لگانے میں خرابی پیش آئی: ${e.message}" else "Unable to set alarm: ${e.message}"
            DeviceActionResult(false, err, "⚠️ $err")
        }
    }

    /**
     * Opens a web URL in browser.
     */
    fun openUrl(url: String, isUrdu: Boolean = true): DeviceActionResult {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            val spoken = if (isUrdu) "ویب پیج کھولا جا رہا ہے۔" else "Opening web link."
            val display = if (isUrdu) "🌐 ویب لنک:\n$cleanUrl" else "🌐 Web Navigation:\n$cleanUrl"
            DeviceActionResult(true, spoken, display, "ACTION_VIEW")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening url", e)
            val err = if (isUrdu) "لنک کھولنے میں ناکامی: ${e.message}" else "Failed to open link: ${e.message}"
            DeviceActionResult(false, err, "⚠️ $err")
        }
    }

    /**
     * Launches an installed application by common name or package.
     */
    fun launchApp(appName: String, isUrdu: Boolean = true): DeviceActionResult {
        val q = appName.lowercase().trim()
        val packageMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "یوٹیوب" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "واٹس ایپ" to "com.whatsapp",
            "camera" to "camera",
            "کیمرہ" to "camera",
            "chrome" to "com.android.chrome",
            "کروم" to "com.android.chrome",
            "settings" to "settings",
            "سیٹنگز" to "settings",
            "calculator" to "calculator",
            "کیلکولیٹر" to "calculator"
        )

        try {
            if (q == "camera" || q == "کیمرہ") {
                val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                val spoken = if (isUrdu) "کیمرہ کھولا جا رہا ہے۔" else "Opening camera."
                return DeviceActionResult(true, spoken, "📷 Camera Online", "IMAGE_CAPTURE")
            }

            if (q == "settings" || q == "سیٹنگز") {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                val spoken = if (isUrdu) "ڈیوائس سیٹنگز کھولی جا رہی ہیں۔" else "Opening device settings."
                return DeviceActionResult(true, spoken, "⚙️ System Settings", "ACTION_SETTINGS")
            }

            val pkg = packageMap[q]
            if (pkg != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    val spoken = if (isUrdu) "$appName ایپ کھولی جا رہی ہے۔" else "Opening $appName."
                    return DeviceActionResult(true, spoken, "🚀 Launched $appName", "LAUNCH_APP")
                }
            }

            // Fallback search in Play Store if not installed
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(appName)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(playStoreIntent)
            val spoken = if (isUrdu) "$appName تلاش کرنے کے لیے پلے اسٹور کھولا جا رہا ہے۔" else "Searching for $appName on Play Store."
            return DeviceActionResult(true, spoken, "🔍 Play Store Search: $appName", "PLAY_STORE")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: $appName", e)
            val err = if (isUrdu) "$appName کھولنے میں دشواری: ${e.message}" else "Unable to launch $appName: ${e.message}"
            return DeviceActionResult(false, err, "⚠️ $err")
        }
    }
}
