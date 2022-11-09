package com.example.cameraapp2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.checkerframework.common.reflection.qual.GetMethod;
import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, ImageAnalysis.Analyzer{

    private static final int PERMISSION_REQUEST_CODE = 0 ;
    private ListenableFuture<ProcessCameraProvider> provider;
    private Bitmap bitmapBuffer;
    private View cropArea;
    private TextView scanHereTextView;
    private Button picture_bt, analysis_bt;
    private Boolean analysis_on;
    private PreviewView pview;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    ProgressBar progressBar;
    Switch switch1;
    Toolbar toolbar;
    ImageView infoImage;
    CronetEngine cronetEngine;

    //private RectOverlay rectOverlay;
    //Canvas rectCanvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(!checkPermission())
            requestPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        //rectCanvas = new Canvas();
        CronetEngine.Builder myBuilder = new CronetEngine.Builder(MainActivity.this);
         cronetEngine = myBuilder.build();

        pview = findViewById(R.id.viewFinder);
        cropArea = findViewById(R.id.crop_area);

        picture_bt = findViewById(R.id.image_capture_button);
        analysis_bt = findViewById(R.id.video_capture_button);
        progressBar = findViewById(R.id.indeterminateBar);
        progressBar.setVisibility(View.INVISIBLE);
        switch1 = findViewById(R.id.switch3);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setVisibility(View.VISIBLE);
        infoImage = findViewById(R.id.imageView);
        infoImage.setVisibility(View.VISIBLE);
        //rectOverlay = findViewById(R.id.rectOverlay);
        //rectOverlay.drawOverlay(rectCanvas);


        picture_bt.setOnClickListener(this);
        analysis_bt.setOnClickListener(this);
        this.analysis_on = false;



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
        imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(getExecutor(), this);
        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .setViewPort(viewPort)
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .addUseCase(imageAnalysis)
                .build();

        cameraProvider.bindToLifecycle(this,cameraSelector, useCaseGroup);

    }

    private boolean checkPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
        return false;
        }
        return true;
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
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
            case R.id.video_capture_button:
                this.analysis_on=!this.analysis_on;
                break;
        }

    }

    public void capturePhoto(){
        Log.d("pviewInfo",pview.getWidth()+"x"+pview.getHeight());
        Size size = new Size(pview.getWidth(),pview.getHeight());
        String pictureName ="ANDREA_"+new SimpleDateFormat("yyyyMMDD_HHmmss").format(new Date())+".jpeg";
        imageCapture.takePicture(getExecutor(), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);
                /*
                ContentValues newPictureDetails = new ContentValues();
                newPictureDetails.put(MediaStore.Images.Media._ID, pictureName);
                newPictureDetails.put(MediaStore.Images.Media.ORIENTATION, String.valueOf(image.getImageInfo().getRotationDegrees()));
                newPictureDetails.put(MediaStore.Images.Media.DISPLAY_NAME, pictureName);
                newPictureDetails.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
                newPictureDetails.put(MediaStore.Images.Media.WIDTH, image.getWidth());
                newPictureDetails.put(MediaStore.Images.Media.HEIGHT, image.getHeight());
                newPictureDetails.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM+"/sistemidigitali");
                OutputStream stream = null;*/
                try{
                    /*Uri picturePublicUri = getApplicationContext()
                            .getContentResolver()
                            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newPictureDetails);
                    stream = getApplicationContext().getContentResolver().openOutputStream(picturePublicUri);*/
                    Bitmap bitmapImage = convertImageProxyToBitmap(image);
                   /* Rect croppingRect = getCroppingRect(rectCanvas);
                    assert(croppingRect.left < croppingRect.right && croppingRect.top < croppingRect.bottom);
                    Bitmap resized = Bitmap.createScaledBitmap(bitmapImage, 1080, 1993, true);
                    Bitmap croppedBmp = Bitmap.createBitmap(resized, 190, 1146, 700, 300);*
//  draw source bitmap into resulting image at given position:
                    new Canvas().drawBitmap(bitmapImage, -croppingRect.left, -croppingRect.top, null);*/
                    byte[] croppedPhoto = cropImage(bitmapImage,pview,cropArea);
                    Bitmap croppedPhotoBitmap = BitmapFactory.decodeByteArray(croppedPhoto,0,croppedPhoto.length);
                    //if(!croppedPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)){

                    //}
                    image.close();
                    //stream.close();
                    //Toast.makeText(getApplicationContext(), "Picture Taken", Toast.LENGTH_SHORT).show();
                    Log.d("INFO", "PICTURE TAKEN");
                    progressBar.setVisibility(View.VISIBLE);
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
        Boolean busCodeScanning = switch1.isChecked();
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage inputImage = InputImage.fromBitmap(image,0);

        Recognizer recognizer1 = new Recognizer(image);


        Integer stopNumber = recognizer1.getStopNumber(new RecognizerCallback() {
            @Override
            public void onCallBack(String stopName) {
                Log.wtf("message","recognized word: "+stopName);
                TperUtilities tper = new TperUtilities(getApplicationContext());
                Log.d("NUMERO", String.valueOf(stopName));
                String nomeFermata = tper.getMoreSimilarBusStop(stopName);
                Log.d("Fermata",nomeFermata);
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(getApplicationContext(),nomeFermata,Toast.LENGTH_SHORT).show();
                if(busCodeScanning){
                    Executor executor = Executors.newSingleThreadExecutor();
                    UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(
                            R.string.HOST+":"+R.string.PORT+"/fermata/"+stopName, new MyUrlRequestCallback(), executor);
                    UrlRequest request = requestBuilder.build();
                    request.start();



                }



            }
        });
    }
    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {

        Rect cropRect = imageProxy.getCropRect();
        Image image = imageProxy.getImage();
        if(this.analysis_on) {


            Log.wtf("Image format", String.valueOf(imageProxy.getFormat()));
            Bitmap result = toBitmap(imageProxy);
            Log.wtf("WxH", result.getWidth()+"x"+result.getHeight());

        }
        imageProxy.close();


    }
    public static String executeGet(String targetURL, String urlParameters) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length",
                    Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private Rect getCroppingRect(Canvas canvas){
        int canvasW = canvas.getWidth();
        int canvasH = canvas.getHeight();
        Point centerOfCanvas = new Point(canvasW / 2, canvasH / 2);
        int rectW = 700;
        int rectH = 300;
        int left = centerOfCanvas.x - (rectW / 2);
        int top = centerOfCanvas.y - (rectH / 2);
        int right = centerOfCanvas.x + (rectW / 2);
        int bottom = centerOfCanvas.y + (rectH / 2);
        Rect rect = new Rect(left, top, right, bottom);
        return rect;
    }

    private Bitmap toBitmap(@NonNull ImageProxy image) {
        if(bitmapBuffer == null){
            bitmapBuffer = Bitmap.createBitmap(image.getWidth(),image.getHeight(),Bitmap.Config.ARGB_8888);
        }
        bitmapBuffer.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
        return bitmapBuffer;
    }



}