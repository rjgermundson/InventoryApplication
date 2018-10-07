package com.example.riley.inventoryapplication.Model;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * This class controls access to an SQLite database
 */
public class SQLiteHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ProductDatabase.db";

    /**
     * Constructor for an SQLiteHelper
     * @param context
     */
    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final String TABLE_NAME = "ProductsAvailable";
    private static final String COLUMN_CODE = "BarcodeID";
    private static final String COLUMN_BRAND = "Brand";
    private static final String COLUMN_PRODUCT = "Product";

    /**
     * On creation of this activity creates an SQLite database table for
     * products
     *
     * @param database The database
     */
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("create table " + TABLE_NAME + " (" + COLUMN_CODE + " VARCHAR, " + COLUMN_BRAND + " VARCHAR, " + COLUMN_PRODUCT + " VARCHAR);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(database);
    }

    /**
     * Inserts the given product into the database
     *
     * @param profile The product to be added to the database
     * @throws IllegalArgumentException profile == null
     */
    public void insertRecord(ProductProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException();
        }
        if (!contains(profile)) {
            SQLiteDatabase database = getReadableDatabase();
            ContentValues content = createContent(profile);
            database.insert(TABLE_NAME, null, content);
        }
    }

    /**
     * Closes the readable database
     */
    public void closeDatabase() {
        getReadableDatabase().close();
    }

    /**
     * Updates the given product in the database
     *
     * @requires the product given is already in the database
     * @param profile The profile to be updated
     */
    public void updateRecord(ProductProfile profile) {
        SQLiteDatabase database = getReadableDatabase();
        ContentValues content = createContent(profile);
        database.update(TABLE_NAME, content, COLUMN_CODE + " = ?", new String[]{profile.getBarcodeId()});
    }

    /**
     * Returns the content values for the given product
     *
     * @param profile The product whose fields will be converted to content values
     * @return The content values matching the given profile
     */
    private ContentValues createContent(ProductProfile profile) {
        ContentValues content = new ContentValues();
        content.put(COLUMN_CODE, profile.getBarcodeId());
        content.put(COLUMN_BRAND, profile.getBrandName());
        content.put(COLUMN_PRODUCT, profile.getProductName());
        return content;
    }

    /**
     * Deletes the given product from the database
     *
     * @param profile The product to be deleted
     */
    public void deleteRecord(ProductProfile profile) {
        SQLiteDatabase database = getReadableDatabase();
        database.delete(TABLE_NAME, COLUMN_CODE + " = ?", new String[]{profile.getBarcodeId()});
        database.close();
    }

    /**
     * Searches the database for the given query. Searches based on product name, and brand name
     *
     * @param query Search value
     * @return A list of products in the database that match the given query
     */
    public ArrayList<ProductProfile> search(String query) {
        SQLiteDatabase database = getReadableDatabase();
        ArrayList<ProductProfile> products = new ArrayList<>();
        String[] tokens = query.split(" ");
        String plike = "";
        String blike = "";
        for (int i = 0; i < tokens.length - 1; i++) {
            plike = COLUMN_PRODUCT + " LIKE \"%" + tokens[i] + "%\" OR ";
            blike = COLUMN_BRAND + " LIKE \"%" + tokens[i] + "%\" OR ";
        }
        plike += COLUMN_PRODUCT + " LIKE \"%" + tokens[tokens.length - 1] + "%\"";
        blike += COLUMN_BRAND + " LIKE \"%" + tokens[tokens.length - 1] + "%\"";
        Cursor cursor = database.query(TABLE_NAME, null, plike + " OR " + blike, null, null, null, COLUMN_BRAND, null);
        ProductProfile currProfile;
        for (int index = 0; index < cursor.getCount(); index++) {
            cursor.moveToNext();
            currProfile = new ProductProfile(cursor.getString(0), cursor.getString(1), cursor.getString(2));
            products.add(currProfile);
        }
        cursor.close();
        database.close();
        return products;
    }

    /**
     * Searches the database for the given barcode
     *
     * @param barcode The barcode to search for
     * @return Return the product profile associated with the given barcode
     * Null if not found in database
     */
    public ProductProfile findByBarcode(String barcode) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, null, COLUMN_CODE + " LIKE " + barcode, null, null, null, null, "1");
        try {
            if (cursor.getCount() == 1) {
                return new ProductProfile(cursor.getString(0), cursor.getString(1), cursor.getString(2));
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    /**
     * Determines whether a product exists in the database matching the given query
     * Based on brand name or product name
     *
     * @param profile The profile to be checked
     * @return True if the database contains the product
     * False otherwise
     */
    public boolean contains(ProductProfile profile) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_NAME, null, COLUMN_CODE + " LIKE " + profile.getBarcodeId(), null, null, null, null, "1");
        try {
            return cursor.getCount() == 1;
        } finally {
            cursor.close();
        }
    }

    /**
     * Delete all entries in the table
     */
    public void deleteAll() {
        this.getReadableDatabase().execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(this.getReadableDatabase());
    }
}