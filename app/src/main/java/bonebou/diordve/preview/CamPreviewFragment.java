package bonebou.diordve.preview;

import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import bonebou.diordve.R;
import bonebou.diordve.calibrate.CalibrateFragment;

public class CamPreviewFragment extends CamPreviewFragmentSink {

    final static String ARG_PARAM1 = "param1";
    String msg; // used to pass stuff between instances

    CamPreviewFrameCallback mPreview;
    CalibrateFragment calibrate_fragment;

    OnFragmentInteractionListener mListener;
    private boolean is_calibrate;

    RelativeLayout calibrate_fragment_container;

    CamPreviewFragment self = this;

    public CamPreviewFragment ( ) {
        // Required empty public constructor
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction( byte[] bytes );
    }

    public static CamPreviewFragment newInstance(String msg) {
        CamPreviewFragment fragment = new CamPreviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, msg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (self.getArguments() != null) {
            self.msg = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_cam_preview, container, false);

        self.mPreview = (CamPreviewFrameCallback) v.findViewById(R.id.cam_preview);

        super.imgview_over_surface = (ImageView) v.findViewById(R.id.imgview_over_surface);
        self.mPreview.set_imgview_over_surface(super.imgview_over_surface);

        //set child fragment - calibrate sliders
        self.calibrate_fragment = CalibrateFragment.newInstance();
        super.getChildFragmentManager().beginTransaction().replace(R.id.calibrate_fragment_container, self.calibrate_fragment).commit();

        self.mPreview.set_calibrate_fragment(self.calibrate_fragment);

        self.calibrate_fragment_container = (RelativeLayout) v.findViewById(R.id.calibrate_fragment_container);

        super.tv_rich_poor = (TextView) v.findViewById(R.id.tv_rich_poor);
        self.mPreview.set_tv_rich_poor(super.tv_rich_poor);

        return v;
    }

    @Override
    public void onResume() {

        super.onResume();
        self.safeCameraOpen();
    }

    @Override
    public void onPause() {

        super.onPause();
        self.releaseCameraAndPreview();
    }

    @Override
    public void onDetach() {

        super.onDetach();
        self.mListener = null;
    }

    //=====================

    boolean safeCameraOpen ( ) {

        boolean qOpened = false;

        try {
            releaseCameraAndPreview();
            super.mCamera = Camera.open( super.get_camera_id() );
            if ( qOpened = (super.mCamera != null) ) {

                super.set_camera_display_orientation ( super.get_camera_facing() );

                super.set_camera_params();

                self.mPreview.setCamera(super.mCamera);
            }
        } catch (Exception e) {
            Log.e ( LOG_TAG, "failed to open Camera" ) ;
            e.printStackTrace();
        }

        return qOpened;
    }

    void releaseCameraAndPreview ( ) {
        self.mPreview.setCamera(null);
        if (super.mCamera != null) {
            super.mCamera.release();
            super.mCamera = null;
        }
    }

    //=====================

    Camera.PictureCallback picture_callback = new Camera.PictureCallback( ) {

        @Override
        public void onPictureTaken ( byte[] data, Camera camera ) {

            Log.i ( LOG_TAG, "picture_callback" ) ;

            self.mPreview.setCamera(null);

            self.check_interface();

            if ( self.mListener != null ) {
                Log.i ( LOG_TAG, "we have mListener - calling it..." ) ;
                self.mListener.onFragmentInteraction( data );
            }
        }
    };

//    credits :: http://stackoverflow.com/questions/10891742/android-takepicture-not-making-sound - HOLLO
    Camera.ShutterCallback shutter_callback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
        /* Empty Callbacks play a sound! */
        }
    };

    void check_interface ( ) {

        if ( self.getActivity() instanceof OnFragmentInteractionListener ) {
            self.mListener = (OnFragmentInteractionListener) self.getActivity();
        } else {
            Log.e( LOG_TAG, "act not implemented onfrag interface!!!" );
        }
    }

    //=====================
//    external/declarative onclick and other events
    //=====================

    //    bound in layout xml => get_picture_callback()
    public void onclick_take_photo ( View v ) {

//        super.mCamera.setPreviewCallback(null); /*onPreviewFrame*/
        super.mCamera.setPreviewCallbackWithBuffer(null); /*onPreviewFrame*/
        super.mCamera.takePicture ( self.shutter_callback, null, self.picture_callback ) ;
        Log.i ( LOG_TAG, "onclick_take_photo" );
    }

    public void onclick_calibrate(View v) {

        if(self.calibrate_fragment_container==null) return;

        if(self.is_calibrate){
            self.is_calibrate=false; self.calibrate_fragment_container.setVisibility(View.GONE);
        }
        else {
            self.is_calibrate=true; self.calibrate_fragment_container.setVisibility(View.VISIBLE);
        }
    }
}
