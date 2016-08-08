package bonebou.diordve;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import bonebou.diordve.imgProcessOCR.ImgProcessOCRFragment;
import bonebou.diordve.preview.CamPreviewFragment;

public class EvdroidActivity extends EvdroidActivity_RollerUpload
        implements CamPreviewFragment.OnFragmentInteractionListener, ImgProcessOCRFragment.OnFragmentInteractionListener {

    final static String ROOT_FOLDER_PATH = Environment.getExternalStorageDirectory().getAbsolutePath(); /* doesn't end with / */;

//  credits:  https://www.shaneenishry.com/blog/2014/08/17/ndk-with-android-studio/ - god save the women and indians - for helping me use existing libs, yep! in jniLibs!!!
    // Native JNI - load libraries
    static {
        System.loadLibrary("pngt");
        System.loadLibrary("lept");
        System.loadLibrary("tess");
        System.loadLibrary("jni_java_native_bridge");
    }

    public native boolean initOcr ( String root_folder_path );

    CamPreviewFragment prev_frag;
    ImgProcessOCRFragment imgp_frag;

//    opencv
    BaseLoaderCallback mLoaderCallback_OpenCV = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected ( int status ) {
            switch ( status ) {
                case LoaderCallbackInterface.SUCCESS: {
                    self.on_opencv_loaded();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private boolean isOcrInited = true;
    public boolean getIsOcrInited(){ return this.isOcrInited; }

    private EvdroidActivity self = this;

//    act launched
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        self.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        self.requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

//        initOcr(ROOT_FOLDER_PATH);

        Log.i(LOG_TAG, "oncreate");

        self.load_opencv();
    }

    void load_opencv(){

        self.isOcrInited = false;
        if ( !OpenCVLoader.initDebug() ) {

            Log.d(LOG_TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, self, self.mLoaderCallback_OpenCV);
        }
        else {
            Log.d ( LOG_TAG, "OpenCV library found inside package. Using it!");
            self.mLoaderCallback_OpenCV.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

//    after opencv is loaded - start the fragment, which starts the actual job
    void on_opencv_loaded(){

        self.setContentView(R.layout.activity_evdroid);

        self.prev_frag = CamPreviewFragment.newInstance(null);
        super.getFragmentManager().beginTransaction().replace(R.id.fragment_container, self.prev_frag).commit();

        new ImgProcessOCR_initOcr().execute ( "initOcr" ); /*init the tess once in native JNI via async thread to boost performance*/
    }

//  ===========

//    bound in layout xml => get_picture_callback()
    public void onclick_take_photo(View v) {

        if (self.prev_frag != null)
            self.prev_frag.onclick_take_photo(v);
    }

    public void onclick_img_process(View v) {

        if (self.imgp_frag != null)
            self.imgp_frag.onclick_img_process(v);
    }

    public void onclick_torch(View v) {

        if (self.prev_frag != null)
            self.prev_frag.onclick_torch(v);
    }

    public void onclick_calibrate(View v){
        if (self.prev_frag != null)
            self.prev_frag.onclick_calibrate(v);
    }

    // toggles the OCR an status text view/preview + buttons visibility - to allow the capture pic taking the whole screen
    public void onclick_img_capture_preview_toggle (View v) {
        if(self.imgp_frag!=null)
            self.imgp_frag.onclick_img_capture_preview_toggle(v);
    }

// CamPreviewFragment interface implementation
    @Override
    public void onFragmentInteraction(byte[] bytes) {

        Log.i(LOG_TAG, "ImgProcessOCRFrag onFragmentInteraction");

        self.imgp_frag = ImgProcessOCRFragment.newInstance(bytes);
        super.getFragmentManager().beginTransaction().replace(R.id.fragment_container, self.imgp_frag).commit();
    }

// ImgProcessOCRFrag interface implementation
    @Override
    public void onFragmentInteraction(String msg) {

        Log.i(LOG_TAG, "PreviewFrag onFragmentInteraction");

        self.prev_frag = CamPreviewFragment.newInstance(msg);
        super.getFragmentManager().beginTransaction().replace(R.id.fragment_container, self.prev_frag).commit();
    }

    //====================
    // init the tess once in native JNI via async thread to boost performance
    class ImgProcessOCR_initOcr extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground ( String... params ) {

            // JNI native call
            self.initOcr( ROOT_FOLDER_PATH );
            Log.i(LOG_TAG,"tess OCR inited");

            self.isOcrInited = true;

            return null;
        }
    }
}
