/*
 * Framework code written for the Multimedia course taught in the first year
 * of the UvA Informatica bachelor.
 *
 * Nardi Lam, 2015 (based on code by I.M.J. Kamps, S.J.R. van Schaik, R. de Vries, 2013)
 */

package nl.uva.multimedia;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import nl.uva.multimedia.audio.AudioListener;
import nl.uva.multimedia.audio.AudioPlayer;
import nl.uva.multimedia.audio.AudioVisualizationView;
import nl.uva.multimedia.audio.WaveFileAudioSource;
import nl.uva.multimedia.audio.MicrophoneAudioSource;

/*
 * An activity containing the basics for an audio processing application.
 */
public class AudioActivity extends Activity implements AudioListener {

    /*** Source constants (position in list) ***/
    public static final int SOURCE_FILE = 0;
    public static final int SOURCE_MICROPHONE = 1;

    private WaveFileAudioSource wfas;
    private MicrophoneAudioSource mas;
    private AudioPlayer audioPlayer;

    int firstTime = 1;
    float decay = 1;
    long delay = 1000;

    Handler handler = new Handler();
    CircularBuf buf;

    private AudioVisualizationView avv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Dit is het "beginpunt" van de applicatie!
        // Als je vanaf hier de code stap voor stap doorloopt zul je alles tegen moeten komen.
        // De layout is gedefiniëerd in res/layout/activity_audio.xml, dit wordt ingesteld via
        // this.setContentView() hieronder.

        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_audio);

        /* Create sources: */
        this.wfas = new WaveFileAudioSource(this);
        this.mas = new MicrophoneAudioSource(this);

        /* Initialiseer de slider */
        SeekBar seekBar = (SeekBar)this.findViewById(R.id.seekBar);
        seekBar.setProgress(0);

        SeekBar seekBar1 = (SeekBar)this.findViewById(R.id.seekBar2);
        seekBar.setProgress(0);

        final TextView decay_view = (TextView)this.findViewById(R.id.textView3);
        final TextView delay_view = (TextView)this.findViewById(R.id.textView);

        /* Seekbar listeners voor decay & delay instellingen */
        seekBar.setOnSeekBarChangeListener (
                new SeekBar.OnSeekBarChangeListener() {

                    /* Check of slider is veranderd */
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        delay = i;
                        delay_view.setText("Delay: " + Long.toString(delay / 1000) + "s");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

        seekBar1.setOnSeekBarChangeListener (
                new SeekBar.OnSeekBarChangeListener() {

                    /* Check of slider is veranderd */
                    @Override
                    public void onProgressChanged(SeekBar seekBar2, int i, boolean b) {
                        decay = i;
                        decay_view.setText("Decay: " + Double.toString(decay) + "%");
                        decay = (float)(decay / 100);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

        this.audioPlayer = new AudioPlayer();

        Spinner sourceSpinner = (Spinner)this.findViewById(R.id.source_spinner);

        /* Switching between sources: */
        sourceSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case AudioActivity.SOURCE_FILE:
                                AudioActivity.this.switchToFile();
                                break;
                            case AudioActivity.SOURCE_MICROPHONE:
                                AudioActivity.this.switchToMicrophone();
                                break;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

        /* "Load file" button: */
        findViewById(R.id.load_audio_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Start an external Activity for choosing an audio file, result is returned in
                 * onActivityResult(). */
                firstTime = 1;
                Intent it = new Intent();
                it.setType("audio/x-wav");
                it.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(it, "Load audio file from..."),
                        SOURCE_FILE);
            }
        });

        /* Play/pause button: */
        findViewById(R.id.play_pause_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioActivity.this.wfas.toggle();
                Button b = (Button)v;
                if (AudioActivity.this.wfas.isPlaying()) {
                    b.setText(R.string.pause_icon);
                    AudioActivity.this.audioPlayer.open();
                }
                else {
                    b.setText(R.string.play_icon);
                    AudioActivity.this.audioPlayer.close();
                }
            }
        });

        /* Rewind button: */
        findViewById(R.id.rewind_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firstTime = 1;
                try {
                    AudioActivity.this.wfas.rewind();
                    AudioActivity.this.setPaused();
                    AudioActivity.this.audioPlayer.close();
                } catch (IOException e) {
                    Toast.makeText(AudioActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        /* Record switch: */
        ((CompoundButton)findViewById(R.id.record_toggle)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            AudioActivity.this.mas.start();
                            AudioActivity.this.audioPlayer.open();
                        } else {
                            AudioActivity.this.mas.stop();
                            AudioActivity.this.audioPlayer.close();
                        }
                    }
                });

        /* Select an audio file as default source: */
        sourceSpinner.setSelection(AudioActivity.SOURCE_FILE);
    }

    private void switchToFile() {
        this.audioPlayer.close();
        this.mas.stop();
        this.wfas = new WaveFileAudioSource(this);
        this.wfas.setOnAudioListener(this);

        /* Switch out controls: */
        findViewById(R.id.file_controls).setVisibility(View.VISIBLE);
        findViewById(R.id.mic_controls).setVisibility(View.GONE);
    }

    private void setPaused() {
        ((Button)findViewById(R.id.play_pause_button)).setText(R.string.play_icon);
    }

    private void switchToMicrophone() {
        this.audioPlayer.close();
        this.wfas.pause();
        this.mas.setOnAudioListener(this);

        /* Switch out controls: */
        findViewById(R.id.file_controls).setVisibility(View.GONE);
        findViewById(R.id.mic_controls).setVisibility(View.VISIBLE);
    }

    @Override
    public void onAudio(final short[] pcm, int sampleRate, int channels) {
        // TODO: Hier wordt de bronaudio doorgestuurd ter output of verdere verwerking.
        // Een goed punt om in te haken als je nog iets anders met de audio wil doen!

        int size                = pcm.length;
        final long delay_ms     = delay;
        final int sampleRate_f  = sampleRate;
        final int channels_f    = channels;

        /* Indien eerste keer aanroep deze methode */
        if (firstTime == 1) {
            buf = new CircularBuf(size,1000);

            firstTime = 0;
        }

        /* Stop de pcm in de circular-buffer */
        buf.append(pcm);

        /* Speel de source af */
        this.audioPlayer.onAudio(pcm, sampleRate, channels);


        /* Runnable die de source zachter afspeelt */
        Runnable r = new Runnable() {
            public void run() {
                short[] pcm_new;

                /* Speelt alle pcm's in juiste volgorde af */
                for (int i = 0; i < buf.amount_pcm; i++) {
                    pcm_new = buf.getIndex(i);

                    boolean playAgain = false;

                    for (int j = 0; j < buf.amount_elements; j++) {
                        pcm_new[j] = (short) (pcm_new[j] * decay);
                        if (pcm_new[j] > 1)
                            playAgain = true;
                    }

                    if (playAgain)
                        audioPlayer.onAudio(pcm_new, sampleRate_f, channels_f);
                }

            }
        };


        /* Als deze methode niet nog eens aan wordt geroepen, voer dan de Runnable uit na
         * delay_ms aantal miliseconde
         */

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(r, delay_ms);
    }


    /* When an audio file has been loaded, the result gets delivered back_ here: */
    protected void onActivityResult(int requestCode, int resultCode, Intent it) {
        super.onActivityResult(requestCode, resultCode, it);

        /* If we have an audio file... */
        if (requestCode == SOURCE_FILE && resultCode == RESULT_OK && it != null) {
            try {
                /* ...pass it to the WaveFileAudioSource: */
                this.wfas.loadFromUri(it.getData());
                this.setPaused();
                this.audioPlayer.close();
            } catch (IOException e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    /* Deze klasse vormt een cirkel buffer
     * Hierbij geldt dat als als er een element uit gaat, dit telkens het oudste element is, en als deze
     * buffer vol zit dan gaat ook het oudste element eruit.
     */
    public class CircularBuf {

        public int amount_elements;
        public int amount_pcm;
        public short[][] buffer;
        public int end_index;

        /* Initialisatie */
        public CircularBuf(int size, int n_pcms) {
            this.amount_elements =  size;
            this.amount_pcm      = n_pcms;
            this.end_index       = 0;

            this.buffer = new short[size][n_pcms];


        }

        /* Voeg een pcm toe aan de buffer */
        public void append(short[] pcm) {
            for (int i = 0; i < this.amount_elements; i++) {
                this.buffer[i][this.end_index] = pcm[i];
            }

            this.end_index = this.end_index + 1;
        }

        /* Returned de pcm op een gegeven index in de buffer */
        public short[] getIndex(int index) {
            short[] pcm = new short[this.amount_elements];

            for (int i = 0; i < this.amount_elements; i++) {
                pcm[i] = this.buffer[i][index];
            }

            return pcm;
        }

        /* Verwijdert oudste element en returned deze pcm */
        public short[] delete() {
            short[] pcm = new short[amount_elements];

            /* Lege lijst */
            if (this.end_index == 0) {
                return pcm;
            }

            /* Neem de pcm over die eruit wordt gehaald */
            for (int i = 0; i < this.amount_elements; i++) {
                pcm[i] = this.buffer[i][0];
                this.buffer[i][0] = 0;
            }

            /* Schuif alle overige pcms een element op naar links */
            for (int i = 0; i < this.end_index; i++) {
                for (int j = 0; j < this.amount_elements; j++) {
                    this.buffer[j][i] = this.buffer[j][i + 1];
                }
            }

            this.end_index = this.end_index - 1;

            return pcm;
        }

    }
}

