package com.example.tickclockapp

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TickClockScreen()
                }
            }
        }
    }
}

@Composable
fun TickClockScreen() {
    var isRunning by remember { mutableStateOf(false) }
    var totalSeconds by remember { mutableIntStateOf(0) }
    var cycleSeconds by remember { mutableIntStateOf(0) }
    val activity = LocalContext.current as? ComponentActivity

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning) {
                // Tone logic based on 30-sec cycle
                cycleSeconds++
                if (cycleSeconds > 30) {
                    cycleSeconds = 1
                }
                
                playToneForSecond(cycleSeconds)
                
                // Cumulative timer
                totalSeconds++
                
                delay(1000)
            }
        }
    }

    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    val timeFormatted = "%02d:%02d".format(minutes, remainingSeconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier
                    .width(140.dp)
                    .height(80.dp)
            ) {
                Text(if (isRunning) "Pause" else "Start", fontSize = 20.sp)
            }
            Button(
                onClick = { activity?.finish() },
                modifier = Modifier
                    .width(140.dp)
                    .height(80.dp)
            ) {
                Text("Exit", fontSize = 20.sp)
            }
        }
        
        Text(
            text = timeFormatted,
            fontSize = 36.sp, // Doubled from 18.sp
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

private fun playToneForSecond(cycleSecond: Int) {
    val freq392 = 392.0 // G4
    val freq440 = 440.0 // A4
    val freq523 = 523.0 // C5
    val freq659 = 659.0 // E5
    
    when (cycleSecond) {
        // 1-13: Short tones (392 Hz)
        in 1..13 -> generateTone(freq392, 100)
        // 14: Long tone spanning 2 seconds (440 Hz)
        14 -> generateTone(freq440, 1800)
        15 -> { }
        // 16-25: Silent
        in 16..25 -> { }
        // 26-28: 3 short (523 Hz)
        in 26..28 -> generateTone(freq523, 100)
        // 29: Long tone spanning 2 seconds (659 Hz)
        29 -> generateTone(freq659, 1800)
        30 -> { }
    }
}

private fun generateTone(freqHz: Double, durationMs: Int) {
    val sampleRate = 44100
    val numSamples = (durationMs * sampleRate / 1000)
    val sample = DoubleArray(numSamples)
    val generatedSnd = ByteArray(2 * numSamples)

    // Fade-in and Fade-out parameters (to prevent clicking)
    val fadeDurationMs = 10
    val fadeSamples = (fadeDurationMs * sampleRate / 1000)

    for (i in 0 until numSamples) {
        var amplitude = 1.0
        
        // Apply Fade-in
        if (i < fadeSamples) {
            amplitude = i.toDouble() / fadeSamples
        } 
        // Apply Fade-out
        else if (i > numSamples - fadeSamples) {
            amplitude = (numSamples - i).toDouble() / fadeSamples
        }

        sample[i] = amplitude * sin(2.0 * PI * i.toDouble() / (sampleRate.toDouble() / freqHz))
    }

    var idx = 0
    for (dVal in sample) {
        val valShort = (dVal * 32767).toInt().toShort()
        generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
        generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
    }

    val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setBufferSizeInBytes(generatedSnd.size)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    audioTrack.setVolume(AudioTrack.getMaxVolume())
    audioTrack.write(generatedSnd, 0, generatedSnd.size)
    audioTrack.play()
    
    // Clean up
    Thread {
        Thread.sleep(durationMs.toLong() + 200)
        audioTrack.stop()
        audioTrack.release()
    }.start()
}
