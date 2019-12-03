package com.example.grayd.cameraapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScanPictureActivity extends AppCompatActivity {

    private static final int ACTIVITY_START_CAMERA_APP = 0;
    private static final int STORAGE_PERMISSION_CODE = 0;
    private static final int PRICES_LIST_CROP_CODE = 777;
    private static final int ITEMS_LIST_CROP_CODE = 999;
    private final String TAG = "@@@@@@@@@!!!!!!!!!!!!!!";
    private ImageView capturedPhotoImageView;
    private Button cameraButton;
    private Button scanPhotoButton;
    private Button cropPricesButton;
    private Button cropItemsButton;
    private Bitmap priceListBitmap;
    private Bitmap itemListBitmap;
    private Bitmap fullPicture;
    private Bitmap capturedImage;
    private boolean priceListCropped;
    private boolean itemListCropped;
    private boolean permissionsAccepted;
    private CameraModel fbVisionImage;
    private File originalPhotoFile;
    private ProgressBar progressBar;
    private TextView loadingData;

    // TODO: 2019-03-31 Create Documentation for the methods in here.
    // TODO: 2019-08-13 Change animation so it slides the other way when returning to home screen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_picture);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        fbVisionImage = new CameraModel();
        priceListCropped = false;
        itemListCropped = false;
        permissionsAccepted = false;
        capturedPhotoImageView = findViewById(R.id.capturePhotoImageview);
        checkAndroidVersion();
        configureProgressBar();
        configureCropPricesButton();
        configureCropItemsButton();
        configureCameraButton();
        configureScanButton();
    }

    private void configureCameraButton() {
        /**
         * Check whether Writing to External Storage has been permitted on phone.
         */
        cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(ScanPictureActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    if (permissionsAccepted) {
                        // Storage permission has been granted
                        takePhoto();
                    }
                }
                else{
                    // Storage permission has not yet been granted.
                    requestStoragePermission();
                }
            }
        });
    }

    private void configureProgressBar() {
        /**
         * Set the ProgressBar to invisible.
         */
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        loadingData = findViewById(R.id.loadingDataText);
        loadingData.setVisibility(View.INVISIBLE);
    }

    private void configureScanButton() {
        /**
         * Add the scanPhoto method to the onClick scan button
         */
        scanPhotoButton = findViewById(R.id.scanPhotoButton);
        scanPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanPhoto();
            }
        });
    }

    private void configureCropPricesButton() {
        /**
         * If the image was taken, allow the user to call the crop photo method.
         */
        cropPricesButton = findViewById(R.id.cropPriceListButton);
        cropPricesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fbVisionImage.imageWasTaken()) {
                    priceListCropped = true;
                    cropRequest(Uri.fromFile(originalPhotoFile), PRICES_LIST_CROP_CODE);
                }
                else {
                    Toast.makeText(getApplicationContext(), "Picture has not been taken yet.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void configureCropItemsButton() {
        /**
         * If the image was taken, allow the user to call the crop photo method.
         */
        cropItemsButton = findViewById(R.id.cropItemsButton);
        cropItemsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fbVisionImage.imageWasTaken()){
                    itemListCropped = true;
                    cropRequest(Uri.fromFile(originalPhotoFile), ITEMS_LIST_CROP_CODE);
                }
                else {
                    Toast.makeText(getApplicationContext(), "Picture has not been taken yet.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void takePhoto() {
        /**
         * Start the Camera Intent to capture an image.
         */
        Intent callCameraAppIntent = new Intent();
        // Intent that passes a message to an application that responds to action_image_camera.
        callCameraAppIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = null;

        try{
            photoFile = createImageFile();
            originalPhotoFile = photoFile;
        }
        catch(IOException e){
            Log.d(TAG, "takePhoto: in IOException ");
            e.printStackTrace();
        }

        callCameraAppIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
        // Starting activity passing callCameraAppIntent above. Then returns to this activity.
        startActivityForResult(callCameraAppIntent, ACTIVITY_START_CAMERA_APP);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        /**
         * @Input: int requestCode - to see whether the cropRequest method, CropImage, or Camera activity is called.
         * @Input: int resultCode  - to see whether the activity can be started successfully.
         * @Input: Intent data     - to start the CropImage activity.
         */

        if( requestCode == ACTIVITY_START_CAMERA_APP && resultCode == RESULT_OK){
            capturedImage = fbVisionImage.decodeFile(fbVisionImage.getImageFileLocation());
            capturedPhotoImageView.setImageBitmap(capturedImage);
            fbVisionImage.setImageTaken(true);
        }

        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            cropRequest(Uri.fromFile(originalPhotoFile), 0);
        }

        // Result from cropping activity
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                try {
                    int cropSelection = data.getIntExtra("buttonID", 797);
                    fullPicture = MediaStore.Images.Media.getBitmap(getContentResolver(), result.getUri());

                    if(cropSelection == ITEMS_LIST_CROP_CODE) {
                        itemListBitmap = fullPicture;
                    }
                    else {
                        priceListBitmap = fullPicture;
                    }

                    capturedPhotoImageView.setImageBitmap(fullPicture);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File createImageFile() throws IOException {
        /**
         * @Return: Uses CameraModel to create a File for the image.
         */
        File imageFile = fbVisionImage.createImageFile();
        return imageFile;
    }

    private void requestStoragePermission(){
        /**
         * Asks user to accept the storage permissions.
         */
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this).setTitle("Permission Required").setMessage("This Storage Permission is required so " +
                    "that the AI model can have a more clear picture to work with").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(ScanPictureActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                }
            })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        }
        else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        /**
         * @Input: int requestCode - For various permissions
         * @Input: String[] permissions - for @NotNull parameter override
         * @Input: int[] grantResults - Permission results given
         */

        if(requestCode == STORAGE_PERMISSION_CODE){
            if( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission was granted", Toast.LENGTH_SHORT).show();
                cameraButton.setClickable(true);
            }
            else {
                Toast.makeText(this, "Permission was denied", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == 555 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionsAccepted = true;
            cameraButton.setClickable(true);
            //pickImage();
        }
        else {
            Toast.makeText(this, R.string.message_no_storage_permission, Toast.LENGTH_LONG).show();
            cameraButton.setClickable(false);
        }
    }

    private void scanPhoto() {
        /**
         * Passing the file locations (original, cropped items, and cropped prices) into the RecognizedTextActivity activity.
         */

        if ( fbVisionImage.imageWasTaken() ) {

            if(listsCropped()) {

                // Only allow the user to press the scan button once. App crashes otherwise.
                scanPhotoButton.setClickable(false);
                fadeOutImage(capturedPhotoImageView);
                enableProgressBar();

                // Create an instance of the inner (nested) Progress class Asynchronously. This delegates the AI model work
                // to another thread while leaving the main UI thread open. This is so I can provide the progress bar (loading circle)
                // functionality because the text recognition takes some time to complete.
                new Progress().execute();
            }
            else {
                Toast.makeText(getApplicationContext(), "Please crop both the item and price list!", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "Picture not taken yet.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void addFilenamesToIntent() {
        /**
         * Compressing the Bitmap image and adding the filename of where it is stored on the phone externally into the intent to be
         * ... passed to the next activity (RecognizedTextActivity).
         */
        try {
            String originalPhoto = "bitmap.png";
            FileOutputStream fileOutputStream = this.openFileOutput(originalPhoto, Context.MODE_PRIVATE);
            capturedImage.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);

            try {
                // Cleanup
                fileOutputStream.close();
                capturedImage.recycle();

                // Getting cropped photo filenames
                String[] filenames = getCroppedFilenames();
                filenames[2] = originalPhoto;

                // Putting image files' details into the intent to send to RecognizedTextActivity class.
                Intent intent = new Intent(this, RecognizedTextActivity.class);
                intent.putExtra("images", filenames);
                startActivity(intent);
            } catch (IOException e) {
                Log.d(TAG, "scanPhoto: IOException");
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            Log.d(TAG, "scanPhoto: File not found exception");
            e.printStackTrace();
        }
    }

    private class Progress extends AsyncTask<Void, Void, Integer> {
        /**
         * Inner (nested) class which extends AsyncTask to allow multiple threads to run at the same time.
         * This class completes the AI text recognition work while the main UI thread can update the View components.
         *
         */

        @Override
        protected Integer doInBackground(Void... voids) {
            addFilenamesToIntent();
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            disableProgressBar();
        }
    }

    private String[] getCroppedFilenames() {
        /**
         * Adding the cropped file name locations to the list of filenames.
         */
        String[] filenames = new String[3];

        if (itemListCropped) {

            try {
                String itemListFilename = "itemList.png";
                FileOutputStream fileOutputStream = this.openFileOutput(itemListFilename, Context.MODE_PRIVATE);
                itemListBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                filenames[0] = itemListFilename;

                try {
                    // Cleanup
                    fileOutputStream.close();
                    capturedImage.recycle();
                }
                catch(IOException e){
                    e.printStackTrace();
                }
            }
            catch(FileNotFoundException e){
                e.printStackTrace();
            }
        }

        if (priceListCropped) {

            try {
                String priceListFilename = "priceList.png";
                FileOutputStream fileOutputStream = this.openFileOutput(priceListFilename, Context.MODE_PRIVATE);
                priceListBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                filenames[1] = priceListFilename;

                try {
                    // Cleanup
                    fileOutputStream.close();
                    capturedImage.recycle();
                }
                catch(IOException e){
                    e.printStackTrace();
                }
            }
            catch(FileNotFoundException e){
                e.printStackTrace();
            }
        }
        return filenames;
    }

    public void checkAndroidVersion() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 555);
        }
        else {
            Log.d(TAG, "checkAndroidVersion: Android version approved permissions!");
        }
    }

    private void cropRequest(Uri imageUri, int cropDistinguish) {
        /**
         * @Input: URI - for the CropImage activity
         * @Input: int cropDistinguish - value which let's it know where to save the resulting cropped image. This is so that
         * ... the items crop bitmap image can be saved separate from the prices list bitmap. This is so they can be processed separately.
         */
        if(imageUri != null) {

            if((fbVisionImage.imageWasTaken()) && (permissionsAccepted)){
                Intent intent = CropImage.activity(imageUri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMultiTouchEnabled(true)
                        .getIntent(getApplicationContext());
                intent.putExtra("buttonID", cropDistinguish);
                startActivityForResult(intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE);
            }

            else{
                Toast.makeText(getApplicationContext(), "Picture has not been taken yet.", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Log.d(TAG, "cropRequest: Image URI is null.");
            throw new IllegalArgumentException("Image cannot be null. Probably not taken yet.");
        }
    }

    @Override
    public void onBackPressed() {
        /**
         * Takes the user back to the HomePage Activity.
         */
        Intent homePageIntent = new Intent(this, HomePageActivity.class);
        startActivity(homePageIntent);
    }

    private boolean listsCropped() {

        if (priceListCropped && itemListCropped) {
            return true;
        }
        else {
            return false;
        }
    }

    private void enableProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        loadingData.setVisibility(View.VISIBLE);
    }

    private void disableProgressBar() {
        progressBar.setVisibility(View.GONE);
        loadingData.setVisibility(View.GONE);
    }

    private void fadeOutImage(final ImageView imageView) {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(7500);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Do nothing
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Do nothing
            }
        });

        imageView.startAnimation(fadeOut);
    }
}