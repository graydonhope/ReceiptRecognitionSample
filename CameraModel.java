package com.example.grayd.cameraapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

public class CameraModel extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private boolean imageWasTaken = false;
    private String imageFileLocation;
    private FirebaseVisionImage fbVisionImage;
    private FirebaseVisionTextRecognizer textRecognizer;
    private static boolean textRecognized = false;
    private Task<FirebaseVisionText> result;
    private FirebaseVisionText recognizedText;
    private final String TAG = "@@@@@@@@@!!!!!!!!!!!!!!";
    private List<String> blocks = new ArrayList<>();
    private List<String> lines = new ArrayList<>();
    private ArrayList<String> itemsOnReceipt = new ArrayList<>();
    private ArrayList<String> pricesOnReceipt = new ArrayList<>();
    private ArrayList<Double> pricesOfItems = new ArrayList<>();

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Get the angle by which an image must be rotated given the devices current
     * orientation
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int getRotationCompensation(String cameraId, Activity activity, Context context)
            throws CameraAccessException {
        // Get devices current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the devices rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION);
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

        int result;
        switch (rotationCompensation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                Log.e("@@@@@@!!!!!!!!", "Bad Rotation Value: " + rotationCompensation);
        }
        return result;
    }

    protected File createImageFile() throws IOException {
        /**
         * Create the file path location where the image will be saved to the phone. Use a timestamp as a way to create unique path.
         */
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String imageName = "IMAGE_" + timeStamp + "_";
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageName, ".jpg", storageDirectory);
        imageFileLocation = image.getAbsolutePath();
        return image;
    }

    public String getImageFileLocation() {
        /**
         * @Return: The file location of the image to be saved.
         */
        return this.imageFileLocation;
    }

    public Bitmap decodeFile(String path) {
        /**
         * @Input: Image file path
         * @Return: Rotate the Bitmap image to the default 'native' orientation to compensate for any 'tilt' or 'rotation'
         * the user may have incurred while the picture was being captured. Essentially rotates the Bitmap photo so that it is the correct orientation.
         */
        int orientation;
        try {
            if (path == null) {
                return null;
            }
            // decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            // Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE = 70;
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 0;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE
                        || height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale++;
            }
            // decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            Bitmap bm = BitmapFactory.decodeFile(path, o2);
            Bitmap bitmap = bm;

            ExifInterface exif = new ExifInterface(path);

            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            Log.e("ExifInteface .........", "rotation =" + orientation);

            //exif.setAttribute(ExifInterface.ORIENTATION_ROTATE_90, 90);

            Log.e("orientation", "" + orientation);
            Matrix m = new Matrix();

            if ((orientation == ExifInterface.ORIENTATION_ROTATE_180)) {
                m.postRotate(180);
                //m.postScale((float) bm.getWidth(), (float) bm.getHeight());
                // if(m.preRotate(90)){
                Log.e("in orientation", "" + orientation);
                bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
                return bitmap;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                m.postRotate(90);
                Log.e("in orientation", "" + orientation);
                bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
                return bitmap;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                m.postRotate(270);
                Log.e("in orientation", "" + orientation);
                bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
                return bitmap;
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    protected void setImageTaken(boolean wasTaken) {
        /**
         * @Input: Boolean value that holds whether the image was taken or not.
         */
        imageWasTaken = wasTaken;
    }

    protected boolean imageWasTaken() {
        /**
         * @Return: Returns a boolean value if the image has been taken.
         */
        return this.imageWasTaken;
    }

    protected void createFbVisionImage(Bitmap capturedImage) {
        /**
         * Creates the FirebaseVisionImage from the captured Bitmap image for the CameraModel to use later.
         * @Input: Bitmap of the captured image
         */
        if (capturedImage != null) {
            fbVisionImage = FirebaseVisionImage.fromBitmap(capturedImage);
            textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }
        else {
            throw new IllegalArgumentException("Bitmap image cannot be null");
        }
    }

    public FirebaseVisionImage getFbVisionImage() {
        /**
         * @Return: Returns the FirebaseVisionImage.
         */
        return this.fbVisionImage;
    }

    protected void processImage(FirebaseVisionImage fbVisionImage, final boolean isItemList, final ProcessImageOnSuccessListener listener) {
        /**
         * @Input: FirebaseVisionImage          - Used by the FirebaseVisionTextRecognizer
         * @Input: Boolean isItemList           - Gives method information needed to know how to 'clean' the data for later processing. Also tells where to set the text.
         * @Input ProcessImageOnSuccessListener - Interface which ensures the setTextFromProcessedImage is implemented by the calling method in RecognizedTextActivity.
         * ... This is required so that RecognizedTextActivity waits for the onSuccess callback before proceeding to set the text. Otherwise it would happen
         * ... too quickly and set null values because the image hasn't been processed in time. the 'on device' AI model takes some time brooo.
         *
         */
        // result = Task<FirebaseVisionText> returned by processImage method.
        result = textRecognizer.processImage(fbVisionImage)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        textRecognized = true;
                        recognizedText = firebaseVisionText;

                        // Organize by lines
                        String resultText = firebaseVisionText.getText();
                        for (FirebaseVisionText.TextBlock block: firebaseVisionText.getTextBlocks()) {
                            String blockText = block.getText();
                            blocks.add(blockText);
                            // Keep this for later use (potentially to show some cool stats on recognition)

                            // Float blockConfidence = block.getConfidence();
                            //List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
                            //Point[] blockCornerPoints = block.getCornerPoints();
                            // Rect blockFrame = block.getBoundingBox();
                            for (FirebaseVisionText.Line line : block.getLines()) {
                                String lineText = line.getText();

                                if (isItemList) {
                                    // Check if the line of text contains a number. This is part of the organization process of the data.
                                    char[] checkIfDigit = lineText.toCharArray();
                                    boolean hasDigit = hasDigit(checkIfDigit);

                                    if(!hasDigit) {
                                        itemsOnReceipt.add(lineText);
                                    }
                                }
                                else {
                                    // Remove all letters.
                                    String noLetters = removeLetters(lineText);

                                    try {
                                        Double priceOfItem = Double.valueOf(noLetters);
                                        pricesOfItems.add(priceOfItem);
                                        pricesOnReceipt.add(String.valueOf(priceOfItem));
                                    }
                                    catch (NumberFormatException e) {
                                        e.printStackTrace();
                                    }
                                }
                                lines.add(lineText);
                                //Float lineConfidence = line.getConfidence();
                                //List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                                //Point[] lineCornerPoints = line.getCornerPoints();
                                //Rect lineFrame = line.getBoundingBox();
                            }
                        }

                        if (isItemList) {
                            listener.setTextFromProcessedImage(itemsOnReceipt);
                        }
                        else {
                            listener.setTextFromProcessedImage(pricesOnReceipt);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: process image failure.");
                    }
                });
    }

    protected FirebaseVisionText getFirebaseVisionText() {
        /**
         * @Return: The FirebaseVisionText object which contains all the recognition details (e.g: blocks, lines, accuracy ... )
         */
        if (textRecognized) {
            return recognizedText;
        }
        else {
            Log.d(TAG, "getRecognizedText: Get recognized text returned null...");
            return null;
        }
    }

    protected ArrayList<Double> getPriceList() {
        /**
         * @Return: The ArrayList of prices in the converted (from String values) to Double form.
         */
        if(pricesOfItems == null){
            Log.d(TAG, "getPriceList: Inside CameraModel. PriceList is null when being sent");
        }
        return this.pricesOfItems;
    }

    public List<String> getBlocks() {
        return this.blocks;
    }

    public List<String> getLines() {
        return this.lines;
    }

    public ArrayList<String> getListItems() {
        return this.itemsOnReceipt;
    }

    private boolean hasDigit(char[] textToCheck){
        /**
         * Used for removing unnecessary values from the item list.
         * @Input: char[] of a line of text recognized.
         * @Return: Whether the the char[] of text contains any digits.
         */
        for (char i : textToCheck) {
            if(Character.isDigit(i)){
                return true;
            }
        }

        return false;
    }

    private String removeLetters(String contaminatedText) {
        /**
         * Used for cleaning the price values.
         * @Input: Text that may contain letters
         * @Return: A String of digits only.
         */
        String formattedValue = "";
        if (contaminatedText != null) {
            String noLowercase = contaminatedText.replaceAll("([a-z])", "");
            String noUppercase = noLowercase.replaceAll("([A-Z])", "");
            String noComma= noUppercase.replace(",", ".");
            formattedValue = noComma.replace(" ", "" );
        }
        else {
            throw new IllegalArgumentException("Text cannot be null.");
        }

        return formattedValue;
    }
}
