package sjsu.cmpe277.myandroidmulti.Vision


import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sjsu.cmpe277.myandroidmulti.R
import sjsu.cmpe277.myandroidmulti.databinding.FragmentCameraBinding


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CameraFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"
private const val IMMERSIVE_FLAG_TIMEOUT = 500L

class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding

    private lateinit var outputFileUri: Uri

    private lateinit var firebasemodel: FirebaseMLkitCustomModel

    var toplabels: List<String>? = null

    companion object {
        const val ODT_PERMISSIONS_REQUEST: Int = 1
        const val ODT_REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate<FragmentCameraBinding>(inflater,
            R.layout.fragment_camera,container,false)

        //this.context = getContext();
        //cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        binding.buttonCamera.setOnClickListener {view: View ->
            val action = CameraFragmentDirections.actionCameraFragmentToCameraActivity()
            view.findNavController().navigate(action)
        }

        binding.photoview.setOnClickListener {
            val packageManager = activity!!.packageManager
            val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePhotoIntent.resolveActivity(packageManager) != null) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "MLKit_codelab")
                outputFileUri = activity!!.contentResolver
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!

                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
                startActivityForResult(takePhotoIntent, ODT_REQUEST_IMAGE_CAPTURE)
            }
        }

        binding.floatingCameraButton.setOnClickListener {
            val packageManager = activity!!.packageManager
            val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePhotoIntent.resolveActivity(packageManager) != null) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "MLKit_codelab")
                outputFileUri = activity!!.contentResolver
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!

                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
                startActivityForResult(takePhotoIntent, ODT_REQUEST_IMAGE_CAPTURE)
            }
        }

        if (checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED) {

            binding.floatingCameraButton.isEnabled = false
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                ODT_PERMISSIONS_REQUEST
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            firebasemodel = FirebaseMLkitCustomModel(activity!!)
            firebasemodel.initialize()

            //modelInterpreter = createRemoteModelInterpreter()
            activity!!.runOnUiThread {
                //firebasemodel.toplabels
                //button_run.isEnabled = true
            }
        }
