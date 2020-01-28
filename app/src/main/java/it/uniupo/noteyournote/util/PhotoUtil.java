package it.uniupo.noteyournote.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PhotoUtil {

    // targetW e targetH sono le dimensioni dell'ImageButton
    // Il seguente metodo permette di ridimensiorare un'immagine
    public static Bitmap setPic(Context context, int targetW, int targetH, Uri uri) throws IOException {
        // Ottiene le dimensioni della bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determina di quanto ridimensionare l'immagine
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        String photoPath = getRealPathFromURI(context, uri);
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);

        bitmap = rotateImageIfRequired(photoPath, bitmap);

        return bitmap;
    }

    // Metodo che permete di ruotare l'immagine se e' necessario
    private static Bitmap rotateImageIfRequired(String photoPath, Bitmap bitmap) throws IOException {
        ExifInterface exifInterface = new ExifInterface(photoPath);
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateBitmap(bitmap, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateBitmap(bitmap, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateBitmap(bitmap, 270);
            default:
                return bitmap;
        }
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    // Metodo che permette di trovare il percorso reale della foto
    private static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] column = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, column, null, null, null);
            int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // Metodo che permette la conversione di un'immagine da formato BLOB a Bitmap
    public static Bitmap getBitmapFromBytes(byte[] bytes) {
        if (bytes != null) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return null;
    }

    // Metodo che permette la conversione di un'immagine da formato Bitmap a BLOB
    public static byte[] getBytesFromBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
            return out.toByteArray();
        }
        return null;
    }
}
