package bonebou.diordve.imgProcessOCR;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import bonebou.diordve.EvdroidActivity_RollerUpload;
import bonebou.diordve.R;

public class ImgProcessOCRFragment extends Fragment {

    public native boolean saveMiddleClass ( String root_folder_path, String img_unique_no_ext, long inputImage );  /*!not Boolean!!!*/
    public native boolean terminateOcrRecognition( );

    final static String PHOTO_PREFIX = "smc"; /*prefix of the high resolution photo/picture taken*/
    final static String ROOT_FOLDER_PATH = Environment.getExternalStorageDirectory().getAbsolutePath(); /* doesn't end with / */
    final static String IMG_CAPTURE_PATH = ROOT_FOLDER_PATH + "/tessdata/img/"+PHOTO_PREFIX+".jpg";

    static final String LOG_TAG = "ImgProcessOCRFragment";
    static final String ARG_PARAM1 = "param1";

    Handler mHandler = new Handler(Looper.getMainLooper());

    TextView tv_dump;
    ImageView imgview_result;
    RelativeLayout lay_wrap_text_n_buttons;
    byte[] bytes;

    OnFragmentInteractionListener mListener;
    ImgProcessOCR_processPic task;

    ImgProcessOCRFragment self = this;

    public ImgProcessOCRFragment() {
        // Required empty public constructor
    }

    //    INTERFACE
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String msg);
    }

    public static ImgProcessOCRFragment newInstance ( byte[] bytes ) {
        ImgProcessOCRFragment fragment = new ImgProcessOCRFragment();
        Bundle args = new Bundle();
        args.putByteArray(ARG_PARAM1, bytes);
        fragment.setArguments(args); /*!*/
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(LOG_TAG, "create");
        if (getArguments() != null) {

            Log.i ( LOG_TAG, "on create has args"  );
            self.bytes = self.getArguments().getByteArray(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_img_process_ocr, container, false);

        self.tv_dump = (TextView) v.findViewById(R.id.tv_dump);
        self.tv_dump.setMovementMethod(new ScrollingMovementMethod()); /*scrolling for text view OCR results*/

        self.imgview_result = (ImageView) v.findViewById(R.id.imgview_result);

        self.lay_wrap_text_n_buttons = (RelativeLayout) v.findViewById(R.id.lay_wrap_text_n_buttons);

        Log.i ( LOG_TAG, "oncview"  );

        if ( self.bytes!=null ) {
            Log.i ( LOG_TAG, "has bytes" );
            self.tv_dump.setText ( "bytes gotten::" + self.bytes.length );
//            self.set_img_from_bytes();
            // pic
            self.task = new ImgProcessOCR_processPic();
            self.task.execute ( self.bytes );
            self.tv_dump.setText ( "" );
        }

        return v;
    }

    void set_img_from_bytes(){

        Bitmap bm = BitmapFactory.decodeByteArray(self.bytes, 0, self.bytes.length);
        self.imgview_result.setImageBitmap(bm);
    }

    @Override
    public void onDetach ( ) {
        super.onDetach();
        self.mListener = null;
    }

    //==================
    void append_txt ( final String txt ) {

        self.mHandler.post ( new Runnable() {
            @Override
            public void run ( ) {

                if(txt==null) return;

                if ( txt.contains("RESET_CLEAR_IMG")  )
//                    self.imgview_result.setImageURI(Uri.parse(IMG_CAPTURE_PATH+"?time=fuck"));
                    Log.i(LOG_TAG, "RESET_CLEAR_IMG");
                else if ( txt.contains("DISPLAY_IMG") ) {
                    self.imgview_result.setImageURI(Uri.parse(IMG_CAPTURE_PATH));
                    Log.i(LOG_TAG, "DISPLAY_IMG");
                }
                else if ( txt.contains("CCLLEEAARR") ) {
                    self.tv_dump.setText ( "" );
                    Log.i(LOG_TAG, "CCLLEEAARR");
                }
                else {
                    // self.tv.append ( txt + "\n" );
                    self.tv_dump.append(txt);
                    Log.i ( LOG_TAG, txt );
                }
            }

        } );
    }

    // called by the c++ JNI
    public void messageMe ( String text ) {
        self.append_txt ( text );
    }
    //==================

    // this chap internally saves a pic from the native/c++
    class ImgProcessOCR_processPic extends AsyncTask<byte[], Integer, String> {

        @Override
        protected String doInBackground ( byte[]... params ) {

            Log.i ( LOG_TAG, "do in bg start" );

            self.messageMe("RESET_CLEAR_IMG");
            // this.publishProgress(1);
            byte[] data = params[0];

            Log.i ( LOG_TAG, "should send to native with byte count :: " + Long.toString(data.length) );

            Mat mat=new Mat();
            Bitmap bmp = BitmapFactory.decodeByteArray ( data, 0, data.length );
            Utils.bitmapToMat ( bmp, mat ); bmp.recycle();
            Imgproc.cvtColor ( mat, mat, Imgproc.COLOR_RGB2BGR );

            Log.i ( LOG_TAG, "calling save middle class native :: " + Long.toString(data.length) );
            // JNI native call
            self.saveMiddleClass ( ROOT_FOLDER_PATH /*static*/, PHOTO_PREFIX, mat.getNativeObjAddr() ) ;
            Log.i ( LOG_TAG, "got back from save middle native fork" );
            mat.release();

            return null;
        }
    }

    //=================
    void check_interface ( ) {

        if (self.getActivity() instanceof OnFragmentInteractionListener) {
            self.mListener = (OnFragmentInteractionListener) self.getActivity();
        }
        else {
            Log.e ( LOG_TAG, "act not implemented onfrag interface!!!" ) ;
        }
    }

    //=================
    // red or green (OK, come again buttons click )
    public void onclick_img_process ( View v ) {

        self.check_interface();

        if ( self.mListener != null ) {

            if ( self.task !=null && !self.task.isCancelled() ) {

                Log.i ( LOG_TAG, "~~~~~~~~~~~~~~canceling self.task~~~~~~~~~~~" );
                self.task.cancel(true);
            }

            // native call to terminate (heavy) OCR recognition
            terminateOcrRecognition();

            boolean is_ok = v.getId()==R.id.btn_ok;
            String msg = (is_ok) ? "OK": "AGAIN";

            if ( is_ok ) ((EvdroidActivity_RollerUpload)self.getActivity()).upload_ocr_n_img();

            self.mListener.onFragmentInteraction(msg);
        }
    }

    public void onclick_img_capture_preview_toggle(View v) {

        if ( self.lay_wrap_text_n_buttons.getVisibility()==View.GONE )
            self.lay_wrap_text_n_buttons.setVisibility(View.VISIBLE);
        else
            self.lay_wrap_text_n_buttons.setVisibility(View.GONE);
    }

}
