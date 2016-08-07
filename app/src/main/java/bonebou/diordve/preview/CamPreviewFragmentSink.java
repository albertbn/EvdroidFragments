package bonebou.diordve.preview;

import android.app.ActionBar;
import android.app.Fragment;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by albert on 7/30/16.
 * idea is this is a house-keeping class, sink is a place in the house that sometimes looks messy,
 * no one wants to deal and look a it,
 * but can't do without it
 */
public class CamPreviewFragmentSink extends Fragment {

    final static String LOG_TAG = "CamPreviewFragment";
    final static int PHOTO_WIDTH = 2048, PHOTO_HEIGHT = 1536, PHOTO_QUALITY_PERCENT = 100;
    final int num_of_cameras = Camera.getNumberOfCameras();

    protected Camera mCamera;
    protected ImageView imgview_over_surface; /*set by child*/
    protected TextView tv_rich_poor; /*set by child*/

    boolean is_torch_on;
    boolean is_camera_front = false;
    int get_camera_facing ( ) {
        return self.is_camera_front ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    int camera_id = -1;
    int get_camera_id(){

        if(self.camera_id<0){
            if(self.find_back_facing_camera()<0){
                self.find_front_facing_camera();
            }
        }
        return self.camera_id;
    }

    CamPreviewFragmentSink self = this;

    //============

    void set_camera_display_orientation (int cameraId) {

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = self.getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        self.mCamera.setDisplayOrientation(result);
    }

    void set_camera_params() {

        Camera.Parameters params = self.mCamera.getParameters();

        //picture/capture/photo size
        Camera.Size picture_size = get_closest_size (
                self.PHOTO_WIDTH, self.PHOTO_HEIGHT, params.getSupportedPictureSizes() );
        params.setPictureSize ( picture_size.width, picture_size.height );
        params.setJpegQuality ( PHOTO_QUALITY_PERCENT );

        //preview size - NOTE, the width and height are swapped
        ViewGroup.LayoutParams imgview_params = self.imgview_over_surface.getLayoutParams();
        Camera.Size preview_size = get_closest_size (
                imgview_params.height, imgview_params.width,
                params.getSupportedPreviewSizes() );
        Log.i ( LOG_TAG, "set_cam_params :: " + Integer.toString(imgview_params.height) + ", " + Integer.toString(imgview_params.width) );
        params.setPreviewSize ( preview_size.width, preview_size.height );

        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        self.mCamera.setParameters(params);
    }

    int find_front_facing_camera() {

        for (int i = 0; i < self.num_of_cameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                self.camera_id = i;
                self.is_camera_front = true;
                break;
            }
        }
        return self.camera_id;
    }

    int find_back_facing_camera() {
        for (int i = 0; i < self.num_of_cameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                self.camera_id = i;
                self.is_camera_front = false;
                break;
            }
        }
        return self.camera_id;
    }

    public void onclick_torch ( View v ) {
        Camera.Parameters params = self.mCamera.getParameters();
        if ( self.is_torch_on ) {
            self.is_torch_on=false;
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        }
        else {
            self.is_torch_on=true;
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }
        self.mCamera.setParameters(params);
    }

    //================
    static Camera.Size get_closest_size(
            int width,
            int height,
            List<Camera.Size> sizes
    ) {
        Camera.Size result=null;
        for ( Camera.Size size : sizes ) {

            if ( size.width<=width && size.height<=height ) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if ( newArea>resultArea ) {
                        result=size;
                    }
                }
            }
        }

        return result;
    }
}
