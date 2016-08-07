package bonebou.diordve.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

import bonebou.diordve.R;
import bonebou.diordve.calibrate.CalibrateFragment;

/**
 * Created by abentov on 7/27/16.
 * native object detection goes here...
 */
public class CamPreviewFrameCallback extends CamPreview implements Camera.PreviewCallback  {

    // native libs loaded in main class caller - does object detection - white rectangle/paper invoice
    public native boolean colourDetect (
            int width, int height, byte[] NV21frame_data_bytes, int[] pixels,
            long mat_out_vec_vec_point, String root_folder_path, int[] hsv6 );

    static final String ROOT_FOLDER_PATH =  Environment.getExternalStorageDirectory().getAbsolutePath();
    static final Scalar COLOUR_OBJ_CONTOURS_SCALAR = new Scalar(164,240,64,255); /*green*/
    static final int LINE_WIDTH_PX_OBJ_CONTOURS = 3;

    private boolean is_processing = false;
    private Bitmap bitmap;

    Handler mHandler = new Handler(Looper.getMainLooper());

    //    ==================

    //    SETTER from outside
    ImageView imgview_over_surface;
    public void set_imgview_over_surface ( ImageView  value ) {
        this.imgview_over_surface = value;
    }

    //    SETTER from outside
    CalibrateFragment calibrate_fragment;
    public void set_calibrate_fragment(CalibrateFragment value){ this.calibrate_fragment = value; }

    TextView tv_rich_poor;
    public void set_tv_rich_poor(TextView value){ this.tv_rich_poor = value;    }

    private boolean isOcrInited = super.activity.getIsOcrInited();

    CamPreviewFrameCallback self = this;
    CamPreviewFrameCallback(Context context, AttributeSet aset) {

        super(context, aset);
    }

    @Override
    public void onPreviewFrame ( byte[] bytes, Camera camera ) {

        long tStart = System.currentTimeMillis();
        if ( !self.is_processing && (self.isOcrInited || super.activity.getIsOcrInited()) ) {

            if(!self.isOcrInited) { self.isOcrInited=true; self.tv_rich_poor.setText ( R.string.POOR_RICH ); }

//        self.test_case_preview_bytes(bytes, camera);

//            object_detect_params odp = new object_detect_params(
//                    self.calibrate_fragment.get_hsv_from_sliders(), bytes);
//
//            // this.mHandler.post(this.do_image_processing);
//            new CameraPreview_objectDetect().execute (odp);

            self.object_detect(self.calibrate_fragment.get_hsv_from_sliders(), bytes);
            camera.addCallbackBuffer(bytes);
        }
        else if ( !self.isOcrInited ){ self.tv_rich_poor.setText ( R.string.POOR_RICH_OCR_INIT ); }

        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        Log.i ( LOG_TAG, "on pev frame elapsed :: " + Long.toString(tDelta) );
    }

    int[] pixels = null;
    int width=0, height=0;
    void object_detect ( int[] hsv6, byte[] frame_data_bytes ) {

        Log.i(LOG_TAG, "object_detect");

        self.is_processing = true;

        if ( self.width<1 ) {
            Camera.Parameters parameters = self.mCamera.getParameters();
            self.width = parameters.getPreviewSize().width;
            self.height = parameters.getPreviewSize().height;
            self.pixels = new int[self.width * self.height];
            Log.i(LOG_TAG, "width ::" + Integer.toString(self.width) + ", height :: " + Integer.toString(self.height) );
        }

        Mat mat_out_vec_vec_point = new Mat();

        // call native JNI c++
        self.colourDetect ( self.width, self.height, frame_data_bytes, self.pixels,
                mat_out_vec_vec_point.nativeObj, /*!*/
                ROOT_FOLDER_PATH /*!*/, hsv6 );

//        pixels = null;

        List<MatOfPoint> contours_poly2 = new ArrayList<>(); /*will have the points for the object outlines, they will be drawn by drawContours*/
        Converters.Mat_to_vector_vector_Point ( mat_out_vec_vec_point, contours_poly2 );

        mat_out_vec_vec_point.release ( );

        self.bitmap = Bitmap.createBitmap ( height, width, Bitmap.Config.ARGB_8888 ) ; /*mind the height and width reversed - PORTRAIT mode*/

        Mat mat = new Mat();
        Utils.bitmapToMat ( self.bitmap, mat );
        for  ( int i = 0; i < contours_poly2.size(); ++i ) {
            Imgproc.drawContours(mat, contours_poly2, i, COLOUR_OBJ_CONTOURS_SCALAR, LINE_WIDTH_PX_OBJ_CONTOURS);
        }
        Utils.matToBitmap ( mat, self.bitmap );
        mat.release();

//        self.set_bitmap();
        self.imgview_over_surface.setImageBitmap ( self.bitmap ) ;

        self.is_processing = false;
    }

    //=======================

    //test functions

    int randomWithRange ( int min, int max )
    {
        int range = (max - min) + 1;
        return (int)(Math.random() * range) + min;
    }

    void test_case_preview_bytes (  byte[] bytes, Camera camera  ) {

        Log.i ( LOG_TAG+"_framecb", "on prev frame" + java.util.Arrays.toString(self.calibrate_fragment.get_hsv_from_sliders()) );

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);

        paint.setColor( randomWithRange( Integer.parseInt("ffffff",16), Integer.parseInt("000000",16) ) *-1 );
//        paint.setColor(Color.GREEN);
//        Log.i("prev callback", "colour: " + Integer.toString(paint.getColor()));
//        Log.i("prev callback", "colour yellow is: " + Integer.toString(Color.YELLOW));

        int x = 480/4; x += randomWithRange(1,x);
        int y = 640/4; y += randomWithRange(1,y);

        Bitmap bmap = Bitmap.createBitmap ( 480, 640, Bitmap.Config.ARGB_8888 ) ;

        if(bmap==null) return;

        Canvas c = new Canvas(bmap); if(c==null) return;

        c.drawCircle ( x, y, 70 /*radius*/, paint );

        this.imgview_over_surface.setImageBitmap(bmap);
    }

    //=======================

    // called by the async thread CameraPreview_objectDetect
    void set_bitmap ( ) {
        self.mHandler.post(self.run_set_bitmap);
    }

    Runnable run_set_bitmap = new Runnable ( ) {

        public void run() {
            self.imgview_over_surface.setImageBitmap ( self.bitmap ) ;
//            self.bitmap.recycle(); /*!dont*/
//            self.bitmap = null;
        }
    };

    class object_detect_params {
        int[] hsv; byte[] frame_data_bytes;
        object_detect_params(int[] hsv, byte[] frame_data_bytes){

            this.hsv = hsv; this.frame_data_bytes = frame_data_bytes;

        }
    }

    class CameraPreview_objectDetect extends AsyncTask<object_detect_params, Integer, String> {

        @Override
        protected String doInBackground ( object_detect_params... params ) {

            //=========
            return null;
        }
    }
}
