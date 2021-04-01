package com.exampledtse.faceverificationdemo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationAnalyzer
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationAnalyzerFactory
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessCapture
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessCaptureResult
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var analyzer: MLFaceVerificationAnalyzer
    private lateinit var templateFrame: MLFrame
    private lateinit var compareFrame: MLFrame


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            );
        }

        analyzer = MLFaceVerificationAnalyzerFactory.getInstance().faceVerificationAnalyzer

        compared_image.setOnClickListener {

            val callback = object : MLLivenessCapture.Callback {
                override fun onSuccess(result: MLLivenessCaptureResult?) {

                    if (result?.isLive!!) {
                        //Liveness detection throuw is a live image
                        val imageBitmap = result?.bitmap
                        imageBitmap?.let {
                            compared_image.setImageBitmap(imageBitmap)
                            compareFrame = MLFrame.fromBitmap(imageBitmap)

                            analyzer.asyncAnalyseFrame(compareFrame)
                                .addOnSuccessListener { verificationResult ->
                                    if (verificationResult[0].similarity > THRESHOLD)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Success verification",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    else
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Failed verification",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Failed verification",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                }
                        }
                    } else {
                        //Liveness detection throuw is not real image of user
                        Toast.makeText(this@MainActivity, "Liveness detection failed.", Toast.LENGTH_SHORT).show()
                    }

                }

                override fun onFailure(errorCode: Int) {

                    Log.e(LOG_TAG,"Liveness detection error code $errorCode")

                }

            }

            val capture = MLLivenessCapture.getInstance();
            capture.startDetect(this, callback)
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
            }
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission needed to continue", Toast.LENGTH_LONG)
                    .show();
                finish()
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
        const val REQUEST_CAMERA_PERMISSION = 1992
        val LOG_TAG: String = MainActivity::class.java.simpleName
        const val THRESHOLD = 0.7231
    }
}