package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1) Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // 2) Create list + adapter FIRST (important)
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // 3) Firestore setup AFTER list + adapter exist
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }

            if (value != null) {
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");
                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        // 4) Add button -> open dialog
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        // 5) Tap -> edit dialog
        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            if (city == null) return;

            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });

        // 6) Long press -> delete (participation exercise)
        cityListView.setOnItemLongClickListener((parent, view, position, id) -> {
            City city = cityArrayAdapter.getItem(position);
            if (city == null) return true;

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete City")
                    .setMessage("Delete " + city.getName() + " (" + city.getProvince() + ")?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        citiesRef.document(city.getName()).delete()
                                .addOnSuccessListener(unused ->
                                        Log.d("Firestore", "Deleted: " + city.getName()))
                                .addOnFailureListener(e ->
                                        Log.e("Firestore", "Delete failed", e));
                        // Snapshot listener will refresh the list automatically
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            return true;
        });
    }

    @Override
    public void updateCity(City city, String name, String province) {
        // If your CityDialog edits name/province, you must update Firestore too.
        // Easiest approach: delete old doc (by old name) + add new doc (by new name).
        // But we need the OLD name to delete correctly.

        // For now, do the UI update:
        city.setName(name);
        city.setProvince(province);
        cityArrayAdapter.notifyDataSetChanged();

        // TODO (only if your lab requires edit persistence):
        // Implement a proper update strategy once you tell me how CityDialogFragment passes the "old name".
    }

    @Override
    public void addCity(City city) {
        // Firestore write (snapshot listener will pull it in, but adding locally is fine too)
        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city)
                .addOnFailureListener(e -> Log.e("Firestore", "Add failed", e));
    }

    public void addDummyData() {
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        citiesRef.document(m1.getName()).set(m1);
        citiesRef.document(m2.getName()).set(m2);
    }
}
