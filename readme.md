# Lung Disease Classifier

An Android application that uses TensorFlow Lite to classify chest X-ray images as normal or viral pneumonia.

## Overview

This application uses a machine learning model trained in Edge Impulse to analyze chest X-ray images and classify them as either normal or indicating viral pneumonia. The model achieves 97.3% accuracy on the validation set.

## Features

- **Take photos** using the device camera
- **Upload images** from the gallery
- **Real-time classification** of chest X-ray images
- **Detailed results** showing probability scores for each class
- **User-friendly interface** with clear visual indicators

## Technical Details

- Built for Android 5.0 (API level 21) and above
- Uses TensorFlow Lite for model inference
- Implements quantized int8 model for efficient performance
- Handles both camera capture and gallery selection
- Displays results in a stylish modal dialog

## Model Information

- Binary classification (normal vs. viral pneumonia)
- Input: 300×300 RGB images
- Model type: Convolutional Neural Network
- Accuracy: 97.3%
- Trained using Edge Impulse

## Installation

1. Clone this repository
2. Open the project in Android Studio
3. Build and run on your Android device

```bash
git clone https://github.com/zbluee/lung-disease-classifier-edge-ai.git
cd lung-disease-classifier-edge-ai.git
```

## Usage

1. Open the app
2. Either take a picture using the "Take Picture" button or upload an existing X-ray image using the "Upload Image" button
3. Wait for the classification result
4. View the detailed breakdown of classification probabilities

## Dependencies

```gradle
dependencies {
    implementation 'org.tensorflow:tensorflow-lite:2.10.0'
    implementation 'org.tensorflow:tensorflow-lite-support:0.4.2'
    implementation 'androidx.camera:camera-camera2:1.2.1'
    implementation 'androidx.camera:camera-lifecycle:1.2.1'
    implementation 'androidx.camera:camera-view:1.2.1'
    implementation 'pub.devrel:easypermissions:3.0.0'
    
}
```

## Project Structure

- `TFLiteClassifier.java`: Handles the TensorFlow Lite model loading and inference
- `MainActivity.java`: Controls the user interface and camera/gallery interactions
- `activity_main.xml`: Main layout file
- `dialog_classification_result.xml`: Layout for the results dialog

## License

[MIT License](LICENSE)

## Acknowledgments

- X-ray dataset provided by [Kaggle lung-disease datasets](https://www.kaggle.com/datasets/fatemehmehrparvar/lung-disease)
- Model trained using [Edge Impulse](https://www.edgeimpulse.com/)
- Application based on the guide from [Edge Impulse blog](https://www.edgeimpulse.com/blog/training-deploying-lung-disease-classifier-on-android-guide-for-android-developers-and-ml-engineers/)