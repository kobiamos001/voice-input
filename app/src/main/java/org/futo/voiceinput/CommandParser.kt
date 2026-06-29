package org.futo.voiceinput

import android.content.Context
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

    fun parseAndExecute(context: Context, text: String, onStatusUpdate: (String) -> Unit) {
        val cleanText = text.lowercase().trim()
        Log.d(TAG, "Command received for parsing: $cleanText")

        // פירוק משפט הקלט למילים בודדות
        val inputWords = cleanText.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // 1. קבוצת מילים: צילום מסך
        val screenshotKeywords = listOf("צילום", "תצלם", "צלם", "מסך")

        // 2. קבוצת מילים: כיבוי המכשיר
        val shutdownKeywords = listOf("כיבוי", "תכבה", "לכבות", "כבה", "טלפון", "נגן", "מכשיר")

        // 3. קבוצת מילים: הפעלה מחדש (Reboot)
        val rebootKeywords = listOf("מחדש", "הפעלה", "הפעל", "תפעיל", "נגן", "מכשיר", "טלפון")

        // 4. קבוצת מילים: הפעלת מוזיקה
        val playKeywords = listOf("נגן", "תפעיל", "הפעל", "מוזיקה", "שיר", "להשמיע", "תשמיע")

        // 5. קבוצת מילים: עצירת מוזיקה
        val stopKeywords = listOf("עצור", "תעצור", "תפסיק", "שקט", "מוזיקה", "שיר")

        // חישוב התאמה חכמה לכל קבוצה
        val screenshotScore = countFuzzyMatches(inputWords, screenshotKeywords)
        val shutdownScore = countFuzzyMatches(inputWords, shutdownKeywords)
        val rebootScore = countFuzzyMatches(inputWords, rebootKeywords)
        val playScore = countFuzzyMatches(inputWords, playKeywords)

        // מונע הפעלת פקודת עצירה אם המשתמש אמר מילת הפעלה (למשל "תפעיל מוזיקה")
        val hasPlayIntent = inputWords.any { word ->
            listOf("תפעיל", "נגן", "הפעל", "תשמיע").any { isFuzzyMatch(word, it) }
        }
        val stopScore = if (hasPlayIntent) 0 else countFuzzyMatches(inputWords, stopKeywords)

        // מציאת הציון הגבוה ביותר
        val maxScore = maxOf(screenshotScore, shutdownScore, rebootScore, playScore, stopScore)

        // תנאי סף: דורש לפחות 2 מילות מפתח תואמות (או דומות מאוד)
        if (maxScore < 2) {
            onStatusUpdate(context.getString(R.string.command_unrecognized))
            Log.d(TAG, "Command ignored. Max score ($maxScore) is below the threshold of 2.")
            return
        }

        when {
            maxScore == screenshotScore && screenshotScore >= 2 -> {
                onStatusUpdate(context.getString(R.string.command_executing_screenshot))
                val timeStamp = System.currentTimeMillis()
                runAsRoot("mkdir -p /sdcard/Pictures && screencap -p /sdcard/Pictures/Screenshot_$timeStamp.png")
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
            else -> {
                onStatusUpdate(context.getString(R.string.command_not_understood))
            }
        }
    }
}
