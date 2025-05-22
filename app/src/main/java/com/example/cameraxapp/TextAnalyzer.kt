package com.example.cameraxapp

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextAnalyzer(private val onTextDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var lastUpdateTime = 0L
    private val updateInterval = 1000L

    private var lastText: String = ""


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
                val newText = visionText.text.trim()
                if (newText.isNotEmpty() && newText != lastText) {
                    lastText = newText
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
}
