package com.example.cameraxapp.textRecognition

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.atan2

class TextAnalyzer(private val onTextDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var lastUpdateTime = 0L
    private val updateInterval = 1000L

//    private var lastText: String = ""


    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < updateInterval) {
            imageProxy.close()
            return
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val newText = extractHorizontalText(visionText)
                if (newText.isNotEmpty() ) { //&& newText != lastText
//                    lastText = newText
                    lastUpdateTime = currentTime
                    onTextDetected(newText)
                } else {
                    // Don't emit if text is empty or same as last
                    Log.d("TextAnalyzer", "Text unchanged or empty")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TextAnalyzer", "Text recognition failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun extractHorizontalText(visionText: com.google.mlkit.vision.text.Text): String {
        val horizontalLines = mutableListOf<String>()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val points = line.cornerPoints
                if (points != null && points.size >= 2) {
                    val dx = points[1].x - points[0].x
                    val dy = points[1].y - points[0].y
                    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                    val normalizedAngle = (angle + 360) % 360

                    if (normalizedAngle in 75.0..105.0 || normalizedAngle in 255.0..285.0) {
                        continue // skip vertical lines
                    }
                }
                horizontalLines.add(line.text)
            }
        }

        return horizontalLines.joinToString("\n").trim()
    }

}