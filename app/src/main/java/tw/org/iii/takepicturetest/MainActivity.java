package tw.org.iii.takepicturetest;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.bm.library.PhotoView;
import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    private ImageView imageView, folder, camera;
    private String mCurrentPhotoPath;
    private Uri photoURI;
    private Uri photoOutputUri = null; // 图片最终的输出文件的 Uri
    private static final int CROP_PHOTO_REQUEST_CODE = 5; // 裁剪图片返回的 requestCode
    private File photoFile;
    private PhotoView photoView;
    private int screenWidth;
    private Uri dataUri;
    private Bitmap bitmap;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission();
        photoView = findViewById(R.id.main_photoView);
        folder = findViewById(R.id.main_folder);
        camera = findViewById(R.id.main_camera);
        File file = Environment.getDataDirectory();
//            Log.v("brad", file.getAbsolutePath());
        File picture = getExternalFilesDir("Picture");
//        Log.v("brad", picture.getAbsolutePath());
    }

    private void init(){
        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFile();
            }
        });
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //檢查是否有照相功能
                PackageManager pmgr = getPackageManager();
                if(pmgr.hasSystemFeature(PackageManager.FEATURE_CAMERA)){
                    dispatchTakePictureIntent();
                }
            }
        });

    }
    //請求權限：CAMERA & WRITE_EXTERNAL_STORAGE
    private void requestPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                                 Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                 Manifest.permission.READ_EXTERNAL_STORAGE},
                    0);
        }else{
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init();
        }else{
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            bitmap = null;
            dataUri = FileProvider.getUriForFile
                    (this, "tw.org.iii.takepicturetest", photoFile);
            try {
//                bitmap = BitmapFactory.decodeStream(
//                        getContentResolver().openInputStream(dataUri));
                bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                photoView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }


//            setPic();


//            Log.v("brad", "photoFile = " + photoFile.getAbsolutePath());
//            Glide.with(this)
//                    .load(photoFile)
//                    .into(photoView);
//            photoView.enable();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        // 確保有相機來處理intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go

            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File...
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(MainActivity.this,
                        "tw.org.iii.takepicturetest",
                        photoFile);
//                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(Intent.createChooser(takePictureIntent, "TakePhoto"), REQUEST_TAKE_PHOTO);
                Log.v("brad", "photoURI = " + photoURI);
                galleryAddPic();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        // timeStamp的格式-> 20180420_004839
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        /**  getExternalFilesDir() 需要給的是type參數
         *   傳回的是該app packagename 底下,參數的系統位置
         *  storage/emulated/0/Android/data/tw.org.iii.takepicturetest/files/Pictures
         */
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        Log.v("brad", "image = " + image.getAbsolutePath());
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
    /**
     * 將照片新增到系統媒體提供商，讓您的照片易於訪問
     * 如果您將照片保存到提供的目錄中 getExternalFilesDir()，
     * 則媒體掃描程序無法訪問這些文件，因為它們對您的應用程序是私人的。
     */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    //取得螢幕大小
    private void getScreenSize(){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int newHeight = screenWidth / 16 * 9;
    }
    // 解碼縮放的圖像
    private void setPic() {
        // Get the dimensions of the View
        int targetW = photoView.getWidth();
        int targetH = photoView.getHeight();
        Log.v("brad", targetW + ":" + targetH);
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        //bmOptions將不會生成bitmap
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image (確定縮小圖像的程度)
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        //表示對圖像像素的縮放比例
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        photoView.setImageBitmap(bitmap);
    }


    /**
     * 上傳檔案用
     * @param
     *
     * / http://36.235.38.228:8080/fsit04/photo?user_id=1  傳完到這邊看有沒有成功
     */

    private void uploadFile() {
        String uploadUrl = "http://36.235.39.18:8080/fsit04/saveFile";
        final byte[] data ;
        //路徑上傳
//        File upload =new File(sdroot,"檔案的路徑");
//        data =filePathToByte(upload);


        //BitMap上傳
//        Bitmap bmp = BitmapFactory.decodeResource(resources,R.drawable.test);
        data=bitmapToBytes(bitmap);
        Log.v("brad", "getWidth()" + bitmap.getWidth());
        VolleyMultipartRequest multipartRequest =
                new VolleyMultipartRequest(
                        Request.Method.POST,
                        uploadUrl,
                        new Response.Listener<NetworkResponse>(){
                            @Override
                            public void onResponse(NetworkResponse response) {
                                Log.v("brad", "code: " + response.statusCode);
                            }
                        },
                        null){
                    @Override
                    protected Map<String, DataPart> getByteData()
                            throws AuthFailureError {

                        HashMap<String,DataPart> params = new HashMap<>();
                        //傳檔案
                        params.put("file",new DataPart("iii05.jpg", data));

                        return params;
                    }

                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        HashMap<String, String> m1 =new HashMap<>();
                        //使用者ID
                        m1.put("user_id","1");
                        //景點ID
                        m1.put("total_id","1");
                        //lat
                        m1.put("lat","25.00");
                        //lng
                        m1.put("lng","121.00");

                        return m1;
                    }
                };

        queue.add(multipartRequest);
    }

    /**
     *
     * @param file  檔案路徑轉BYTE 陣列
     * @return
     */
    private byte[] filePathToByte(File file){
        byte[] data = new byte[(int) file.length()];
        try {
            FileInputStream fin = new FileInputStream(file);
            fin.read(data);
            fin.close();
        }catch (Exception e){

        }

        return data;
    }
    /**
     *
     * @param bm    Bitmap 轉Byte[];
     * @return
     */
    private byte[] bitmapToBytes(Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

}
