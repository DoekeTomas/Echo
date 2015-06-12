/*
 * Framework code written for the Multimedia course taught in the first year
 * of the UvA Informatica bachelor.
 *
 * Nardi Lam, 2015 (based on code by I.M.J. Kamps, S.J.R. van Schaik, R. de Vries, 2013)
 */

package nl.uva.multimedia.audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class AudioVisualizationView extends View implements AudioListener {

    public AudioVisualizationView(Context context) {
        super(context);
    }

    public AudioVisualizationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AudioVisualizationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private int sampleRate = AudioPlayer.DEFAULT_SAMPLE_RATE;
    private int channels = AudioPlayer.DEFAULT_CHANNEL_AMOUNT;
    private long sampleCounter = 0;
    private double timePlayed = 0;

    private final Paint paint = new Paint(); {
        this.paint.setTextSize(15);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText("Sample rate: " + sampleRate, 10, 20, this.paint);
        canvas.drawText("Channels: " + channels, 10, 50, this.paint);
        canvas.drawText("Amount of samples played: " + sampleCounter, 10, 80, this.paint);
        canvas.drawText("Time played: " + timePlayed, 10, 110, this.paint);
    }

    @Override
    public void onAudio(short[] pcm, int sampleRate, int channels) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.sampleCounter += (pcm.length / channels);
        this.timePlayed = this.sampleCounter / (double)this.sampleRate;
        this.invalidate();
    }
    
}
