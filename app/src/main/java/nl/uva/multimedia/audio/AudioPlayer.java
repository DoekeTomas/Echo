/*
 * Framework code written for the Multimedia course taught in the first year
 * of the UvA Informatica bachelor.
 *
 * Nardi Lam, 2015 (based on code by I.M.J. Kamps, S.J.R. van Schaik, R. de Vries, 2013)
 */

package nl.uva.multimedia.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioPlayer implements AudioListener {
    public final static int DEFAULT_SAMPLE_RATE = 44100; //Hz
    public final static int DEFAULT_CHANNEL_AMOUNT = 2;

    private int sampleRate = DEFAULT_SAMPLE_RATE;
    private int channels = DEFAULT_CHANNEL_AMOUNT;

    private AudioTrack audioTrack;

    public AudioPlayer() {
        this.audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, this.sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                2 * this.getBufferSize(), AudioTrack.MODE_STREAM);
    }

    public int getBufferSize() {
        return AudioTrack.getMinBufferSize(this.sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
    }

    private void setChannels(int channels) {
        this.channels = channels;
        int format = AudioFormat.CHANNEL_OUT_MONO;
        if (channels == 2) format = AudioFormat.CHANNEL_OUT_STEREO;
        boolean wasPlaying = this.audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
        this.audioTrack.pause(); this.audioTrack.flush();
        this.audioTrack.stop(); this.audioTrack.release();
        this.audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, this.sampleRate,
                format, AudioFormat.ENCODING_PCM_16BIT, 2 * this.getBufferSize(),
                AudioTrack.MODE_STREAM);
        if (wasPlaying)
            this.audioTrack.play();
    }

    @Override
    public void onAudio(short[] pcm, int sampleRate, int channels) {
        if (sampleRate != this.sampleRate) {
            this.audioTrack.setPlaybackRate(sampleRate);
            this.sampleRate = sampleRate;
        }
        if (channels != this.channels)
            this.setChannels(channels);
        this.audioTrack.write(pcm, 0, pcm.length);
    }

    public void open() {
        this.audioTrack.play();
    }

    public void close() {
        this.audioTrack.stop();
    }
}
