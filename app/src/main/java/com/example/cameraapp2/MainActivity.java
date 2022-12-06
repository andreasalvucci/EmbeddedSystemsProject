package com.example.cameraapp2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.GnssAntennaInfo;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int PERMISSION_REQUEST_CODE = 0 ;
    private ListenableFuture<ProcessCameraProvider> provider;
    private Bitmap bitmapBuffer;
    private View cropArea;
    private TextView scanHereTextView;
    private Button picture_bt;
    private PreviewView pview;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private ProgressBar progressBar;
    private SwitchMaterial switch1;
    private Toolbar toolbar;
    private CronetEngine cronetEngine;
    private Handler handler;
    private MaterialButton zoomInButton;
    private MaterialButton zoomOutButton;
    private MaterialButton torchButton;
    private MaterialButton helpButton;
    private boolean torchIsOn = false;
    private final String SERVER_HOSTNAME = "https://tper-backend.onrender.com";
    private boolean isInternetAvailable = false;

    private final float ZOOM_STEP = 0.1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();








        if(!checkPermission())
            requestPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));


        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d("NET_MONITORING", "NETWORK IS AVAILABLE");
                isInternetAvailable = true;
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                isInternetAvailable = false;
                showNoConnectionDialog();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                final boolean unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
            }
        };

        ConnectivityManager connectivityManager =
                null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            connectivityManager = (ConnectivityManager) getSystemService(ConnectivityManager.class);
        }
        connectivityManager.requestNetwork(networkRequest, networkCallback);

        CronetEngine.Builder myBuilder = new CronetEngine.Builder(MainActivity.this);
        cronetEngine = myBuilder.build();

        zoomInButton = findViewById(R.id.zoomInbutton);
        zoomOutButton = findViewById(R.id.zoomOutButton);
        torchButton = findViewById(R.id.torchButton);
        helpButton = findViewById(R.id.helpButton);


        handler = new Handler(getMainLooper());

        pview = findViewById(R.id.viewFinder);
        cropArea = findViewById(R.id.crop_area);

        scanHereTextView = findViewById(R.id.scan_here_text_view);
        scanHereTextView.setVisibility(View.VISIBLE);

        picture_bt = findViewById(R.id.image_capture_button);
        progressBar = findViewById(R.id.indeterminateBar);
        progressBar.setVisibility(View.INVISIBLE);
        switch1 = findViewById(R.id.switch3);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setVisibility(View.VISIBLE);
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    scanHereTextView.setText(R.string.scan_here_text_view_text_name);
                }
                else{
                    scanHereTextView.setText(R.string.scan_here_text_view_text_code);
                }
            }
        });


        picture_bt.setOnClickListener(this);


        provider = ProcessCameraProvider.getInstance(this);
        provider.addListener( () ->
        {
            try{
                ProcessCameraProvider cameraProvider = provider.get();
                startCamera(cameraProvider);

            }
            catch(Exception e){
                e.printStackTrace();
            }

        }, getExecutor());

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HelpBottomSheetDialog helpBottomSheetDialog = new HelpBottomSheetDialog();
                helpBottomSheetDialog.show(getSupportFragmentManager(),"ModalBottomSheet");
            }
        });


    }

    private Executor getExecutor(){
        return ContextCompat.getMainExecutor(this);
    }



    private void startCamera(ProcessCameraProvider cameraProvider){
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        Preview preview = new Preview.Builder().build();
        ViewPort viewPort = pview.getViewPort();
        preview.setSurfaceProvider(pview.getSurfaceProvider());
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();
        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .setViewPort(viewPort)
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .build();
        Camera camera = cameraProvider.bindToLifecycle(this,cameraSelector, useCaseGroup);
        CameraControl cameraControl = camera.getCameraControl();

        pview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                MeteringPointFactory factory = pview.getMeteringPointFactory();
                MeteringPoint point = factory.createPoint(motionEvent.getX(), motionEvent.getY());
                FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();
                cameraControl.startFocusAndMetering(action);
                return false;
            }
        });

        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float linearZoom = camera.getCameraInfo().getZoomState().getValue().getLinearZoom();
                if(linearZoom>0.9f){
                    Toast.makeText(getApplicationContext(),R.string.maximum_level_of_zoom_reached,Toast.LENGTH_SHORT).show();
                }
                else{
                    cameraControl.setLinearZoom(linearZoom+ZOOM_STEP);
                }
            }
        });

        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float linearZoom = camera.getCameraInfo().getZoomState().getValue().getLinearZoom();
                if(linearZoom<=0.1f){
                    Toast.makeText(getApplicationContext(),R.string.minimum_level_of_zoom_reached,Toast.LENGTH_SHORT).show();
                }
                else{
                    cameraControl.setLinearZoom(linearZoom-ZOOM_STEP);
                }
            }
        });

        torchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchTorchState(cameraControl);
            }
        });


    }

    private void switchTorchState(CameraControl cameraControl){
        cameraControl.enableTorch(!torchIsOn);
        torchIsOn = !torchIsOn;
        switchTorchIcon();
    }
    private void switchTorchIcon(){
        if(torchIsOn){
            torchButton.setIcon(getResources().getDrawable(R.drawable.ic_baseline_flashlight_off_24));
        }
        else{
            torchButton.setIcon(getResources().getDrawable(R.drawable.ic_baseline_flashlight_on_24));
        }
    }

    private boolean checkPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS) != PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        }
        return true;
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET,
        Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.WRITE_SETTINGS}, PERMISSION_REQUEST_CODE);
    }

    public void onRequestPermissionResult(int requestCode, String permissions[], int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case PERMISSION_REQUEST_CODE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getApplicationContext(), "Permission granted", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();

                }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.image_capture_button:
                capturePhoto();

                break;
            /*case R.id.video_capture_button:
                this.analysis_on=!this.analysis_on;
                break;*/
        }

    }

    private void showNoConnectionDialog(){
        NoInternetBottomSheetDialog noInternetBottomSheetDialog = new NoInternetBottomSheetDialog();
        noInternetBottomSheetDialog.show(getSupportFragmentManager(), "ModalBottomSheet");
    }

    public void capturePhoto(){
        Log.d("pviewInfo",pview.getWidth()+"x"+pview.getHeight());
        Size size = new Size(pview.getWidth(),pview.getHeight());
        String pictureName ="ANDREA_"+new SimpleDateFormat("yyyyMMDD_HHmmss").format(new Date())+".jpeg";
        imageCapture.takePicture(getExecutor(), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);

                try{
                    Bitmap bitmapImage = convertImageProxyToBitmap(image);
                    byte[] croppedPhoto = cropImage(bitmapImage,pview,cropArea);
                    Bitmap croppedPhotoBitmap = BitmapFactory.decodeByteArray(croppedPhoto,0,croppedPhoto.length);

                    image.close();
                    progressBar.setVisibility(View.VISIBLE);
                    cropArea.setVisibility(View.INVISIBLE);
                    runInference(croppedPhotoBitmap);
                }
                catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

                }

            }
        });
    }

    private Bitmap convertImageProxyToBitmap(ImageProxy image) {
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        BitmapFactory.decodeByteArray(bytes,0, bytes.length);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
        Matrix matrix = new Matrix();
        matrix.postRotate(image.getImageInfo().getRotationDegrees());
        bitmap = Bitmap.createBitmap(bitmap,0,0, image.getWidth(), image.getHeight(), matrix, false);
        return bitmap;
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        int allocationSize = getBitmapByteSize(bitmap);
        ByteBuffer buffer = ByteBuffer.allocate(allocationSize);
        bitmap.copyPixelsToBuffer(buffer);
        bitmap.recycle();
        return buffer.array();
    }
    public static int getBitmapByteSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        } else {
            //others
            return bitmap.getRowBytes() * bitmap.getHeight();
        }
    }
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("east8.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private byte[] cropImage(Bitmap bitmap, View frame, View reference){
        int heightOriginal = frame.getHeight();
        int widthOriginal = frame.getWidth();
        int heightFrame = reference.getHeight();
        int widthFrame = reference.getWidth();
        int leftFrame = reference.getLeft();
        int topFrame = reference.getTop();
        int heightReal = bitmap.getHeight();
        int widthReal = bitmap.getWidth();
        int widthFinal = widthFrame * widthReal / widthOriginal;
        int heightFinal = heightFrame * heightReal / heightOriginal;
        int leftFinal = leftFrame * widthReal / widthOriginal;
        int topFinal = topFrame * heightReal / heightOriginal;
        Bitmap bitmapFinal = Bitmap.createBitmap(
                bitmap,
                leftFinal, topFinal, widthFinal, heightFinal
        );

        ByteArrayOutputStream stream =new ByteArrayOutputStream();
        bitmapFinal.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                stream
        ); //100 is the best quality possibe
        return stream.toByteArray();
    }

    public void runInference(Bitmap image){
        Boolean busCodeScanning = !switch1.isChecked();
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage inputImage = InputImage.fromBitmap(image,0);

        Recognizer recognizer1 = new Recognizer(image);


        Integer stopNumber = recognizer1.getStopNumber(new RecognizerCallback() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onCallBack(String stopName) {

                TperUtilities tperUtilities = new TperUtilities(MainActivity.this);

                /*
                We check whether the numerical code of the bus stop exists. If it doesn't, we cannot go further
                and we notify the user.
                 */
                if(busCodeScanning && !tperUtilities.codeIsBusStop(stopName)){
                    Log.d("NUMERO", "Numero non esistente");
                    progressBar.setVisibility(View.INVISIBLE);
                    cropArea.setVisibility(View.VISIBLE);
                    if(!isFinishing())
                        showBusStopNotExistingDialog();
                }
                else{
                    Log.wtf("message","recognized word: "+stopName);
                    TperUtilities tper = new TperUtilities(getApplicationContext());
                    Log.d("NUMERO", String.valueOf(stopName));
                    String nomeFermata = tper.getMoreSimilarBusStop(stopName);
                    Log.d("Fermata",nomeFermata);
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(),nomeFermata,Toast.LENGTH_SHORT).show();
                    cropArea.setVisibility(View.VISIBLE);
                    cropArea.setBackground(MainActivity.this.getResources()
                            .getDrawable(R.drawable.rectangle_round_corners_green));

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            cropArea.setBackground(MainActivity.this.getResources()
                                    .getDrawable(R.drawable.rectangle_round_corners_red));
                        }
                    },3000);
                    if(busCodeScanning){
                        Executor executor = Executors.newSingleThreadExecutor();
                        String url = SERVER_HOSTNAME+"/fermata/"+stopName;
                        Log.d("LASTRING",url);
                        UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(url
                                , new MyUrlRequestCallback(getSupportFragmentManager(),tper.getBusStopByCode(Integer.valueOf(stopName)),progressBar), executor);
                        UrlRequest request = requestBuilder.build();
                        request.start();
                    }
                    else{

                        List<GeoPoint> coordinateFermate = tper.getCoupleOfCoordinatesByStopName(stopName);
                        List<Integer> codiciFermate = tper.getCodesByStopName(tper.getMoreSimilarBusStop(stopName));

                        /* se esiste una sola fermata che si chiama così, allora è inutile far scegliere all'utente un
                            marcatore sulla mappa, facciamo partire subito la richiesta
                             */
                        if(codiciFermate.size()==1){
                            int codice = codiciFermate.get(0);
                            Executor executor = Executors.newSingleThreadExecutor();
                            String url = "https://tper-backend.onrender.com/fermata/"+codice;
                            Log.d("LASTRING",url);
                            UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(url
                                    , new MyUrlRequestCallback(getSupportFragmentManager(),tper.getBusStopByCode(codice),progressBar), executor);
                            UrlRequest request = requestBuilder.build();
                            request.start();
                        }
                        // altrimenti facciamo scegliere all'utente una determinata fermata
                        else {
                            MapBottomSheetDialog mapBottomSheetDialog = new MapBottomSheetDialog(getApplicationContext(), coordinateFermate, codiciFermate, tper);
                            mapBottomSheetDialog.show(getSupportFragmentManager(), "ModalBottomSheet");
                        }
                    }
                }
            }
        });
    }

    private Bitmap toBitmap(@NonNull ImageProxy image) {
        if(bitmapBuffer == null){
            bitmapBuffer = Bitmap.createBitmap(image.getWidth(),image.getHeight(),Bitmap.Config.ARGB_8888);
        }
        bitmapBuffer.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
        return bitmapBuffer;
    }

    private void showBusStopNotExistingDialog(){
        new MaterialAlertDialogBuilder(MainActivity.this, R.style.AppTheme)
                .setTitle("Attenzione")
                .setIcon(R.drawable.ic_baseline_error_24)
                .setMessage(R.string.non_existent_bus_stop_code)
                .setPositiveButton("OK", null)
                .setNegativeButton("Aiuto", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        showBusStopCodeTutorial();
                    }
                })
                .show();

    }
    private void showBusStopCodeTutorial(){
        Toast.makeText(getApplicationContext(),"Aiuto premuto", Toast.LENGTH_SHORT).show();
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            //You can replace it with your name
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }

    public boolean isConnectingToInternet() {
        ConnectivityManager cm = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            try {
                URL url = new URL("http://www.google.com");
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(300);
                urlc.connect();
                if (urlc.getResponseCode() == 200) {
                    return true;
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                return false;
            }
        }
        return false;
    }
    }