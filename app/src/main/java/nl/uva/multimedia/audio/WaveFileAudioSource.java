/*
 * Framework code written for the Multimedia course taught in the first year
 * of the UvA Informatica bachelor.
 *
 * Nardi Lam, 2015 (based on code by I.M.J. Kamps, S.J.R. van Schaik, R. de Vries, 2013)
 */

package nl.uva.multimedia.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/* Audio source that reads an external .wav file and provides it as a series of PCM buffers. */
/* FIXME: handle 8-bit wavs (detect and convert (upsample) byte buffer properly) */
public class WaveFileAudioSource implements AudioSource {

    private final WaveFileAudioSource self = this;
    private Context context;

    private Uri uri;
    private InputStream is;
    private int sampleRate;
    private int channels;
    private ByteBuffer byteBuffer;
    private short[] shortBuffer;

    private boolean fileLoaded = false;

    private AudioListener listener = null;

    public WaveFileAudioSource(Context context) {
        this.context = context;
    }

    public void loadFromUri(Uri uri) throws IOException {
        this.pause();

        this.uri = uri;
        this.is = this.context.getContentResolver().openInputStream(uri);

        /* Read metadata from header and ready InputStream */
        byte[] srBytes = new byte[4], chBytes = new byte[2];
        this.is.skip(22);
        this.is.read(chBytes, 0, 2);
        this.is.read(srBytes, 0, 4);
        this.is.skip(16);

        this.sampleRate = WaveFileAudioSource.bytesToInt(srBytes, 0, 4);
        this.channels = WaveFileAudioSource.bytesToInt(chBytes, 0, 2);
        this.shortBuffer = new short[AudioTrack.getMinBufferSize(this.sampleRate,
                this.channels == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT)];
        this.byteBuffer = ByteBuffer.wrap(new byte[this.shortBuffer.length * 2])
                .order(ByteOrder.LITTLE_ENDIAN);

        this.fileLoaded = true;
    }

    private boolean playing = false;

    private final Runnable readBuffer = new Runnable() {
        @Override
        public void run() {
            if (self.playing) {
                int bytesRead = 0;
                try {
                    bytesRead = self.is.read(self.byteBuffer.array());
                } catch (IOException e) {
                    Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                boolean fullBuffer = bytesRead == self.byteBuffer.array().length;

                self.byteBuffer.asShortBuffer().get(self.shortBuffer);
                if (!fullBuffer)
                    Arrays.fill(self.shortBuffer, bytesRead / 2, self.shortBuffer.length, (short)0);
                if (self.listener != null)
                    self.listener.onAudio(self.shortBuffer, self.sampleRate, self.channels);

                if (fullBuffer)
                    self.readLater();
            }
        }
    };

    private void readLater() {
        new Handler(this.context.getMainLooper()).postDelayed(readBuffer, 1);
    }

    public void play() {
        if (!this.playing && this.fileLoaded) {
            this.readLater();
            this.playing = true;
        }
    }

    public void pause() {
        if (this.playing) {
            this.playing = false;
        }
    }

    public void toggle() {
        if (this.playing) this.pause(); else this.play();
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public void rewind() throws IOException {
        if (fileLoaded) {
            this.pause();
            this.is.close();
            this.is = this.context.getContentResolver().openInputStream(uri);
            this.is.skip(44);
        }
    }

    @Override
    public void setOnAudioListener(AudioListener listener) {
        this.listener = listener;
    }

    /* Convert bytes to an integer (little endian). */
    private static int bytesToInt(byte[] bytes, int offset, int length) {
        int result = 0;
        for (int i = 0; i < length; i++)
            result += (bytes[i + offset] & 0xFF) << i * 8;
        return result;
    }
}
