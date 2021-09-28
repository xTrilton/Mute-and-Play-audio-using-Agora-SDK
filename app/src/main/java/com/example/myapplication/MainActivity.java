package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;

import io.agora.rtc.Constants;
import io.agora.rtc.IAudioEffectManager;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.ChannelMediaOptions;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.rtc.video.VideoCanvas;


public class MainActivity extends AppCompatActivity{
    private RtcEngine mRtcEngine;
    private IAudioEffectManager audioEffectManager;

    // Permissions
    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // Handle SDK Events
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // set first remote user to the main bg video container
                    setupRemoteVideoStream(uid);
                }
            });
        }

        // remote user has left channel
        @Override
        public void onUserOffline(int uid, int reason) { // Tutorial Step 7
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserLeft();
                }
            });
        }

        // remote user has toggled their video
        @Override
        public void onRemoteVideoStateChanged(final int uid, final int state, int reason, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserVideoToggle(uid, state);
                }
            });
        }
    };
    /**
     * To ensure smooth communication, limit the size of the audio effect file.
     * We recommend using this method to preload the audio effect before calling the joinChannel method.
     */
    private void preloadAudioEffect(){
        // Gets the global audio effect manager.
        audioEffectManager = mRtcEngine.getAudioEffectManager();
        int id = 0;
        audioEffectManager.preloadEffect(id++, Environment.getExternalStorageDirectory().getPath()+"/Song/Caiiro.mp3");
        audioEffectManager.playEffect(
                0,  // The sound ID of the audio effect file to be played.
                Environment.getExternalStorageDirectory().getPath()+"/Song/Caiiro.mp3",  // The file path of the audio effect file.
                -1,   // The number of playback loops. -1 means an infinite loop.
                1,    // pitch	The pitch of the audio effect. The value ranges between 0.5 and 2. The default value is 1 (no change to the pitch). The lower the value, the lower the pitch.
                0.0,  // Sets the spatial position of the effect. 0 means the effect shows ahead.
                100,  // Sets the volume. The value ranges between 0 and 100. 100 is the original volume.
                true, // Sets whether to publish the audio effect.
                0 // Start position
        );
        // Pauses all audio effects.
        audioEffectManager.pauseAllEffects();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.bass).setVisibility(View.GONE); // set the join button hidden
        findViewById(R.id.beautify).setVisibility(View.GONE); // set the join button hidden

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)) {
            initAgoraEngine();
        }


    }

    private void initAgoraEngine() {
        try {
            /** if you encounter any problems check the code segement below*/

            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
            preloadAudioEffect();
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
        setupSession();
    }

    private void setupSession() {
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);

        mRtcEngine.enableVideo();

        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x480, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideoFeed() {

        // setup the container for the local user
        FrameLayout videoContainer = findViewById(R.id.floating_video_container);
        SurfaceView videoSurface = RtcEngine.CreateRendererView(getBaseContext());
        videoSurface.setZOrderMediaOverlay(true);
        videoContainer.addView(videoSurface);
        mRtcEngine.setupLocalVideo(new VideoCanvas(videoSurface, VideoCanvas.RENDER_MODE_FIT, 0));
    }

    private void setupRemoteVideoStream(int uid) {
        // setup ui element for the remote stream
        FrameLayout videoContainer = findViewById(R.id.bg_video_container);
        // ignore any new streams that join the session
        if (videoContainer.getChildCount() >= 1) {
            return;
        }

        SurfaceView videoSurface = RtcEngine.CreateRendererView(getBaseContext());
        videoContainer.addView(videoSurface);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(videoSurface, VideoCanvas.RENDER_MODE_FIT, uid));
        mRtcEngine.setRemoteSubscribeFallbackOption(io.agora.rtc.Constants.STREAM_FALLBACK_OPTION_AUDIO_ONLY);

    }



    // join the channel when user clicks UI button
    public void onjoinChannelClicked(View view) {
        mRtcEngine.joinChannel(null, "test-channel", "Extra Optional Data", 0); // if you do not specify the uid, Agora will assign one.
        setupLocalVideoFeed();
        findViewById(R.id.joinBtn).setVisibility(View.GONE); // set the join button hidden
        findViewById(R.id.bass).setVisibility(View.VISIBLE); // set the join button hidden
        findViewById(R.id.beautify).setVisibility(View.VISIBLE); // set the join button hidden


    }



    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    private void removeVideo(int containerID) {
        FrameLayout videoContainer = findViewById(containerID);
        videoContainer.removeAllViews();
    }

    private void onRemoteUserVideoToggle(int uid, int state) {
        FrameLayout videoContainer = findViewById(R.id.bg_video_container);

        SurfaceView videoSurface = (SurfaceView) videoContainer.getChildAt(0);
        videoSurface.setVisibility(state == 0 ? View.GONE : View.VISIBLE);

        // add an icon to let the other user know remote video has been disabled
        if(state == 0){
            ImageView noCamera = new ImageView(this);
            noCamera.setImageResource(R.drawable.video_disabled);
            videoContainer.addView(noCamera);
        } else {
            ImageView noCamera = (ImageView) videoContainer.getChildAt(1);
            if(noCamera != null) {
                videoContainer.removeView(noCamera);
            }
        }
    }

    private void onRemoteUserLeft() {
        removeVideo(R.id.bg_video_container);
    }



    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    REQUESTED_PERMISSIONS,
                    requestCode);
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(LOG_TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(LOG_TAG, "Need permissions " + Manifest.permission.RECORD_AUDIO + "/" + Manifest.permission.CAMERA);
                    break;
                }

                initAgoraEngine();
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;
    }

        public void playAudio (View w){
            mRtcEngine.startAudioMixing(Environment.getExternalStorageDirectory().getPath()+"/Song/Caiiro.mp3", false, false, -1, 0);
            String timeString = new SimpleDateFormat("mm:ss").format(mRtcEngine.getAudioMixingDuration(Environment.getExternalStorageDirectory().getPath()+"/Song/Caiiro.mp3"));
           //adjusting the volume
            mRtcEngine.adjustAudioMixingVolume(90);
            Toast.makeText(getApplicationContext(),
                    "just played the song",
                    Toast.LENGTH_LONG);



        }

        public void stopAudio (View v){
            mRtcEngine.stopAudioMixing();
            Toast.makeText(getApplicationContext(),
                    "stopped playing music ",
                    Toast.LENGTH_LONG);
        }
    public void muteMicrophone(View view) {
        ImageView btn = (ImageView) view;
        if (btn.isSelected()) {
            btn.setSelected(false);
            btn.setImageResource(R.drawable.audio_toggle_btn);
        } else {
            btn.setSelected(true);
            btn.setImageResource(R.drawable.audio_toggle_active_btn);
        }

        mRtcEngine.muteLocalAudioStream(btn.isSelected());


    }


}


