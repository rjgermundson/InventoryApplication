package com.example.riley.inventoryapplication.View;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.example.riley.inventoryapplication.Model.ProductProfile;
import com.example.riley.inventoryapplication.Model.SQLiteHelper;
import com.example.riley.inventoryapplication.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class SearchScreen extends AppCompatActivity {

    private SQLiteHelper sqLiteHelper;
    private AlertDialog.Builder alertDialogBuilder;
    private String currQuery;
    private String updateRowID;
    private TextView numberOfProducts;
    private List<ProductProfile> productsFound = new ArrayList<>();
    private Queue<ProductProfile> currentProducts = new LinkedBlockingQueue<>();
    private int page = 0;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.search_screen_layout);
        sqLiteHelper = new SQLiteHelper(getApplicationContext());
        alertDialogBuilder= new AlertDialog.Builder(SearchScreen.this);
        numberOfProducts = findViewById(R.id.products_found);
        Button left = findViewById(R.id.left_page_button);
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (page > 0) {
                    page--;
                    displaySearch();
                }
            }
        });
        Button right = findViewById(R.id.right_page_button);
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (page * 100 + 100 < productsFound.size()) {
                    page++;
                    displaySearch();
                }
            }
        });
        setupSearchListener();
    }

    // Setup the search bar
    private void setupSearchListener() {
        final SearchView currSearchView = (SearchView) findViewById(R.id.search_bar);
        currSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currSearchView.clearFocus();
                page = 0;
                currQuery = query;
                productsFound = sqLiteHelper.search(query);
                productsFound.size();
                setNumber();
                displaySearch();
                return false;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    // Display the products that match the string or contain the string in the product or brand name
    private void displaySearch() {
        ((LinearLayout) findViewById(R.id.leftoverLayout)).removeAllViews();
        currentProducts.clear();
        int last = page * 100 + 100;
        if (last > productsFound.size()) {
            last = productsFound.size();
        }
        System.err.println(page * 100 + " : " + last);
        List<ProductProfile> currPage = productsFound.subList(page * 100, last);
        currentProducts.addAll(currPage);
        System.err.println(currentProducts.size());
        runOnUiThread(new ViewCreator());
    }

    // Thread for creating the inflatable views for each product entry
    private class ViewCreator implements Runnable {
        public void run() {
            LinearLayout inflateParentView;
            ProductProfile currProductProfile;
            while (!currentProducts.isEmpty()) {
                currProductProfile = currentProducts.remove();
                final ViewHolder someViewHolder = new ViewHolder();
                final View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.inflate_view, null);
                setViewHolder(someViewHolder, view, currProductProfile);
                inflateParentView = view.findViewById(R.id.inflateParent);
                setupInflateListeners(view, someViewHolder, inflateParentView);
            }
        }
    }

    // Object to hold the information for the product. Information used in constructing the correct product entry page
    private class ViewHolder {
        TextView textView;
        String barcode;
        String brand;
        String product;
    }

    // Set each view holder to the given information from the product profile removed from the queue of all product profiles
    private void setViewHolder(ViewHolder someViewHolder, View view, ProductProfile currProductProfile) {
        someViewHolder.barcode = currProductProfile.getBarcodeId();
        someViewHolder.brand = currProductProfile.getBrandName();
        someViewHolder.product = currProductProfile.getProductName();
        someViewHolder.textView = view.findViewById(R.id.productIdentifier);
        someViewHolder.textView.setText(someViewHolder.product);
        view.setTag(currProductProfile.getBarcodeId());
    }

    // Set the functionality of each view holder. To change color on whether the user is touching the entry. Also
    // sets up the update and delete alert
    private void setupInflateListeners(final View currView, final ViewHolder someViewHolder, LinearLayout inflateParentView) {

        final CharSequence[] options = {"Update", "Delete"};
        inflateParentView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                alertDialogBuilder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int choice) {
                        if (choice == 0) {
                            updateRowID = currView.getTag().toString();
                            update(someViewHolder);
                        } else {
                            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(SearchScreen.this);
                            deleteDialog.setTitle("Delete Product Entry?");
                            deleteDialog.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ProductProfile profile = new ProductProfile(someViewHolder.barcode, someViewHolder.brand, someViewHolder.product);
                                    sqLiteHelper.deleteRecord(profile);
                                    setNumber();
                                    productsFound = sqLiteHelper.search(currQuery);
                                    displaySearch();
                                }
                            });

                            deleteDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });
                            deleteDialog.show();
                        }
                    }
                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
            }
        });

        inflateParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ProductEntry.class);
                putInto(intent, someViewHolder);
                startActivity(intent);
            }
        });
        LinearLayout linearView = ((LinearLayout) findViewById(R.id.leftoverLayout));
        linearView.addView(currView);
    }

    // Set up the intent for the product entry or update page
    private void putInto(Intent intent, ViewHolder someViewHolder) {
        intent.putExtra("ID", updateRowID);
        intent.putExtra("barcode", someViewHolder.barcode);
        intent.putExtra("brand", someViewHolder.brand);
        intent.putExtra("product", someViewHolder.product);
    }

    // Send the user to the update page for the requested product entry
    private void update(ViewHolder someViewHolder) {
        Intent intent = new Intent(getApplicationContext(), AddEntry.class);
        putInto(intent, someViewHolder);
        intent.putExtra("update", true);
        startActivity(intent);
    }

    // Comparator to alphabetize the products list
    private static class alphabetize implements Comparator<ProductProfile> {
        @Override
        public int compare(ProductProfile lhp, ProductProfile rhp) {
            return lhp.getBrandName().compareTo(rhp.getBrandName()); // Simple string comparison
        }
    }

    private void setNumber() {
        if (productsFound.size() == 1) {
            numberOfProducts.setText(productsFound.size() + " Product Found");
        } else {
            numberOfProducts.setText(productsFound.size() + " Products Found");
        }
    }
}
