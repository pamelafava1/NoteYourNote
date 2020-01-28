package it.uniupo.noteyournote.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.firebase.firestore.Blob;

import java.util.ArrayList;
import java.util.List;

import it.uniupo.noteyournote.model.Note;

public class DatabaseManager {

    private DatabaseHelper databaseHelper;

    public DatabaseManager(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public void close() {
        databaseHelper.close();
    }

    // Permette di inserire una nuova nota all'interno del database locale
    public long insert(String id, String title, String description, byte[] image, byte[] audio, String location, double latitude, double longitude) {
        // Abilita la modalita' di scrittura
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper._ID, id);
        values.put(DatabaseHelper.TITLE, title);
        values.put(DatabaseHelper.DESCRIPTION, description);
        values.put(DatabaseHelper.IMAGE, image);
        values.put(DatabaseHelper.AUDIO, audio);
        values.put(DatabaseHelper.LOCATION, location);
        values.put(DatabaseHelper.LATITUDE, latitude);
        values.put(DatabaseHelper.LONGITUDE, longitude);
        long newRowId = -1;
        try {
            // Inserisce la nuova riga, restituendo il valore della chiave primaria della nuova riga
            newRowId = database.insert(DatabaseHelper.TABLE_NAME, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newRowId;
    }

    // Permette di recuperare tutte le note dal database locale
    public List<Note> fetch() {
        // Abilita la modalita' di lettura
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        String[] columns = {
                DatabaseHelper._ID,
                DatabaseHelper.TITLE,
                DatabaseHelper.DESCRIPTION,
                DatabaseHelper.IMAGE,
                DatabaseHelper.AUDIO,
                DatabaseHelper.LOCATION,
                DatabaseHelper.LATITUDE,
                DatabaseHelper.LONGITUDE};
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME, columns, null, null, null, null, null);
        List<Note> dataset = new ArrayList<>();
        while (cursor.moveToNext()) {
            Blob image = null, audio = null;
            if (cursor.getBlob(3) != null) {
                image = Blob.fromBytes(cursor.getBlob(3));
            }
            if (cursor.getBlob(4) != null) {
                audio = Blob.fromBytes(cursor.getBlob(4));
            }
            Note note = new Note(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    image,
                    audio,
                    cursor.getString(5),
                    cursor.getDouble(6),
                    cursor.getDouble(7));
            dataset.add(note);
        }
        cursor.close();
        return dataset;
    }

    // Permette di recuperare una nota con un determinato id dal database locale
    public Note fetch(String id) {
        // Abilita la modalita' di lettura
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        String[] columns = {
                DatabaseHelper._ID,
                DatabaseHelper.TITLE,
                DatabaseHelper.DESCRIPTION,
                DatabaseHelper.IMAGE,
                DatabaseHelper.AUDIO,
                DatabaseHelper.LOCATION,
                DatabaseHelper.LATITUDE,
                DatabaseHelper.LONGITUDE};
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME, columns, DatabaseHelper._ID + "=?", new String[]{id}, null, null, null);
        Note note = null;
        while (cursor.moveToNext()) {
            Blob image = null, audio = null;
            if (cursor.getBlob(3) != null) {
              image = Blob.fromBytes(cursor.getBlob(3));
            }
            if (cursor.getBlob(4) != null) {
                audio = Blob.fromBytes(cursor.getBlob(4));
            }
            note = new Note(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    image,
                    audio,
                    cursor.getString(5),
                    cursor.getDouble(6),
                    cursor.getDouble(7));
        }
        cursor.close();
        return note;
    }

    // Permette di modificare una nota all'interno del database locale
    public long update(String _id, String title, String description, byte[] image, byte[] audio, String location, double latitude, double longitude) {
        // Abilita la modalita' di scrittura
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.TITLE, title);
        values.put(DatabaseHelper.DESCRIPTION, description);
        values.put(DatabaseHelper.IMAGE, image);
        values.put(DatabaseHelper.AUDIO, audio);
        values.put(DatabaseHelper.LOCATION, location);
        values.put(DatabaseHelper.LATITUDE, latitude);
        values.put(DatabaseHelper.LONGITUDE, longitude);
        return database.update(DatabaseHelper.TABLE_NAME, values, DatabaseHelper._ID + " = '" + _id + "'", null);
    }

    // Permette di eliminare una nota all'interno del database locale
    public long delete(String _id) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        return database.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper._ID + " = '" + _id + "'", null);
    }
}
