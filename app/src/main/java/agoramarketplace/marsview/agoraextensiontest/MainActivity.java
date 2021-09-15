package agoramarketplace.marsview.agoraextensiontest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;



import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import agoramarketplace.marsview.extension.ExtensionManager;
import agoramarketplace.marsview.extension.MarsviewRequestHelper;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;


public class MainActivity extends AppCompatActivity implements io.agora.rtc2.IMediaExtensionObserver {

    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private String appId ;
    private String channelName ;
    private final static String TAG = "Agora_Marsview_Java";
    private static final int PERMISSION_REQ_ID = 22;
    private FrameLayout localVideoContainer;
    private FrameLayout remoteVideoContainer;
    private Button rtcOnOffButton;
    private TextView mTextView;
    private ProgressBar pgsBar;
    private RtcEngine mRtcEngine;
    private SurfaceView mRemoteView;
//    private String temporaryToken = "006bcc18c336ece4d46a679ec11f1612071IAB5+O8K7WEhqOaLqSlfr6GjlvT1u7oqJ3UoAKqNHClGbeEVD4QAAAAAEAA8nW45DDcqYQEAAQAMNyph";
    private String temporaryToken;
    private String audioId;

    private String API_KEY ;
    private String API_SECRET ;

    private boolean extensionEnabled = true;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        appId = getString(R.string.agora_app_id);
        channelName = getString(R.string.agora_channel);
        temporaryToken = getString(R.string.agora_access_token);
        API_KEY = getString(R.string.marsview_apiKey);
        API_SECRET = getString(R.string.marsview_apiSecret);

        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.transcript_view);
        pgsBar = (ProgressBar)findViewById(R.id.pBar);
        initUI();
        rtcOnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!extensionEnabled){
                    enableExtension();
                }
                else{
                    disableExtension();
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        destroyAgoraEngine();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermission() {
        Log.d(TAG, "checkPermission");
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)) {
            initAgoraEngine();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED){
            initAgoraEngine();
        }
    }

    private void initUI() {

        localVideoContainer = findViewById(R.id.view_container);
        remoteVideoContainer = findViewById(R.id.remote_video_view_container);
        rtcOnOffButton = findViewById(R.id.rtc_on_off);
        checkPermission();
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private void initAgoraEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = this;
            config.mAppId = appId;

            config.addExtension(ExtensionManager.EXTENSION_NAME);
            config.mExtensionObserver = this;
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String s, int i, int i1) {
                    Log.d(TAG, "onJoinChannelSuccess");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mRtcEngine.startPreview();
                        }
                    });
                }

                @Override
                public void onFirstRemoteVideoDecoded(final int i, int i1, int i2, int i3) {
                    super.onFirstRemoteVideoDecoded(i, i1, i2, i3);
                    Log.d(TAG, "onFirstRemoteVideoDecoded  uid = " + i);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setupRemoteVideo(i);
                        }
                    });
                }

                @Override
                public void onUserJoined(int i, int i1) {
                    super.onUserJoined(i, i1);
                    Log.d(TAG, "onUserJoined  uid = " + i);
                }

                @Override
                public void onUserOffline(final int i, int i1) {
                    super.onUserOffline(i, i1);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onRemoteUserLeft();
                        }
                    });
                }
            };
            mRtcEngine = RtcEngine.create(config);
            //extension is enabled by default
            Log.d(TAG, "Here");
            mRtcEngine.enableExtension(ExtensionManager.EXTENSION_VENDOR_NAME, ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, true);
            setupLocalVideo();

            VideoEncoderConfiguration configuration = new VideoEncoderConfiguration(640, 360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE);
            mRtcEngine.setVideoEncoderConfiguration(configuration);
            mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_STANDARD, Constants.AUDIO_SCENARIO_DEFAULT);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

            mRtcEngine.enableLocalVideo(true);
            mRtcEngine.enableLocalAudio(true);
            mRtcEngine.enableVideo();
            mRtcEngine.enableAudio();
            Log.d(TAG, "api call join channel");

            mRtcEngine.setExtensionProperty(ExtensionManager.EXTENSION_VENDOR_NAME, ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, "API_KEY", API_KEY); // login to app.marsivew.ai
            mRtcEngine.setExtensionProperty(ExtensionManager.EXTENSION_VENDOR_NAME, ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, "API_SECRET", API_SECRET); // login to app.marsview.ai

            mRtcEngine.joinChannel(temporaryToken, channelName, "", 0);
            mRtcEngine.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupLocalVideo() {
        SurfaceView view = RtcEngine.CreateRendererView(this);
        view.setZOrderMediaOverlay(true);
        localVideoContainer.addView(view);
        mRtcEngine.setupLocalVideo(new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        mRtcEngine.setLocalRenderMode(Constants.RENDER_MODE_HIDDEN);
    }

    private void setupRemoteVideo(int uid) {
        // Only one remote video view is available for this
        // tutorial. Here we check if there exists a surface
        // view tagged as this uid.
        int count = remoteVideoContainer.getChildCount();
        View view = null;
        for (int i = 0; i < count; i++) {
            View v = remoteVideoContainer.getChildAt(i);
            if (v.getTag() instanceof Integer && ((int) v.getTag()) == uid) {
                view = v;
            }
        }

        if (view != null) {
            return;
        }

        Log.d(TAG, " setupRemoteVideo uid = " + uid);
        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
        remoteVideoContainer.addView(mRemoteView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        mRtcEngine.setRemoteRenderMode(uid, Constants.RENDER_MODE_HIDDEN);
        mRemoteView.setTag(uid);
    }

    private void onRemoteUserLeft() {
        removeRemoteVideo();
    }

    private void removeRemoteVideo() {
        if (mRemoteView != null) {
            remoteVideoContainer.removeView(mRemoteView);
        }
        mRemoteView = null;
    }

    private void destroyAgoraEngine(){
        mRtcEngine.leaveChannel();
        mRtcEngine.destroy();
        mRtcEngine = null;
    }
    private void disableExtension(){
        mRtcEngine.enableExtension(ExtensionManager.EXTENSION_VENDOR_NAME, ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, false);
        rtcOnOffButton.setText(getString(R.string.enable_button_text));
        extensionEnabled = false;
    }
    private void enableExtension(){

        mRtcEngine.enableExtension(ExtensionManager.EXTENSION_VENDOR_NAME, ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, true);
        rtcOnOffButton.setText(getString(R.string.disable_button_text));
        extensionEnabled = true;
    }

    @Override
    public void onEvent(String vendor, String extension, String key, String value) {
        Log.d(TAG, "\nkey: " + key+ "\nValue: " + value);
        if( vendor.equalsIgnoreCase("Marsview") && extension.equalsIgnoreCase("TranscriptProvider")){

            if(key.equalsIgnoreCase("transactionId")){
                // Marsview provides a helper class to facilitate the developer in posting compute models
                // and get processing state of each model and metadata afterwards.
                final MarsviewRequestHelper requestHelper = new MarsviewRequestHelper(API_KEY, API_SECRET); //project api key, project api secret
                try {
                    JSONObject data = new JSONObject(value);
                    final String txnId = data.getString("txnId");
//                    final String txnId = "txn-1c6q6cepksu8h998-1630061414684";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pgsBar.setVisibility(View.VISIBLE);
                        }
                    });

                    final JSONArray enableModels = new JSONArray("[{'modelType':'speech_to_text', 'modelConfig': {'automatic_punctuation': true, 'custom_vocabulary': ['Marsview', 'Communications'], 'enableKeywords': true, 'enableSuggestedIntents': true, 'topics': {'threshold': 0.5}}}]");
//                     This is to enable models that you require before producing transcription.
//                     remember that transcription is only produced after the required models are
//                     enabled.
                    String computeDataResponse = requestHelper.postComputeDataRequest(txnId, enableModels);
                    Log.d(TAG, computeDataResponse);

                    final int delay = 1000 * 60 ;
                    Timer timer = new Timer();

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // To get the processing state of each model that is enabled u can use the method
                            // getProcessingState
                            String processingStateResponse = requestHelper.getProcessingState(txnId);
                            Log.d(TAG, processingStateResponse);
                            try{
                                JSONObject processingStateJson = new JSONObject(processingStateResponse);
                                if(processingStateJson.getBoolean("status")){
                                    JSONArray EnableModels = processingStateJson.getJSONObject("data").getJSONArray("enableModels");
                                    boolean processed = true;
                                    for(int j = 0; j < EnableModels.length(); j++){
                                        if(EnableModels.getJSONObject(j)
                                                .getJSONObject("state")
                                                .getString("status").equalsIgnoreCase("processed")){
                                            continue;
                                        }
                                        else if(EnableModels.getJSONObject(j)
                                                .getJSONObject("state")
                                                .getString("status").equalsIgnoreCase("error")){
                                            Log.v(TAG, "Error Generating Transcript");
                                            mTextView.setText("Error in computing one of the models, try changing the models!");
                                            this.cancel();
                                            break;
                                        }else{
                                            processed = false;
                                            break;
                                        }
                                    }
                                    if(processed)
                                    {
                                        // to get the metadata of the audio processed provide the transaction id string
                                        // to the get requestMetadata function.
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                pgsBar.setVisibility(View.GONE);
                                            }
                                        });
                                        mTextView.setText("");
                                        String MetadataResponse = requestHelper.getRequestMetadata(txnId);
                                        Log.d(TAG, MetadataResponse);

                                        JSONObject MetadataJson = new JSONObject(MetadataResponse);
                                        if(MetadataJson.getBoolean("status")){
                                            JSONObject data = MetadataJson.getJSONObject("data");
                                            JSONArray transcript = data.getJSONArray("transcript");
                                            for(int i = 0; i < transcript.length(); i++){
                                                String sentence = transcript.getJSONObject(i).getString("sentence");
                                                mTextView.append(sentence + "\n");
                                            }
                                        }
                                        this.cancel();
                                    }

                                }else{
                                    this.cancel();
                                }
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    },0, delay);

                }catch(Exception e){
                    Log.d(TAG, e.toString());
                }
            }
            else if(key.equalsIgnoreCase("connectionState")){
                try{
                    JSONObject  reader = new JSONObject(value);
                    String connectionState = reader.getString("connection_state");
                    Log.d(TAG, connectionState);
                    if(!connectionState.equals("true")){
                        // provide proper api key, secret key , user ID and re enable the transcription
                        // service again.
                    }else{
                        audioId = reader.getString("audio_id"); // store audio_id
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}