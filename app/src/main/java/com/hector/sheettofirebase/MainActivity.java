package com.hector.sheettofirebase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.WriteBatch;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    //UI element declarations
    Button pushDataButton;
    EditText sheetIDEditText,collectionNameEditText,rangeEditText,apiKeyEditText;


    //Variable declarations
    String apiKey;
    String sheetID;
    String range;
    String collectionName;

    //Sheet connection requirements
    HttpTransport transport;
    JsonFactory factory;
    Sheets sheetsService;
    ValueRange result;

    //Firebase elements declarations
    FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI element connecting
        apiKeyEditText = findViewById(R.id.apiKey);
        collectionNameEditText = findViewById(R.id.collectionName);
        sheetIDEditText = (EditText) findViewById(R.id.sheetID);
        rangeEditText = findViewById(R.id.sheetRange);
        pushDataButton = (Button) findViewById(R.id.pushData);


        //onClick action listener
        pushDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apiKey = apiKeyEditText.getText().toString();
                collectionName = collectionNameEditText.getText().toString();
                range = rangeEditText.getText().toString();
                sheetID = sheetIDEditText.getText().toString();
                if(collectionName.isEmpty() || range.isEmpty() || sheetID.isEmpty() || apiKey.isEmpty()){
                    apiKeyEditText.setError("Every Field is required");
                }else {
                    new DataHandler().execute();
                }
            }
        });
    }

    class DataHandler extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            readDataFromSheet();
            return null;
        }

    }

    //Data reading from sheet
    private void readDataFromSheet() {
        transport = AndroidHttp.newCompatibleTransport();
        factory = JacksonFactory.getDefaultInstance();
        sheetsService = new Sheets.Builder(transport, factory, null)
                .setApplicationName("My Code")
                .build();

        try {
            result = sheetsService.spreadsheets().values()
                    .get(sheetID, range)
                    .setKey(apiKey)
                    .execute();
            int numRows = result.getValues() != null ? result.getValues().size() : 0;
            if(numRows !=0 ) {
                Log.d("SUCCESS =>", "rows retrieved " + result.getValues());
                pushDataToFireStore(numRows);
            }
        }
        catch (IOException ex){
            Log.d("FAILED =>",ex.getLocalizedMessage());
        }
    }

    //Data writing to firebase
    private void pushDataToFireStore(int numRows) {
        firebaseFirestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        firebaseFirestore.setFirestoreSettings(settings);


        List<List<Object>> values = result.getValues();
        WriteBatch batch = firebaseFirestore.batch();

        for (int i=1;i<numRows;i++){
            DocumentReference data = firebaseFirestore.collection(collectionName).document(values.get(0).get(i).toString());;
            Map<String, Object> dataVlaues = new HashMap<>();
            for (int j=1;j<numRows;j++){
                dataVlaues.put(String.valueOf(values.get(0).get(j)),String.valueOf(values.get(i).get(j)));
            }
            batch.set(data,dataVlaues);
        }
        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(getApplicationContext(),"Successfully data transferred",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),"Data transfer error",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
