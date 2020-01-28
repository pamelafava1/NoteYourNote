package it.uniupo.noteyournote.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "NoteYourNote.db";
    private static final int DATABASE_VERSIONE = 1;
    static final String TABLE_NAME = "Note";
    static final String _ID = "_id";
    static final String TITLE = "title";
    static final String DESCRIPTION = "description";
    static final String IMAGE = "image";
    static final String AUDIO = "audio";
    static final String LOCATION = "location";
    static final String LATITUDE = "latitude";
    static final String LONGITUDE = "longitude";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSIONE);
    }

    // Viene invocato nel momento in cui non si trova nello spazio dell'applicazione un database con nome indicato nel costruttore
    // Quindi verra' invocato una sola volta, nel momento in cui il database non esiste ancora
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + _ID + " TEXT, "
                + TITLE + " TEXT, "
                + DESCRIPTION + " TEXT, "
                + IMAGE + " BLOB, "
                + AUDIO + " BLOB, "
                + LOCATION + " TEXT, "
                + LATITUDE + " DOUBLE, "
                + LONGITUDE + " DOUBLE);";
        db.execSQL(CREATE_TABLE);
    }

    // Viene invocato nel momento in cui si richiede una versione del database piu' aggiornata di quella presente su disco
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
