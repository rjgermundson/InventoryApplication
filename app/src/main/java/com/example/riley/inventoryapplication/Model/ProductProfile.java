package com.example.riley.inventoryapplication.Model;

/**
 * This is a storage class for products
 */
public class ProductProfile {
    // Representation invariant
    //  barcodeID != null
    //  brandName != null
    //  productName != null

    private String barcodeID, brandName, productName;

    /**
     * Constructor for a product profile
     * @param bid The barcode for this product
     * @param brand The brand of this product
     * @param product The actual product name
     */
    public ProductProfile(String bid, String brand, String product) {
        barcodeID = bid;
        brandName = brand;
        productName = product;
    }

    /**
     * Return the barcode of this product as a string
     * @return The barcode of this object as a string
     */
    public String getBarcodeId() {
        return barcodeID;
    }

    /**
     * Return the brand of this product
     * @return The brand of this product
     */
    public String getBrandName() {
        return brandName;
    }

    /**
     * Return the name of this product
     * @return The name of this product
     */
    public String getProductName() {
        return productName;
    }


}