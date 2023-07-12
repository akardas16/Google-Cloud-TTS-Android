package darren.gcptts.main;
import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;



import java.util.Locale;

import darren.gcptts.CloudSpeechService;
import darren.gcptts.R;
import darren.gcptts.VoiceRecorder;
import darren.gcptts.VoiceView;

/**
 * Created by Pranav on 23/8/19.
 */
public class ListenVoiceActivity extends AppCompatActivity implements VoiceView.OnRecordListener {
    private static String TAG = "ListenVoiceActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private VoiceView mStartStopBtn;

    private CloudSpeechService mCloudSpeechService;
    private VoiceRecorder mVoiceRecorder;

    private boolean mIsRecording = false;

    // Resource caches

    private Handler mHandler;



    TextView saySomething;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speechlayout);

        saySomething = findViewById(R.id.sayTextID);


        initViews();


    }




    //Function to initialize the views
    private void initViews() {

        mStartStopBtn = findViewById(R.id.recordButton);
        mStartStopBtn.setOnRecordListener(this);
        mHandler = new Handler(Looper.getMainLooper());
    }

    private final CloudSpeechService.Listener mCloudSpeechServiceListener = new CloudSpeechService.Listener() {
        @Override
        public void onSpeechRecognized(final String text, final boolean isFinal) {
            if(isFinal) {
                if (mVoiceRecorder != null) mVoiceRecorder.dismiss();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Check if the user has stopped talking
                    if (isFinal) {
                        saySomething.setText(text);
                        stopVoiceRecorder();
                        mStartStopBtn.changePlayButtonState(VoiceView.STATE_NORMAL);
                    }
                    else {
                         saySomething.setText(text);
                         if (text.toLowerCase().contains("stop")){
                             saySomething.setText("");
                             stopVoiceRecorder();
                             mStartStopBtn.changePlayButtonState(VoiceView.STATE_NORMAL);
                         }
                         if (text.toLowerCase().contains("youtube") || text.toLowerCase().contains("open")){
                             watchYoutubeVideo("7sOVc6dPQAc");
                         }

                        }
                    }
            });
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder)
        {
            mCloudSpeechService = CloudSpeechService.from(binder);
            mCloudSpeechService.addListener(mCloudSpeechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mCloudSpeechService = null;
        }

    };

    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            if (mCloudSpeechService != null)
            {
                mCloudSpeechService.startRecognizing(mVoiceRecorder.getSampleRate(), Locale.getDefault().getLanguage());
            }
        }

        @Override
        public void onVoice(final byte[] buffer, int size) {
            if (mCloudSpeechService != null) {
                mCloudSpeechService.recognize(buffer, size);
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int amplitude = (buffer[0] & 0xff) << 8 | buffer[1];
                    double amplitudeDb3 = 20 * Math.log10((double) Math.abs(amplitude) / 32768);
                    float radius2 = (float) Math.log10(Math.max(1, amplitudeDb3)) * dp2px(ListenVoiceActivity.this, 20);
                    mStartStopBtn.animateRadius(radius2 * 10);
                }
            });
        }

        @Override
        public void onVoiceEnd() {
            if (mCloudSpeechService != null) {
                mCloudSpeechService.finishRecognizing();
            }
        }

    };

    @Override
    public void onRecordStart() {
        startStopRecording();
    }

    @Override
    public void onRecordFinish() {
        startStopRecording();
    }

    private void startStopRecording() {
        Log.d(TAG, "# startStopRecording # : " + mIsRecording);
        if (mIsRecording) {
            mStartStopBtn.changePlayButtonState(VoiceView.STATE_NORMAL);
            stopVoiceRecorder();
        } else {
            bindService(new Intent(this, CloudSpeechService.class), mServiceConnection,
                    BIND_AUTO_CREATE);
            mStartStopBtn.changePlayButtonState(VoiceView.STATE_RECORDING);
            startVoiceRecorder();
        }
    }

    //Function to start recording
    private void startVoiceRecorder()
    {
        Log.d(TAG, "# startVoiceRecorder #");
        mIsRecording = true;
        if (mVoiceRecorder != null)
        {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }



    //Function to stop recording
    private void stopVoiceRecorder() {
        Log.d(TAG, "# stopVoiceRecorder #");
        mIsRecording = false;
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }
    public void watchYoutubeVideo(String id) {
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + id));
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            startActivity(webIntent);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bindService(new Intent(this, CloudSpeechService.class), mServiceConnection,
                        BIND_AUTO_CREATE);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    public static int dp2px(Context context, int dp) {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context
                .getResources().getDisplayMetrics());
        return px;
    }

    //Function to display the set of 5 recent searches




    //Function to deactivate the voice listener
    void stopListening()
    {
        saySomething.setText("");
        stopVoiceRecorder();
        mStartStopBtn.changePlayButtonState(VoiceView.STATE_NORMAL);

    }

    @Override
    public void onStart() {
        super.onStart();
        // Prepare Cloud Speech API
        //Check for permission to record voice
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    public void onStop() {
        // Stop listening to voice
        // Stop Cloud Speech API
        super.onStop();
    }
    //Deactivates the voice listener when some activity comes in the foreground
    @Override
    public void onPause() {
        super.onPause();
        if (mIsRecording) {
           stopListening();
            if (mCloudSpeechService != null) {
                mCloudSpeechService.removeListener(mCloudSpeechServiceListener);
                unbindService(mServiceConnection);
                mCloudSpeechService = null;
            }
        }

    }
}
