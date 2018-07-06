package com.example.android.lazyimageload;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class ImageLoader {
    private static final String LOG_TAG = ImageLoader.class.getName();
    //initialize the size of memorycache
    MemoryCache memoryCache;
    FileCache fileCache;
    //Create Map (collection) to store imageview and image url in key value pair
    private Map<ImageView,String> imageViewStringMap = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    ExecutorService executorService;
    //handler to display images in UI thread
    Handler handler = new Handler();
    // default image show in list (Before online image download)
    final int stub_id = R.drawable.ic_launcher_background;
    private Context mContext;

    public ImageLoader(Context context){
        //initialize cachedir file in filecache
        memoryCache = new MemoryCache();
        Log.d(LOG_TAG,"memory cache initialised");
        fileCache = new FileCache(context);
        Log.d(LOG_TAG,"file cache initialised");
        // Creates a thread pool that reuses a fixed number of
        // threads operating off a shared unbounded queue.
        executorService = Executors.newFixedThreadPool(5);
        mContext =context;
    }
    public void displayImage(String url, ImageView imageView){
        //Store image and url in Map
        imageViewStringMap.put(imageView,url);
        //Check image is stored in MemoryCache Map or not (see MemoryCache.java)
        Bitmap bitmap = memoryCache.get(url);
        if(bitmap!=null){
            imageView.setImageBitmap(bitmap);
            Log.d(LOG_TAG,"image exists in memory cache");
        }else{
            //queue Photo to download from url
            queuePhoto(url,imageView);
            Log.d(LOG_TAG,"image doesn't exist in memory cache,queue to get image");
            //Before downloading image show default image
            imageView.setImageResource(stub_id);
        }
    }

    private void queuePhoto(String url,ImageView imageView){
        // Store image and url in PhotoToLoad object
        PhotoToLoad photoToLoad = new PhotoToLoad(url,imageView);
        // pass PhotoToLoad object to PhotosLoader runnable class
        // and submit PhotosLoader runnable to executers to run runnable
        // Submits a PhotosLoader runnable task for execution
        executorService.submit(new PhotosLoader(photoToLoad));
    }
    private class PhotoToLoad{
        private String url;
        private ImageView imageView;
        private PhotoToLoad(String url,ImageView imageView){
            this.url = url;
            this.imageView = imageView;
        }

        public String getUrl() {
            return url;
        }

        public ImageView getImageView() {
            return imageView;
        }
    }

    private class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;

        public PhotosLoader(PhotoToLoad photoToLoad) {
            this.photoToLoad = photoToLoad;
        }

        @Override
        public void run() {
           try{
               //imageview is resued after user has scrolled to a new position
                if(imageViewReused(photoToLoad)){
                    Log.d(LOG_TAG,"1 cp, imageview is reused, cancel load/download image");
                    return;
                }
               // download image from web url or local file
                Bitmap bmp = getBitmap(photoToLoad.getUrl());
               // set image data in Memory Cache
               memoryCache.put(photoToLoad.getUrl(),bmp);
               if(imageViewReused(photoToLoad)){
                   Log.d(LOG_TAG,"2 cp, imageview is reused, cancel load/download image");
                   return;
               }
               // Get bitmap to display
               BitmapDisplayer bitmapDisplayer = new BitmapDisplayer(bmp,photoToLoad);
               handler.post(bitmapDisplayer);
           }catch (Throwable throwable){
               throwable.printStackTrace();
           }
        }
    }

    private boolean imageViewReused(PhotoToLoad photoToLoad){
        String url = imageViewStringMap.get(photoToLoad.getImageView());
        if(!url.equals(photoToLoad.getUrl())){
            //Check url is already exist in imageViews MAP
            Log.d(LOG_TAG,"IMAGEVIEWREUSED METHOD CALLED, and imageview is reused");
            return true;
        }
        Log.d(LOG_TAG,"IMAGEVIEWREUSED METHOD CALLED, and imageview is not reused");
        return false;
    }

    private Bitmap getBitmap(String url){
        //if the image exists in local storage file, get it and return it
        File file = fileCache.getFile(url);
        Log.d(LOG_TAG,"create a file based on url");
        //from SD cache
        //CHECK : if trying to decode file which not exist in cache return null
        Log.d(LOG_TAG,"decode file from local storage");
        Bitmap bmp = decodeFile(file);
        if(bmp!=null){
            Log.d(LOG_TAG,"image exists in file cache, return the image");
            return bmp;
        }
        // else: Download image file from web
        try {
            Bitmap bitmap = null;
            URL imageUrl = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) imageUrl.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(true);
            InputStream inputStream = connection.getInputStream();
            // Constructs a new FileOutputStream that writes to file
            // if file not exist then it will create file
            OutputStream outputStream = new FileOutputStream(file);
            // See Utils class CopyStream method
            // It will copy each pixel from input stream and
            // write pixels to output stream (file)
            Utils.copyStream(inputStream,outputStream);
            Log.d(LOG_TAG,"decode file downloaded from internet");
            Log.d(LOG_TAG,"Now the outputstream is:"+outputStream);
            Log.d(LOG_TAG,"Now the new file is: "+file);
            bitmap = decodeFile(file);
            outputStream.close();
            connection.disconnect();
            Log.d(LOG_TAG,"image downloaded from web");
            return bitmap;
        }catch (Throwable throwable){
            throwable.printStackTrace();
            if(throwable instanceof OutOfMemoryError) memoryCache.clear();
            return null;
        }
    }

    private Bitmap decodeFile(File file){
        try{
            //refer to android document:load large bitmap efficiently
            //Decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            //still query the bitmap without allocating the memory
            options.inJustDecodeBounds = true;
            FileInputStream fileInputStream = new FileInputStream(file);
            Log.d(LOG_TAG,"the filestream to be decoded down is:"+fileInputStream);
            BitmapFactory.decodeStream(fileInputStream,null,options);
            fileInputStream.close();
            //Find the correct scale value. It should be the power of 2.

            // Set width/height of recreated image
            final int REQUIRED_SIZE = 85;
            int width_tmp = options.outWidth, height_tmp = options.outHeight;
            int scale = 1;
            Log.d(LOG_TAG,"the width is:"+width_tmp+"the height is:"+height_tmp+"the scale is: "+scale);
            while(true){
                if(width_tmp < REQUIRED_SIZE || height_tmp < REQUIRED_SIZE){
                    break;
                }
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }
            Log.d(LOG_TAG,"the new width is:"+width_tmp+"the new height is:"+height_tmp+"the scale is: "+scale);
            //decode with current scale values
            BitmapFactory.Options options1 = new BitmapFactory.Options();
            options1.inSampleSize = scale;
            options1.inJustDecodeBounds = false;
           // options.inSampleSize = scale;
            FileInputStream fileInputStream1 = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream1,null,options1);

            //Log.d(LOG_TAG,"bitmap resized to 85");
            //Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream,null,options);
            Log.d(LOG_TAG,"the filestream to be decoded down is:"+fileInputStream1);

            return bitmap;
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    class BitmapDisplayer implements Runnable{
        Bitmap bitmap;
        PhotoToLoad photoToLoad;

        public BitmapDisplayer(Bitmap bitmap, PhotoToLoad photoToLoad) {
            this.bitmap = bitmap;
            this.photoToLoad = photoToLoad;
        }

        @Override
        public void run() {
            if(imageViewReused(photoToLoad)){
                Log.d(LOG_TAG,"3 cp, imageview is reused, cancel load/download image");
                return;
            }

            if(bitmap!=null){
                photoToLoad.getImageView().setImageBitmap(bitmap);
                Log.d(LOG_TAG,"imageview is not reused and bitmap has been loaded either from local file or web");
            } else{
                photoToLoad.getImageView().setImageResource(stub_id);
                Log.d(LOG_TAG,"imageview is not reused, but image is null");
            }

        }
    }

    public void clearCache(){
        memoryCache.clear();
        fileCache.clear();
        Log.d(LOG_TAG,"memory and file cache cleared");
    }

}
