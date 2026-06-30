package org.futo.voiceinput

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import java.io.DataOutputStream

object CommandParser {
    private const val TAG = "CommandParser"

    // חישוב מרחק לוינשטיין (Levenshtein Distance) למדידת דמיון בין מילים
    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val temp = cost
            cost = newCost
            newCost = temp
        }
        return cost[lhsLength]
    }

    // פונקציית השוואה חכמה שמתעלמת משגיאות כתיב קלות
    private fun isFuzzyMatch(word1: String, word2: String): Boolean {
        if (word1 == word2) return true

        // מילים קצרות מדי (פחות מ-3 אותיות) לא נשווה בצורה גמישה כדי למנוע טעויות
        if (word1.length < 3 || word2.length < 3) return word1 == word2

        val distance = levenshteinDistance(word1, word2)

        // הגדרת סף השגיאה המותר: אות אחת במילים קצרות, ועד 2 אותיות במילים ארוכות (6+ אותיות)
        val maxAllowedDistance = if (word2.length >= 6) 2 else 1
        return distance <= maxAllowedDistance
    }

    // ספירת התאמות חכמות בין מילות המשפט למילות המפתח
    private fun countFuzzyMatches(inputWords: List<String>, keywords: List<String>): Int {
        var count = 0
        for (keyword in keywords) {
            if (inputWords.any { isFuzzyMatch(it, keyword) }) {
                count++
            }
        }
        return count
    }

    // הרצת פקודת רוט חסינה לקפיאות
    private fun runAsRoot(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)

            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            Thread {
                try {
                    process.inputStream.bufferedReader().use { it.readText() }
                } catch (e: Exception) {}
            }.start()

            Thread {
                try {
                    process.errorStream.bufferedReader().use { it.readText() }
                } catch (e: Exception) {}
            }.start()

            val exitVal = process.waitFor()
            exitVal == 0
        } catch (e: Exception) {
            Log.e(TAG, "Root execution failed: $command", e)
            false
        }
    }

    // פתיחת הגדרות בצורה בטוחה באמצעות Intent
    private fun openSettingsActivity(context: Context, action: String): Boolean {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings activity: $action", e)
            false
        }
    }

    fun parseAndExecute(context: Context, text: String, onStatusUpdate: (String) -> Unit) {
        val cleanText = text.lowercase().trim()
        Log.d(TAG, "Command received for parsing: $cleanText")

        // פירוק משפט הקלט למילים בודדות
        val inputWords = cleanText.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // --- קבוצות מילים קיים וחדש ---
        val screenshotKeywords = listOf("צילום", "תצלם", "צלם", "מסך")
        val shutdownKeywords = listOf("כיבוי", "תכבה", "לכבות", "כבה", "טלפון", "נגן", "מכשיר")
        val rebootKeywords = listOf("מחדש", "הפעלה", "הפעל", "תפעיל", "נגן", "מכשיר", "טלפון")
        val playKeywords = listOf("נגן", "תפעיל", "הפעל", "מוזיקה", "שיר", "להשמיע", "תשמיע")
        val stopKeywords = listOf("עצור", "תעצור", "תפסיק", "שקט", "מוזיקה", "שיר")
        val screenOnKeywords = listOf("להדליק", "תדליק", "הדלק", "הדלקה", "מסך")
        val screenOffKeywords = listOf("לכבות", "תכבה", "כבה", "כיבוי", "מסך")
        val volumeUpKeywords = listOf("להגביר", "תגביר", "הגבר", "הגברה", "ווליום", "קול", "רעש")
        val volumeDownKeywords = listOf("להנמיך", "תנמיך", "הנמך", "הנמכה", "ווליום", "קול", "להחליש", "תחליש")
        val nextTrackKeywords = listOf("הבא", "קדימה", "שיר", "העבר", "תעביר")
        val prevTrackKeywords = listOf("הקודם", "אחורה", "שיר", "העבר", "תעביר")
        val bluetoothOnKeywords = listOf("להפעיל", "תפעיל", "הפעל", "בלוטוס", "בלוטות", "שן", "כחולה")
        val bluetoothOffKeywords = listOf("לכבות", "תכבה", "כבה", "כיבוי", "בלוטוס", "בלוטות", "שן", "כחולה")
        val marketKeywords = listOf("חנות", "אפליקציות", "מרקט", "הורדות", "hmarket", "החנות")
        val galleryKeywords = listOf("גלריה", "תמונות", "אלבום", "גלריית", "הגלריה", "תצלומים")
        val radioOnKeywords = listOf("להפעיל", "תפעיל", "הפעל", "רדיו", "לשמוע", "שמע", "תדליק", "הדלק", "פתח", "לפתוח")
        val fileTransferKeywords = listOf("העברת", "קבצים", "להעביר", "קובץ", "file", "transfer", "transporter", "tfile")

        // קבוצות הגדרות (כלליות וספציפיות)
        val bluetoothSettingsKeywords = listOf("הגדרות", "בלוטוס", "בלוטות", "פתח", "לפתוח")
        val appsSettingsKeywords = listOf("הגדרות", "יישומים", "אפליקציות", "ישומים", "פתח", "לפתוח")
        val generalSettingsKeywords = listOf("הגדרות", "מכשיר", "טלפון", "הגדרות המכשיר", "פתח", "לפתוח", "תפתח")

        // חישוב התאמה חכמה לכל קבוצה
        val screenshotScore = countFuzzyMatches(inputWords, screenshotKeywords)
        val shutdownScore = countFuzzyMatches(inputWords, shutdownKeywords)
        val rebootScore = countFuzzyMatches(inputWords, rebootKeywords)
        val playScore = countFuzzyMatches(inputWords, playKeywords)
        val screenOnScore = countFuzzyMatches(inputWords, screenOnKeywords)
        val screenOffScore = countFuzzyMatches(inputWords, screenOffKeywords)
        val volumeUpScore = countFuzzyMatches(inputWords, volumeUpKeywords)
        val volumeDownScore = countFuzzyMatches(inputWords, volumeDownKeywords)
        val nextTrackScore = countFuzzyMatches(inputWords, nextTrackKeywords)
        val prevTrackScore = countFuzzyMatches(inputWords, prevTrackKeywords)
        val bluetoothOnScore = countFuzzyMatches(inputWords, bluetoothOnKeywords)
        val bluetoothOffScore = countFuzzyMatches(inputWords, bluetoothOffKeywords)
        val marketScore = countFuzzyMatches(inputWords, marketKeywords)
        val galleryScore = countFuzzyMatches(inputWords, galleryKeywords)
        val radioOnScore = countFuzzyMatches(inputWords, radioOnKeywords)
        val fileTransferScore = countFuzzyMatches(inputWords, fileTransferKeywords)
        
        // ציוני הגדרות
        val bluetoothSettingsScore = countFuzzyMatches(inputWords, bluetoothSettingsKeywords)
        val appsSettingsScore = countFuzzyMatches(inputWords, appsSettingsKeywords)
        val generalSettingsScore = countFuzzyMatches(inputWords, generalSettingsKeywords)

        // מונע הפעלת פקודת עצירה אם המשתמש אמר מילת הפעלה (למשל "תפעיל מוזיקה")
        val hasPlayIntent = inputWords.any { word ->
            listOf("תפעיל", "נגן", "הפעל", "תשמיע").any { isFuzzyMatch(word, it) }
        }
        val stopScore = if (hasPlayIntent) 0 else countFuzzyMatches(inputWords, stopKeywords)

        // מציאת הציון הגבוה ביותר מכלל הפעולות
        val maxScore = maxOf(
            screenshotScore, shutdownScore, rebootScore, playScore, stopScore,
            screenOnScore, screenOffScore, volumeUpScore, volumeDownScore,
            nextTrackScore, prevTrackScore, bluetoothOnScore, bluetoothOffScore,
            bluetoothSettingsScore, appsSettingsScore, generalSettingsScore,
            marketScore, galleryScore, radioOnScore, fileTransferScore
        )

        // תנאי סף: דורש לפחות 2 מילות מפתח תואמות (או דומות מאוד)
        if (maxScore < 2) {
            onStatusUpdate(context.getString(R.string.command_unrecognized))
            Log.d(TAG, "Command ignored. Max score ($maxScore) is below the threshold of 2.")
            return
        }

        when {
            maxScore == screenshotScore && screenshotScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_screenshot))
                runAsRoot("input keyevent 120")
            }
            maxScore == shutdownScore && shutdownScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_shutdown))
                runAsRoot("svc power shutdown || reboot -p")
            }
            maxScore == rebootScore && rebootScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_reboot))
                runAsRoot("reboot")
            }
            maxScore == playScore && playScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_play))
                runAsRoot("input keyevent 126")
            }
            maxScore == stopScore && stopScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_stop))
                runAsRoot("input keyevent 127")
            }
            maxScore == screenOnScore && screenOnScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_screen_on))
                runAsRoot("input keyevent 224") // KEYCODE_WAKEUP
            }
            maxScore == screenOffScore && screenOffScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_screen_off))
                runAsRoot("input keyevent 223") // KEYCODE_SLEEP
            }
            maxScore == volumeUpScore && volumeUpScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_volume_up))
                runAsRoot("input keyevent 24 && input keyevent 24 && input keyevent 24") // דילוג של 3 שלבים
            }
            maxScore == volumeDownScore && volumeDownScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_volume_down))
                runAsRoot("input keyevent 25 && input keyevent 25 && input keyevent 25") // דילוג של 3 שלבים
            }
            maxScore == nextTrackScore && nextTrackScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_next_track))
                runAsRoot("input keyevent 87") // KEYCODE_MEDIA_NEXT
            }
            maxScore == prevTrackScore && prevTrackScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_prev_track))
                runAsRoot("input keyevent 88") // KEYCODE_MEDIA_PREVIOUS
            }
            maxScore == bluetoothOnScore && bluetoothOnScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_bluetooth_on))
                runAsRoot("svc bluetooth enable")
            }
            maxScore == bluetoothOffScore && bluetoothOffScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_bluetooth_off))
                runAsRoot("svc bluetooth disable")
            }
            
            // הגדרות ספציפיות מקבלות קדימות
            maxScore == bluetoothSettingsScore && bluetoothSettingsScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_bluetooth_settings))
                openSettingsActivity(context, Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            maxScore == appsSettingsScore && appsSettingsScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_apps_settings))
                openSettingsActivity(context, Settings.ACTION_APPLICATION_SETTINGS)
            }
            maxScore == generalSettingsScore && generalSettingsScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_settings))
                openSettingsActivity(context, Settings.ACTION_SETTINGS)
            }
            
            maxScore == marketScore && marketScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_open_market))
                runAsRoot("monkey -p com.hpower.hmarket -c android.intent.category.LAUNCHER 1")
            }
            maxScore == galleryScore && galleryScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_open_gallery))
                runAsRoot("monkey -p com.hpower.koshergallery -c android.intent.category.LAUNCHER 1")
            }
            maxScore == radioOnScore && radioOnScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_radio_on))
                runAsRoot("monkey -p com.android.fmradio -c android.intent.category.LAUNCHER 1")
            }
            maxScore == fileTransferScore && fileTransferScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_open_file_transfer))
                runAsRoot("monkey -p com.tans.tfiletransporter -c android.intent.category.LAUNCHER 1")
            }
            else -> {
                onStatusUpdate(context.getString(R.string.command_not_understood))
            }
        }
    }
}
