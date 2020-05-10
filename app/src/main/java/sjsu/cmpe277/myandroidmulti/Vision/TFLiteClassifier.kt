package sjsu.cmpe277.myandroidmulti.Vision

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class TFLiteClassifier(private val context: Context) {
    /** The model type used for classification.  */
    var QUANTIZED_model = false

    private var interpreter: Interpreter? = null

    /** Optional GPU delegate for accleration.  */
    private var gpuDelegate: GpuDelegate? = null

    /** Optional NNAPI delegate for accleration.  */
    private var nnApiDelegate: NnApiDelegate? = null

//    /** Input image TensorBuffer.  */
//    private val inputImageBuffer: TensorImage? = null
//
//    /** Output probability TensorBuffer.  */
//    private val outputProbabilityBuffer: TensorBuffer? = null
//
//    /** Processer to apply post processing of the output probability.  */
//    private val probabilityProcessor: TensorProcessor? = null

    var isInitialized = false
        private set

    var labels = ArrayList<String>()

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    private var inputImageWidth: Int = 0
    private var inputImageHeight: Int = 0
    private var modelInputSize: Int = 0

    fun initialize(): Task<Void> {
        return Tasks.call(
            executorService,
            Callable<Void> {
                initializeInterpreter()
                null
            }
        )
    }

    @Throws(IOException::class)
    private fun initializeInterpreter() {



        val assetManager = context.assets
        //val model = loadModelFile(assetManager, "mobilenet_v1_1.0_224.tflite") //mobilenet_v1_1.0_224/tflite
        //labels = loadLines(context, "labels.txt") //labels.txt

        val model = loadModelFile(assetManager, "automl-tflite-flower.tflite")
        labels = loadLines(context, "automl-tflite-flower.txt")

        val options = Interpreter.Options()
//        gpuDelegate = GpuDelegate()
//        options.addDelegate(gpuDelegate)
        nnApiDelegate = NnApiDelegate()
        options.addDelegate(nnApiDelegate)

        val interpreter = Interpreter(model, options)

        val inputShape = interpreter.getInputTensor(0).shape() //shape 1 224 224 3
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        val inputdatatype = interpreter.getInputTensor(0).dataType();
        modelInputSize = inputdatatype.byteSize() * inputImageWidth * inputImageHeight * CHANNEL_SIZE
        //modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * CHANNEL_SIZE
        //602112
        //modelInputSize = inputImageWidth * inputImageHeight * CHANNEL_SIZE
        //602112
        if (inputdatatype.byteSize()==1)
        {
            QUANTIZED_model = true
        }
        val outputdataShape = interpreter.getOutputTensor(0).shape() //{1, NUM_CLASSES}
        val probabilityDataType = interpreter.getOutputTensor(0).dataType()
        this.interpreter = interpreter

        isInitialized = true
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    @Throws(IOException::class)
    fun loadLines(context: Context, filename: String): ArrayList<String> {
        val s = Scanner(InputStreamReader(context.assets.open(filename)))
        val labels = ArrayList<String>()
        while (s.hasNextLine()) {
            labels.add(s.nextLine())
        }
        s.close()
        return labels
    }

    private fun getMaxResult(result: FloatArray): Int {
        var probability = result[0]
        var index = 0
        for (i in result.indices) {
            if (probability < result[i]) {
                probability = result[i]
                index = i
            }
        }
        return index
    }

    private fun getMaxResultq(result: ByteArray): Int {
        val t0 = result[0].toFloat()
        val t0b = result[0].toInt()
        val t1 = result[1].toUInt()
        val t2 = result[2].toFloat() / 255
        val t3= result[3].toFloat()
        val t4 = result[4].toFloat()
        var probability = result[0].toUInt()
        var index = 0
        for (i in result.indices) {
            if (probability < result[i].toUInt()) {
                probability = result[i].toUInt()
                index = i
            }
        }
        return index
    }


    private fun classify(bitmap: Bitmap): String {

        check(isInitialized) { "TF Lite Interpreter is not initialized yet." }
        val resizedImage =
            Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
        //bitmap is image input 1280 720 size, resizedImage is 224*224

        val byteBuffer = convertBitmapToByteBuffer(resizedImage)

        val startTime = SystemClock.uptimeMillis()

        var index = 0

        if (QUANTIZED_model)
        {
            var output = Array(1) { ByteArray(labels.size) } //[1,5]
            interpreter?.run(byteBuffer, output)
            index = getMaxResultq(output[0])
        }else
        {
            var output = Array(1) { FloatArray(labels.size) }
            interpreter?.run(byteBuffer, output)
            index = getMaxResult(output[0])
        }

        val endTime = SystemClock.uptimeMillis()
        var inferenceTime = endTime - startTime
        //var index = getMaxResult(output[0])
        var result = "Prediction is ${labels[index]}\nInference Time $inferenceTime ms"

        return result
    }

    fun classifyAsync(bitmap: Bitmap): Task<String> {
        return Tasks.call(executorService, Callable<String> { classify(bitmap) })
    }

    fun close() {
        Tasks.call(
            executorService,
            Callable<String> {
                interpreter?.close()
                if (gpuDelegate != null) {
                    gpuDelegate!!.close()
                    gpuDelegate = null
                }

                Log.d(TAG, "Closed TFLite interpreter.")
                null
            }
        )
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)//150528
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until inputImageWidth) {
            for (j in 0 until inputImageHeight) {
                val pixelVal = pixels[pixel++]
                if (QUANTIZED_model == true) {
                    byteBuffer.put((pixelVal shr 16 and 0xFF).toByte())
                    byteBuffer.put((pixelVal shr 8 and 0xFF).toByte())
                    byteBuffer.put((pixelVal and 0xFF).toByte())
                }else{
                    byteBuffer.putFloat(((pixelVal shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    byteBuffer.putFloat(((pixelVal shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    byteBuffer.putFloat(((pixelVal and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }
        bitmap.recycle()

        return byteBuffer
    }

    companion object {
        private const val TAG = "TfliteClassifier"
        private const val FLOAT_TYPE_SIZE = 4
        private const val CHANNEL_SIZE = 3
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
    }
}