package com.megster.cordova;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.hiddentao.cordova.filepath.FilePath;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.util.List;

public class FileChooser extends CordovaPlugin {

    private static final String TAG = "FileChooser";
    private static final String ACTION_OPEN = "open";
    private static final int PICK_FILE_REQUEST = 1;
    private static final int PICK_SCOPED_STORAGE_PERMISSION = 100;

    public static final String MIME = "mime";

    CallbackContext callback;
    //public boolean canPorceed=true;
    public int scopedStorage_Permission_Count=0;
    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {

        Log.e("FileOpener","oncreate scopedStorage_Permission_Count:"+scopedStorage_Permission_Count);
        boolean canPorceed=true;
        try{
             context_ = this.cordova.getActivity().getApplicationContext();
            // if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //     //Jason added for android 11 access external file permission
            //     ActivityCompat.requestPermissions(cordova.getActivity(),
            //             new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
            //                     Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 1);
            //     if (!Environment.isExternalStorageManager() && scopedStorage_Permission_Count<4) {
            //         canPorceed=false;
            //         Intent intent = new Intent();
            //         intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            //         Uri uri = Uri.fromParts("package", cordova.getActivity().getPackageName(), null);
            //         intent.setData(uri);
            //         //cordova.getActivity().startActivity(intent);
            //         cordova.startActivityForResult(this, intent, PICK_SCOPED_STORAGE_PERMISSION);
            //         Toast.makeText(cordova.getActivity(),"Please enable the permission in order to attach files",Toast.LENGTH_LONG);
            //     }
            // }
        }catch (Exception ec){
            Log.e("FileOpener","oncreate request api storage exc:"+ec.getMessage());
        }

        if (canPorceed && action.equals(ACTION_OPEN)) {
            Log.d(TAG,"=>chooseFile");
            JSONObject filters = inputs.optJSONObject(0);
            chooseFile(filters, callbackContext);
            return true;
        }

        return false;
    }


    private void moveFile(String inputPath, String inputFile, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            //new File(inputPath + inputFile).delete();


        }

        catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e("tag", e.getMessage());
        }

    }

    private void saveFile(File source, File destination){
        try {
            //File source = new File(sourceUri.getPath());

            FileChannel src = null;
            try {
                src = new FileInputStream(source).getChannel();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            FileChannel dst = new FileOutputStream(destination).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private File copyFileToInternalStorage(Context mContext, Uri uri, String newDirName)

    {
        File output=null;
        Cursor returnCursor = mContext.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);/*
     * Get the column indexes of the data in the Cursor,

     *     * move to the first row in the Cursor, get the data,

     *     * and display it.

     * */

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);

        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);

        String size = java.lang.Long.toString(returnCursor.getLong(sizeIndex));
        Log.e(TAG,"get file copy size:"+size);
        Log.e(TAG,"get file copy file name:"+name);//2019-ncov-factsheet (1).pdf


        if (newDirName != "")

        {

            File dir = new File(mContext.getFilesDir().toString());//+ "/" +    newDirName

            if (!dir.exists())

            {

                dir.mkdir();

            }

            try {
                output = new File(mContext.getFilesDir().toString()
                       // + "/" + newDirName

                        + "/" + URLEncoder.encode(name, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else

        {

            try {
                output = new File(mContext.getFilesDir().toString()

                        + "/" + URLEncoder.encode(name, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

        try {

            InputStream inputStream =

                    mContext.getContentResolver().openInputStream(uri);

            FileOutputStream outputStream = new FileOutputStream(output);

            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int  bytesAvailable = inputStream.available();
            Log.e(TAG,"HAS done copied bytesAvailable "+bytesAvailable);
            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            Log.e(TAG,"HAS done copied bufferSize "+bufferSize);
            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }

            Log.e(TAG,"HAS done copied to "+newDirName);
            if(output.isFile()){
                Log.e(TAG,"HAS done copied to is file "+newDirName);
                if(output.exists()){
                    Log.e(TAG,"HAS done copied to is file exists "+newDirName+" len:"+ output.length());

                }else{
                    Log.e(TAG,"HAS done copied to is file not exist "+newDirName);
                }
            }
            inputStream.close();
            outputStream.close();

        } catch (Exception e) {

            Log.e("Exception", e.getMessage());

        }

        return output;

    }




    public void chooseFile(JSONObject filter, CallbackContext callbackContext) {
        String uri_filter = filter.has(MIME) ? filter.optString(MIME) : "*/*";

        // type and title should be configurable

        Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //ACTION_OPEN_DOCUMENT_TREE
        if(false){
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (context_ == null) {
                    context_ = this.cordova.getActivity().getApplicationContext();
                }
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                //intent.addCategory(Intent.ACTION_GET_CONTENT);
//                StorageManager sm = (StorageManager) context_.getSystemService(Context.STORAGE_SERVICE);
//
//                //intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
//                //String startDir = "Android";
//                //String startDir = "Download"; // Not choosable on an Android 11 device
//                //String startDir = "DCIM";
//                //String startDir = "DCIM/Camera";  // replace "/", "%2F"
//                //String startDir = "DCIM%2FCamera";
//                String startDir = "Documents";
//
//                Uri uri = intent.getParcelableExtra("android.provider.extra.INITIAL_URI");
//
//                String scheme = uri.toString();
//
//                Log.d(TAG, "INITIAL_URI scheme: " + scheme);
//
//                scheme = scheme.replace("/root/", "/document/");
//
//                scheme += "%3A" + startDir;
//
//                uri = Uri.parse(scheme);
                Log.d(TAG, "uri:INITIAL_URI Environment.DIRECTORY_DOWNLOADS " + Environment.DIRECTORY_DOCUMENTS);
               // Log.d(TAG, "uri:INITIAL_URI uri " + uri.toString());
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,  Environment.DIRECTORY_DOCUMENTS);
                //intent.putExtra("android.provider.extra.INITIAL_URI", uri);
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cordova.startActivityForResult(this, intent, PICK_FILE_REQUEST);
            }

        }else{
            intent.setType(uri_filter);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            Intent chooser = Intent.createChooser(intent, "Select File");
            cordova.startActivityForResult(this, chooser, PICK_FILE_REQUEST);
        }



        Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT startActivityForResult");
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }








    ///Jaason added get real path for android 11

    public int ANDROID11_SELECTDOC_TREE=909;


    public Context context_;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_FILE_REQUEST && callback != null) {
            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT startActivityForResult onActivityResult");
            if (resultCode == Activity.RESULT_OK) {

                Uri uri = data.getData();

                if (uri != null) {
                    Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT startActivityForResult onActivityResult uri IS NOT NULL");
                    Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT startActivityForResult onActivityResult uri IS NOT NULL:"+ uri.toString());
                    if(context_==null){
                        context_ = this.cordova.getActivity().getApplicationContext();
                    }
                    if(false) {// Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
                        cordova.startActivityForResult(this, intent, ANDROID11_SELECTDOC_TREE);
                        Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT startActivityForResult ANDROID Q FOR NEW EXTRA_INITIAL_URI:"+ uri.toString());
                    }else{

//                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//                            //content://com.android.externalstorage.documents/document/primary:Documents/2019-ncov-factsheet (1).pdf
//                            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT file uri ori:"+ uri.toString());
//                            cordova.getContext().getContentResolver().takePersistableUriPermission(uri,
//                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//                            DocumentFile df_=DocumentFile.fromSingleUri( cordova.getContext(),uri);
//
//                            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT file uri canread:"+ df_.canRead());
//                            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT file uri df_.getName():"+ df_.getName());
//                            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT file uri df_.canWrite():"+ df_.canWrite());
//                            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT file uri df_.getUri():"+ df_.getUri());
//                            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT file uri df_.getType():"+ df_.getType());
//
//                            DocumentFile[] arr_=df_.listFiles();
//                            for (DocumentFile f:arr_ ) {
//                                if(f.isDirectory()){
//                                    Uri folder=  f.getUri();
//                                    Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT loop DocumentFile folder:"+ folder.toString());
//
//                                }else{
//                                    Uri file_=  f.getUri();
//                                    Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT loop DocumentFile file_:"+ FilePath.getPath(context_,file_) );
//
//                                }
//                            }
//
//                        }
                        File desti=null;

                        try{

                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                File source = new File(FilePath.getPath(cordova.getContext(), uri));

//                            String tempPathc=cordova.getContext().getFilesDir().getAbsolutePath() + File.separator +source.getName();
//                            desti=new File(tempPathc);
                                desti = copyFileToInternalStorage(cordova.getContext(), uri, "tmp_folder");
                               // Log.d(TAG, "=>chooseFile ACTION_GET_CONTENT startActivityForResult onActivityResult uri IS ok= resultPath copied done => :" + desti.getAbsolutePath());
                                //desti = new File(resultPath);
                                Log.d(TAG, "=>chooseFile ACTION_GET_CONTENT startActivityForResult onActivityResult uri IS ok= resultPath copied done copiedF => :" + desti.getAbsolutePath());
                                //saveFile(source,desti);
                            }

                        }catch(Exception ec){
                            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT startActivityForResult onActivityResult uri IS not ok");
                            Log.e(TAG,ec.getMessage());
                        }


                        String actualPath="";//FilePath.getPath(context_,uri);
                        actualPath=uri.toString();
                        Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT startActivityForResult onActivityResult uri IS NOT NULL actualPath:"+ actualPath);
                        if(desti!=null){
                            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT startActivityForResult onActivityResult uri desti uri return:"+  desti.toURI());
                            actualPath=desti.toURI().toString();
                        }
                        if(actualPath==null){
                            actualPath=uri.toString();
                        }
                        Log.w(TAG, uri.toString());
                        callback.success(actualPath);
                    }

                } else {
                    Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT startActivityForResult onActivityResult uri IS NULL");
                    callback.error("File uri was null");

                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                // keep this string the same as in iOS document picker plugin
                // https://github.com/iampossible/Cordova-DocPicker
                callback.error("User canceled.");
            } else {

                callback.error(resultCode);
            }
            scopedStorage_Permission_Count=0;
        }else if(requestCode == PICK_SCOPED_STORAGE_PERMISSION ){
            scopedStorage_Permission_Count++;
        }else if(ANDROID11_SELECTDOC_TREE==requestCode && callback != null){
            if (resultCode == Activity.RESULT_OK) {

                Uri uri = data.getData();

                if (uri != null) {

                    cordova.getContext().getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    DocumentFile df_=DocumentFile.fromTreeUri( cordova.getContext(),uri);
                    DocumentFile[] arr_=df_.listFiles();
                    for (DocumentFile f:arr_ ) {
                        if(f.isDirectory()){
                            Uri folder=  f.getUri();
                            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT loop DocumentFile folder:"+ folder.toString());

                        }else{
                            Uri file_=  f.getUri();
                            Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT loop DocumentFile file_:"+ FilePath.getPath(context_,file_) );

                        }
                    }

                    if(context_==null){
                        context_ = this.cordova.getActivity().getApplicationContext();
                    }
                    Log.d(TAG,"=>chooseFile ACTION_GET_CONTENT startActivityForResult ANDROID Q DONE GET URI NOT NULL:"+ uri.toString());
                    String actualPath=uri.toString();
                    if(actualPath==null){
                        actualPath=uri.toString();
                    }
                    Log.w(TAG, uri.toString());
                    callback.success(actualPath);

                } else {

                    callback.error("File uri was null");

                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                // keep this string the same as in iOS document picker plugin
                // https://github.com/iampossible/Cordova-DocPicker
                callback.error("User canceled.");
            } else {

                callback.error(resultCode);
            }
        }
    }
}
