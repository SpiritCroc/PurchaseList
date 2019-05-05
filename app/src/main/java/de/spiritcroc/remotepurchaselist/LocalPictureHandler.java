package de.spiritcroc.remotepurchaselist;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class LocalPictureHandler {

    private static final String TAG = LocalPictureHandler.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String LOCAL_PICTURE_PATH = "picture_upload_cache";

    // Don't instantiate
    private LocalPictureHandler() {}

    public static String importLocalPicture(Context context, Intent intent) {
        if (intent == null) {
            Log.e(TAG, "importLocalPicture called with null intent");
            return null;
        }
        Uri uri = intent.getData();
        if (uri == null) {
            Log.e(TAG, "importLocalPicture called with null data");
            return null;
        }

        InputStream inputStream = null;
        BufferedInputStream in = null;
        OutputStream out = null;

        String type = context.getContentResolver().getType(uri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
        if (DEBUG) Log.d(TAG, "Inserting file with type " + type);
        try {
            // Copy file to app memory
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException("Could not open inputStream from intent");
            }
            in = new BufferedInputStream(inputStream);
            File target = generateLocalPictureFile(context, extension);
            out = new FileOutputStream(target);
            byte[] data = new byte[1024];
            int len;
            while ((len = in.read(data)) > 0) {
                out.write(data, 0, len);
            }
            // Return resulting uri
            String result = target.toURI().toString();
            if (DEBUG) Log.d(TAG, "Inserted new picture: " + result);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /*
    public static String importCapturePictureThumbnail(Context context, Intent intent) {
        if (intent == null) {
            Log.e(TAG, "importCapturePicture called with null intent");
            return null;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.e(TAG, "importCapturePicture called with null extras");
            return null;
        }
        Bitmap bitmap = (Bitmap) intent.getExtras().get("data");
        if (bitmap == null) {
            Log.e(TAG, "importCapturePicture did not get any bitmap");
            return null;
        }
        FileOutputStream out = null;
        try {
            File target = generateLocalPictureFile(context, "jpg");
            out = new FileOutputStream(target);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            // Return resulting uri
            String result = target.toURI().toString();
            if (DEBUG) Log.d(TAG, "Inserted new picture: " + result);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    */

    public static void removeLocalPicture(String uri) {
        if (DEBUG) Log.d(TAG, "removeLocalPicture " + uri);
        File f = fileFromURI(uri);
        if (f != null) {
            f.delete();
        }
    }

    private static File generateLocalPictureFile(Context context, String extension) {
        File dir = getLocalPictureDir(context);
        dir.mkdirs();
        String name = System.currentTimeMillis() + "." + extension;
        return new File(dir, name);
    }

    public static File generateCapturePictureFile(Context context, String extension) {
        return generateLocalPictureFile(context, extension);
    }

    private static File getLocalPictureDir(Context context) {
        return new File(context.getFilesDir(), LOCAL_PICTURE_PATH);
    }

    public static void clear(Context context) {
        if (DEBUG) Log.d(TAG, "clear ");
        rmdir(getLocalPictureDir(context));
    }

   private static void rmdir(File dir) {
        if (dir == null) return;
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                rmdir(file);
            }
        }
        dir.delete();
    }

    public static File fileFromURI(String uri) {
        try {
            return new File(new URI(uri));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