//        binding.button.setOnClickListener { view: View ->
//            //viewModel.yourname = binding.inputName.text.toString()
//            viewModel.yourname.value = binding.inputName.text.toString()
//
//            val action = TitleFragmentDirections.actionTitleFragmentToQuestionFragment(riskscore = viewModel.riskscore.value!!)
//            view.findNavController().navigate(action)
//
//        }

        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_camera, container, false)
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ODT_REQUEST_IMAGE_CAPTURE &&
            resultCode == Activity.RESULT_OK) {

            val image = getCapturedImage()

            // display capture image
            binding.photoview.setImageBitmap(image)
            //imageView.setImageBitmap(image)

            // run through ODT and display result
            runCustomObjectDetection(image)
            runObjectDetection(image)
        }
    }

    /**
     * getCapturedImage():
     *     Decodes and center crops the captured image from camera.
     */
    private fun getCapturedImage(): Bitmap {

        val srcImage = FirebaseVisionImage
            .fromFilePath(activity!!, outputFileUri).bitmap

        // crop image to match imageView's aspect ratio
        val scaleFactor = Math.min(
            srcImage.width / binding.photoview.width.toFloat(),
            srcImage.height / binding.photoview.height.toFloat()
        )

        val deltaWidth = (srcImage.width - binding.photoview.width * scaleFactor).toInt()
        val deltaHeight = (srcImage.height - binding.photoview.height * scaleFactor).toInt()

        val scaledImage = Bitmap.createBitmap(
            srcImage, deltaWidth / 2, deltaHeight / 2,
            srcImage.width - deltaWidth, srcImage.height - deltaHeight
        )
        srcImage.recycle()
        return scaledImage

    }

    private fun runCustomObjectDetection(bitmap: Bitmap) {

        var result = "Results: "
        firebasemodel.runModelInference(bitmap)
            .addOnSuccessListener {
                var topLabels = it
                for (label in topLabels) {
                    result = "$result $label"
                }
                binding.labelresults.text = result
            }
            .addOnFailureListener{
                val msg = "Error running model inference"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                Log.e("firebasemodel", msg, it)
            }


    }

    /**
     * MLKit Object Detection Function
     */
    private fun runObjectDetection(bitmap: Bitmap) {
        //ref: https://firebase.google.com/docs/ml-kit/android/detect-objects
        // Step 1: create MLKit's VisionImage object
        val image = FirebaseVisionImage.fromBitmap(bitmap)

        // Step 2: acquire detector object
        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        val detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)

        // Step 3: feed given image to detector and setup callback
        detector.processImage(image)
            .addOnSuccessListener {
                // Task completed successfully
                // Post-detection processing : draw result
                val drawingView = DrawingView(activity!!, it)
                drawingView.draw(Canvas(bitmap))
                activity!!.runOnUiThread {
                    binding.photoview.setImageBitmap(bitmap)
                }
            }
            .addOnFailureListener {
                // Task failed with an exception
                Toast.makeText(
                    activity!!, "Oops, something went wrong!",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

}

/**
 * DrawingView class:
 *    onDraw() function implements drawing
 *     - boundingBox
 *     - Category
 *     - Confidence ( if Category is not CATEGORY_UNKNOWN )
 */
class DrawingView(context: Context, var visionObjects: List<FirebaseVisionObject>) : View(context) {

    companion object {
        // mapping table for category to strings: drawing strings
        val categoryNames: Map<Int, String> = mapOf(
            FirebaseVisionObject.CATEGORY_UNKNOWN to "Unknown",
            FirebaseVisionObject.CATEGORY_HOME_GOOD to "Home Goods",
            FirebaseVisionObject.CATEGORY_FASHION_GOOD to "Fashion Goods",
            FirebaseVisionObject.CATEGORY_FOOD to "Food",
            FirebaseVisionObject.CATEGORY_PLACE to "Place",
            FirebaseVisionObject.CATEGORY_PLANT to "Plant"
        )
    }

    val MAX_FONT_SIZE = 96F

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        debugPrint(visionObjects)

        for (item in visionObjects) {
            // draw bounding box
            pen.color = Color.RED
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = item.getBoundingBox()
            canvas.drawRect(box, pen)

            // Draw result category, and confidence
            val tags: MutableList<String> = mutableListOf()
            tags.add("Category: ${categoryNames[item.classificationCategory]}")
            if (item.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                tags.add("Confidence: ${item.classificationConfidence!!.times(100).toInt()}%")
            }

            var tagSize = Rect(0, 0, 0, 0)
            var maxLen = 0
            var index: Int = -1

            for ((idx, tag) in tags.withIndex()) {
                if (maxLen < tag.length) {
                    maxLen = tag.length
                    index = idx
                }
            }

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(tags[index], 0, tags[index].length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F

            // draw tags onto bitmap (bmp is in upside down format)
            for ((idx, txt) in tags.withIndex()) {
                canvas.drawText(
                    txt, box.left + margin,
                    box.top + tagSize.height().times(idx + 1.0F), pen
                )
            }
        }
    }

    private fun debugPrint(visionObjects : List<FirebaseVisionObject>) {
        val LOG_MOD = "MLKit-ODT"
        for ((idx, obj) in visionObjects.withIndex()) {
            val box = obj.boundingBox

            Log.d(LOG_MOD, "Detected object: ${idx} ")
            Log.d(LOG_MOD, "  Category: ${obj.classificationCategory}")
            Log.d(LOG_MOD, "  trackingId: ${obj.trackingId}")
            //Log.d(LOG_MOD, "  entityId: ${obj.entityId}")
            Log.d(LOG_MOD, "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")
            if (obj.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                val confidence: Int = obj.classificationConfidence!!.times(100).toInt()
                Log.d(LOG_MOD, "  Confidence: ${confidence}%")
            }
        }
    }
}

