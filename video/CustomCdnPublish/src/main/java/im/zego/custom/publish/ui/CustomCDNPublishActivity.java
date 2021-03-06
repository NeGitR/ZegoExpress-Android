package im.zego.custom.publish.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import im.zego.common.GetAppIDConfig;
import im.zego.common.util.AppLogger;
import im.zego.common.util.SettingDataUtil;
import im.zego.common.widgets.log.FloatingView;
import im.zego.custom.cdn.publish.R;
import im.zego.custom.cdn.publish.databinding.CustomCdnPublishLayoutBinding;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoLanguage;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.entity.ZegoCDNConfig;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoPlayerConfig;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;

import java.util.ArrayList;
import java.util.Date;


public class CustomCDNPublishActivity extends AppCompatActivity {
    CustomCdnPublishLayoutBinding binding;
    ImageButton ib_local_mic;
    ImageButton ib_remote_stream_audio;
    boolean publishMicEnable = true;
    boolean playStreamMute = true;

    public static ZegoExpressEngine engine = null;
    private String userID;
    private String userName;
    String roomID;
    String publishStreamID;
    String playStreamID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.custom_cdn_publish_layout);
        /** ???????????? */
        /** Request permission */
        checkOrRequestPermission();

        /** ???????????????????????? */
        /** Add floating log view */
        FloatingView.get().add();
        /** ??????SDK????????? */
        /** Record SDK version */
        AppLogger.getInstance().i("SDK version : %s", ZegoExpressEngine.getVersion());
        binding.btnInit.setText(getString(R.string.tx_init_sdk));
        binding.btnLogin.setText(getString(R.string.tx_basic_login_room));
        binding.btnStartPublish.setText(getString(R.string.tx_basic_publish));
        binding.btnStartPlay.setText(getString(R.string.tx_basic_play));
        TextView tvAppID = findViewById(R.id.tv_appid);
        tvAppID.setText("AppID: " + SettingDataUtil.getAppId());

        /** ???????????????????????????????????????ID */
        /** RoomID used by example */
        roomID = "room123";
        TextView tvRoomID = findViewById(R.id.tv_roomid);
        tvRoomID.setText(getString(R.string.room_id)+ roomID);

        /** ?????????????????????ID????????????????????????????????????ID????????????????????? */
        /** Generate random user ID to avoid user ID conflict and mutual influence when different mobile phones are used */
        String randomSuffix = String.valueOf(new Date().getTime()%(new Date().getTime()/1000));
        userID = "user" + randomSuffix;
        userName = "user" + randomSuffix;
        TextView tvUserID = findViewById(R.id.tv_userid);
        tvUserID.setText(getString(R.string.user_id) + userID);

        /** ??????????????????ID, ????????????ID?????????ID???????????????????????????????????????????????????????????????ID */
        /** Generate a random streamID. the default publishing streamID is the same as playing streamID. you can modify in the UI */
        publishStreamID = "s" + randomSuffix;
        playStreamID = "s" + randomSuffix;
        EditText et = findViewById(R.id.ed_publish_stream_id);
        et.setText(publishStreamID);
        et = findViewById(R.id.ed_play_stream_id);
        et.setText(playStreamID);

        /** ???????????????????????? */
        /** Local MIC switch */
        ib_local_mic = findViewById(R.id.ib_local_mic);
        /** ????????????????????????????????? */
        /** Switch for mute audio output */
        ib_remote_stream_audio = findViewById(R.id.ib_remote_mic);
    }

    @Override
    protected void onDestroy() {
        // Release SDK resources
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }

    /** ????????????????????? */
    /** Click Init Button */
    public void ClickInit(View view) {
        Button button = (Button)view;
        if (button.getText().equals(getString(R.string.tx_init_sdk))) {
            /** ?????????SDK, ??????????????????????????????????????? */
            /** Initialize SDK, use test environment, access to general scenarios */
            engine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), getApplication(), null);
            AppLogger.getInstance().i(getString(R.string.tx_init_sdk_ok));
            Toast.makeText(this, getString(R.string.tx_init_sdk_ok), Toast.LENGTH_SHORT).show();
            button.setText(getString(R.string.tx_uninit_sdk));
            engine.setDebugVerbose(true, ZegoLanguage.CHINESE);
            engine.setEventHandler(new IZegoEventHandler() {
                @Override
                public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                    /** ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????SDK???????????????????????? */
                    /** Room status update callback: after logging into the room, when the room connection status changes
                     * (such as room disconnection, login authentication failure, etc.), the SDK will notify through the callback
                     */
                    AppLogger.getInstance().i("onRoomStateUpdate: roomID = " + roomID + ", state = " + state + ", errorCode = " + errorCode);
                }


                @Override
                public void onRoomUserUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoUser> userList) {
                    /** ???????????????????????????????????????????????????????????????????????????SDK???????????????????????? */
                    /** User status is updated. After logging into the room, when a user is added or deleted in the room,
                     * the SDK will notify through this callback
                     */
                    AppLogger.getInstance().i("onRoomUserUpdate: roomID = " + roomID + ", updateType = " + updateType);
                    for (int i = 0; i < userList.size(); i++) {
                        AppLogger.getInstance().i("userID = " + userList.get(i).userID + ", userName = " + userList.get(i).userName);
                    }
                }

                @Override
                public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList,JSONObject extendedData) {
                    /** ????????????????????????????????????????????????????????????????????????????????????SDK???????????????????????? */
                    /** The stream status is updated. After logging into the room, when there is a new publish or delete of audio and video stream,
                     * the SDK will notify through this callback */
                    AppLogger.getInstance().i("onRoomStreamUpdate: roomID = " + roomID + ", updateType = " + updateType);
                    for (int i = 0; i < streamList.size(); i++) {
                        AppLogger.getInstance().i("streamID = " + streamList.get(i).streamID);
                    }
                }

                @Override
                public void onDebugError(int errorCode, String funcName, String info) {
                    /** ???????????????????????? */
                    /** Printing debugging error information */
                    AppLogger.getInstance().i("onDebugError: errorCode = " + errorCode + ", funcName = " + funcName + ", info = " + info);
                }

                @Override
                public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                    /** ????????????????????????????????????????????????????????????????????????????????????????????????????????????SDK???????????????????????? */
                    /** After calling the stream publishing interface successfully, when the status of the stream changes,
                     * such as the exception of streaming caused by network interruption, the SDK will notify through this callback
                     */
                    AppLogger.getInstance().i("onPublisherStateUpdate: streamID = " + streamID + ", state = " + state + ", errCode = " + errorCode);
                }

                @Override
                public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
                    /** ????????????????????????????????????????????????????????????????????????????????????????????????????????????SDK???????????????????????? */
                    /** After calling the streaming interface successfully, when the status of the stream changes,
                     * such as network interruption leading to abnormal situation, the SDK will notify through
                     * this callback */
                    AppLogger.getInstance().i("onPlayerStateUpdate: streamID = " + streamID + ", state = " + state + ", errCode = " + errorCode);
                    }


            });
        }
        else {
            /** ???????????? */
            /** Destroy Engine */
            ZegoExpressEngine.destroyEngine(null);
            engine = null;
            AppLogger.getInstance().i(getString(R.string.tx_uninit_sdk_ok));
            Toast.makeText(this, getString(R.string.tx_uninit_sdk_ok), Toast.LENGTH_SHORT).show();
            button.setText(getString(R.string.tx_init_sdk));
        }

    }

    /** ?????????????????? */
    /** Click Login Button */
    public void ClickLogin(View view) {
        if (engine == null) {
            Toast.makeText(this, getString(R.string.tx_sdk_not_init), Toast.LENGTH_SHORT).show();
            return;
        }
        Button button = (Button)view;
        if (button.getText().equals(getString(R.string.tx_basic_login_room))) {
            /** ?????????????????? */
            /** Create user */
            ZegoUser user = new ZegoUser(userID, userName);

            /** ?????????????????? */
            /** Begin to login room */
            engine.loginRoom(roomID, user, null);
            AppLogger.getInstance().i("Login room OK, userID = " + userID + " , userName = " + userName);
            Toast.makeText(this, getString(R.string.tx_basic_login_room_ok), Toast.LENGTH_SHORT).show();
            button.setText(getString(R.string.tx_basic_logout_room));
        }
        else {
            /** ?????????????????? */
            /** Begin to logout room */
            engine.logoutRoom(roomID);
            AppLogger.getInstance().i("Logout room OK, userID = " + userID + " , userName = " + userName);
            Toast.makeText(this, getString(R.string.tx_basic_logout_room_ok), Toast.LENGTH_SHORT).show();
            button.setText(getString(R.string.tx_basic_login_room));
        }
    }

    /** ?????????????????? */
    /** Click Publish Button */
    public void ClickPublish(View view) {
        if (engine == null) {
            Toast.makeText(this, getString(R.string.tx_sdk_not_init), Toast.LENGTH_SHORT).show();
            return;
        }

        Button button = (Button)view;
        if (button.getText().equals(getString(R.string.tx_basic_publish))) {
            EditText et = findViewById(R.id.ed_publish_stream_id);
            String streamID = et.getText().toString();
            publishStreamID = streamID;

            ZegoCDNConfig zegoCDNConfig = new ZegoCDNConfig();
            zegoCDNConfig.url = binding.publishCdnUrl.getText().toString();
            zegoCDNConfig.authParam = "";
            /** ????????????CDN??????, ??????????????????????????? **/
            engine.enablePublishDirectToCDN(true, zegoCDNConfig);

            /** ???????????? */
            /** Begin to publish stream */
            engine.startPublishingStream(streamID);
            AppLogger.getInstance().i("Publish stream OK, streamID = " + streamID);
            View local_view = findViewById(R.id.local_view);
            Toast.makeText(this, getString(R.string.tx_basic_publish_ok), Toast.LENGTH_SHORT).show();

            /** ??????????????????????????????????????? */
            /** Start preview and set the local preview view. */
            engine.startPreview(new ZegoCanvas(local_view));

            AppLogger.getInstance().i("Start preview OK");
            Toast.makeText(this, getString(R.string.tx_basic_preview_ok), Toast.LENGTH_SHORT).show();
            button.setText(getString(R.string.tx_basic_stop_publish));
        }
        else {
            /** ???????????? */
            /** Begin to stop publish stream */
            engine.stopPublishingStream();

            /** ?????????????????? */
            /** Start stop preview */
            engine.stopPreview();
            /** CDN?????????????????? **/
            ZegoCDNConfig zegoCDNConfig = new ZegoCDNConfig();
            zegoCDNConfig.url = binding.publishCdnUrl.getText().toString();
            zegoCDNConfig.authParam = "";
            engine.enablePublishDirectToCDN(false, zegoCDNConfig);
            AppLogger.getInstance().i("Stop publish stream OK");
            Toast.makeText(this, getString(R.string.tx_basic_stop_publish_ok), Toast.LENGTH_SHORT).show();
            button.setText(getString(R.string.tx_basic_publish));
        }
    }

    /** ?????????????????? */
    /** Click Play Button */
    public void ClickPlay(View view) {
        if (engine == null) {
            Toast.makeText(this, getString(R.string.tx_sdk_not_init), Toast.LENGTH_SHORT).show();
            return;
        }

        Button button = (Button)view;
        if (button.getText().equals(getString(R.string.tx_basic_play))) {
            EditText et = findViewById(R.id.ed_play_stream_id);
            String streamID = et.getText().toString();
            playStreamID = streamID;
            View play_view = findViewById(R.id.remote_view);


            /** CDN?????????????????? **/
            ZegoCDNConfig zegoCDNConfig = new ZegoCDNConfig();
            zegoCDNConfig.url = binding.playCdnUrl.getText().toString();
            zegoCDNConfig.authParam = "";
            ZegoPlayerConfig zegoPlayerConfig = new ZegoPlayerConfig();
            zegoPlayerConfig.cdnConfig = zegoCDNConfig;
            /** ???????????? */
            /** Begin to play stream */
            engine.startPlayingStream(playStreamID, new ZegoCanvas(play_view), zegoPlayerConfig);

            engine.muteSpeaker(playStreamMute);
            AppLogger.getInstance().i("Start play stream OK, streamID = " + playStreamID);
            Toast.makeText(this, getString(R.string.tx_basic_play_ok), Toast.LENGTH_SHORT).show();
            button.setText(getString(R.string.tx_basic_stop_play));
        }
        else {
            /** ???????????? */
            /** Begin to stop play stream */
            EditText et = findViewById(R.id.ed_play_stream_id);
            String streamID = et.getText().toString();
            engine.stopPlayingStream(streamID);
            //engine.stopPlayingStream(playStreamID);
            AppLogger.getInstance().i("Stop play stream OK, streamID = " + streamID);
            Toast.makeText(this, getString(R.string.tx_basic_stop_play_ok), Toast.LENGTH_SHORT).show();
            button.setText(getString(R.string.tx_basic_play));
        }
    }

    public void enableLocalMic(View view) {
        if (engine == null) {
            Toast.makeText(this, getString(R.string.tx_sdk_not_init), Toast.LENGTH_SHORT).show();
            return;
        }

        publishMicEnable = !publishMicEnable;

        if (publishMicEnable) {
            ib_local_mic.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_bottom_microphone_on));
        } else {
            ib_local_mic.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_bottom_microphone_off));
        }

        /* Enable Mic*/
        engine.muteMicrophone(!publishMicEnable);
    }

    public void enableRemoteMic(View view) {
        if (engine == null) {
            Toast.makeText(this, getString(R.string.tx_sdk_not_init), Toast.LENGTH_SHORT).show();
            return;
        }

        playStreamMute = !playStreamMute;

        if (playStreamMute) {
            ib_remote_stream_audio.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_bottom_microphone_off));
        } else {
            ib_remote_stream_audio.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_bottom_microphone_on));
        }

        /* Enable Mic*/
        engine.muteSpeaker(playStreamMute);
    }

    /** ????????????????????? */
    /** Check and request permission */
    public boolean checkOrRequestPermission() {
        String[] PERMISSIONS_STORAGE = {
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO"};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_STORAGE, 101);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        FloatingView.get().attach(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FloatingView.get().detach(this);
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, CustomCDNPublishActivity.class);
        activity.startActivity(intent);
    }
}
