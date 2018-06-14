package com.example.riley.inventoryapplication.View;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.riley.inventoryapplication.Camera.Camera;
import com.example.riley.inventoryapplication.Model.ProductProfile;
import com.example.riley.inventoryapplication.Model.SQLiteHelper;
import com.example.riley.inventoryapplication.R;

/**
 * This class allows the user to add an entry to the database or update the entry in the
 * case that they have decided to update it
 */
public class AddEntry extends AppCompatActivity {

    private SQLiteHelper sqLiteHelper;
    private String barcode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_menu_layout);
        sqLiteHelper = new SQLiteHelper(getApplicationContext());
        setValues();
        setButton();
    }

    /**
     * Sets up the save button. Sends the user back to the camera if they were initially using the camera to update
     * Otherwise sends them back to the hub
     */
    private void setButton() {
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String brand = ((EditText) findViewById(R.id.etBrand)).getText().toString();
                String product = ((EditText) findViewById(R.id.etProduct)).getText().toString();
                ProductProfile currProductProfile = new ProductProfile(barcode, brand, product);
                Intent intent = new Intent();
                if (!brand.equals("") && !product.equals("")) {
                    if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("update") && getIntent().getExtras().getBoolean("update")) {
                        // If we are updating we want to return to the search menu
                        sqLiteHelper.updateRecord(currProductProfile);
                        intent = new Intent(getApplicationContext(), SearchScreen.class);
                        finish();
                    } else {
                        // Otherwise we return to the main activity menu

                        sqLiteHelper.insertRecord(currProductProfile);
                        intent = new Intent(getApplicationContext(), Hub.class);
                    }
                } else {
                    intent = new Intent(getApplicationContext(), Hub.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                sqLiteHelper.closeDatabase();
                startActivity(intent);
            }
        });
    }

    /**
     * Set the initial values of the entry page
     */
    private void setValues() {
        TextView currTextView = findViewById(R.id.textView2);
        barcode = getIntent().getExtras().getString("barcode");
        if (getIntent().getExtras().getBoolean("update")) {
            ((TextView) findViewById(R.id.etBrand)).setText(getIntent().getExtras().getString("brand"));
            ((TextView) findViewById(R.id.etProduct)).setText(getIntent().getExtras().getString("product"));
        }
        currTextView.setText(barcode);
    }
}