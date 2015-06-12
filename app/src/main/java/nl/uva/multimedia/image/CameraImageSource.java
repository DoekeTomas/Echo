/*
 * Framework code written for the Multimedia course taught in the first year
 * of the UvA Informatica bachelor.
 *
 * Nardi Lam, 2015 (based on code by I.M.J. Kamps, S.J.R. van Schaik, R. de Vries, 2013)
 */

package nl.uva.multimedia.image;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;

/*
 * An ImageSource that passes on frames recieved from the Android camera API.
 */
public class CameraImageSource implements ImageSource, Camera.PreviewCallback {

    /*** Camera types (simplified) ***/
    public static final int BACK_CAMERA = 0;
    public static final int FRONT_CAMERA = 1;

    private boolean hasCamera = false;
    private Camera camera = null;

    private int backCameraId = -1;
    private int frontCameraId = -1;
    private int currentCamera = BACK_CAMERA;

    private int[] currentImage = null;
    private int imageWidth, imageHeight;

    private ImageListener listener = null;

    private final Display display;
    private final Context context;

    public CameraImageSource(Context context) {
        int numCameras = Camera.getNumberOfCameras();

        for (int id = 0; id < numCameras; id++) {
            Camera.CameraInfo info = new Camera.CameraInfo(); Camera.getCameraInfo(id, info);
            if (this.backCameraId == -1 && info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                this.backCameraId = id;
            }
            if (this.frontCameraId == -1 && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                this.frontCameraId = id;
            }
        }

        this.context = context;
        this.display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
    }

    @Override
    public void setOnImageListener(ImageListener listener) {
        if (listener != null) {
            // Acquire camera
            if (!this.hasCamera)
                this.acquireCamera();
            this.listener = listener;
        } else {
            // Release camera
            if (this.hasCamera)
                this.releaseCamera();
            this.listener = null;
        }
    }

    private void releaseCamera() {
        if (this.camera != null) {
            this.camera.stopPreview();
            this.camera.setPreviewCallback(null);
            this.camera.release();
            this.camera = null;
        }

        this.hasCamera = false;
    }

    /* Something to make the camera API think it's rendering directly to something. */
    private final SurfaceTexture dummySurface = new SurfaceTexture(42);

    private void acquireCamera() {
        this.releaseCamera();

        if (currentCamera == FRONT_CAMERA) {
            this.camera = Camera.open(frontCameraId);
        } else {
            this.camera = Camera.open(backCameraId);
        }

        this.camera.setPreviewCallback(this);
        try {
            this.camera.setPreviewTexture(dummySurface);
        } catch (IOException e) {
            Toast.makeText(this.context, e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        this.camera.startPreview();

        this.hasCamera = true;
    }

    public void switchTo(int cameraType) {
        if (cameraType == BACK_CAMERA && this.backCameraId != -1)
            this.currentCamera = BACK_CAMERA;
        else if (cameraType == FRONT_CAMERA && this.frontCameraId != -1)
            this.currentCamera = FRONT_CAMERA;

        if (this.hasCamera)
            acquireCamera();
    }

    private boolean frozen = false;

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    byte[] yuvBuffer = null;

    /* Called when a new image arrives from the camera */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(this.currentCamera, info);

        this.imageWidth = size.width;
        this.imageHeight = size.height;
        int numPixels = this.imageWidth * this.imageHeight;
        if (this.currentImage == null || this.currentImage.length != numPixels)
            this.currentImage = new int[numPixels];
        /* The incoming image data is YUV, so we have to convert it. */
        convertYUV420SPtoARGB(this.currentImage, data, this.imageWidth, this.imageHeight,
                display.getRotation(), info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);

        switch (display.getRotation()) {
            case Surface.ROTATION_90: case Surface.ROTATION_270:
                this.imageWidth = size.height;
                this.imageHeight = size.width;
            break;
        }

        if (!this.frozen && this.listener != null)
            this.listener.onImage(this.currentImage, this.imageWidth, this.imageHeight);
    }

    /*** YUV to RGB conversion ***/

    private static final int y_scale  = (int)(1.164063 * 1024);
    private static final int rv_scale = (int)(1.595703 * 1024);
    private static final int gu_scale = (int)(0.390625 * 1024);
    private static final int gv_scale = (int)(0.813477 * 1024);
    private static final int bu_scale = (int)(2.017578 * 1024);

    /**
     * convert YUV data to RGB data.
     *
     * @param rgb Output array of integers, containing ARGB data, in that
     * order, preallocate this to the correct size for the image.
     *
     * @param yuv Input array of YUV byte data, as supplied by Android cameras
     * @param w The width of the output image
     * @param h The height of the output image
     */
    public static void convertYUV420SPtoARGB(int[] rgb, byte[] yuv, int w, int h, int r, boolean f) {

		/* Sanity check, we won't do anything for wrongly sized data */
        if (yuv.length != (w * h) + 2 * (w/2 * h/2)) {
            return;
        }

        int frame_size = w * h;

        int y_plane  = 0;
        int uv_plane = frame_size;

        int y_value  = 0;
        int u_value  = 0;
        int v_value  = 0;

        int r_value  = 0;
        int g_value  = 0;
        int b_value  = 0;

        for (int y = 0; y < h; ++y) {
			/* Use the same strip in the UV-plane for every two vertical
			 * Y-components.
			 */
            uv_plane = frame_size + (y / 2) * w;

            for (int x = 0; x < w; ++x) {
				/* Grab the Y-component from the Y-plane, subtract 16 to fix
				 * the range.
				 */
                y_value = (yuv[y_plane] & 0xFF) - 16;

				/* Extract the UV-components from the UV-plane, and use it for
				 * every two horizontal Y-components.
				 */
                if ((x % 2) == 0) {
					/* Subtract 128 to fix the range. */
                    v_value = (yuv[uv_plane++] & 0xFF) - 128;
                    u_value = (yuv[uv_plane++] & 0xFF) - 128;
                }

				/* Multiply the YUV-vector with the YUVtoRGB matrix. */
                y_value *= y_scale;
                r_value = (y_value + rv_scale * v_value) / 1024;
                g_value = (y_value - gu_scale * u_value - gv_scale * v_value) /
                        1024;
                b_value = (y_value + bu_scale * u_value) / 1024;

				/* Clamp the RGB-vector. */
                if (r_value > 255)
                    r_value = 255;
                else if (r_value < 0)
                    r_value = 0;

                if (g_value > 255)
                    g_value = 255;
                else if (g_value < 0)
                    g_value = 0;

                if (b_value > 255)
                    b_value = 255;
                else if (b_value < 0)
                    b_value = 0;

                int x2 = x;
                if (f) x2 = w - x - 1;

                int end = y * w + x2;
                if (r == 2) {
                    end = (h - y - 1) * w + (w - x2 - 1);
                } else if (r == 1) {
                    end = (w - x2 - 1) * h + y;
                } else if (r == 3) {
                    end = x2 * h + (h - y - 1);
                }

				/* Pack the RGB-vector. */
                rgb[end] = (0xff << 24) | (r_value << 16) | (g_value << 8) |
                        (b_value << 0);

                ++y_plane;
            }
        }
    }

}
