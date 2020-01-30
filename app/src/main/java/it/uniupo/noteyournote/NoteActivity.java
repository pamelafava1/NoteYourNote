package it.uniupo.noteyournote;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import it.uniupo.noteyournote.database.DatabaseManager;
import it.uniupo.noteyournote.model.Note;
import it.uniupo.noteyournote.util.PhotoUtil;
import it.uniupo.noteyournote.util.Util;

public class NoteActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private String mTimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    private Uri mImageUri;
    private byte[] mImage, mAudio;
    private String mFileName, mLocation;
    private double mLatitude, mLongitude;
    private TextInputLayout mTitleWrapper, mLocationWrapper;
    private EditText mEditTitle, mEditLocation, mEditDescription;
    private ImageButton mBtnAddImage;
    private Button mBtnRecord, mBtnPlay, mBtnSpeech;
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private boolean mStartRecording = true;
    private boolean mStartPlaying = true;
    private TextToSpeech mTextToSpeech;
    private static final int REQUEST_CAPTURE_IMAGE = 1000;
    private static final int REQUEST_GALLERY_IMAGE = 1001;
    private static final int REQUEST_RECORD_AUDIO = 1002;
    private static final int ONE_MB = 1000000;
    private static final String TAG = "NoteActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mTitleWrapper = findViewById(R.id.title_wrapper);
        mEditTitle = findViewById(R.id.edit_title);
        mBtnAddImage = findViewById(R.id.btn_add_image);
        mBtnRecord = findViewById(R.id.btn_record);
        mBtnPlay = findViewById(R.id.btn_play);
        mLocationWrapper = findViewById(R.id.location_wrapper);
        mEditLocation = findViewById(R.id.note_location);
        mEditDescription = findViewById(R.id.note_description);
        mBtnSpeech = findViewById(R.id.btn_speech);

        if (getIntent().hasExtra("id")) {
            retrieveNote();
            mTimeStamp = getIntent().getStringExtra("id");
        }

        mTextToSpeech = new TextToSpeech(this, this);

        mBtnAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePicker();
            }
        });

        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Util.checkPermission(NoteActivity.this, Manifest.permission.RECORD_AUDIO)) {
                    onRecord(mStartRecording);
                } else {
                    ActivityCompat.requestPermissions(NoteActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
                }
            }
        });

        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlay(mStartPlaying);
            }
        });

        mBtnSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textToSpeak();
            }
        });
    }

    // AlertDialog che permette all'utente di scegliere se scattare una foto oppure scegliere un'immagine dalla galleria del dispositivo
    private void imagePicker() {
        final String[] items = {getString(R.string.camera), getString(R.string.gallery)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setTitle(getString(R.string.upload_from))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (items[which].equals(getString(R.string.camera))) {
                            if (Util.checkPermission(NoteActivity.this, Manifest.permission.CAMERA) && Util.checkPermission(NoteActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                captureImage();
                            } else {
                                ActivityCompat.requestPermissions(NoteActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAPTURE_IMAGE);
                            }
                        } else if (items[which].equals(getString(R.string.gallery))) {
                            if (Util.checkPermission(NoteActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                                pickImage();
                            } else {
                                ActivityCompat.requestPermissions(NoteActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_GALLERY_IMAGE);
                            }
                        }
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Metodo che permette di scegliere un'immagine dalla galleria del dispositivo
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }

    // Metodo che permette di acquisire una foto utilizzando l'app della fotocamera del dispositivo
    private void captureImage() {
        ContentValues values = new ContentValues();
        mImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CAPTURE_IMAGE:
                    handleResult();
                    break;
                case REQUEST_GALLERY_IMAGE:
                    if (data != null) {
                        mImageUri = data.getData();
                        handleResult();
                    }
                    break;
            }
        } else if(resultCode == RESULT_CANCELED) {
            Toast.makeText(this, getString(R.string.cancelled), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleResult() {
        try {
            Bitmap bitmap = PhotoUtil.setPic(this, mBtnAddImage.getWidth(), mBtnAddImage.getHeight(), mImageUri);
            mImage = PhotoUtil.getBytesFromBitmap(bitmap);
            if (mImage.length >= ONE_MB) {
                Toast.makeText(this, getString(R.string.image_error_message), Toast.LENGTH_SHORT).show();
                mImage = null;
                return;
            }
            mBtnAddImage.setImageBitmap(PhotoUtil.getBitmapFromBytes(mImage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Se l'utente sta registrando tutti i bottoni vengono disabilitati
    private void onRecord(boolean start) {
        mBtnPlay.setEnabled(!start);
        mBtnAddImage.setEnabled(!start);
        mBtnSpeech.setEnabled(!start);
        if (start) {
            mStartRecording = false;
            mBtnRecord.setText(getString(R.string.stop_record_audio));
            stopPlaying();
            mBtnPlay.setText(getString(R.string.play));
            startRecording();
        } else {
            mStartRecording = true;
            mBtnRecord.setText(getString(R.string.recorder));
            stopRecording();
        }
    }

    // Metodo che permette di registrare un audio
    private void startRecording() {
        mFileName = this.getExternalCacheDir().getAbsolutePath();
        mFileName += "/" + mTimeStamp + ".3gp";
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
        mRecorder.start();
    }

    // Metodo che permette di fermare la registrazione dell'audio
    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    // Se l'utente sta ascoltando la registrazione tutti i bottoni vengono disabilitati
    private void onPlay(boolean start) {
        mBtnSpeech.setEnabled(!start);
        mBtnAddImage.setEnabled(!start);
        mBtnRecord.setEnabled(!start);
        if (start) {
            mStartPlaying = false;
            mBtnPlay.setText(getString(R.string.stop));
            startPlaying();
        } else {
            mStartPlaying = true;
            mBtnPlay.setText(getString(R.string.play));
            stopPlaying();
        }
    }

    // Metodo che permette di ascoltare la registrazione
    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        // Quando viene completato l'ascolto della registrazione i bottoni vengono abilitati
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mStartPlaying = true;
                mBtnSpeech.setEnabled(true);
                mBtnAddImage.setEnabled(true);
                mBtnRecord.setEnabled(true);
                mBtnPlay.setText(getString(R.string.play));
            }
        });
    }

    // Metodo che permette di fermare l'ascolto della registrazione
    private void stopPlaying() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private byte[] getBytesFromLocalFile() {
        if (mFileName == null) {
            return null;
        }
        File file = new File(mFileName);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            bufferedInputStream.read(bytes, 0, bytes.length);
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    private void getLocalFileFromBytes(byte[] bytes) {
        try {
            File tmp = File.createTempFile("tmp", ".3gp", getCacheDir());
            tmp.deleteOnExit();
            FileOutputStream outputStream = new FileOutputStream(tmp);
            outputStream.write(bytes);
            outputStream.close();
            mFileName = tmp.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Address> getLocationFromAddress(String strAddress) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocationName(strAddress, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addresses;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = mTextToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "This language is not supported");
            } else {
                mBtnSpeech.setEnabled(true);
            }
        } else {
            Log.e(TAG, "Failed to initialize");
        }
    }

    private void textToSpeak() {
        String text = mEditDescription.getText().toString();
        mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void updateUI(Note note) {
        if (note.getImage() != null) {
            mImage = note.getImage().toBytes();
            mBtnAddImage.setImageBitmap(PhotoUtil.getBitmapFromBytes(mImage));
        }
        if (note.getAudio() != null) {
            getLocalFileFromBytes(note.getAudio().toBytes());
            mBtnPlay.setEnabled(true);
        }
        mEditTitle.setText(note.getTitle());
        mEditDescription.setText(note.getDescription());
        mLocation = note.getLocation();
        mEditLocation.setText(mLocation);
        mLatitude = note.getLatitude();
        mLongitude = note.getLongitude();
    }

    // Metodo che permette di recuperare una nota
    private void retrieveNote() {
        // Se l'utente non e' loggato recupera la nota dal database locale
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            DatabaseManager databaseManager = new DatabaseManager(this);
            Note note = databaseManager.fetch(getIntent().getStringExtra("id"));
            updateUI(note);
        } else {
            // Altrimenti dal Cloud Firestore
            if (Util.isNetworkAvailable(this)) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore
                        .getInstance()
                        .collection(uid)
                        .document(getIntent().getStringExtra("id"))
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.exists()) {
                                    Note note = documentSnapshot.toObject(Note.class);
                                    if (note != null) {
                                        updateUI(note);
                                    }
                                }
                            }
                        });
            }
        }
    }

    private void actionDone() {
        String title = mEditTitle.getText().toString().trim();
        String description = mEditDescription.getText().toString().trim();
        mAudio = getBytesFromLocalFile();
        // Se c'e' l'id vuol dire la nota e' gia' esistente e si effettua una modifica della nota stessa
        if (getIntent().hasExtra("id")) {
            editNote(title, description);
        } else {
            // Altrimenti si crea la nuova nota
            saveNote(title, description);
        }
    }

    // Metodo che permette di modificare una nota
    private void editNote(String title, String description) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            DatabaseManager databaseManager = new DatabaseManager(this);
            long row = databaseManager.update(mTimeStamp, title, description, mImage, mAudio, mLocation, mLatitude, mLongitude);
            databaseManager.close();
            if (row != -1) {
                Toast.makeText(this, getString(R.string.note_updated_successfully), Toast.LENGTH_SHORT).show();
                clearActivityStack();
            } else {
                Toast.makeText(this, getString(R.string.error_message), Toast.LENGTH_SHORT).show();
            }
        } else {
            if (Util.isNetworkAvailable(this)) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                HashMap<String, Object> hashMap = new HashMap<>();
                if (mImage != null) {
                    Blob blob = Blob.fromBytes(mImage);
                    hashMap.put("image", blob);
                }
                if (mAudio != null) {
                    Blob blob = Blob.fromBytes(mAudio);
                    hashMap.put("audio", blob);
                }
                hashMap.put("title", title);
                hashMap.put("description", description);
                hashMap.put("location", mLocation);
                hashMap.put("latitude", mLatitude);
                hashMap.put("longitude", mLongitude);
                FirebaseFirestore
                        .getInstance()
                        .collection(uid)
                        .document(mTimeStamp)
                        .update(hashMap)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Note updated successfully");
                                Toast.makeText(NoteActivity.this, getString(R.string.note_saved_successfully), Toast.LENGTH_SHORT).show();
                                clearActivityStack();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error updating note", e);
                                Toast.makeText(NoteActivity.this, getString(R.string.error_message), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    // Metodo che permette di salvare una nota
    private void saveNote(String title, String description) {
        // Se l'utente non e' loggato cancella la nota viene salvata nel database locale
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            DatabaseManager databaseManager = new DatabaseManager(this);
            long row = databaseManager.insert(mTimeStamp, title, description, mImage, mAudio, mLocation, mLatitude, mLongitude);
            databaseManager.close();
            if (row != -1) {
                Toast.makeText(this, getString(R.string.note_saved_successfully), Toast.LENGTH_SHORT).show();
                clearActivityStack();
            } else {
                Toast.makeText(this, getString(R.string.error_message), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Altrimenti nel Cloud Firestore
            if (Util.isNetworkAvailable(this)) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                Blob mImageBlob = null, mAudioBlob = null;
                if (mImage != null) {
                    mImageBlob = Blob.fromBytes(mImage);
                }
                if (mAudio != null) {
                    mAudioBlob = Blob.fromBytes(mAudio);
                }
                Note note = new Note(mTimeStamp, title, description, mImageBlob, mAudioBlob, mLocation, mLatitude, mLongitude);
                FirebaseFirestore
                        .getInstance()
                        .collection(uid)
                        .document(mTimeStamp)
                        .set(note)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Note saved successfully");
                                Toast.makeText(NoteActivity.this, getString(R.string.note_saved_successfully), Toast.LENGTH_SHORT).show();
                                clearActivityStack();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error saving note", e);
                                Toast.makeText(NoteActivity.this, getString(R.string.error_message), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    // Metodo che permette di cancellare una nota
    private void deleteNote(String id) {
        // Se l'utente non e' loggato cancella la nota dal database locale
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            DatabaseManager databaseManager = new DatabaseManager(this);
            long row = databaseManager.delete(id);
            databaseManager.close();
            if (row != -1) {
                Toast.makeText(this, getString(R.string.note_deleted_successfully), Toast.LENGTH_SHORT).show();
                clearActivityStack();
            } else {
                Toast.makeText(this, getString(R.string.error_message), Toast.LENGTH_SHORT).show();
            }
            // Altrimenti dal Cloud Firestore
        } else {
            if (Util.isNetworkAvailable(this)) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore
                        .getInstance()
                        .collection(uid)
                        .document(id)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Note deleted");
                                Toast.makeText(NoteActivity.this, getString(R.string.note_deleted_successfully), Toast.LENGTH_SHORT).show();
                                clearActivityStack();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error deleting note", e);
                                Toast.makeText(NoteActivity.this, getString(R.string.error_message), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_menu, menu);
        if (!getIntent().hasExtra("id")) {
            MenuItem actionDelete = menu.findItem(R.id.action_delete);
            actionDelete.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_delete:
                deleteNote(getIntent().getStringExtra("id"));
                return true;
            case R.id.action_done:
                mTitleWrapper.setError("");
                mLocationWrapper.setError("");
                // Si controlla che il titolo della nota non sia vuoto
                if (!TextUtils.isEmpty(mEditTitle.getText().toString())) {
                    if (!TextUtils.isEmpty(mEditLocation.getText().toString().trim())) {
                        List<Address> addresses = getLocationFromAddress(mEditLocation.getText().toString().trim());
                        if (addresses != null && !addresses.isEmpty()) {
                            Address l = addresses.get(0);
                            mLatitude = l.getLatitude();
                            mLongitude = l.getLongitude();
                            mLocation = mEditLocation.getText().toString().trim();
                            actionDone();
                        } else {
                            mLatitude = 0.0;
                            mLongitude = 0.0;
                            mLocation = null;
                            mLocationWrapper.setError(getString(R.string.place_not_found));
                        }
                    } else {
                        actionDone();
                    }
                } else {
                    mTitleWrapper.setError(getString(R.string.title_empty_error_message));
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage(getString(R.string.exit_note_message))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearActivityStack();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void clearActivityStack() {
        Intent intent = new Intent(NoteActivity.this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAPTURE_IMAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    captureImage();
                }
                break;
            case REQUEST_GALLERY_IMAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImage();
                }
                break;
            case REQUEST_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onRecord(mStartRecording);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }
}
