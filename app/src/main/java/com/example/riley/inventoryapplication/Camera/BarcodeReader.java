package com.example.riley.inventoryapplication.Camera;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.SparseArray;
import android.view.Surface;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.nio.ByteBuffer;

public class BarcodeReader {
    private static BarcodeReader reader;
    private Camera callback;
    private ImageReader imageReader;
    private BarcodeDetector barcodeDetector;
    private SparseArray<Barcode> currBarcode;

    /**
     * Constructor for a BarcodeReader
     * @param reader The ImageReader that will feed the barcode reader images
     * @param handler The handler for this reader
     * @throws IllegalArgumentException
     *         callback == null || reader == null || handler == null
     */
    public BarcodeReader(Camera callback, ImageReader reader, Handler handler) {
        if (reader == null || handler == null) {
            throw new IllegalArgumentException();
        }
        barcodeDetector = new BarcodeDetector.Builder(callback.getApplicationContext()).setBarcodeFormats(Barcode.UPC_A | Barcode.EAN_13).build();
        this.callback = callback;
        this.imageReader = reader;
        reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image currImage = imageReader.acquireLatestImage();
                if (currImage != null) {
                    Frame currFrame = convertImageToFrame(currImage);
                    currImage.close();
                    checkDetect(currFrame);
                }
            }
        }, handler);
    }

    /**
     * Converts image into a usable frame which can be checked for a barcode
     * @param image The image to be converted
     * @return A Frame from the given image
     */
    private Frame convertImageToFrame(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        if (buffer != null) {
            Frame currFrame;
            currFrame = new Frame.Builder().setImageData(buffer, imageReader.getWidth(), imageReader.getHeight(), ImageFormat.NV21).build();
            return currFrame;
        }
        return null;
    }

    /**
     * Checks whether the given frame contains a barcode. On detect, notifies the current
     * Camera activity
     * @param frame The frame to be checked
     */
    private void checkDetect(Frame frame) {
        SparseArray<Barcode> code = barcodeDetector.detect(frame);
        if (code != null && code.size() > 0 && code != currBarcode) {
            currBarcode = code;
            String stringCurrBarcode = currBarcode.valueAt(0).rawValue;
            callback.onDetect(stringCurrBarcode);
        }
    }

    /**
     * Returns the surface of the image reader
     * @return Returns the surface of the image reader
     */
    public Surface getSurface() {
        return imageReader.getSurface();
    }

}
