package com.ranit.botscraft.utils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

    public static File from(Context context, Uri uri) throws IOException {

        InputStream inputStream =
                context.getContentResolver().openInputStream(uri);

        File tempFile = new File(
                context.getCacheDir(),
                "upload_" + System.currentTimeMillis() + ".jpg"
        );

        OutputStream out = new FileOutputStream(tempFile);

        byte[] buf = new byte[1024];
        int len;

        while ((len = inputStream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        out.close();
        inputStream.close();

        return tempFile;
    }
}

