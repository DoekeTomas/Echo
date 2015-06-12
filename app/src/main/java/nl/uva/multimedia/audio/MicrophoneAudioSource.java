/*
 * Framework code written for the Multimedia course taught in the first year
 * of the UvA Informatica bachelor.
 *
 * Nardi Lam, 2015 (based on code by I.M.J. Kamps, S.J.R. van Schaik, R. de Vries, 2013)
 */

package nl.uva.multimedia.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.Arrays;

public class MicrophoneAudioSource implements AudioSource {
    public final static int DEFAULT_SAMPLE_RATE = 44100; //Hz

    private int sampleRate = DEFAULT_SAMPLE_RATE;

    private AudioRecord audioRecord;
    private short[] buffer;

    private AudioListener listener = null;

    private Context context;

    public MicrophoneAudioSource(Context context) {
        this.buffer = new short[this.getBufferSize()];

        createAudioRecord();

        this.context = context;
    }

    private void createAudioRecord() {
        this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, this.sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                this.getBufferSize() * 2);
    }

    private final Runnable readBuffer = new Runnable() {
        @Override
        public void run() {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.read(buffer, 0, getBufferSize());

                if (listener != null)
                    listener.onAudio(buffer, sampleRate, 1);

                readLater();
            }
        }
    };

    private void readLater() {
        new Handler(this.context.getMainLooper()).postDelayed(readBuffer, 1);
    }

    public int getBufferSize() {
        return AudioTrack.getMinBufferSize(this.sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT) * 2;
    }

    public void start() {
        this.audioRecord.startRecording();
        this.readLater();
    }

    public void stop() {
        this.audioRecord.stop();
        new Handler(this.context.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                audioRecord.release();
                createAudioRecord();
            }
        }, 1);
    }

    @Override
    public void setOnAudioListener(AudioListener listener) {
        this.listener = listener;
    }
}
