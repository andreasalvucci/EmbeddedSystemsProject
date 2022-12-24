package com.example.cameraapp2

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import com.example.cameraapp2.permissions.positionPermissions
import com.example.cameraapp2.permissions.scanImagePermissions
import com.example.cameraapp2.tper.TperUtilities
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lorenzofelletti.permissions.PermissionManager
import com.lorenzofelletti.permissions.dispatcher.dsl.*
import org.chromium.net.CronetEngine
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var waitingForTperTextView: TextView
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cropArea: View? = null
    private lateinit var scanHereTextView: TextView
    private lateinit var pictureBtn: Button
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private var progressBar: ProgressBar? = null
    private var scanByStopNameSwitch: SwitchMaterial? = null
    private var toolbar: Toolbar? = null
    private lateinit var cronetEngine: CronetEngine
    private var handler: Handler? = null
    private lateinit var zoomInButton: MaterialButton
    private lateinit var zoomOutButton: MaterialButton
    private lateinit var torchButton: MaterialButton
    private lateinit var helpButton: MaterialButton

    private val permissionManager = PermissionManager(this)

    private var torchIsOn = false

    private val executor
        get() = ContextCompat.getMainExecutor(this)

    private val tperUtilities by lazy { TperUtilities(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // declarative permission flow
        permissionManager.buildRequestResultsDispatcher {
            withRequestCode(PERMISSION_REQUEST_CODE) {
                checkPermissions(scanImagePermissions)
                showRationaleDialog(getString(R.string.camera_rationale_dialog))
                doOnGranted {}
                doOnDenied {
                    showPermissionsDeniedDialog(this@MainActivity)
                }
            }
            withRequestCode(POSITION_REQUEST_CODE) {
                checkPermissions(positionPermissions)
                showRationaleDialog(getString(R.string.position_rationale_dialog))
                doOnDenied {
                    showPositionPermissionsDeniedDialog(this@MainActivity)
                }
            }
        }

        handler = Handler(Looper.getMainLooper())

        permissionManager.checkRequestAndDispatch(PERMISSION_REQUEST_CODE)

        setContentView(R.layout.activity_main)
        Configuration.getInstance().load(
            applicationContext, PreferenceManager.getDefaultSharedPreferences(
                applicationContext
            )
        )
        waitingForTperTextView = findViewById(R.id.waiting_for_tper_response_main)
        waitingForTperTextView.visibility = View.INVISIBLE

        cronetEngine = CronetEngine.Builder(this@MainActivity).build()
        zoomInButton = findViewById(R.id.zoomInbutton)
        zoomOutButton = findViewById(R.id.zoomOutButton)
        torchButton = findViewById(R.id.torchButton)
        helpButton = findViewById(R.id.helpButton)
        previewView = findViewById(R.id.viewFinder)
        cropArea = findViewById(R.id.crop_area)
        scanHereTextView = findViewById(R.id.scan_here_text_view)
        scanHereTextView.visibility = View.VISIBLE
        pictureBtn = findViewById(R.id.image_capture_button)
        progressBar = findViewById(R.id.indeterminateBar)
        progressBar?.visibility = View.INVISIBLE
        scanByStopNameSwitch = findViewById(R.id.scan_by_stop_name)
        toolbar = findViewById(R.id.toolbar)
        toolbar?.visibility = View.VISIBLE
        scanByStopNameSwitch?.setOnCheckedChangeListener { _, scanByName ->
            if (scanByName) {
                scanHereTextView.setText(R.string.scan_here_text_view_text_name)
            } else {
                scanHereTextView.setText(R.string.scan_here_text_view_text_code)
            }
        }
        pictureBtn.setOnClickListener(this)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera(cameraProvider: ProcessCameraProvider) {
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        imageCapture =
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
        cameraProvider.unbindAll()
        val useCaseGroup =
            UseCaseGroup.Builder().addUseCase(preview).addUseCase(imageCapture).build()
        val camera =
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, useCaseGroup)
        val cameraControl = camera.cameraControl
        previewView.setOnTouchListener { _: View?, motionEvent: MotionEvent ->
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(motionEvent.x, motionEvent.y)
            val action = FocusMeteringAction.Builder(point).build()
            cameraControl.startFocusAndMetering(action)
            false
        }
        zoomInButton.setOnClickListener {
            val linearZoom = camera.cameraInfo.zoomState.value!!.linearZoom
            if (linearZoom > 0.9f) {
                Toast.makeText(
                    applicationContext, R.string.maximum_level_of_zoom_reached, Toast.LENGTH_SHORT
                ).show()
            } else {
                cameraControl.setLinearZoom(linearZoom + ZOOM_STEP)
            }
        }
        zoomOutButton.setOnClickListener {
            val linearZoom = camera.cameraInfo.zoomState.value!!.linearZoom
            if (linearZoom <= 0.1f) {
                Toast.makeText(
                    applicationContext, R.string.minimum_level_of_zoom_reached, Toast.LENGTH_SHORT
                ).show()
            } else {
                cameraControl.setLinearZoom(linearZoom - ZOOM_STEP)
            }
        }
        torchButton.setOnClickListener { switchTorchState(cameraControl) }
    }

    private fun switchTorchState(cameraControl: CameraControl) {
        cameraControl.enableTorch(!torchIsOn)
        torchIsOn = !torchIsOn
        switchTorchIcon()
    }

    private fun switchTorchIcon() {
        if (torchIsOn) {
            torchButton.icon = ResourcesCompat.getDrawable(
                resources, R.drawable.ic_baseline_flashlight_off_24, null
            )
        } else {
            torchButton.icon = ResourcesCompat.getDrawable(
                resources, R.drawable.ic_baseline_flashlight_on_24, null
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.dispatchOnRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.image_capture_button -> capturePhoto()
        }
    }

    private fun capturePhoto() {
        Log.d(TAG, "pViewInfo ${previewView.width} x ${previewView.height}")
        Size(previewView.width, previewView.height)

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
            bitmap, leftFinal, topFinal, widthFinal, heightFinal
        )
        val stream = ByteArrayOutputStream()
        bitmapFinal.compress(
            Bitmap.CompressFormat.JPEG, 100, stream
        ) //100 is the best quality possible
        return stream.toByteArray()
    }

    fun runInference(image: Bitmap?) {
        val busCodeScanning = !scanByStopNameSwitch!!.isChecked
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        InputImage.fromBitmap(image!!, 0)
        val recognizer = Recognizer(image)
        recognizer.getStopNumber { stopName ->
            /* We check whether the numerical code of the bus stop exists. If it doesn't, we cannot go further
             * and we notify the user. */

            if (busCodeScanning && !tperUtilities.codeIsBusStop(stopName)) {
                Log.d(TAG, "NUMBER: Non existent number")
                progressBar!!.visibility = View.INVISIBLE
                cropArea!!.visibility = View.VISIBLE
                if (!isFinishing) showBusStopNotExistingDialog()
            } else {
                Log.wtf("message", "recognized word: $stopName")
                Log.d(TAG, "NUMBER: $stopName")
                val busStopName = tperUtilities.getMoreSimilarBusStop(stopName)
                Log.d(TAG, "bus Stop: $busStopName")
                progressBar!!.visibility = View.INVISIBLE
                val toastText = busStopName.ifEmpty { getString(R.string.bus_stop_not_recognized) }
                Toast.makeText(applicationContext, toastText, Toast.LENGTH_SHORT).show()
                cropArea!!.visibility = View.VISIBLE

                makeCropAreaGreenFor()

                if (busCodeScanning) {
                    val executor: Executor = Executors.newSingleThreadExecutor()
                    val url = "$HOST/fermata/$stopName"
                    Log.d(TAG, "URL: $url")
                    val requestBuilder = cronetEngine.newUrlRequestBuilder(
                        url, MyUrlRequestCallback(
                            supportFragmentManager,
                            tperUtilities.getBusStopByCode(Integer.valueOf(stopName)),
                            progressBar,
                            waitingForTperTextView
                        ), executor
                    )
                    val request = requestBuilder.build()
                    request.start()
                } else {
                    val busStopsCoordinates =
                        tperUtilities.getCoupleOfCoordinatesByStopName(stopName)
                    val busStopsCodes = tperUtilities.getCodesByStopName(
                        tperUtilities.getMoreSimilarBusStop(
                            stopName
                        )
                    )

                    /* se esiste una sola fermata che si chiama così, allora è inutile far scegliere all'utente un
                     * marcatore sulla mappa, facciamo partire subito la richiesta */
                    dispatchInferenceResults(busStopsCodes, busStopsCoordinates)
                }
            }
        }
    }

    private fun dispatchInferenceResults(
        busStopsCodes: List<Int>, busStopsCoordinates: List<GeoPoint>
    ) {
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
                val url = "$HOST/fermata/$stopCode"
                Log.d(TAG, "URL $url")
                val requestBuilder = cronetEngine.newUrlRequestBuilder(
                    url, MyUrlRequestCallback(
                        supportFragmentManager,
                        tperUtilities.getBusStopByCode(stopCode),
                        progressBar,
                        waitingForTperTextView
                    ), executor
                )
                val request = requestBuilder.build()
                request.start()
            }
            else -> {
                // replaces the onGranted action for the location permission to show the map
                // with the correct markers
                permissionManager.dispatcher.replaceEntryOnGranted(POSITION_REQUEST_CODE) {
                    val mapBottomSheetDialog = MapBottomSheetDialog(
                        applicationContext, busStopsCoordinates, busStopsCodes, tperUtilities
                    )
                    mapBottomSheetDialog.show(
                        supportFragmentManager, "ModalBottomSheet"
                    )
                }

                // checks the permissions, and dispatch the correct action
                permissionManager.checkRequestAndDispatch(POSITION_REQUEST_CODE)
            }
        }
    }

    private fun makeCropAreaGreenFor(durationInMillis: Long = 3000) {
        cropArea!!.background = ResourcesCompat.getDrawable(
            resources, R.drawable.rectangle_round_corners_green, null
        )
        handler?.postDelayed({
            cropArea!!.background = ResourcesCompat.getDrawable(
                resources, R.drawable.rectangle_round_corners_red, null
            )
        }, durationInMillis)
    }

    private fun showBusStopNotExistingDialog() {
        val busStopNotExistingDialog = ErrorBottomSheetDialog()
        busStopNotExistingDialog.show(supportFragmentManager, "ErrorBottomSheetDialog")
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PERMISSION_REQUEST_CODE = 0
        private const val POSITION_REQUEST_CODE = 1
        private const val ZOOM_STEP = 0.1f
        private const val HOST = "https://tper-backend.onrender.com"
    }
}