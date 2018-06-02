package app.izhang.faciallogin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class MainActivity extends AppCompatActivity {

    private static int CAMERA_PHOTO_REQUEST_CODE = 1111;
    private static int CAMERA_PHOTO_VERIFY_REQUEST = 1001;
    private final int CAMERA_PERMISSION_REQUEST = 1;


    private ImageView tempImageView;
    private File savedImageFile;
    private File compareImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button regbtn = this.findViewById(R.id.btn_reg);
        Button logBtn = this.findViewById(R.id.btn_login);
        tempImageView = this.findViewById(R.id.imageView);

        regbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAndRequestPermissions();
            }
        });

        logBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verifyImages();
            }
        });

        savedImageFile = new File(getApplicationContext().getFilesDir(), FileManager.BASE_IMG_REF);
        if(savedImageFile.exists()){
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = null;
            try {
                bitmap = handleSamplingAndRotationBitmap(this, Uri.fromFile(savedImageFile.getAbsoluteFile()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(bitmap != null) tempImageView.setImageBitmap(bitmap);
        }


    }

    private void checkAndRequestPermissions(){
        // Check for permissions
        // Checking if external storage permissions is available
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }else{
            startCameraIntentForRegister();
        }

    }

    private void verifyImages(){

        savedImageFile = new File(getApplicationContext().getFilesDir(), FileManager.BASE_IMG_REF);
        if(!savedImageFile.exists()){
            Toast.makeText(getApplicationContext(), "Please register an image first", Toast.LENGTH_LONG).show();
            return;
        }

        Intent launchCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        compareImageFile = new File(getApplicationContext().getFilesDir(), FileManager.COMPARE_IMG_REF);

        launchCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                FileProvider.getUriForFile(
                        this,
                        "app.izhang.faciallogin.fileprovider",
                        savedImageFile)
        );

        // Checks to see if the phone has a camera app/hardware
        if(launchCameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(launchCameraIntent, CAMERA_PHOTO_VERIFY_REQUEST);
        }
    }

    private void startCameraIntentForRegister(){
        Intent launchCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        savedImageFile = new File(getApplicationContext().getFilesDir(), FileManager.BASE_IMG_REF);

        Log.v("MainActivity", this.getApplicationContext().getPackageName());
        launchCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                FileProvider.getUriForFile(
                        this,
                        "app.izhang.faciallogin.fileprovider",
                        savedImageFile)
        );

        // Checks to see if the phone has a camera app/hardware
        if(launchCameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(launchCameraIntent, CAMERA_PHOTO_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCameraIntentForRegister();

                } else {
                    Toast.makeText(this, "Please accept the permissions to continue", Toast.LENGTH_LONG).show();
                }
                return;
            }


            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CAMERA_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            Toast.makeText(getApplicationContext(), "Successfully saved image", Toast.LENGTH_LONG).show();
        }else if(requestCode == CAMERA_PHOTO_VERIFY_REQUEST && resultCode == RESULT_OK){
            Toast.makeText(getApplicationContext(), "Successfully verified image ", Toast.LENGTH_LONG).show();
        }
    }


    /**************** Methods taken from: https://www.samieltamawy.com/how-to-fix-the-camera-intent-rotated-image-in-android/  to resolve rotation issues ********************/
    /**
     * This method is responsible for solving the rotation issue if exist. Also scale the images to
     * 1024x1024 resolution
     *
     * @param context       The current context
     * @param selectedImage The Image URI
     * @return Bitmap image results
     * @throws IOException
     */
    public static Bitmap handleSamplingAndRotationBitmap(Context context, Uri selectedImage)
            throws IOException {
        int MAX_HEIGHT = 1024;
        int MAX_WIDTH = 1024;

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(selectedImage);
        BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        imageStream = context.getContentResolver().openInputStream(selectedImage);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

        img = rotateImageIfRequired(img, selectedImage);
        return img;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options   An options object with out* params already populated (run through a decode*
     *                  method with inJustDecodeBounds==true
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {

        ExifInterface ei = new ExifInterface(selectedImage.getPath());
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

}
