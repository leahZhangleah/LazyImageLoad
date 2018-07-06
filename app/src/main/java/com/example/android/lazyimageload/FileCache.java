package com.example.android.lazyimageload;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class FileCache {
    private static final String LOG_TAG = FileCache.class.getName();
    private File cacheDir;
    public FileCache(Context context){
        //initiate cachedir
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            //if SDCARD is mounted (SDCARD is present on device and mounted)
            cacheDir = new File(Environment.getExternalStorageDirectory(),"LazyList");
            Log.d(LOG_TAG,"create file cache directory from external storage dir");
        }else {
            // if checking on simulator the create cache dir in your application context
            cacheDir = context.getCacheDir();
            Log.d(LOG_TAG,"create file cache directory from cache dir");
        }
        if (!cacheDir.exists()){
            Log.d(LOG_TAG,"file cache directory doesn't exist. create a new one");
            cacheDir.mkdir();
        }
    }

    public File getFile(String url){
        //Identify images by hashcode or encode by URLEncoder.encode.
        //make unique file based on url
        String filename = String.valueOf(url.hashCode());
        File f = new File(cacheDir,filename);
        return f;
    }

    public void clear(){
        //list all files inside cache directory
        File[] files = cacheDir.listFiles();
        if(files==null){
            return;
        }
        for(File file:files){
            file.delete();
        }
    }
}
