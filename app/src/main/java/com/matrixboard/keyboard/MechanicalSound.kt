package com.matrixboard.keyboard

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** 4 synthesised mechanical-switch sounds. No audio files needed. */
object MechanicalSound {
    const val CLICKY = 0
    const val THOCK = 1
    const val LINEAR = 2
    const val POP = 3

    val NAMES = arrayOf("Clicky", "Thock", "Linear", "Pop")

    fun play(profile: Int) {
        try {
            val sr = 44100
            val (durMs, data) = when (profile) {
                CLICKY -> 35 to clicky(sr)
                THOCK -> 90 to thock(sr)
                LINEAR -> 45 to linearTap(sr)
                else -> 70 to pop(sr)
            }
            val n = sr * durMs / 1000
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(n * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(data, 0, minOf(n, data.size))
            track.play()
            // Static track is short; release after it finishes.
            Thread {
                try { Thread.sleep(durMs + 20L) } catch (_: Exception) {}
                try { track.stop(); track.release() } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {
            // Sound must never break typing.
        }
    }

    private fun sine(sr: Int, freq: Double, ms: Int, vol: Double, decay: Double): ShortArray {
        val n = sr * ms / 1000
        return ShortArray(n) { i ->
            val t = i.toDouble() / sr
            val env = Math.exp(-decay * i.toDouble() / n)
            (sin(2 * PI * freq * t) * vol * env * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun noise(sr: Int, ms: Int, vol: Double, decay: Double): ShortArray {
        val n = sr * ms / 1000
        return ShortArray(n) { i ->
            val env = Math.exp(-decay * i.toDouble() / n)
            (Random.nextInt(-32767, 32767) * vol * env).toInt().toShort()
        }
    }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val n = maxOf(a.size, b.size)
        return ShortArray(n) { i ->
            val x = (if (i < a.size) a[i].toInt() else 0) + (if (i < b.size) b[i].toInt() else 0)
            x.coerceIn(-32767, 32767).toShort()
        }
    }

    private fun clicky(sr: Int) = mix(noise(sr, 25, 0.9, 6.0), sine(sr, 2600.0, 20, 0.35, 8.0))
    private fun thock(sr: Int) = mix(sine(sr, 170.0, 90, 0.9, 3.0), noise(sr, 40, 0.25, 4.0))
    private fun linearTap(sr: Int) = sine(sr, 750.0, 45, 0.35, 4.0)
    private fun pop(sr: Int): ShortArray {
        val n = sr * 70 / 1000
        return ShortArray(n) { i ->
            val t = i.toDouble() / sr
            val freq = 650.0 - 350.0 * i.toDouble() / n
            val env = Math.exp(-4.0 * i.toDouble() / n)
            (sin(2 * PI * freq * t) * 0.7 * env * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
