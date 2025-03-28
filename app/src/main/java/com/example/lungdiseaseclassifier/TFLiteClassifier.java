package com.example.lungdiseaseclassifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class TFLiteClassifier {
    private static final String TAG = "TFLiteClassifier";

    // Input is 300x300 RGB image as floats
    private static final int IMAGE_WIDTH = 300;
    private static final int IMAGE_HEIGHT = 300;
    private static final int NUM_CHANNELS = 3;
    private static final int NUM_CLASSES = 2;

    // Model expects float32 values (4 bytes per channel)
    private static final int BYTES_PER_CHANNEL = 4;

    private final Interpreter tflite;
    private final ByteBuffer inputBuffer;
    private final float[][] outputBuffer;

    private final String[] labels = {"normal", "viral pneumonia"};

    public TFLiteClassifier(Context context) throws IOException {
        Log.d(TAG, "Starting to load model");
        try {
            MappedByteBuffer modelBuffer = loadModelFile(context, "lung_classifier.lite");
            Log.d(TAG, "Model file loaded successfully");

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            tflite = new Interpreter(modelBuffer, options);
            Log.d(TAG, "Interpreter created successfully");

            // 300x300x3x4 = 1,080,000 bytes (float32 values)
            inputBuffer = ByteBuffer.allocateDirect(IMAGE_WIDTH * IMAGE_HEIGHT * NUM_CHANNELS * BYTES_PER_CHANNEL);
            inputBuffer.order(ByteOrder.nativeOrder());

            outputBuffer = new float[1][NUM_CLASSES];

            Log.d(TAG, "TFLite model loaded successfully with input buffer size: " +
                    (IMAGE_WIDTH * IMAGE_HEIGHT * NUM_CHANNELS * BYTES_PER_CHANNEL) + " bytes");
        } catch (Exception e) {
            Log.e(TAG, "Error loading model", e);
            throw e;
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        try {
            // Create a temporary file from the asset
            InputStream inputStream = context.getAssets().open(modelPath);
            File tempFile = File.createTempFile("tflite_model", null, context.getCacheDir());

            // Copy asset to the temp file
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, read);
            }
            inputStream.close();
            fileOutputStream.close();

            // Map the file to memory
            FileInputStream fileInputStream = new FileInputStream(tempFile);
            FileChannel fileChannel = fileInputStream.getChannel();
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

            // Clean up
            fileInputStream.close();

            Log.d(TAG, "Model loaded successfully with size: " + mappedByteBuffer.capacity() + " bytes");
            return mappedByteBuffer;
        } catch (IOException e) {
            Log.e(TAG, "Error loading model file: " + e.getMessage(), e);
            throw e;
        }
    }

    public Map<String, Float> classify(Bitmap image) {
        // Resize the image to the required dimensions
        Bitmap resizedImage = Bitmap.createScaledBitmap(image, IMAGE_WIDTH, IMAGE_HEIGHT, true);

        // Convert bitmap to byte buffer
        preprocessImageToByteBuffer(resizedImage);

        // Run inference
        tflite.run(inputBuffer, outputBuffer);

        // Map results to labels
        Map<String, Float> results = new HashMap<>();
        for (int i = 0; i < NUM_CLASSES; i++) {
            results.put(labels[i], outputBuffer[0][i]);
        }

        return results;
    }

    private void preprocessImageToByteBuffer(Bitmap bitmap) {
        inputBuffer.rewind();

        int[] pixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
        bitmap.getPixels(pixels, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        for (int pixel : pixels) {
            // Extract RGB values and normalize to [0, 1] for float model
            float r = ((pixel >> 16) & 0xFF) / 255.0f;
            float g = ((pixel >> 8) & 0xFF) / 255.0f;
            float b = (pixel & 0xFF) / 255.0f;

            // Put as floats (float32 model)
            inputBuffer.putFloat(r);
            inputBuffer.putFloat(g);
            inputBuffer.putFloat(b);
        }
    }

    public String[] getLabels() {
        return labels;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}