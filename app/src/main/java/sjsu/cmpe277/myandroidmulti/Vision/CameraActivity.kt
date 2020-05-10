package sjsu.cmpe277.myandroidmulti.Vision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_camera.*
import sjsu.cmpe277.myandroidmulti.R
import sjsu.cmpe277.myandroidmulti.databinding.ActivityCameraBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val REQUEST_CODE_PERMISSIONS = 10

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imagePreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis

    private lateinit var imageCapture: ImageCapture

    private lateinit var videoCapture: VideoCapture

    private lateinit var previewView: PreviewView

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var outputDirectory: File

    private lateinit var cameraInfo: CameraInfo
    private lateinit var cameraControl: CameraControl

    private var tfLiteClassifier: TFLiteClassifier = TFLiteClassifier(this)
    private val objectDetectorConfig = TFLiteObjectDetectionAnalyzer.Config(
        minimumConfidence = 0.5f,
        numDetection = 10,
        inputSize = 300,
        isQuantized = true,
        modelFile = "detect.tflite",
        labelsFile = "labelmap.txt"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setContentView(R.layout.activity_camera)

        previewView = binding.previewView

        if (allPermissionsGranted()) {
            previewView.post { startCamera() }
            previewView.addOnLayoutChangeListener{ _, _, _, _, _, _, _, _, _ ->
                updateTransform()
            }

        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        outputDirectory = getOutputDirectory(this)

        binding.cameraCaptureButton.setOnClickListener {
            takePicture()
        }

        tfLiteClassifier.initialize()
            .addOnSuccessListener {
                   Log.i(TAG, "Success in setting up the classifier.")
            }
            .addOnFailureListener {
                    e -> Log.e(TAG, "Error in setting up the classifier.", e)
            }

    }

    //ref: https://developer.android.com/training/camerax/take-photo
    private fun takePicture() {
        val file = createFile(
            outputDirectory,
            FILENAME,
            PHOTO_EXTENSION
        )
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val msg = "Photo capture succeeded: ${file.absolutePath}"
                Log.d(TAG, msg)
                previewView.post {
                    //Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    Snackbar.make(previewView, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                val msg = "Photo capture failed: ${exception.message}"
                previewView.post {
                    //Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    Snackbar.make(previewView, msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show()
                }
            }
        })
    }

    private fun startCamera() {

        bindCameraUseCases()


    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = previewView.display.rotation

        //A CameraSelector instance will be created and passed to bindToLifecycle function.
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        // Bind the CameraProvider to the LifeCycleOwner
        //need an instance of ProcessCameraProvider
        cameraProviderFuture = ProcessCameraProvider.getInstance(this) //will be obtained asynchronously using the static method
        cameraProviderFuture.addListener(Runnable {
            // CameraProvider
            val cameraProvider = cameraProviderFuture.get()
            imagePreview = Preview.Builder()
                .build()

            imageCapture = ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)//Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()
//                .also {
//                    it.setAnalyzer(executor, LuminosityAnalyzer { luma ->
//                        // Values returned from our analyzer are passed to the attached listener
//                        // We log image analysis results here - you should do something useful
//                        // instead!
//                        binding.resultslabel.text = luma.toString()
//                        Log.i(TAG, "Average luminosity: $luma")
//                    })
//                }

            //Object detection analyzer
            imageAnalysis.setAnalyzer(executor, TFLiteObjectDetectionAnalyzer(this,objectDetectorConfig,::onDetectionResult))

            //Using TFLite model for image classification
//            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
//                val rotationDegrees = image.imageInfo.rotationDegrees
//                // insert your code here.
//                val bitmap = image.toBitmap()
//                tfLiteClassifier.classifyAsync(bitmap)
//                    .addOnSuccessListener {
//                        binding.resultslabel.text = it
//                        image.close()
//                    }
//                    .addOnFailureListener { e ->
//                        Log.e(TAG, "Error in the classifier.", e)
//                    }
//            })
                //CameraX produces images in YUV_420_888 format.

            //A CameraSelector instance will be created and passed to bindToLifecycle function.
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, imageCapture, imagePreview)
            imagePreview.setSurfaceProvider(previewView.createSurfaceProvider(camera.cameraInfo))
            cameraInfo = camera.cameraInfo
            cameraControl = camera.cameraControl

        }, ContextCompat.getMainExecutor(this))
    }

    private fun onDetectionResult(result: TFLiteObjectDetectionAnalyzer.Result) {
        //binding.result_overlay.
        //binding.resultOverlay.updateResults(result)
        //result_overlay.updateResults(result)
        detectionresult_overlay.updateResults(result)
    }

    fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    //ref: https://developer.android.com/training/camerax/preview
    private fun bindPreview(cameraProvider : ProcessCameraProvider) {
//        var preview : Preview = Preview.Builder()
//            .build()
//        var cameraSelector : CameraSelector = CameraSelector.Builder()
//            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//            .build()
//        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
//        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.cameraInfo))

        imagePreview = Preview.Builder()
            .build()

        imageCapture = ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
            val rotationDegrees = image.imageInfo.rotationDegrees
            // insert your code here.
            //LuminosityAnalyzer()
        })//CameraX produces images in YUV_420_888 format.

        //A CameraSelector instance will be created and passed to bindToLifecycle function.
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, imageCapture, imagePreview)
        imagePreview.setSurfaceProvider(previewView.createSurfaceProvider(camera.cameraInfo))
        cameraInfo = camera.cameraInfo
        cameraControl = camera.cameraControl

    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun updateTransform() {
        // TODO: Implement camera viewfinder transformations
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                previewView.post { startCamera() }
            } else {
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Our custom image analysis class.
     *
     * <p>All we need to do is override the function `analyze` with our desired operations. Here,
     * we compute the average luminosity of the image by looking at the Y plane of the YUV frame.
     */

    private class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        /**
         * Used to add listeners that will be called with each luma computed
         */
        fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

        /**
         * Helper extension function used to extract a byte array from an image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        /**
         * Analyzes an image to produce a result.
         *
         * <p>The caller is responsible for ensuring this analysis method can be executed quickly
         * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
         * images will not be acquired and analyzed.
         *
         * <p>The image passed to this method becomes invalid after this method returns. The caller
         * should not store external references to this image, as these references will become
         * invalid.
         *
         * @param image image being analyzed VERY IMPORTANT: Analyzer method implementation must
         * call image.close() on received images when finished using them. Otherwise, new images
         * may not be received or the camera may stall, depending on back pressure setting.
         *
         */
        override fun analyze(image: ImageProxy) {
            // If there are no listeners attached, we don't need to perform analysis
            if (listeners.isEmpty()) {
                image.close()
                return
            }

            // Keep track of frames analyzed
            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            // Compute the FPS using a moving average
            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            // Analysis could take an arbitrarily long amount of time
            // Since we are running in a different thread, it won't stall other use cases

            lastAnalyzedTimestamp = frameTimestamps.first

            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
            val buffer = image.planes[0].buffer

            // Extract image data from callback object
            val data = buffer.toByteArray()

            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }

            // Compute average luminance for the image
            val luma = pixels.average()

            // Call all listeners with new value
            listeners.forEach { it(luma) }

            image.close()
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        private const val PHOTO = 0
        private const val VIDEO = 1

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(sjsu.cmpe277.myandroidmulti.R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }

        fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }
}
