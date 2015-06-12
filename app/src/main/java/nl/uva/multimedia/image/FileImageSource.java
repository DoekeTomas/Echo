/*
 * Framework code written for the Multimedia course taught in the first year
 * of the UvA Informatica bachelor.
 *
 * Nardi Lam, 2015 (based on code by I.M.J. Kamps, S.J.R. van Schaik, R. de Vries, 2013)
 */

package nl.uva.multimedia.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;

/*
 * An ImageSource that can load an image from an InputStream (e.g. a file).
 */
public class FileImageSource implements ImageSource {
    private ImageListener listener = null;
    private int[] currentImage = null;
    private int width, height;

    @Override
    public void setOnImageListener(ImageListener listener) {
        if (this.currentImage != null && listener != null)
            listener.onImage(this.currentImage, this.width, this.height);
        this.listener = listener;
    }

    public void loadFromInputStream(InputStream is) {
        Bitmap image = BitmapFactory.decodeStream(is);
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.currentImage = new int[this.width * this.height];
        image.getPixels(this.currentImage, 0, this.width, 0, 0, this.width, this.height);
        if (this.listener != null)
            this.listener.onImage(this.currentImage, this.width, this.height);
    }
}
