package org.akvo.caddisfly.sensor.colorimetry.strip.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Base64;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by linda on 9/13/15
 */
@SuppressWarnings("HardCodedStringLiteral")
public class FileStorage {

    public static String ROOTDIR = "/Akvo Caddisfly";
    private Context context;

    public FileStorage(Context context) {
        this.context = context;
    }

    public static int byteArrayToLeInt(byte[] b) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    public static byte[] leIntToByteArray(int i) {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

    /**
     * Method to check whether external media available and writable. This is adapted from
     * http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
     */

    public static boolean checkExternalMedia() {
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        System.out.println("\n\nExternal Media: readable="
                + mExternalStorageAvailable + " writable=" + mExternalStorageWriteable);

        return mExternalStorageWriteable;
    }

    /**
     * Method to write characters to file on SD card. Note that you must add a
     * WRITE_EXTERNAL_STORAGE permission to the manifest file or this method will throw
     * a FileNotFound Exception because you won't have write permission.
     *
     * @return absolute path name of saved file, or empty string on failure.
     */
    public static String writeBitmapToExternalStorage(Bitmap bitmap, String dirPath, String fileName) {
        // Find the root of the external storage
        // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal
        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + ROOTDIR + dirPath);
        File file = new File(dir, fileName);

        // check if directory exists and if not, create it
        boolean success = true;
        if (!dir.exists()) {
            success = dir.mkdirs();
        }

        if (success) {
            try {
                FileOutputStream f = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(f);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);

                for (byte s : baos.toByteArray()) {
                    bos.write(s);
                }
                bos.close();
                baos.close();
                f.close();
                return file.getAbsolutePath();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // on failure, return empty string
        return "";
    }

    public static void writeLogToSDFile(String filename, String data, boolean append) {

        // Find the root of the external storage.
        // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal

        File root = android.os.Environment.getExternalStorageDirectory();
        System.out.println("\nExternal file system root: " + root);

        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

        File dir = new File(root.getAbsolutePath() + "/download/striptest");
        dir.mkdirs();
        File file = new File(dir, filename);

        try {
            FileWriter writer = new FileWriter(file, append);
            writer.write(data);

            writer.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("\n\nFile written to " + file);
    }

    public boolean writeByteArray(byte[] data, String name) {
        String fileName = name + ".txt";

        FileOutputStream outputStream;

        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            BufferedOutputStream bos = new BufferedOutputStream(outputStream);
            for (byte s : data) {
                bos.write(s);
            }
            bos.close();
            outputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public byte[] readByteArray(String name) throws IOException {
        String fileName = name + ".txt";
        byte[] data;
        int c;

        FileInputStream fis = context.openFileInput(fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedInputStream bos = new BufferedInputStream(fis);

        while ((c = bos.read()) != -1) {
            baos.write(c);

        }

        data = baos.toByteArray();

        bos.close();
        baos.close();
        fis.close();

        return data;
    }

    public void writeToInternalStorage(String name, String json) {
        String fileName = name + ".txt";

        FileOutputStream outputStream;

        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            for (byte s : json.getBytes()) {
                outputStream.write(s);
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeBitmapToInternalStorage(String name, Bitmap bitmap) {

        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);

            writeByteArray(baos.toByteArray(), name);

            baos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String bitmapToBase64String(Bitmap bitmap) throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);

        byte[] img = baos.toByteArray();
        return Base64.encodeToString(img, Base64.DEFAULT);


    }

    public String readFromInternalStorage(String fileName) {

        File file = new File(context.getFilesDir(), fileName);

        try {

            String json = "";
            FileInputStream fis = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                json = json + strLine;
            }
            br.close();
            in.close();
            fis.close();

            return json;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean checkIfFilenameContainsString(final String contains) {

        File file = context.getFilesDir();
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.contains(contains);
            }
        };
        File[] files = file.listFiles(filter);

        System.out.println("***files that contain string: " + files.length);

        return files.length > 0;

    }

    public void deleteFromInternalStorage(final String contains) {
        File file = context.getFilesDir();
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.contains(contains);
            }
        };
        File[] files = file.listFiles(filter);
        for (File f : files) {
            boolean deleted = f.delete();

            System.out.println("***deleted file : " + f.getName() + ": " + deleted);
        }
    }
}
