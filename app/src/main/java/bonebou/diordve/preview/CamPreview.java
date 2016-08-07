package bonebou.diordve.preview;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import bonebou.diordve.EvdroidActivity;

/**
 * Created by abentov on 7/27/16.
 */
public class CamPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    static final String LOG_TAG = "CamPreview";

    SurfaceHolder mHolder;
    Camera mCamera;
    protected EvdroidActivity activity;
    private int buffer_size;
    protected byte[] preview_buffer;

    CamPreview self = this;

    // construct
    CamPreview(Context context, AttributeSet aset ) {

        super(context, aset);

        self.activity = (EvdroidActivity) context;

        Log.i ( LOG_TAG, "prev construct");

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        self.mHolder = super.getHolder();
        self.mHolder.addCallback(self);
        self.mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
//        implemented in child class
//        Log.i ( LOG_TAG, "on prev frame" );
    }

    private void set_buffer_for_preview_callback(){

        Camera.Parameters params = self.mCamera.getParameters();

        Camera.Size preview_size = params.getPreviewSize();
        self.buffer_size = preview_size.width*preview_size.height*3/2;
        self.preview_buffer = new byte[self.buffer_size];
        self.mCamera.addCallbackBuffer(self.preview_buffer);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.i ( LOG_TAG, "surf created");
        try
        {
            if (self.mCamera != null)
            {
                self.mHolder = surfaceHolder;
                self.mCamera.setPreviewDisplay(surfaceHolder);

//                self.mCamera.setPreviewCallback(self); /*onPreviewFrame*/
                self.mCamera.setPreviewCallbackWithBuffer(self); /*onPreviewFrame*/
            }
        }
        catch (IOException exception)
        {
            Log.e( LOG_TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {

        if( self.mCamera==null ) return;

        Log.i( LOG_TAG, "surface changed, w: " + Integer.toString(w) + ", h: " + Integer.toString(h) );
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        Log.i ( LOG_TAG, "surf destroyed");

        // Surface will be destroyed when we return, so stop the preview.
        if ( self.mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            self.mCamera.stopPreview();
//            self.mCamera.setPreviewCallback(null); /*stop onPreviewFrame*/
            self.mCamera.setPreviewCallbackWithBuffer(null); /*stop onPreviewFrame*/
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom){

        Log.i ( LOG_TAG, "onlayout");
    }

    //========================
    public void setCamera(Camera camera) {

        if (self.mCamera == camera) { return; }

        if ( self.mHolder.getSurface() == null ) {
            // preview surface does not exist
            return;
        }

        stopPreviewAndFreeCamera();

        self.mCamera = camera;

        if ( self.mCamera != null ) {

            try {
                self.mCamera.setPreviewDisplay(self.mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Important: Call startPreview() to start updating the preview
            // surface. Preview must be started before you can take a picture.
            self.mCamera.startPreview();
        }
    }

    /**
     * When this function returns, mCamera will be null.
     */
    void stopPreviewAndFreeCamera() {

        if (self.mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            self.mCamera.stopPreview();
//            self.mCamera.setPreviewCallback(null); /*stop onPreviewFrame*/
            self.mCamera.setPreviewCallbackWithBuffer(null); /*stop onPreviewFrame*/

            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            self.mCamera.release();

            self.mCamera = null;
        }
    }
}
