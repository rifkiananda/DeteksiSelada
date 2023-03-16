package com.example.deteksiselada;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.deteksiselada.ml.Model;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    FloatingActionButton gallery_btn, camera_btn, plus_btn;
    Float translationY = 100f;

    OvershootInterpolator interpolator = new OvershootInterpolator();

    private static final String TAG = "MainActivity";

    Boolean isMenuOpen = false;
    TextView textgallery, textcamera, result, confidence;
    ImageView imageView;
    int imageSize = 224;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.result);
        confidence = findViewById(R.id.confidence);
        imageView = findViewById(R.id.imageView);

        getPermission();

        initFabMenu();

    }
    private void initFabMenu() {
        gallery_btn = findViewById(R.id.gallery_btn);
        camera_btn = findViewById(R.id.camera_btn);
        plus_btn = findViewById(R.id.plus_btn);
        textcamera = findViewById(R.id.textcamera);
        textgallery = findViewById(R.id.textgalley);

        camera_btn.setAlpha(0f);
        gallery_btn.setAlpha(0f);
        textgallery.setAlpha(0f);
        textcamera.setAlpha(0f);

        camera_btn.setTranslationY(translationY);
        gallery_btn.setTranslationY(translationY);
        textgallery.setTranslationY(translationY);
        textcamera.setTranslationY(translationY);

        plus_btn.setOnClickListener(this);
        camera_btn.setOnClickListener(this);
        gallery_btn.setOnClickListener(this);
        textcamera.setOnClickListener(this);
        textgallery.setOnClickListener(this);
    }

    private void openMenu() {
        isMenuOpen = !isMenuOpen;

        plus_btn.animate().setInterpolator(interpolator).rotation(45f).setDuration(300).start();
        camera_btn.animate().translationY(0f).alpha(1f).setInterpolator(interpolator).setDuration(300).start();
        gallery_btn.animate().translationY(0f).alpha(1f).setInterpolator(interpolator).setDuration(300).start();
        textcamera.animate().translationY(0f).alpha(1f).setInterpolator(interpolator).setDuration(300).start();
        textgallery.animate().translationY(0f).alpha(1f).setInterpolator(interpolator).setDuration(300).start();
    }

    private void closeMenu() {
        isMenuOpen = !isMenuOpen;

        plus_btn.animate().setInterpolator(interpolator).rotation(0f).setDuration(300).start();
        camera_btn.animate().translationY(translationY).alpha(0f).setInterpolator(interpolator).setDuration(300).start();
        gallery_btn.animate().translationY(translationY).alpha(0f).setInterpolator(interpolator).setDuration(300).start();
        textgallery.animate().translationY(translationY).alpha(0f).setInterpolator(interpolator).setDuration(300).start();
        textcamera.animate().translationY(translationY).alpha(0f).setInterpolator(interpolator).setDuration(300).start();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.plus_btn:
                if (isMenuOpen) {
                    closeMenu();
                } else {
                    openMenu();
                }
                break;
            case R.id.camera_btn:
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 12);
                if (isMenuOpen) {
                    closeMenu();
                } else {
                    openMenu();
                }
                break;
            case R.id.gallery_btn:
                Intent intent1 = new Intent();
                intent1.setAction(Intent.ACTION_GET_CONTENT);
                intent1.setType("image/*");
                startActivityForResult(intent1, 10);
                if (isMenuOpen) {
                    closeMenu();
                } else {
                    openMenu();
                }
                break;
        }
    }

    void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(checkSelfPermission(android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 11);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==11){
            if(grantResults.length>0){
                if(grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void classifyImage(Bitmap image){
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int [] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            for(int i = 0; i < imageSize; i++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++];
                    byteBuffer.putFloat(((val >> 16) & 0xff) * 1.f / 255.f);
                    byteBuffer.putFloat(((val >> 8) & 0xff) * 1.f / 255.f);
                    byteBuffer.putFloat((val & 0xff) * 1.f / 255.f);
                }
            }


            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            int maxPos = 0;
            float maxConfidences = 0;
            for(int i = 0; i < confidences.length; i++){
                if(confidences[i] > maxConfidences){
                    maxConfidences = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {"bolting", "Kelas 1", "jamur", "Kelas 2", "mata kodok", "rusak"};

            result.setText(classes[maxPos]);

            String s = "";
            for(int i = 0; i < classes.length; i++){
                s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
            }
            confidence.setText(s);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==10){
            if(data!=null){
                Uri dat = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);
                classifyImage(bitmap);
            }
        }

        else if(requestCode == 12 && resultCode == RESULT_OK){
            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(image.getWidth(), image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            imageView.setImageBitmap(image);

            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}