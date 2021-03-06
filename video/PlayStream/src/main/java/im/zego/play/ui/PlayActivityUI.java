package im.zego.play.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;


import org.json.JSONObject;

import im.zego.common.util.SettingDataUtil;
import im.zego.common.widgets.SnapshotDialog;
import im.zego.play.databinding.ActivityPlayBinding;
import im.zego.play.databinding.PlayInputStreamIdLayoutBinding;

import java.util.ArrayList;
import java.util.Date;

import im.zego.common.entity.SDKConfigInfo;
import im.zego.common.entity.StreamQuality;
import im.zego.common.ui.BaseActivity;
import im.zego.common.util.AppLogger;
import im.zego.play.R;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.callback.IZegoPlayerTakeSnapshotCallback;
import im.zego.zegoexpress.callback.IZegoPublisherTakeSnapshotCallback;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoStreamResourceMode;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoEngineConfig;
import im.zego.zegoexpress.entity.ZegoPlayerConfig;
import im.zego.zegoexpress.entity.ZegoRoomExtraInfo;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;

public class PlayActivityUI extends BaseActivity {


    private ActivityPlayBinding binding;
    private PlayInputStreamIdLayoutBinding layoutBinding;
    private String mStreamID;
    private StreamQuality streamQuality = new StreamQuality();
    private SDKConfigInfo sdkConfigInfo = new SDKConfigInfo();
    private ZegoExpressEngine engine;
    private String userID;
    private String userName;
    private String roomID;
    private SnapshotDialog snapshotDialog;
    public static ZegoViewMode viewMode = ZegoViewMode.ASPECT_FILL;
    private static ZegoStreamResourceMode resourceMode =ZegoStreamResourceMode.DEFAULT;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_play);
        layoutBinding = binding.layout;
        layoutBinding.startButton.setText(getString(R.string.tx_start_play));
        // ??????DataBinding ????????????bean?????????UI?????????
        // ???????????????????????????????????? setText ??????????????????????????????
        binding.setQuality(streamQuality);
        binding.setConfig(sdkConfigInfo);
        ZegoEngineConfig config =new ZegoEngineConfig();
        config.advancedConfig.put("prefer_play_ultra_source","1");
        ZegoExpressEngine.setEngineConfig(config);
        // ?????????SDK
        engine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), getApplication(), null);
        AppLogger.getInstance().i("createEngine");
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "userid-" + randomSuffix;
        userName = "username-" + randomSuffix;


        engine.setEventHandler(new IZegoEventHandler() {

            @Override
            public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
                if (state == ZegoPlayerState.PLAYING) {
                    if (errorCode == 0) {
                        mStreamID = streamID;
                        AppLogger.getInstance().i("play stream success, streamID : %s", streamID);
                        Toast.makeText(PlayActivityUI.this, getString(R.string.tx_play_success), Toast.LENGTH_SHORT).show();

                        // ????????????????????????????????????
                        binding.title.setTitleName(getString(R.string.tx_playing));
                        binding.playSnapshot.setVisibility(View.VISIBLE);

//                        binding.decodeKeyLv.setVisibility(View.VISIBLE);
                    } else {
                        // ??????????????? ?????? mStreamID ???????????? null ???
                        mStreamID = null;
                        // ????????????????????????????????????
                        binding.title.setTitleName(getString(R.string.tx_play_fail));
                        AppLogger.getInstance().i("play stream fail, streamID : %s, errorCode : %d", streamID, errorCode);
                        Toast.makeText(PlayActivityUI.this, getString(R.string.tx_play_fail), Toast.LENGTH_SHORT).show();
                        // ????????????????????????????????????
                        showInputStreamIDLayout();
                    }
                }
            }

            @Override
            public void onPlayerQualityUpdate(String streamID, im.zego.zegoexpress.entity.ZegoPlayStreamQuality quality) {
                /**
                 * ??????????????????, ??????????????????3?????????
                 * ????????? {@link com.zego.zegoliveroom.ZegoLiveRoom#setPlayQualityMonitorCycle(long)} ??????????????????
                 */
                /**
??????????????????????????????????* Pull stream quality update, the callback frequency defaults once every 3 seconds
??????????????????????????????????* The callback frequency can be modified through {@link com.zego.zegoliveroom.ZegoLiveRoom # setPlayQualityMonitorCycle (long)}
??????????????????????????????????*/
                streamQuality.setFps(String.format(getString(R.string.frame_rate)+" %f", quality.videoRecvFPS));
                streamQuality.setBitrate(String.format(getString(R.string.bit_rate)+" %f kbs", quality.videoKBPS));
                streamQuality.setAvTimestampDiff(String.format(getString(R.string.avTimestampDiff)+" %s ms", quality.avTimestampDiff));
                AppLogger.getInstance().i("onPlayerQualityUpdate peerToPeerDelay:%s  delay???%s  rtt:%s  level:%s  videoRenderFPS:%s  videoKBPS:%s",quality.peerToPeerDelay,quality.delay,quality.rtt,quality.level,quality.videoRenderFPS,quality.videoKBPS);
            }

            @Override
            public void onPlayerVideoSizeChanged(String streamID, int width, int height) {
                // ????????????????????????,startPlay????????????????????????????????????????????????(??????????????????)?????????????????????.
                // Video width and height change notification, after startPlay, if the video width or height changes (the first value will be), you will receive this notification
                streamQuality.setResolution(String.format(getString(R.string.resolution)+" %dX%d", width, height));
            }

            @Override
            public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                /** ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????SDK???????????????????????? */
                /** Room status update callback: after logging into the room, when the room connection status changes
                 * (such as room disconnection, login authentication failure, etc.), the SDK will notify through the callback
                 */
                AppLogger.getInstance().i("onRoomStateUpdate: roomID = " + roomID + ", state = " + state + ", errorCode = " + errorCode);
                streamQuality.setRoomID(String.format("RoomID : %s", roomID));
                if (state == ZegoRoomState.DISCONNECTED) {
                    binding.title.setTitleName(getString(R.string.loss_connect));
                } else if (state == ZegoRoomState.CONNECTED) {
                    binding.title.setTitleName("");
                }
                if (errorCode != 0) {
                    Toast.makeText(PlayActivityUI.this, String.format("login room fail, errorCode: %d", errorCode), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList,JSONObject extendedData) {

            }

            @Override
            public void onRoomExtraInfoUpdate(String roomID, ArrayList<ZegoRoomExtraInfo> roomExtraInfoList) {
                for(int i=0;i<roomExtraInfoList.size();i++) {
                    AppLogger.getInstance().i( "onRoomExtraInfoUpdate roomID:" + roomID + "  key:" + roomExtraInfoList.get(i).key+"  value:"+roomExtraInfoList.get(i).value+"  updateTime:"+roomExtraInfoList.get(i).updateTime+"  userID:"+roomExtraInfoList.get(i).updateUser.userID+"  "+roomExtraInfoList.get(i).updateUser.userName);
                    binding.txRoomExtraInfo.setText("roomExtraInfo:"+roomExtraInfoList.get(i).value);
                }

            }
        });

        binding.playSnapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(snapshotDialog==null){
                    snapshotDialog =new SnapshotDialog(PlayActivityUI.this,R.style.SnapshotDialog);
                }
                engine.takePlayStreamSnapshot(mStreamID, new IZegoPlayerTakeSnapshotCallback() {
                    @Override
                    public void onPlayerTakeSnapshotResult(int i, Bitmap bitmap) {
                        snapshotDialog.show();
                        snapshotDialog.setSnapshotBitmap(bitmap);
                    }
                });
            }
        });
