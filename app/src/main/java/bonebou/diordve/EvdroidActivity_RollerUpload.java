package bonebou.diordve;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by albert on 8/8/16.
 * helper functions and upload stuff to roller
 */
public class EvdroidActivity_RollerUpload extends Activity {

    static final String LOG_TAG = "EvdroidActivity";
    static final String UPLOAD_URL = "http://kwee.herokuapp.com/eupload";

    static final String TEXT_FILE = "ocr.txt"; /*prefix of the high resolution photo/picture taken*/
    static final String ROOT_FOLDER_PATH = Environment.getExternalStorageDirectory().getAbsolutePath(); /* doesn't end with / */
    static final String TESS_PATH = "/tessdata/";
    static final String TEXT_FILE_PATH = ROOT_FOLDER_PATH + TESS_PATH + TEXT_FILE;

    private EvdroidActivity_RollerUpload self = this;

    public void upload_ocr_n_img ( ) {

//        TODO - add img as well
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {

                String id_inv = self.get_id_n_timestemp();

                File file_orig = new File ( TEXT_FILE_PATH );
                File file_copy = new File ( ROOT_FOLDER_PATH + TESS_PATH +id_inv+".txt" );

                try {
                    self.copy_file(file_orig, file_copy);
                } catch (IOException ex) {
                    Log.e ( LOG_TAG, self.err_str(ex));
                }

                try {
                    String text = getStringFromFile ( file_copy );
                    file_copy.delete();

                    Log.i(LOG_TAG, "stupid movie ::" + text);

                    self.post_text ( text, id_inv );


                } catch ( Exception ex ) {

                    Log.e ( LOG_TAG, self.err_str(ex));
                }
            }
        });

        thread.start();
    }
    /**
     * post text, used code from the NFC...
     */
    private void post_text ( String text, String id_inv ) {

        try {

            final ArrayList<NameValuePair> nameValuePairs = new  ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("text",text ));
            nameValuePairs.add(new BasicNameValuePair("account", self.get_acc() ));
            nameValuePairs.add(new BasicNameValuePair("id_inv", id_inv ));

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(UPLOAD_URL);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
            httpclient.execute(httppost);

        } catch ( Exception ex ){
            Log.e ( LOG_TAG, "ERRORE post_text :: " + self.err_str(ex));
        }
    }

    //========================

    private void copy_file ( File src, File dst ) throws IOException {

        InputStream in=null;
        OutputStream out=null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        finally {

            if(in!=null) in.close();
            if(out!=null) out.close();
        }
    }

    //	get first acc from phone
    private String get_acc( ) {

        // get first one
        Account[] accounts = AccountManager.get(self).getAccounts();

        return ( accounts!=null && accounts.length>0 ) ? accounts[0].name : null ;
    }

    /**
     * get unique id per device
     */
    private static String get_id_device(){

        //credits: http://www.pocketmagic.net/android-unique-device-id/
        //http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id/9186943#9186943 stansult, Jared Burrows
        //the above is one hell of a post!!! wow
        @SuppressWarnings("deprecation")
        String id = "35" + //we make this look like a valid IMEI
                Build.BOARD.length()%10+ Build.BRAND.length()%10 +
                Build.CPU_ABI.length()%10 + Build.DEVICE.length()%10 +
                Build.DISPLAY.length()%10 + Build.HOST.length()%10 +
                Build.ID.length()%10 + Build.MANUFACTURER.length()%10 +
                Build.MODEL.length()%10 + Build.PRODUCT.length()%10 +
                Build.TAGS.length()%10 + Build.TYPE.length()%10 +
                Build.USER.length()%10 ; //13 digits

        return id;
    }

    /**
     * get device id + timestemp + random
     */
    private static String get_id_n_timestemp(){

        Random randomGenerator = new Random();

        return get_id_device() + '_' + System.currentTimeMillis() + '_' + randomGenerator.nextInt(65535);
    }

    /**
     * print stack
     */
    private String err_str ( Exception ex ) {

        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter( writer );
        ex.printStackTrace( printWriter );
        printWriter.flush();

        return writer.toString();
    }

    //===========================

    private static String getStringFromFile (File fl) throws Exception {

        FileInputStream fin=null;
        String ret;

        try {
            fin = new FileInputStream(fl);
            ret = convertStreamToString(fin);
        }
        finally {

            if(fin!=null) fin.close();
        }
        return ret;
    }

    private static String convertStreamToString(InputStream is) throws Exception {

        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();

        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ( (line = reader.readLine()) != null ) {
                sb.append(line).append('\n');
            }
        }
        finally {

            reader.close();
        }
        return sb.toString();
    }
}
