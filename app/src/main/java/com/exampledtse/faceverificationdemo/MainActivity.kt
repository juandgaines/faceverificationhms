package com.exampledtse.faceverificationdemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationAnalyzer
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationAnalyzerFactory
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var analyzer: MLFaceVerificationAnalyzer
    private lateinit var templateFrame: MLFrame
    private lateinit var compareFrame: MLFrame


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        analyzer = MLFaceVerificationAnalyzerFactory.getInstance().faceVerificationAnalyzer

        compared_image.setOnClickListener {

            dispatchTakePictureIntent(REQUEST_IMAGE_CAPTURE_COMPARED)
        }
        reference_image.setOnClickListener {
            dispatchTakePictureIntent(REQUEST_IMAGE_CAPTURE_REFERENCE)
        }
    }

    private fun dispatchTakePictureIntent(intentCode: Int) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, intentCode)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE_REFERENCE -> {
                    reference_image.setImageBitmap(imageBitmap)
                    templateFrame = MLFrame.fromBitmap(imageBitmap)

                    val results = analyzer.setTemplateFace(templateFrame)

                    results.forEach { templateResult ->
                        Log.d(LOG_TAG, "${templateResult.faceInfo}")
                    }

                }
                REQUEST_IMAGE_CAPTURE_COMPARED -> {
                    compared_image.setImageBitmap(imageBitmap)
                    compareFrame = MLFrame.fromBitmap(imageBitmap)

                    analyzer.asyncAnalyseFrame(compareFrame)
                        .addOnSuccessListener {verificationResult->
                            if(verificationResult[0].similarity>THRESHOLD)
                                Toast.makeText(this, "Success verification", Toast.LENGTH_SHORT).show()
                            else
                                Toast.makeText(this, "Failed verification", Toast.LENGTH_SHORT).show()

                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed verification", Toast.LENGTH_SHORT).show()
                        }

                }
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        analyzer.stop()
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE_REFERENCE = 1990
        const val REQUEST_IMAGE_CAPTURE_COMPARED = 1991
        val LOG_TAG: String = MainActivity::class.java.simpleName
        const val THRESHOLD=0.7231
    }
}