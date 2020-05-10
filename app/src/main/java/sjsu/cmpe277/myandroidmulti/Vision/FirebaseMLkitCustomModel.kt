package sjsu.cmpe277.myandroidmulti.Vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.installations.remote.TokenResult
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.custom.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.math.min

class FirebaseMLkitCustomModel(private val context: Context) {
    //ref:https://firebase.google.com/docs/ml-kit/android/use-custom-models
    /** Data structure holding pairs of <label, confidence> for each inference result */
    data class LabelConfidence(val label: String, val confidence: Float)

    var toplabels: List<String>? = null
    //var toplabels: List<String>()// = emptyList<String>()

    /** Labels corresponding to the output of the vision model. */
    private val labelList by lazy {
        BufferedReader(InputStreamReader(context.resources.assets.open(LABEL_PATH))).lineSequence().toList()
    }

    /** Preallocated buffers for storing image data. */
    private val imageBuffer = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)

    /** Firebase model interpreter used for the local model from assets */
    private lateinit var modelInterpreter: FirebaseModelInterpreter

    fun initialize()
    {
        modelInterpreter = createLocalModelInterpreter()// createLocalModelInterpreter()
    }

    /** Input options used for our Firebase model interpreter */
    private val modelInputOutputOptions by lazy {
        val inputDims = arrayOf(DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE)
        val outputDims = arrayOf(DIM_BATCH_SIZE, labelList.size)
        FirebaseModelInputOutputOptions.Builder()
            .setInputFormat(0, FirebaseModelDataType.BYTE, inputDims.toIntArray())
            .setOutputFormat(0, FirebaseModelDataType.BYTE, outputDims.toIntArray())
            .build()
    }

    /** Initialize a local model interpreter from assets file */
    fun createLocalModelInterpreter(): FirebaseModelInterpreter {
        // Select the first available .tflite file as our local model
        val localModelName = context.resources.assets.list("")?.firstOrNull {
            //it.endsWith(".tflite")
            //it.equals("mobilenet_v1_1.0_224_quant.tflite")
            it.equals(LOCAL_MODEL_FILE)
        }?: throw(RuntimeException("Don't forget to add the tflite file to your assets folder"))
        Log.d(TAG, "Local model found: $localModelName")

        // Create an interpreter with the local model asset
        val localModel =
            FirebaseCustomLocalModel.Builder().setAssetFilePath(localModelName).build()
        val localInterpreter = FirebaseModelInterpreter.getInstance(
            FirebaseModelInterpreterOptions.Builder(localModel).build())!!
        Log.d(TAG, "Local model interpreter initialized")

        // Return the interpreter
        return localInterpreter
    }

//    fun classifyAsync(bitmap: Bitmap): Task<String> {
//        return Tasks.call(executorService, Callable<String> { classify(bitmap) })
//    }

    /** Uses model to make predictions and interpret output into likely labels. */
    //Task<List<String>>
    fun runModelInference(image: Bitmap): Task<List<String>> {

        // Create input data.
        val imgData = convertBitmapToByteBuffer(image)

        // Create model inputs from our image data.
        val modelInputs = FirebaseModelInputs.Builder().add(imgData).build()

        return modelInterpreter.run(modelInputs, modelInputOutputOptions).continueWith {
            val inferenceOutput = it.result?.getOutput<Array<ByteArray>>(0)!!

            // Display labels on the screen using an overlay
            val topLabels = getTopLabels(inferenceOutput)
            //graphic_overlay.clear()
            //graphic_overlay.add(LabelGraphic(graphic_overlay, topLabels))
            toplabels = topLabels
            topLabels
        }

//        try {
//            // Create model inputs from our image data.
//            val modelInputs = FirebaseModelInputs.Builder().add(imgData).build()
//
//            // Perform inference using our model interpreter.
//            val task = modelInterpreter.run(modelInputs, modelInputOutputOptions).continueWith {
//                val inferenceOutput = it.result?.getOutput<Array<ByteArray>>(0)!!
//
//                // Display labels on the screen using an overlay
//                val topLabels = getTopLabels(inferenceOutput)
//                //graphic_overlay.clear()
//                //graphic_overlay.add(LabelGraphic(graphic_overlay, topLabels))
//                toplabels = topLabels
//                topLabels
//            }
//            return task
//
//        } catch (exc: FirebaseMLException) {
//            val msg = "Error running model inference"
//            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
//            Log.e(TAG, msg, exc)
//            return exc
//        }

    }

    /** Gets the top labels in the results. */
    @Synchronized
    private fun getTopLabels(inferenceOutput: Array<ByteArray>): List<String> {
        // Since we ran inference on a single image, inference output will have a single row.
        val imageInference = inferenceOutput.first()

        // The columns of the image inference correspond to the confidence for each label.
        return labelList.mapIndexed { idx, label ->
            LabelConfidence(label, (imageInference[idx] and 0xFF.toByte()) / 255.0f)

            // Sort the results in decreasing order of confidence and return only top 3.
        }.sortedBy { it.confidence }.reversed().map { "${it.label}:${it.confidence}" }
            .subList(0, min(labelList.size, RESULTS_TO_SHOW))
    }

    /** Writes Image data into a `ByteBuffer`. */
    @Synchronized
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }
        val scaledBitmap =
            Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true)
        scaledBitmap.getPixels(
            imageBuffer, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
        // Convert the image to int points.
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val `val` = imageBuffer[pixel++]
                imgData.put((`val` shr 16 and 0xFF).toByte())
                imgData.put((`val` shr 8 and 0xFF).toByte())
                imgData.put((`val` and 0xFF).toByte())
            }
        }
        return imgData
    }

    /** Writes Image data into a `ByteBuffer`. */
    @Synchronized
    private fun convertBitmapToByteBufferFloat(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(
            4*DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }
        val scaledBitmap =
            Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true)
        scaledBitmap.getPixels(
            imageBuffer, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
        // Convert the image to int points.
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val `val` = imageBuffer[pixel++]
                imgData.put((`val` shr 16 and 0xFF).toByte())
                imgData.put((`val` shr 8 and 0xFF).toByte())
                imgData.put((`val` and 0xFF).toByte())
            }
        }
        return imgData
    }

    companion object {
        private val TAG = "FirebaseMLKit"

        /** Name of the label file stored in Assets. */
        //private const val LABEL_PATH = "labels.txt"//"catdoglabels.txt"//
        //private const val LOCAL_MODEL_FILE = "mobilenet_v1_1.0_224_quant.tflite"//"catdogconverted_model.tflite"//"mobilenet_v1_1.0_224_quant.tflite" //

        private const val LABEL_PATH = "automl-tflite-flower.txt"//"catdoglabels.txt"//
        private const val LOCAL_MODEL_FILE = "automl-tflite-flower.tflite"//"catdogconverted_model.tflite"//"mobilenet_v1_1.0_224_quant.tflite" //

        /** Name of the remote model in Firebase. */
        private const val REMOTE_MODEL_NAME = "mobilenet_v1_224_quant"

        /** Number of results to show in the UI. */
        private const val RESULTS_TO_SHOW = 3

        /** Dimensions of inputs. */
        private const val DIM_BATCH_SIZE = 1
        private const val DIM_PIXEL_SIZE = 3
        private const val DIM_IMG_SIZE_X = 224
        private const val DIM_IMG_SIZE_Y = 224

        /** Utility function for loading and resizing images from app asset folder. */
        fun decodeBitmapAsset(context: Context, filePath: String): Bitmap =
            context.assets.open(filePath).let { BitmapFactory.decodeStream(it) }
    }

}