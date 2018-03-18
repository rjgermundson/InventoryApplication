package com.example.riley.inventoryapplication.View;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.example.riley.inventoryapplication.Model.ProductProfile;
import com.example.riley.inventoryapplication.Model.SQLiteHelper;
import com.example.riley.inventoryapplication.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Activity designed to import the pricebook information from pricebook.txt in
 * external storage
 */
public class ImportPage extends AppCompatActivity {
    private static final ForkJoinPool POOL = new ForkJoinPool();
    private static final int EXTRA_ELEMENTS = 7;
    private static final int SEQUENTIAL_CUTOFF = 10;

    private SQLiteHelper sqLiteHelper;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.import_docu_layout);
        sqLiteHelper = new SQLiteHelper(getApplicationContext());
        setButton();
    }

    /**
     * Initialize the import button
     */
    private void setButton() {
        Button importButton = findViewById(R.id.importDocButton);
        importButton.setOnClickListener(new ImportListener());
    }

    private class ImportListener implements View.OnClickListener {
        private ImportListener() {
            super();
        }

        @Override
        public void onClick(View view) {
            List<String> rawProducts = getRawData(getPriceBook());
            ImportTask task = new ImportTask(rawProducts, 0, rawProducts.size());
            POOL.invoke(task);
            System.err.println("HELLO");
        }

        /**
         * Get all products from the given file, storing them in the local database
         * @requires pricebook is properly formatted
         * @param pricebook The file containing the pricebook
         * @return A list containing all products from the given pricebook
         */
        private List<String> getRawData(File pricebook) {
            List<String> rawProducts = new ArrayList<>();
            try {
                Scanner scanner = new Scanner(pricebook);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.length() > 1 && !line.contains("Grocery") && !line.contains("$Unit") && !line.contains("Contact")) {
                        rawProducts.add(line);
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return rawProducts;
        }

        /**
         * Get the properly named pricebook from external storage
         */
        private File getPriceBook() {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED) {
                return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Pricebook.txt");
            }
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, getResources().getInteger(R.integer.read_external_code));
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Pricebook.txt");
        }

        /**
         * Thread task for importing raw products as database entries
         */
        private class ImportTask extends RecursiveAction {
            private List<String> list;
            private int left, right;

            /**
             * Constructor for import task
             * @param list The list of products to be added to the database
             * @param left The left bound in the list of the products this thread
             *             will add to the database
             * @param right The right bound in the list of the products this thread
             *              will add to the database
             */
            private ImportTask(List<String> list, int left, int right) {
                this.list = list;
                this.left = left;
                this.right = right;
            }

            public void compute() {
                if (right - left <= SEQUENTIAL_CUTOFF) {
                    for (int i = left; i < right; i++) {
                        String currLine = list.get(i);
                        Scanner scanner = new Scanner(currLine);
                        String brand = "";
                        while (!scanner.hasNextLong() && scanner.hasNext()) {
                            brand = brand + scanner.next() + " ";
                            if (scanner.hasNextLong()) {
                                long next = scanner.nextLong();
                                if (next < 100) {
                                    brand = brand + next + " ";
                                } else {
                                    String barcode = constructBarcode(next);
                                    String product = getProduct(scanner, barcode);
                                    sqLiteHelper.insertRecord(new ProductProfile(barcode, brand, product));
                                }
                            }
                        }
                    }
                } else {
                    int mid = (right + left) / 2; // In the case that the sample size is too large we may need to change this to avoid overflow
                    ImportTask leftTask = new ImportTask(list, left, mid);
                    ImportTask rightTask = new ImportTask(list, mid, right);
                    leftTask.fork();
                    rightTask.compute();
                    leftTask.join();
                }
            }

            /**
             * Constructs the whole barcode from the given long
             * @requires Long.toString(barcodeAsLong).length < 14
             * @param barcodeAsLong The long to be converted to a full barcode
             * @return The given barcode as a full string representing the same barcode, including
             *         the check digit for the barcode
             */
            private String constructBarcode(Long barcodeAsLong) {
                String barcodeAsString = Long.toString(barcodeAsLong);
                if (barcodeAsString.length() < 12) {
                    barcodeAsString = addCheckDigit(barcodeAsLong);
                }
                while (barcodeAsString.length() < 12) {
                    barcodeAsString = barcodeAsString + "0";
                }
                return barcodeAsString;
            }

            /**
             * Gets and adds the check digit for the barcode
             * @param barcodeAsLong The barcode whose check digit will be found
             * @return The barcode with the added check digit
             */
            private String addCheckDigit(Long barcodeAsLong) {
                long origBarcode = barcodeAsLong;
                int sum = 0;
                while (barcodeAsLong > 0) {
                    sum += (barcodeAsLong % 10) * 3;
                    barcodeAsLong = barcodeAsLong / 10;
                    sum += (barcodeAsLong % 10);
                    barcodeAsLong = barcodeAsLong / 10;
                }
                sum = sum % 10;
                int checkDigit = (10 - sum) % 10;
                return "" + ((origBarcode * 10) + checkDigit);
            }

            /**
             * Get the product name from the remainder of the line
             * @param scanner The scanner containing the rest of the line
             * @return The full name of the product
             */
            private String getProduct(Scanner scanner, String barcode) {
                Stack<String> stack = new Stack<>();
                while (scanner.hasNext()) {
                    stack.push(scanner.next());
                }
                if (stack.size() < 8) {
                    System.err.println(stack.size() + " : " + barcode);
                    while (!stack.isEmpty()) {

                        System.err.println(stack.pop());
                    }
                }
                for (int i = 0; i < EXTRA_ELEMENTS; i++) {
                    stack.pop();
                }
                String product = "";
                while (!stack.isEmpty()) {
                    product = stack.pop() + " " + product;
                }
                return product;
            }



        }

    }


}