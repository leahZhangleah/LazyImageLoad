package com.example.android.lazyimageload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    public static void copyStream(InputStream is, OutputStream os){
        final int buffer_size = 1024;
        try{
            byte[] bytes = new byte[buffer_size];
            int bytesRead;
            while((bytesRead = is.read(bytes))!=-1){
                os.write(bytes,0,bytesRead);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
