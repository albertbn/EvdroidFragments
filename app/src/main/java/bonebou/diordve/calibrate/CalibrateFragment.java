package bonebou.diordve.calibrate;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import bonebou.diordve.R;
import bonebou.diordve.preview.CamPreviewFrameCallback;

public class CalibrateFragment extends Fragment {

    //HSV for white is (0,0,255)
    // final int H_MIN=1, S_MIN=1, V_MIN=0;
    static final int H_MIN=1, S_MIN=1, V_MIN=100;
    static final int H_MAX=179, S_MAX=100, V_MAX=255;

    SeekBar bar_h_low, bar_h_high, bar_s_low, bar_s_high, bar_v_low, bar_v_high;
    TextView h_low_text, h_high_text, s_low_text, s_high_text, v_low_text, v_high_text;

    CalibrateFragment self = this;
    public CalibrateFragment() {
        // Required empty public constructor
    }

    public static CalibrateFragment newInstance() {
        CalibrateFragment fragment = new CalibrateFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_calibrate, container, false);

        //slider text displays
        self.h_low_text = (TextView) v.findViewById(R.id.h_low_text); self.h_high_text = (TextView) v.findViewById(R.id.h_high_text);
        self.s_low_text = (TextView) v.findViewById(R.id.s_low_text); self.s_high_text = (TextView) v.findViewById(R.id.s_high_text);
        self.v_low_text = (TextView) v.findViewById(R.id.v_low_text); self.v_high_text = (TextView) v.findViewById(R.id.v_high_text);

        self.bar_h_low = (SeekBar) v.findViewById( R.id.bar_h_low); self.bar_h_high = (SeekBar) v.findViewById( R.id.bar_h_high);
        self.bar_s_low = (SeekBar) v.findViewById( R.id.bar_s_low); self.bar_s_high = (SeekBar) v.findViewById( R.id.bar_s_high);
        self.bar_v_low = (SeekBar) v.findViewById( R.id.bar_v_low); self.bar_v_high = (SeekBar) v.findViewById( R.id.bar_v_high);

        self.bind_sliders();

        return v;
    }

//    will be used by the frame callback, to send hsv values to c++
    public int[] get_hsv_from_sliders ( ) {

        int[] hsv6 = new int[]{
                self.bar_h_low.getProgress(),
                self.bar_s_low.getProgress(),
                self.bar_v_low.getProgress(),
                self.bar_h_high.getProgress(),
                self.bar_s_high.getProgress(),
                self.bar_v_high.getProgress()
        };

        return hsv6;
    }

    //========================

    void bind_sliders ( ) {

        self.bar_h_low.setOnSeekBarChangeListener(self.sb_listen); self.bar_h_high.setOnSeekBarChangeListener(self.sb_listen);
        self.bar_s_low.setOnSeekBarChangeListener(self.sb_listen); self.bar_s_high.setOnSeekBarChangeListener(self.sb_listen);
        self.bar_v_low.setOnSeekBarChangeListener(self.sb_listen); self.bar_v_high.setOnSeekBarChangeListener(self.sb_listen);
        //set initial HSV stuff (HSV white is 0,0,255)
        self.bar_h_low.setProgress(H_MIN); self.bar_h_high.setProgress(H_MAX);
        self.bar_s_low.setProgress(S_MIN); self.bar_s_high.setProgress(S_MAX);
        self.bar_v_low.setProgress(V_MIN); self.bar_v_high.setProgress(V_MAX);
    }

    //seek bar generic listeners
    SeekBar.OnSeekBarChangeListener sb_listen = new SeekBar.OnSeekBarChangeListener() {

        int progress = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {

            progress = progressValue;
            String s_progress = Integer.toString(progress);

            switch ( seekBar.getId() ){
                case R.id.bar_h_low:
                    self.h_low_text.setText("h_low: "+s_progress); break;
                case R.id.bar_h_high:
                    self.h_high_text.setText("h_high: "+s_progress); break;
                case R.id.bar_s_low:
                    self.s_low_text.setText("s_low: "+s_progress); break;
                case R.id.bar_s_high:
                    self.s_high_text.setText("s_high: "+s_progress); break;
                case R.id.bar_v_low:
                    self.v_low_text.setText("v_low: "+s_progress); break;
                case R.id.bar_v_high:
                    self.v_high_text.setText("v_high: "+s_progress); break;
                default:
                    self.h_low_text.setText(Integer.toString(progress)+','+ Integer.toString(seekBar.getId())+','+ Integer.toString(seekBar.getId())); break;
            }

            // Toast.makeText(getApplicationContext(), "Changing seekbar progress: "+progress, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // textView.setText("Covered: " + progress + "/" + seekBar.getMax());
            // Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
        }
    };
}
