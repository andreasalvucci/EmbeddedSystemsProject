package com.example.cameraapp2

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.example.cameraapp2.permissions.ScanImagePermissions
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.common.util.concurrent.ListenableFuture
import org.chromium.net.CronetEngine
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.example.cameraapp2.tper.TperUtilities

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var cropArea: View
    private lateinit var scanHereTextView: TextView
    private lateinit var pictureBtn: Button
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var progressBar: ProgressBar
    private lateinit var scanByStopNameSwitch: SwitchMaterial
    private lateinit var toolbar: Toolbar
    private lateinit var zoomInButton: MaterialButton
    private lateinit var zoomOutButton: MaterialButton
    private lateinit var torchButton: MaterialButton
    private lateinit var helpButton: MaterialButton

    private lateinit var provider: ListenableFuture<ProcessCameraProvider>

    private var torchIsOn = false

    private lateinit var cronetEngine: CronetEngine
    private var handler: Handler? = null
    private val executor
        get() = ContextCompat.getMainExecutor(this)


    private lateinit var tperUtilities: TperUtilities

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkPermission()) requestPermission()

        setContentView(R.layout.activity_main)
        Configuration.getInstance().load(
            applicationContext, PreferenceManager.getDefaultSharedPreferences(
                applicationContext
            )
        )

        handler = Handler(mainLooper)

        cronetEngine = CronetEngine.Builder(this@MainActivity).build()

        zoomInButton = findViewById(R.id.zoomInbutton)
        zoomOutButton = findViewById(R.id.zoomOutButton)
        torchButton = findViewById(R.id.torchButton)
        helpButton = findViewById(R.id.helpButton)
        helpButton.setOnClickListener {
            showHelperBottomSheetDialog()
        }

        previewView = findViewById(R.id.viewFinder)
        cropArea = findViewById(R.id.crop_area)
        scanHereTextView = findViewById(R.id.scan_here_text_view)
        scanHereTextView.visibility = View.VISIBLE
        pictureBtn = findViewById(R.id.image_capture_button)
        progressBar = findViewById(R.id.indeterminateBar)
        progressBar.visibility = View.INVISIBLE
        scanByStopNameSwitch = findViewById(R.id.scan_by_stop_name)
        toolbar = findViewById(R.id.toolbar)
        toolbar.visibility = View.VISIBLE
        scanByStopNameSwitch.setOnCheckedChangeListener { _, scanStopNameEnabled ->
            if (scanStopNameEnabled) {
                scanHereTextView.setText(R.string.scan_here_text_view_text_name)
            } else {
                scanHereTextView.setText(R.string.scan_here_text_view_text_code)
            }
        }
        pictureBtn.setOnClickListener(this)
        provider = ProcessCameraProvider.getInstance(this)
        provider.addListener({
            try {
                val cameraProvider = provider.get()
                startCamera(cameraProvider)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, executor)

        tperUtilities = TperUtilities(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        val viewPort = previewView.viewPort

        imageCapture =
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
        val useCaseGroup = UseCaseGroup.Builder().setViewPort(viewPort!!).addUseCase(preview)
            .addUseCase(imageCapture).build()
        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup)
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
            if (linearZoom > MAX_ZOOMABLE_IN_VALUE) {
                Toast.makeText(
                    applicationContext, R.string.maximum_level_of_zoom_reached, Toast.LENGTH_SHORT
                ).show()
            } else {
                cameraControl.setLinearZoom(linearZoom + ZOOM_STEP)
            }
        }
        zoomOutButton.setOnClickListener {
            val linearZoom = camera.cameraInfo.zoomState.value!!.linearZoom
            if (linearZoom < MIN_ZOOMABLE_OUT_VALUE) {
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

    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, ScanImagePermissions, PERMISSION_REQUEST_CODE
        )
    }

    private fun showHelperBottomSheetDialog() {
        val helpBottomSheetDialog = HelpBottomSheetDialog()
        helpBottomSheetDialog.show(supportFragmentManager, "ModalBottomSheet")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
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

        imageCapture.takePicture(executor) { image ->
            try {
                val bitmapImage = convertImageProxyToBitmap(image)
                val croppedPhoto = cropImage(bitmapImage, previewView, cropArea)
                val croppedPhotoBitmap =
                    BitmapFactory.decodeByteArray(croppedPhoto, 0, croppedPhoto.size)
                image.close()
                progressBar.visibility = View.VISIBLE
                cropArea.visibility = View.INVISIBLE
                runInference(croppedPhotoBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
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
        val widthFinal = reference!!.width * bitmap.width / frame!!.width
        val heightFinal = reference.height * bitmap.height / frame.height
        val leftFinal = reference.left * bitmap.width / frame.width
        val topFinal = reference.top * bitmap.height / frame.height

        val bitmapFinal = Bitmap.createBitmap(
            bitmap, leftFinal, topFinal, widthFinal, heightFinal
        )

        val stream = ByteArrayOutputStream()
        bitmapFinal.compress(
            Bitmap.CompressFormat.JPEG, 100, stream
        ) //100 is the best quality possible

        return stream.toByteArray()
    }

    private fun isScanningByStopNumberEnabled(): Boolean {
        return !scanByStopNameSwitch.isChecked
    }

    fun runInference(image: Bitmap?) {
        //TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        //InputImage.fromBitmap(image!!, 0)
        val recognizer = Recognizer(image)
        recognizer.getStopNumber { stopName ->

            /* We check whether the numerical code of the bus stop exists. If it doesn't, we cannot go further
             * and we notify the user. */
            if (isScanningByStopNumberEnabled() && !tperUtilities.codeIsBusStop(stopName)) {
                Log.d(TAG, "NUMBER: Non existent number")
                progressBar.visibility = View.INVISIBLE
                cropArea.visibility = View.VISIBLE
                if (!isFinishing) showBusStopNotExistingDialog()
            } else {
                Log.wtf("message", "recognized word: $stopName")
                val tper = TperUtilities(applicationContext)
                Log.d(TAG, "NUMBER: $stopName")
                val busStopName = tper.getMoreSimilarBusStop(stopName)
                Log.d(TAG, "bus Stop: $busStopName")
                progressBar.visibility = View.INVISIBLE
                val toastText = busStopName.ifEmpty { getString(R.string.bus_stop_not_recognized) }
                Toast.makeText(applicationContext, toastText, Toast.LENGTH_SHORT).show()
                cropArea.visibility = View.VISIBLE
                cropArea.background = ResourcesCompat.getDrawable(
                    resources, R.drawable.rectangle_round_corners_green, null
                )
                handler!!.postDelayed({
                    cropArea.background = ResourcesCompat.getDrawable(
                        resources, R.drawable.rectangle_round_corners_red, null
                    )
                }, 3000)
                if (isScanningByStopNumberEnabled()) {
                    val executor: Executor = Executors.newSingleThreadExecutor()
                    val url = "https://tper-backend.herokuapp.com/fermata/$stopName"
                    Log.d(TAG, "URL: $url")
                    val requestBuilder = cronetEngine.newUrlRequestBuilder(
                        url, MyUrlRequestCallback(
                            supportFragmentManager,
                            tper.getBusStopByCode(Integer.valueOf(stopName)),
                            progressBar
                        ), executor
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
                            progressBar.visibility = View.INVISIBLE
                            cropArea.visibility = View.VISIBLE
                            if (!isFinishing) showBusStopNotExistingDialog()
                        }
                        1 -> {
                            val stopCode = busStopsCodes[0]
                            val executor: Executor = Executors.newSingleThreadExecutor()
                            val url = "https://tper-backend.herokuapp.com/fermata/$stopCode"
                            Log.d(TAG, "URL $url")
                            val requestBuilder = cronetEngine.newUrlRequestBuilder(
                                url, MyUrlRequestCallback(
                                    supportFragmentManager,
                                    tper.getBusStopByCode(stopCode),
                                    progressBar
                                ), executor
                            )
                            val request = requestBuilder.build()
                            request.start()
                        }
                        else -> showMapBottomSheetDialog(busStopsCoordinates, busStopsCodes)
                    }
                }
            }
        }
    }

    private fun showMapBottomSheetDialog(
        busStopsCoordinates: List<GeoPoint>,
        busStopsCodes: List<Int>
    ) {
        val mapBottomSheetDialog = MapBottomSheetDialog(
            applicationContext, busStopsCoordinates, busStopsCodes, tperUtilities
        )
        mapBottomSheetDialog.show(supportFragmentManager, "ModalBottomSheet")
    }

    private fun showBusStopNotExistingDialog() {
        MaterialAlertDialogBuilder(
            this@MainActivity, R.style.AppTheme
        ).setTitle(R.string.non_existent_dialog_title).setIcon(R.drawable.ic_baseline_error_24)
            .setMessage(R.string.non_existent_bus_stop_code)
            .setPositiveButton(R.string.non_existent_dialog_pos_btn, null)
            .setNegativeButton(R.string.non_existent_dialog_neg_btn) { _: DialogInterface?, _: Int -> showHelperBottomSheetDialog() }
            .show()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PERMISSION_REQUEST_CODE = 0
        private const val ZOOM_STEP = 0.1f
        private const val MAX_ZOOMABLE_IN_VALUE = 0.9f
        private const val MIN_ZOOMABLE_OUT_VALUE = 0.1f
    }
}