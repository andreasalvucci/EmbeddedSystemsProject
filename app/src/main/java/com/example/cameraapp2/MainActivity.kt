package com.example.cameraapp2

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.lifecycle.ProcessCameraProvider
import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import com.google.android.material.switchmaterial.SwitchMaterial
import org.chromium.net.CronetEngine
import com.google.android.material.button.MaterialButton
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import android.view.MotionEvent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import android.graphics.BitmapFactory
import kotlin.Throws
import android.app.Activity
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.common.InputImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.DialogInterface
import android.graphics.Matrix
import android.os.Build
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import com.google.common.util.concurrent.ListenableFuture
import org.osmdroid.config.Configuration
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var provider: ListenableFuture<ProcessCameraProvider>? = null
    private var bitmapBuffer: Bitmap? = null
    private var cropArea: View? = null
    private var scanHereTextView: TextView? = null
    private var pictureBtn: Button? = null
    private var previewView: PreviewView? = null
    private lateinit var imageCapture: ImageCapture
    private val imageAnalysis: ImageAnalysis? = null
    private var progressBar: ProgressBar? = null
    private var switch1: SwitchMaterial? = null
    private var toolbar: Toolbar? = null
    private lateinit var cronetEngine: CronetEngine
    private var handler: Handler? = null
    private var zoomInButton: MaterialButton? = null
    private var zoomOutButton: MaterialButton? = null
    private var torchButton: MaterialButton? = null
    private lateinit var helpButton: MaterialButton
    private var torchIsOn = false
    private val executor
        get() = ContextCompat.getMainExecutor(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!checkPermission()) requestPermission()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Configuration.getInstance().load(
            applicationContext, PreferenceManager.getDefaultSharedPreferences(
                applicationContext
            )
        )

        cronetEngine = CronetEngine.Builder(this@MainActivity).build()
        zoomInButton = findViewById(R.id.zoomInbutton)
        zoomOutButton = findViewById(R.id.zoomOutButton)
        torchButton = findViewById(R.id.torchButton)
        helpButton = findViewById(R.id.helpButton)
        handler = Handler(mainLooper)
        previewView = findViewById(R.id.viewFinder)
        cropArea = findViewById(R.id.crop_area)
        scanHereTextView = findViewById(R.id.scan_here_text_view)
        scanHereTextView?.visibility = View.VISIBLE
        pictureBtn = findViewById(R.id.image_capture_button)
        progressBar = findViewById(R.id.indeterminateBar)
        progressBar?.visibility = View.INVISIBLE
        switch1 = findViewById(R.id.switch3)
        toolbar = findViewById(R.id.toolbar)
        toolbar?.visibility = View.VISIBLE
        switch1?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, b ->
            if (b) {
                scanHereTextView?.setText(R.string.scan_here_text_view_text_name)
            } else {
                scanHereTextView?.setText(R.string.scan_here_text_view_text_code)
            }
        })
        pictureBtn?.setOnClickListener(this)
        provider = ProcessCameraProvider.getInstance(this)
        provider!!.addListener({
            try {
                val cameraProvider = provider!!.get()
                startCamera(cameraProvider)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, executor)
        helpButton.setOnClickListener {
            val helpBottomSheetDialog = HelpBottomSheetDialog()
            helpBottomSheetDialog.show(supportFragmentManager, "ModalBottomSheet")
        }
    }

    private fun startCamera(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val preview = Preview.Builder().build()
        val viewPort = previewView!!.viewPort
        preview.setSurfaceProvider(previewView!!.surfaceProvider)
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        val useCaseGroup = UseCaseGroup.Builder()
            .setViewPort(viewPort!!)
            .addUseCase(preview)
            .addUseCase(imageCapture!!)
            .build()
        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup)
        val cameraControl = camera.cameraControl
        previewView!!.setOnTouchListener { _: View?, motionEvent: MotionEvent ->
            val factory = previewView!!.meteringPointFactory
            val point = factory.createPoint(motionEvent.x, motionEvent.y)
            val action = FocusMeteringAction.Builder(point).build()
            cameraControl.startFocusAndMetering(action)
            false
        }
        zoomInButton!!.setOnClickListener {
            val linearZoom = camera.cameraInfo.zoomState.value!!
                .linearZoom
            if (linearZoom > 0.9f) {
                Toast.makeText(
                    applicationContext,
                    R.string.maximum_level_of_zoom_reached,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                cameraControl.setLinearZoom(linearZoom + Companion.ZOOM_STEP)
            }
        }
        zoomOutButton!!.setOnClickListener {
            val linearZoom = camera.cameraInfo.zoomState.value!!
                .linearZoom
            if (linearZoom <= 0.1f) {
                Toast.makeText(
                    applicationContext,
                    R.string.minimum_level_of_zoom_reached,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                cameraControl.setLinearZoom(linearZoom - Companion.ZOOM_STEP)
            }
        }
        torchButton!!.setOnClickListener { switchTorchState(cameraControl) }
    }

    private fun switchTorchState(cameraControl: CameraControl) {
        cameraControl.enableTorch(!torchIsOn)
        torchIsOn = !torchIsOn
        switchTorchIcon()
    }

    private fun switchTorchIcon() {
        if (torchIsOn) {
            torchButton!!.icon = resources.getDrawable(R.drawable.ic_baseline_flashlight_off_24)
        } else {
            torchButton!!.icon = resources.getDrawable(R.drawable.ic_baseline_flashlight_on_24)
        }
    }

    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
                )
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.image_capture_button -> capturePhoto()
        }
    }

    fun capturePhoto() {
        Log.d("pviewInfo", previewView!!.width.toString() + "x" + previewView!!.height)
        val size = Size(previewView!!.width, previewView!!.height)
        val pictureName = "ANDREA_" + SimpleDateFormat("yyyyMMDD_HHmmss").format(Date()) + ".jpeg"
        imageCapture!!.takePicture(executor, object : OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                try {
                    val bitmapImage = convertImageProxyToBitmap(image)
                    val croppedPhoto = cropImage(bitmapImage, previewView, cropArea)
                    val croppedPhotoBitmap =
                        BitmapFactory.decodeByteArray(croppedPhoto, 0, croppedPhoto.size)
                    image.close()
                    progressBar!!.visibility = View.VISIBLE
                    cropArea!!.visibility = View.INVISIBLE
                    runInference(croppedPhotoBitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun convertImageProxyToBitmap(image: ImageProxy): Bitmap {
        val byteBuffer = image.planes[0].buffer
        byteBuffer.rewind()
        val bytes = ByteArray(byteBuffer.capacity())
        byteBuffer[bytes]
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height, matrix, false)
        return bitmap
    }

    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd("east8.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun cropImage(bitmap: Bitmap, frame: View?, reference: View?): ByteArray {
        val heightOriginal = frame!!.height
        val widthOriginal = frame.width
        val heightFrame = reference!!.height
        val widthFrame = reference.width
        val leftFrame = reference.left
        val topFrame = reference.top
        val heightReal = bitmap.height
        val widthReal = bitmap.width
        val widthFinal = widthFrame * widthReal / widthOriginal
        val heightFinal = heightFrame * heightReal / heightOriginal
        val leftFinal = leftFrame * widthReal / widthOriginal
        val topFinal = topFrame * heightReal / heightOriginal
        val bitmapFinal = Bitmap.createBitmap(
            bitmap,
            leftFinal, topFinal, widthFinal, heightFinal
        )
        val stream = ByteArrayOutputStream()
        bitmapFinal.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            stream
        ) //100 is the best quality possibe
        return stream.toByteArray()
    }

    fun runInference(image: Bitmap?) {
        val busCodeScanning = !switch1!!.isChecked
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(image!!, 0)
        val recognizer1 = Recognizer(image)
        val stopNumber = recognizer1.getStopNumber { stopName ->
            val tperUtilities = TperUtilities(this@MainActivity)

            /*
                      We check whether the numerical code of the bus stop exists. If it doesn't, we cannot go further
                      and we notify the user.
                       */if (busCodeScanning && !tperUtilities.codeIsBusStop(stopName)) {
            Log.d("NUMERO", "Numero non esistente")
            progressBar!!.visibility = View.INVISIBLE
            cropArea!!.visibility = View.VISIBLE
            if (!isFinishing) showBusStopNotExistingDialog()
        } else {
            Log.wtf("message", "recognized word: $stopName")
            val tper = TperUtilities(applicationContext)
            Log.d("NUMERO", stopName)
            val nomeFermata = tper.getMoreSimilarBusStop(stopName)
            Log.d("Fermata", nomeFermata)
            progressBar!!.visibility = View.INVISIBLE
            Toast.makeText(applicationContext, nomeFermata, Toast.LENGTH_SHORT).show()
            cropArea!!.visibility = View.VISIBLE
            cropArea!!.background = this@MainActivity.resources
                .getDrawable(R.drawable.rectangle_round_corners_green)
            handler!!.postDelayed({
                cropArea!!.background = this@MainActivity.resources
                    .getDrawable(R.drawable.rectangle_round_corners_red)
            }, 3000)
            if (busCodeScanning) {
                val executor: Executor = Executors.newSingleThreadExecutor()
                val url = "https://tper-backend.herokuapp.com/fermata/$stopName"
                Log.d("LASTRING", url)
                val requestBuilder = cronetEngine!!.newUrlRequestBuilder(
                    url,
                    MyUrlRequestCallback(
                        supportFragmentManager,
                        tper.getBusStopByCode(Integer.valueOf(stopName)),
                        progressBar
                    ),
                    executor
                )
                val request = requestBuilder.build()
                request.start()
            } else {
                val coordinateFermate = tper.getCoupleOfCoordinatesByStopName(stopName)
                val codiciFermate = tper.getCodesByStopName(tper.getMoreSimilarBusStop(stopName))

                /* se esiste una sola fermata che si chiama così, allora è inutile far scegliere all'utente un
                                  marcatore sulla mappa, facciamo partire subito la richiesta
                                   */if (codiciFermate.size == 1) {
                    val codice = codiciFermate[0]
                    val executor: Executor = Executors.newSingleThreadExecutor()
                    val url = "https://tper-backend.herokuapp.com/fermata/$codice"
                    Log.d("LASTRING", url)
                    val requestBuilder = cronetEngine!!.newUrlRequestBuilder(
                        url,
                        MyUrlRequestCallback(
                            supportFragmentManager,
                            tper.getBusStopByCode(codice),
                            progressBar
                        ),
                        executor
                    )
                    val request = requestBuilder.build()
                    request.start()
                } else {
                    val mapBottomSheetDialog = MapBottomSheetDialog(
                        applicationContext,
                        coordinateFermate,
                        codiciFermate,
                        tper
                    )
                    mapBottomSheetDialog.show(supportFragmentManager, "ModalBottomSheet")
                }
            }
        }
        }
    }

    private fun toBitmap(image: ImageProxy): Bitmap? {
        if (bitmapBuffer == null) {
            bitmapBuffer = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        }
        bitmapBuffer!!.copyPixelsFromBuffer(image.planes[0].buffer)
        return bitmapBuffer
    }

    private fun showBusStopNotExistingDialog() {
        MaterialAlertDialogBuilder(this@MainActivity, R.style.AppTheme)
            .setTitle(R.string.non_existent_dialog_title)
            .setIcon(R.drawable.ic_baseline_error_24)
            .setMessage(R.string.non_existent_bus_stop_code)
            .setPositiveButton(R.string.non_existent_dialog_pos_btn, null)
            .setNegativeButton(R.string.non_existent_dialog_neg_btn) { _: DialogInterface?, _: Int -> showBusStopCodeTutorial() }
            .show()
    }

    private fun showBusStopCodeTutorial() {
        Toast.makeText(applicationContext, "Aiuto premuto", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 0
        private const val ZOOM_STEP = 0.1f

        fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
            val allocationSize = getBitmapByteSize(bitmap)
            val buffer = ByteBuffer.allocate(allocationSize)
            bitmap.copyPixelsToBuffer(buffer)
            bitmap.recycle()
            return buffer.array()
        }

        private fun getBitmapByteSize(bitmap: Bitmap): Int {
            return bitmap.allocationByteCount
        }
    }
}