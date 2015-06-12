/*
 * Framework code written for the Multimedia course taught in the first year
 * of the UvA Informatica bachelor.
 *
 * Nardi Lam, 2015 (based on code by I.M.J. Kamps, S.J.R. van Schaik, R. de Vries, 2013)
 */

package nl.uva.multimedia.image;

/*
 * This is a simple generic interface for classes that provide images to be used for processing.
 */
public interface ImageSource {
    void setOnImageListener(ImageListener listener);
}
