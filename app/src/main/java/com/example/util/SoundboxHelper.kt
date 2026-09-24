package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

class SoundboxHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale("en", "IN")
            }
        }
    }

    /**
     * Announces received payment audibly just like PhonePe Smart SoundBox.
     */
    fun announcePayment(amount: Double, receiverName: String, isCredit: Boolean = true, inHindi: Boolean = false) {
        triggerHapticSuccess()

        val textToSpeak = if (isCredit) {
            if (inHindi) {
                "ऑफलाइन-पे पर %.0f रुपये प्राप्त हुए।".format(amount)
            } else {
                "Rupees %.0f received successfully on OfflinePay!".format(amount)
            }
        } else {
            if (inHindi) {
                "%.0f रुपये का भुगतान सफल रहा।".format(amount)
            } else {
                "Payment of Rupees %.0f to %s successful on OfflinePay.".format(amount, receiverName)
            }
        }

        if (isTtsReady) {
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "SoundboxPaymentAnnounce")
        }
    }

    fun triggerHapticSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(120)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