//        binding.playSettings.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String decodekey =binding.edPlayDecodeKey.getText().toString();
//                if(decodekey!=null&&!decodekey.trim().equals("")){
//                    engine.setPlayStreamDecryptionKey(mStreamID,decodekey);
//                }else{
//                    Toast.makeText(PlayActivityUI.this,"key should not be null",Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
        initPreferenceData();
        binding.resourceMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                if (checkedId == R.id.default_mode) {
                    resourceMode = ZegoStreamResourceMode.DEFAULT;
                }else if(checkedId==R.id.cdn_only){
                    resourceMode=ZegoStreamResourceMode.ONLY_CDN;
                }else if(checkedId == R.id.l3_only){
                    resourceMode = ZegoStreamResourceMode.ONLY_L3;
                }else if(checkedId == R.id.rtc_only){
                    resourceMode =ZegoStreamResourceMode.ONLY_RTC;
                }
            }
        });
    }

    private void initPreferenceData() {
        SharedPreferences sp = getSharedPreferences(PlaySettingActivityUI.SHARE_PREFERENCE_NAME, MODE_PRIVATE);
        String mode =sp.getString("play_view_mode","1");
        switch (mode){
            case "0":
                viewMode = ZegoViewMode.ASPECT_FIT;
                break;
            case "1":
                viewMode =ZegoViewMode.ASPECT_FILL;
                break;
            case "2" :
                viewMode =ZegoViewMode.SCALE_TO_FILL;
                break;
            default:
                break;
        }
    }
    @Override
    protected void onDestroy() {
        if (mStreamID != null) {
            engine.stopPlayingStream(mStreamID);
        }

        // ??????????????????????????????????????????
        engine.logoutRoom(roomID);
        engine.setEventHandler(null);
        resourceMode=ZegoStreamResourceMode.DEFAULT;
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mStreamID != null) {
            ZegoCanvas zegoCanvas = new ZegoCanvas(binding.playView);
            zegoCanvas.viewMode = viewMode;
            ZegoPlayerConfig playerConfig =new ZegoPlayerConfig();
            playerConfig.resourceMode=resourceMode;
            ZegoExpressEngine.getEngine().startPlayingStream(mStreamID, zegoCanvas,playerConfig);
        }
    }

    /**
     * button ??????????????????
     * ????????????
     *
     * @param view
     */
    public void onStart(View view) {
        mStreamID = layoutBinding.edStreamId.getText().toString();
        roomID = layoutBinding.edRoomId.getText().toString();

        ZegoUser user = new ZegoUser(userID, userName);
        engine.loginRoom(roomID, user, null);
        // ????????????StreamID??????
        hideInputStreamIDLayout();
        // ?????????????????????
        streamQuality.setStreamID(String.format("StreamID : %s", mStreamID));
        // ????????????
        ZegoCanvas zegoCanvas = new ZegoCanvas(binding.playView);
        zegoCanvas.viewMode = viewMode;
        ZegoPlayerConfig playerConfig =new ZegoPlayerConfig();
        playerConfig.resourceMode=resourceMode;
        engine.startPlayingStream(mStreamID, zegoCanvas,playerConfig);

        binding.resourceMode.setVisibility(View.GONE);
    }

    /**
     * ?????????????????????
     *
     * @param view
     */
    public void goSetting(View view) {
        PlaySettingActivityUI.actionStart(this, mStreamID);
    }


    private void hideInputStreamIDLayout() {
        // ??????InputStreamIDLayout??????
        layoutBinding.getRoot().setVisibility(View.GONE);
        binding.publishStateView.setVisibility(View.VISIBLE);
    }

    private void showInputStreamIDLayout() {
        // ??????InputStreamIDLayout??????
        layoutBinding.getRoot().setVisibility(View.VISIBLE);
        binding.publishStateView.setVisibility(View.GONE);
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, PlayActivityUI.class);
        activity.startActivity(intent);
    }
}
