package com.example.riley.inventoryapplication.View;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.riley.inventoryapplication.R;

/**
 * Activity for viewing a product profile visually. Contains the product name, brand name,
 * and a scannable barcode for the product
 *
 * @specfield BARCODE_SCALE_FACTOR Amount to scale the entire barcode's size by
 * @specfield BARCODE_MIN_WIDTH_PX The minimum number of pixels needed to represent a barcode
 *            in conjunction with scalar
 * @specfield BARCODE_HEIGHT_PX Desired height of the barcode
 */
public class ProductEntry extends AppCompatActivity {
    public static final int BARCODE_SCALE_FACTOR = 4;
    public static final int BARCODE_MIN_WIDTH_PX = BARCODE_SCALE_FACTOR * 131;
    public static final int BARCODE_HEIGHT_PX = BARCODE_SCALE_FACTOR * 70;
    // Binary strings to represent lines in a barcode segment for the left half of the barcode
    private String[] LArray = {"0001101", "0011001", "0010011", "0111101", "0100011", "0110001", "0101111", "0111011", "0110111", "0001011"};
    // Binary strings to represent lines in a barcode segment for the left half of the barcode
    private String[] RArray = {"1110010", "1100110", "1101100", "1000010", "1011100", "1001110", "1010000", "1000100", "1001000", "1110100"};
    private String middle = "01010";
    private String end = "101";
    private String barcode;
    private String brand;
    private String product;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.entry_layout);
        setValues();
        if (barcode.length() >= 12) {
            createBarcode();
        }
    }

    /**
     * Sets the values for the given profile
     */
    private void setValues() {
        barcode = getIntent().getStringExtra("barcode");
        brand = getIntent().getStringExtra("brand");
        product = getIntent().getStringExtra("product");
        setTextViews();
    }

    /**
     * Sets the text fields of the entry page
     */
    private void setTextViews() {
        TextView tvBarcodeText = findViewById(R.id.tvBarcode);
        TextView firstDigit = findViewById(R.id.firstDigit);
        TextView leftHalfDigits = findViewById(R.id.leftHalfDigits);
        TextView rightHalfDigits = findViewById(R.id.rightHalfDigits);
        TextView lastDigit = findViewById(R.id.lastDigit);
        TextView brandTextView = findViewById(R.id.tvBrand);
        TextView productTextView = findViewById(R.id.tvProduct);
        firstDigit.setText(barcode.substring(0, 1));
        if (barcode.length() != 13) {
            // UPC-A
            leftHalfDigits.setText(barcode.substring(1, 6));
            rightHalfDigits.setText(barcode.substring(6, 11));
            lastDigit.setText(barcode.substring(11));
        } else {
            // EAN
            leftHalfDigits.setText(barcode.substring(1, 7));
            rightHalfDigits.setText(barcode.substring(7));
            lastDigit.setText("");
        }
        tvBarcodeText.setText(barcode);
        brandTextView.setText(brand);
        productTextView.setText(product);
    }

    /**
     * Construct the image of a barcode
     */
    private void createBarcode() {
        Bitmap bar = generateBarcodeBitmap();
        ImageView imgView = findViewById(R.id.barcodeImage);
        imgView.setImageBitmap(bar);
    }

    /**
     * Generate a bitmap of the barcode for this product profile. Uses the proper formatting
     *
     * @return The bitmap representation of the product
     */
    private Bitmap generateBarcodeBitmap() {
        Bitmap bars = Bitmap.createBitmap(BARCODE_MIN_WIDTH_PX, BARCODE_HEIGHT_PX, Bitmap.Config.ARGB_8888);
        String stringCode = constructStringCode();
        for (int indexWidth = 18 * BARCODE_SCALE_FACTOR; indexWidth < 113 * BARCODE_SCALE_FACTOR; indexWidth += BARCODE_SCALE_FACTOR) {
            char currChar = stringCode.charAt((indexWidth - 18 * BARCODE_SCALE_FACTOR) / BARCODE_SCALE_FACTOR);
            if (currChar == '1') {
                for (int indexHeight = 3 * BARCODE_SCALE_FACTOR; indexHeight < BARCODE_HEIGHT_PX - 3 * BARCODE_SCALE_FACTOR; indexHeight++) {
                    for (int scaleWidth = 0; scaleWidth < BARCODE_SCALE_FACTOR; scaleWidth++) {
                        bars.setPixel(indexWidth + scaleWidth, indexHeight, Color.BLACK);
                    }
                }
            }
        }
        bars = setBorder(bars);
        return bars;
    }

    /**
     * Returns a string representation of the binary structure of the barcode
     * @return A string representation of the binary form of the barcode
     */
    private String constructStringCode() {
        String stringCode = end;
        for (int index = 0; index < barcode.length(); index++) {
            int currInt = Integer.parseInt("" + barcode.charAt(index));
            if (index < 6) {
                stringCode += LArray[currInt];
            } else {
                if (index == 6) {
                    stringCode += middle;
                }
                stringCode += RArray[currInt];
            }
        }
        stringCode += end;
        return stringCode;
    }

    /**
     * Adds an aesthetically pleasing black box around the given bitmap
     * @param bitmap The bitmap around which a black box will be added
     * @return The same bitmap with a black box around it
     */
    private Bitmap setBorder(Bitmap bitmap) {
        for (int index = 0; index < bitmap.getWidth(); index++) {
            bitmap.setPixel(index, bitmap.getHeight() - 1, Color.BLACK);
            bitmap.setPixel(index, 0, Color.BLACK);
        }
        for (int index = 0; index < bitmap.getHeight(); index++) {
            bitmap.setPixel(0, index, Color.BLACK);
            bitmap.setPixel(bitmap.getWidth() - 1, index, Color.BLACK);
        }
        return bitmap;
    }
}
