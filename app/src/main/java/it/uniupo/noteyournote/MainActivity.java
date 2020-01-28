package it.uniupo.noteyournote;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import it.uniupo.noteyournote.adapter.NoteAdapter;
import it.uniupo.noteyournote.database.DatabaseManager;
import it.uniupo.noteyournote.model.Note;
import it.uniupo.noteyournote.util.Util;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private List<Note> mDataset;
    private SwipeRefreshLayout mRefreshLayout;
    private ListView mListView;
    private ProgressBar mProgressBar;
    private FloatingActionButton mBtnAddNote;
    private NoteAdapter mAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDataset = new ArrayList<>();

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (mAuth.getCurrentUser() == null) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRefreshLayout = findViewById(R.id.refresh_layout);
        mListView = findViewById(R.id.list_view);
        mProgressBar = findViewById(R.id.progress_bar);
        mBtnAddNote = findViewById(R.id.btn_add_note);

        mAdapter = new NoteAdapter(this, R.layout.note_item, mDataset);
        mListView.setAdapter(mAdapter);

        retrieveNote();

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                retrieveNote();
                mRefreshLayout.setRefreshing(false);
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                intent.putExtra("id", mDataset.get(position).getId());
                startActivity(intent);
            }
        });

        mBtnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NoteActivity.class));
            }
        });
    }

    // Permette di recuperare tutte le note
    private void retrieveNote() {
        // Se l'utente non e' loggato recupera le note dal database locale
        if (mAuth.getCurrentUser() == null) {
            mDataset.clear();
            DatabaseManager databaseManager = new DatabaseManager(this);
            mDataset.addAll(databaseManager.fetch());
            mAdapter.notifyDataSetChanged();
            databaseManager.close();
        } else {
            // Altrimenti dal Cloud Firestore
            if (Util.isNetworkAvailable(this)) {
                mProgressBar.setVisibility(View.VISIBLE);
                mDataset.clear();
                String uid = mAuth.getCurrentUser().getUid();
                FirebaseFirestore
                        .getInstance()
                        .collection(uid)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot documentSnapshots) {
                                mProgressBar.setVisibility(View.GONE);
                                for (QueryDocumentSnapshot d : documentSnapshots) {
                                    Note note = d.toObject(Note.class);
                                    mDataset.add(note);
                                }
                                mAdapter.notifyDataSetChanged();
                            }
                        });
            }
        }
    }

    // Aggiunge elementi alla barra delle azioni
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (mAuth.getCurrentUser() == null) {
            MenuItem actionLogout = menu.findItem(R.id.action_logout);
            actionLogout.setVisible(false);
        } else {
            MenuItem actionLogin = menu.findItem(R.id.action_login);
            actionLogin.setVisible(false);
        }
        return true;
    }

    // Gestisce i click degli elementi presenti nella barra delle azioni
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_map:
                startActivity(new Intent(this, MapsActivity.class));
                return true;
            case R.id.action_login:
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            case R.id.action_logout:
                mAuth.signOut();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            mAuth.addAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuth != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
