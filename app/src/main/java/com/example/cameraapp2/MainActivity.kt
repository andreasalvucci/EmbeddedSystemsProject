package com.example.cameraapp2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import androidx.preference.PreferenceManager
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface.ROTATION_0
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import com.example.cameraapp2.tper.TperUtilities
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.chromium.net.CronetEngine
import org.osmdroid.config.Configuration
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var waitingForTperTextView: TextView
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cropArea: View? = null
    private var scanHereTextView: TextView? = null
    private var pictureBtn: Button? = null
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private var progressBar: ProgressBar? = null
    private var scanByStopNameSwitch: SwitchMaterial? = null
    private var toolbar: Toolbar? = null
    private lateinit var cronetEngine: CronetEngine
    private var handler: Handler? = null
    private var zoomInButton: MaterialButton? = null
    private var zoomOutButton: MaterialButton? = null
    private var torchButton: MaterialButton? = null
    private lateinit var helpButton: MaterialButton
    private var torchIsOn = false
    private lateinit var permissionsArray: Array<String>
    private val executor
        get() = ContextCompat.getMainExecutor(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsArray = arrayOf(Manifest.permission.CAMERA,Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.WRITE_EXTERNAL_STORAGE)

        myCheckPermissions(permissionsArray)

        setContentView(R.layout.activity_main)
        Configuration.getInstance().load(
            applicationContext, PreferenceManager.getDefaultSharedPreferences(
                applicationContext
            )
        )
        waitingForTperTextView = findViewById(R.id.waiting_for_tper_response_main)

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
        scanByStopNameSwitch = findViewById(R.id.scan_by_stop_name)
        toolbar = findViewById(R.id.toolbar)
        toolbar?.visibility = View.VISIBLE
        scanByStopNameSwitch?.setOnCheckedChangeListener { _, b ->
            if (b) {
                scanHereTextView?.setText(R.string.scan_here_text_view_text_name)
            } else {
                scanHereTextView?.setText(R.string.scan_here_text_view_text_code)
            }
        }
        pictureBtn?.setOnClickListener(this)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture!!.addListener({
            try {
                val cameraProvider = cameraProviderFuture!!.get()
                startCamera(cameraProvider)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))

        helpButton.setOnClickListener {
            val helpBottomSheetDialog = HelpBottomSheetDialog()
            helpBottomSheetDialog.show(supportFragmentManager, "ModalBottomSheet")
        }
    }

    private fun myCheckPermissions(permissions: Array<String>) {
        for (permission in permissions) {
            // Check if the permission is granted or not
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera(cameraProvider: ProcessCameraProvider) {

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        cameraProvider.unbindAll()
        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageCapture)
            .build()
        val camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, useCaseGroup)
        val cameraControl = camera.cameraControl
        previewView.setOnTouchListener { _: View?, motionEvent: MotionEvent ->
            val factory = previewView.meteringPointFactory
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
                cameraControl.setLinearZoom(linearZoom + ZOOM_STEP)
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
                cameraControl.setLinearZoom(linearZoom - ZOOM_STEP)
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
            torchButton!!.icon = ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_flashlight_off_24,
                null
            )
        } else {
            torchButton!!.icon = ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_baseline_flashlight_on_24,
                null
            )
        }
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

    private fun capturePhoto() {
        Log.d(TAG, "pViewInfo ${previewView.width} x ${previewView.height}")
        Size(previewView.width, previewView.height)
        "ANDREA_" + SimpleDateFormat("yyyyMMDD_HHmmss").format(Date()) + ".jpeg"
        imageCapture.takePicture(executor, object : OnImageCapturedCallback() {
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
        ) //100 is the best quality possible
        return stream.toByteArray()
    }

    fun runInference(image: Bitmap?) {
        val busCodeScanning = !scanByStopNameSwitch!!.isChecked
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        InputImage.fromBitmap(image!!, 0)
        val recognizer1 = Recognizer(image)
        recognizer1.getStopNumber { stopName ->
            val tperUtilities = TperUtilities(this@MainActivity)

            /* We check whether the numerical code of the bus stop exists. If it doesn't, we cannot go further
             * and we notify the user. */
            if (busCodeScanning && !tperUtilities.codeIsBusStop(stopName)) {
                Log.d(TAG, "NUMBER: Non existent number")
                progressBar!!.visibility = View.INVISIBLE
                cropArea!!.visibility = View.VISIBLE
                if (!isFinishing) showBusStopNotExistingDialog()
            } else {
                Log.wtf("message", "recognized word: $stopName")
                val tper = TperUtilities(applicationContext)
                Log.d(TAG, "NUMBER: $stopName")
                val busStopName = tper.getMoreSimilarBusStop(stopName)
                Log.d(TAG, "bus Stop: $busStopName")
                progressBar!!.visibility = View.INVISIBLE
                val toastText = busStopName.ifEmpty { getString(R.string.bus_stop_not_recognized) }
                Toast.makeText(applicationContext, toastText, Toast.LENGTH_SHORT).show()
                cropArea!!.visibility = View.VISIBLE
                cropArea!!.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.rectangle_round_corners_green,
                    null
                )
                handler!!.postDelayed({
                    cropArea!!.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.rectangle_round_corners_red,
                        null
                    )
                }, 3000)
                if (busCodeScanning) {
                    val executor: Executor = Executors.newSingleThreadExecutor()
                    val url = "https://tper-backend.onrender.com/fermata/$stopName"
                    Log.d(TAG, "URL: $url")
                    val requestBuilder = cronetEngine.newUrlRequestBuilder(
                        url,
                        MyUrlRequestCallback(
                            supportFragmentManager,
                            tper.getBusStopByCode(Integer.valueOf(stopName)),
                            progressBar,
                            waitingForTperTextView
                        ),
                        executor
                    )
                    val request = requestBuilder.build()
                    request.start()
                } else {
                    val busStopsCoordinates = tper.getCoupleOfCoordinatesByStopName(stopName)
                    val busStopsCodes =
                        tper.getCodesByStopName(tper.getMoreSimilarBusStop(stopName))

                    /* se esiste una sola fermata che si chiama così, allora è inutile far scegliere all'utente un
                     * marcatore sulla mappa, facciamo partire subito la richiesta */
                    when (busStopsCodes.size) {
                        0 -> {
                            Log.d(TAG, "NUMBER: Non existent number")
                            progressBar!!.visibility = View.INVISIBLE
                            cropArea!!.visibility = View.VISIBLE
                            if (!isFinishing) showBusStopNotExistingDialog()
                        }
                        1 -> {
                            val stopCode = busStopsCodes[0]
                            val executor: Executor = Executors.newSingleThreadExecutor()
                            val url = "https://tper-backend.onrender.com/fermata/$stopCode"
                            Log.d(TAG, "URL $url")
                            val requestBuilder = cronetEngine.newUrlRequestBuilder(
                                url,
                                MyUrlRequestCallback(
                                    supportFragmentManager,
                                    tper.getBusStopByCode(stopCode),
                                    progressBar,
                                    waitingForTperTextView
                                ),
                                executor
                            )
                            val request = requestBuilder.build()
                            request.start()
                        }
                        else -> {
                            val mapBottomSheetDialog = MapBottomSheetDialog(
                                applicationContext,
                                busStopsCoordinates,
                                busStopsCodes,
                                tper
                            )
                            mapBottomSheetDialog.show(supportFragmentManager, "ModalBottomSheet")
                        }
                    }
                }
            }
        }
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
        private val TAG = MainActivity::class.java.simpleName
        private const val PERMISSION_REQUEST_CODE = 0
        private const val ZOOM_STEP = 0.1f
        private const val HOST = "https://tper-backend.onrender.com"
    }
}