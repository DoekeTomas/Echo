/*
 * Framework code written for the Multimedia course taught in the first year
 * of the UvA Informatica bachelor.
 *
 * Nardi Lam, 2015 (based on code by I.M.J. Kamps, S.J.R. van Schaik, R. de Vries, 2013)
 */

package nl.uva.multimedia;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.FileNotFoundException;

import nl.uva.multimedia.image.CameraImageSource;
import nl.uva.multimedia.image.FileImageSource;
import nl.uva.multimedia.image.ImageDisplayView;

/*
 * An activity containing the basics for an image processing application.
 */
public class ImageActivity extends Activity {

    /*** Source constants (position in list) ***/
    final int SOURCE_BACK_CAMERA = 0;
    final int SOURCE_FRONT_CAMERA = 1;
    final int SOURCE_IMAGE = 2;

    private CameraImageSource cis;
    private FileImageSource fis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Dit is het "beginpunt" van de applicatie!
        // Als je vanaf hier de code stap voor stap doorloopt zul je alles tegen moeten komen.
        // De layout is gedefiniëerd in res/layout/activity_image.xml, dit wordt ingesteld via
        // this.setContentView() hieronder.

        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image);

        /* Create sources: */
        this.cis = new CameraImageSource(this);
        this.fis = new FileImageSource();

        Spinner sourceSpinner = (Spinner)this.findViewById(R.id.source_spinner);

        /* Switching between sources: */
        sourceSpinner.setOnItemSelectedListener(
            new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case SOURCE_BACK_CAMERA:
                        ImageActivity.this.cis.switchTo(CameraImageSource.BACK_CAMERA);
                        ImageActivity.this.switchToCamera();
                        break;
                    case SOURCE_FRONT_CAMERA:
                        ImageActivity.this.cis.switchTo(CameraImageSource.FRONT_CAMERA);
                        ImageActivity.this.switchToCamera();
                        break;
                    case SOURCE_IMAGE:
                        ImageActivity.this.switchToImage();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        /* Freeze switch: */
        ((CompoundButton)findViewById(R.id.freeze_toggle)).setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ImageActivity.this.cis.setFrozen(isChecked);
            }
        });

        /* "Load image" button: */
        findViewById(R.id.load_image_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Start an external Activity for choosing an image, result is returned in
                 * onActivityResult(). */
                Intent it = new Intent();
                it.setType("image/*");
                it.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(it, "Load image from..."), SOURCE_IMAGE);
            }
        });

        /* Select the back camera as default source: */
        sourceSpinner.setSelection(SOURCE_BACK_CAMERA);
    }

    private void switchToCamera() {
        // TODO: Onder andere hier wordt een ImageSource aan een View (ter weergave) gekoppeld.
        // Als je nog iets tussen de twee in zou willen plaatsen, is dit dus het moment!

        /* Set camera as active source: */
        ImageDisplayView idv = (ImageDisplayView)findViewById(R.id.display_view);
        if (idv.getImageSource() != this.cis) {
            idv.setImageSource(this.cis);
        }

        /* Switch out controls: */
        findViewById(R.id.load_image_button).setVisibility(View.GONE);
        findViewById(R.id.freeze_control).setVisibility(View.VISIBLE);
    }

    private void switchToImage() {
        /* Set image as active source: */
        ImageDisplayView idv = (ImageDisplayView)findViewById(R.id.display_view);
        if (idv.getImageSource() != this.fis) {
            idv.setImageSource(this.fis);
        }

        /* Switch out controls: */
        findViewById(R.id.load_image_button).setVisibility(View.VISIBLE);
        findViewById(R.id.freeze_control).setVisibility(View.GONE);
    }

    /* When an image is loaded, the result gets delivered back here: */
    protected void onActivityResult(int requestCode, int resultCode, Intent it) {
        super.onActivityResult(requestCode, resultCode, it);

        /* If we have an image... */
        if (requestCode == SOURCE_IMAGE && resultCode == RESULT_OK && it != null) {
            try {
                /* ...open an input stream and pass it to the FileImageSource: */
                fis.loadFromInputStream(this.getContentResolver().openInputStream(it.getData()));
            } catch (FileNotFoundException e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

}
