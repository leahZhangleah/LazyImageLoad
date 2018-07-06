package com.example.android.lazyimageload;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MemoryCache {
    private static final String LOG_TAG = MemoryCache.class.getName();
    /**
     * Constructs an empty <tt>LinkedHashMap</tt> instance with the
     * specified initial capacity, load factor and ordering mode.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @param  accessOrder     the ordering mode - <tt>true</tt> for
     *         access-order, <tt>false</tt> for insertion-order
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    private Map<String,Bitmap> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, Bitmap>(10,1.5f,true));
    //current allocated size
    private long size = 0;
    //max memory cache folder used to download images in bytes
    private long limit = 1000000;

    public MemoryCache(){
        //USE 1/8 of available heap size
        setLimit(Runtime.getRuntime().maxMemory()/8);
    }

    public void setLimit(long new_limit){
        limit = new_limit;
        Log.d(LOG_TAG,"MemoryCache will use up to "+limit/1024/1024+".MB");
    }

    public Bitmap get(String id){
        try{
            if(!cache.containsKey(id)){
                return null;
            }
            return cache.get(id);
        }catch (NullPointerException e){
            e.printStackTrace();
            return null;
        }
    }

    public void put(String id, Bitmap bitmap){
        try{
            //if key exists, replace old bitmap with new bitmap
            if(cache.containsKey(id)){
                size-= getSizeInBytes(cache.get(id));
                cache.put(id,bitmap);
                size += getSizeInBytes(bitmap);
                checkSize();
            }
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }
    }

    private long getSizeInBytes(Bitmap bitmap){
        if (bitmap==null){
            return 0;
        }
        //almost same as bitmap.getbytecount()-->if sdkversion > honeycomb
        return bitmap.getRowBytes()*bitmap.getHeight();
    }

    private void checkSize(){
        Log.d(LOG_TAG,"cache size =" + size+"length="+cache.size());
        if (size>limit){
            //least recently accessed item will be the first one iterated
            Set set = cache.entrySet();
            Log.d(LOG_TAG,"the set value is: "+set);
            Iterator<Map.Entry<String,Bitmap>> iterator = set.iterator();
            while(iterator.hasNext()){
                Map.Entry<String,Bitmap> entry = iterator.next();
                //because we set true for accessorder in map,the earliest accessed entry will be removed first
                size-=getSizeInBytes(entry.getValue());
                iterator.remove();
                if (size < limit){
                    break;
                }
            }
            Log.d(LOG_TAG,"Clean cache.New size "+cache.size());
        }
    }

    public void clear(){
        try{
            //clear cache
            cache.clear();
            size= 0;
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }
}
