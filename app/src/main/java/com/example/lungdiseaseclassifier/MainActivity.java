package com.example.lungdiseaseclassifier;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_PICK_IMAGE = 103;

    // UI Components
    private ImageView imageView;
    private Button captureButton;
    private Button galleryButton;
    private TextView statusTextView;
    private CardView resultsCard;
    private LinearLayout resultsContainer;

    // Processing components
    private TFLiteClassifier classifier;
    private Uri currentPhotoUri;
    private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.imageCard).setOnClickListener(v -> {
            requestStoragePermission();
        });
        // Initialize UI components
        imageView = findViewById(R.id.imageView);
        captureButton = findViewById(R.id.captureButton);
        galleryButton = findViewById(R.id.galleryButton);
        statusTextView = findViewById(R.id.statusTextView);
        resultsCard = findViewById(R.id.resultsCard);
        resultsContainer = findViewById(R.id.resultsContainer);

// Initialize classifier
        try {
            classifier = new TFLiteClassifier(this);
            Log.d("MainActivity", "Classifier initialized successfully");
        } catch (IOException e) {
            Log.e("MainActivity", "Error loading model: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading model: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        // Set up button click listeners
        captureButton.setOnClickListener(v -> {
            requestCameraPermission();
        });

        galleryButton.setOnClickListener(v -> {
            requestStoragePermission();
        });
    }

    @AfterPermissionGranted(REQUEST_CAMERA_PERMISSION)
    private void requestCameraPermission() {
        String[] perms = {Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this, perms)) {
            dispatchTakePictureIntent();
        } else {
            EasyPermissions.requestPermissions(this,
                    "Camera access is needed to capture lung images",
                    REQUEST_CAMERA_PERMISSION, perms);
        }
    }

    @AfterPermissionGranted(REQUEST_STORAGE_PERMISSION)
    private void requestStoragePermission() {
        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            dispatchPickImageIntent();
        } else {
            EasyPermissions.requestPermissions(this,
                    "Storage access is needed to select lung images",
                    REQUEST_STORAGE_PERMISSION, perms);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the file where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                        "com.example.lungclassifier.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    private void dispatchPickImageIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Lung Image"), REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), currentPhotoUri);
                    imageView.setImageBitmap(imageBitmap);
                    classifyImage(imageBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error loading captured image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                try {
                    Uri selectedImageUri = data.getData();
                    InputStream imageStream = getContentResolver().openInputStream(selectedImageUri);
                    imageBitmap = BitmapFactory.decodeStream(imageStream);
                    imageView.setImageBitmap(imageBitmap);
                    classifyImage(imageBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error loading selected image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void classifyImage(Bitmap bitmap) {
        if (classifier == null) {
            statusTextView.setText("Classifier not initialized");
            return;
        }

        statusTextView.setText("Classifying image...");
        resultsCard.setVisibility(View.GONE);
        resultsContainer.removeAllViews();

        // Run classification in a background thread
        new Thread(() -> {
            try {
                final Map<String, Float> results = classifier.classify(bitmap);

                // Update UI on main thread
                runOnUiThread(() -> {
                    displayResults(results);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    statusTextView.setText("Classification error");
                    Toast.makeText(MainActivity.this,
                            "Error during classification: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void displayResults(Map<String, Float> results) {
        statusTextView.setText("Classification complete");

        // Create a dialog with custom style
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_classification_result, null);
        builder.setView(dialogView);

        // Make dialog full width with rounded corners
        builder.setCancelable(true);

        // Get references to views
        TextView diagnosisTextView = dialogView.findViewById(R.id.diagnosisTextView);
        TextView normalValueTextView = dialogView.findViewById(R.id.normalValueTextView);
        TextView pneumoniaValueTextView = dialogView.findViewById(R.id.pneumoniaValueTextView);
        ProgressBar normalProgressBar = dialogView.findViewById(R.id.normalProgressBar);
        ProgressBar pneumoniaProgressBar = dialogView.findViewById(R.id.pneumoniaProgressBar);
        Button closeButton = dialogView.findViewById(R.id.closeButton);

        // Get probabilities
        float normalProb = results.get("normal") * 100;
        float pneumoniaProb = results.get("viral pneumonia") * 100;

        // Set values
        normalValueTextView.setText(String.format("%.1f%%", normalProb));
        pneumoniaValueTextView.setText(String.format("%.1f%%", pneumoniaProb));

        normalProgressBar.setProgress(Math.round(normalProb));
        pneumoniaProgressBar.setProgress(Math.round(pneumoniaProb));

        // Determine diagnosis (highest probability class)
        if (normalProb >= pneumoniaProb) {
            diagnosisTextView.setText("Normal");
            diagnosisTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            diagnosisTextView.setText("Viral Pneumonia");
            diagnosisTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set dialog window attributes for better appearance
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set width to 90% of screen width
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setAttributes(layoutParams);
        }

        // Set close button actions
        closeButton.setOnClickListener(v -> dialog.dismiss());
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up resources
        if (classifier != null) {
            classifier.close();
        }
    }
}