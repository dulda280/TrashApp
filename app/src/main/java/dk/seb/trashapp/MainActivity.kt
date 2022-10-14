package dk.seb.trashapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import dk.seb.trashapp.databinding.ActivityMainBinding
import dk.seb.trashapp.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.min

typealias LumaListener = (luma: Double) -> Unit


class MainActivity : AppCompatActivity() {
    lateinit var result: TextView
    private lateinit var confidence:TextView
    private lateinit var imageView: ImageView
    private lateinit var picture: Button
    private lateinit var binding: ActivityMainBinding
    private val imageSize = 224


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        result = findViewById(R.id.result)
        confidence = findViewById(R.id.confidence)
        imageView = findViewById(R.id.imageView)
        picture = findViewById(R.id.image_capture_button)


        // Set up the listeners for take photo and video capture buttons
        binding.imageCaptureButton.setOnClickListener {
            // Request camera permissions
            if (allPermissionsGranted()) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                // StartActivityForResult to be changed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                startActivityForResult(cameraIntent, 1)
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }

        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 3 && resultCode == RESULT_OK) {
            setContentView(binding.root)
            var image:Bitmap = data?.extras?.get("data") as Bitmap
            val dimensions = min(image.width, image.height)
            image = ThumbnailUtils.extractThumbnail(image, dimensions, dimensions)

            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false)

            binding.imageView.setImageBitmap(image)
            classifyImage(image)


        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun classifyImage(image: Bitmap) {
        val model = Model.newInstance(applicationContext)

    // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        var byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixValues: IntArray = intArrayOf(imageSize*imageSize)
        image.getPixels(pixValues, 0, image.width, 0, 0, image.width, image.height)
        var pixel = 0

        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val pxval = pixValues[pixel++]
                byteBuffer.putFloat((pxval shr 16 and 0xFF) * (1F / 255F))
                byteBuffer.putFloat((pxval shr 8 and 0xFF) * (1F / 255F))
                byteBuffer.putFloat((pxval and 0xFF) * (1F / 255F))
            }
        }

        inputFeature0.loadBuffer(byteBuffer)
    // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer
        val confidences: FloatArray = outputFeature0.floatArray

        var maxPos = 0
        var maxConfidence: Float = 0F
        for (i in confidences.indices)
            if(confidences[i] > maxConfidence){
                maxConfidence = confidences[i]
                maxPos = i

            }

        //var classes: String = {}


        val classes = Array<String>(5){"Cardboard"; "Glass"; "Metal"; "Paper"; "Plastic" }

        result.text = classes[maxPos]

        var t = ""
        for(i in classes.indices){
            t += String.format("%t: %.1f%% \n", classes[i], confidences[i] * 100)
        }
        confidence.text = t
        Toast.makeText(applicationContext, classes[maxPos], Toast.LENGTH_LONG).show()



    // Releases model resources if no longer used.
        model.close()
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(this,
                    "Permissions granted by the user.",
                    Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

