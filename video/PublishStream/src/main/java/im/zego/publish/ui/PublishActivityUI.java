package im.zego.publish.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import org.json.JSONObject;

import im.zego.common.entity.PerformanceStatus;
import im.zego.common.util.SettingDataUtil;
import im.zego.common.widgets.CustomMinSeekBar;
import im.zego.common.widgets.SnapshotDialog;
import im.zego.publish.R;
import im.zego.publish.databinding.ActivityPublishBinding;
import im.zego.publish.databinding.PublishInputStreamIdLayoutBinding;

import java.util.Date;

import im.zego.common.entity.SDKConfigInfo;
import im.zego.common.entity.StreamQuality;
import im.zego.common.ui.BaseActivity;
import im.zego.common.util.AppLogger;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.callback.IZegoPublisherTakeSnapshotCallback;
import im.zego.zegoexpress.callback.IZegoRoomSetRoomExtraInfoCallback;
import im.zego.zegoexpress.constants.ZegoAudioCaptureStereoMode;
import im.zego.zegoexpress.constants.ZegoAudioChannel;
import im.zego.zegoexpress.constants.ZegoAudioCodecID;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoStreamQualityLevel;
import im.zego.zegoexpress.constants.ZegoVideoCodecID;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoAudioConfig;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoPlayStreamQuality;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;

public class PublishActivityUI extends BaseActivity {


    public static ZegoViewMode viewMode = ZegoViewMode.ASPECT_FILL;
    private ActivityPublishBinding binding;
    private PublishInputStreamIdLayoutBinding layoutBinding;
    private StreamQuality streamQuality = new StreamQuality();
    private SDKConfigInfo sdkConfigInfo = new SDKConfigInfo();
    private ZegoExpressEngine engine;
    private String streamID;
    private String userID;
    private String userName;
    private String roomID;
    private String mStreamID;
    private ZegoCanvas zegoCanvas;
    private static boolean changeFlag = false;
    private SnapshotDialog snapshotDialog;
    private PerformanceStatus performanceStatus = new PerformanceStatus();
    private float maxFactor = 1.0f;
    public static ZegoVideoConfig videoConfig = new ZegoVideoConfig();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_publish);

        // ??????DataBinding ????????????bean?????????UI?????????
        // ???????????????????????????????????? setText ??????????????????????????????
        binding.setQuality(streamQuality);
        binding.setConfig(sdkConfigInfo);
        binding.setPerformance(performanceStatus);
        binding.swMic.setChecked(true);
        binding.swCamera.setChecked(true);
        binding.swFrontCamera.setChecked(true);
        layoutBinding = binding.layout;
        layoutBinding.startButton.setText(getString(R.string.tx_start_publish));
        AppLogger.getInstance().i("createEngine");
        // ?????????SDK
        engine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), getApplication(), null);
//        engine.startPerformanceMonitor(2000);
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "userid-" + randomSuffix;
        userName = "username-" + randomSuffix;
        zegoCanvas = new ZegoCanvas(binding.preview);
        layoutBinding.openCaptureStereo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    engine.setAudioCaptureStereoMode(ZegoAudioCaptureStereoMode.ALWAYS);
                    ZegoAudioConfig audioConfig = new ZegoAudioConfig();
                    audioConfig.codecID = ZegoAudioCodecID.NORMAL;
                    audioConfig.channel = ZegoAudioChannel.STEREO;
                    engine.setAudioConfig(audioConfig);
                }
            }
        });
        engine.setEventHandler(new IZegoEventHandler() {

            @Override
            public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                // ?????????????????????errorCode ???0 ?????????????????????
                // ?????????????????????????????????: <a>https://doc.zego.im/CN/308.html</a>
                // Push stream status update, errorCode non-zero means that push stream failed
                // Please refer to the documentation for common error codes of push streaming: <a> https://doc.zego.im/CN/308.html </a>
                if (errorCode == 0) {
                    binding.title.setTitleName(getString(R.string.tx_publish_success));
                    AppLogger.getInstance().i("publish stream success, streamID : %s", streamID);
                    Toast.makeText(PublishActivityUI.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();
                    binding.publishSnapshot.setVisibility(View.VISIBLE);
                } else {
                    binding.title.setTitleName(getString(R.string.tx_publish_fail));
                    AppLogger.getInstance().i("publish stream fail, streamID : %s, errorCode : %d", streamID, errorCode);

                }
            }

            @Override
            public void onPlayerQualityUpdate(String streamID, ZegoPlayStreamQuality quality) {

            }

//            @Override
//            public void onPerformanceStatusUpdate(ZegoPerformanceStatus status) {
//                performanceStatus.setCpuUsageApp(String.format(getString(R.string.cpuUsageApp)+" %s ", status.cpuUsageApp));
//                performanceStatus.setCpuUsageSystem(String.format(getString(R.string.cpuUsageSystem)+" %s ", status.cpuUsageSystem));
//                performanceStatus.setMemoryUsageApp(String.format(getString(R.string.memoryUsageApp)+" %s ", status.memoryUsageApp));
//                performanceStatus.setMemoryUsageSystem(String.format(getString(R.string.memoryUsageSystem)+" %s ", status.memoryUsageSystem));
//                performanceStatus.setMemoryUsedApp(String.format(getString(R.string.memoryUsedApp)+" %s MB", status.memoryUsedApp));
//
//            }

            @Override
            public void onPublisherQualityUpdate(String streamID, im.zego.zegoexpress.entity.ZegoPublishStreamQuality quality) {
                /**
                 * ??????????????????, ??????????????????3?????????
                 * ????????? {@link com.zego.zegoliveroom.ZegoLiveRoom#setPublishQualityMonitorCycle(long)} ??????????????????
                 */
                /**
                 ??????????????????????????????????* Push stream quality update, the callback frequency defaults once every 3 seconds
                 ??????????????????????????????????* The callback frequency can be modified through {@link com.zego.zegoliveroom.ZegoLiveRoom # setPublishQualityMonitorCycle (long)}
                 ??????????????????????????????????*/
                streamQuality.setFps(String.format(getString(R.string.frame_rate) + " %f", quality.videoSendFPS));
                streamQuality.setBitrate(String.format(getString(R.string.bit_rate) + " %f kbs", quality.videoKBPS));
                streamQuality.setHardwareEncode(String.format(getString(R.string.hardware_encode_1) + " %b", quality.isHardwareEncode));
                streamQuality.setNetworkQuality(String.format(getString(R.string.network_quality) + " %s", getQuality(quality.level)));
            }

            @Override
            public void onPublisherVideoSizeChanged(int width, int height, ZegoPublishChannel channel) {
                // ????????????????????????????????????sdk??????????????????
                // When the resolution changes during acquisition, the SDK will call back this method
                streamQuality.setResolution(String.format(getString(R.string.resolution) + " %dX%d", width, height));
            }

            @Override
            public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                /** ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????SDK???????????????????????? */
                /** Room status update callback: after logging into the room, when the room connection status changes
                 * (such as room disconnection, login authentication failure, etc.), the SDK will notify through the callback
                 */
                AppLogger.getInstance().i("onRoomStateUpdate: roomID = " + roomID + ", state = " + state + ", errorCode = " + errorCode);

                if (state == ZegoRoomState.CONNECTED) {
                    binding.progressBar.setVisibility(View.GONE);
                    changeFlag = true;
                    // ????????????
                    engine.startPublishingStream(streamID);
                    // ??????sdk ?????????????????? ??????view ????????????
                    zegoCanvas.viewMode = viewMode;
                    engine.startPreview(zegoCanvas);
                    binding.roomExtraInfoLayout.setVisibility(View.VISIBLE);
//                    binding.encodeKeyLv.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPublisherCapturedVideoFirstFrame(ZegoPublishChannel channel) {
                super.onPublisherCapturedVideoFirstFrame(channel);
                binding.setSeekBar.setVisibility(View.VISIBLE);
                getCameraMaxFactorData();
            }
        });


        // ?????????????????????????????????
        // Monitor camera and microphone switch
        binding.swCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableCamera(isChecked);
                    engine.enableCamera(isChecked);
                }
            }
        });

        binding.swMic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableMic(isChecked);
                    engine.enableAudioCaptureDevice(isChecked);
                }
            }
        });

        binding.swFrontCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableFrontCamera(isChecked);
                    engine.useFrontCamera(isChecked);
                }
            }
        });
        binding.setExtraRoomInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String extraInfo = binding.edExtraInfo.getText().toString().trim();
                if (extraInfo == null || extraInfo.equals("")) {
                    Toast.makeText(PublishActivityUI.this, "extra info should not be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                engine.setRoomExtraInfo(roomID, "zego", extraInfo, new IZegoRoomSetRoomExtraInfoCallback() {
                    @Override
                    public void onRoomSetRoomExtraInfoResult(int i) {
                        AppLogger.getInstance().i("setRoomExtraInfo : %s, errorCode : %d", roomID, i);

                    }
                });
            }
        });
        binding.publishSnapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (snapshotDialog == null) {
                    snapshotDialog = new SnapshotDialog(PublishActivityUI.this, R.style.SnapshotDialog);
                }
                engine.takePublishStreamSnapshot(new IZegoPublisherTakeSnapshotCallback() {
                    @Override
                    public void onPublisherTakeSnapshotResult(int i, Bitmap bitmap) {
                        snapshotDialog.show();
                        snapshotDialog.setSnapshotBitmap(bitmap);
                    }
                });
            }
        });
//        binding.publishEncodeKeySetting.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String  encodeKey =binding.edPublishEncodeKey.getText().toString();
//                if(encodeKey!=null&&!encodeKey.trim().equals("")){
//                    engine.setPublishStreamEncryptionKey(encodeKey);
//                }else{
//                    Toast.makeText(PublishActivityUI.this,"key should not be null",Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
        initViewModePreferenceData();
        initEnableHardWareEncodePreferenceData();
        initFpsPreferenceData();
        binding.setSeekBar.setOnSeekBarChangeListener(new CustomMinSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, float progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar, float progress) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar, float progress) {
                if (engine != null) {
                    engine.setCameraZoomFactor(progress);
                }
            }
        });
    }

    private void initFpsPreferenceData() {
        SharedPreferences sp = getSharedPreferences(PublishSettingActivityUI.SHARE_PREFERENCE_NAME, MODE_PRIVATE);
        String fps = sp.getString("publish_fps", "15");
        if(videoConfig!=null){
            videoConfig.fps = Integer.valueOf(fps);
        }
    }

    private void initEnableHardWareEncodePreferenceData() {
        SharedPreferences sp = getSharedPreferences(PublishSettingActivityUI.SHARE_PREFERENCE_NAME, MODE_PRIVATE);
        boolean enable = sp.getBoolean("publish_hardware_encode", false);
        if(engine!=null){
            engine.enableHardwareEncoder(enable);
        }

    }

    private void getCameraMaxFactorData() {
        if (engine != null) {
            maxFactor = engine.getCameraMaxZoomFactor();
            binding.setSeekBar.setMax(maxFactor);
        }
    }

    private void initViewModePreferenceData() {
        SharedPreferences sp = getSharedPreferences(PublishSettingActivityUI.SHARE_PREFERENCE_NAME, MODE_PRIVATE);
        String viewMode = sp.getString("publish_view_mode", "1");
        switch (viewMode) {
            case "0":
                PublishActivityUI.viewMode = ZegoViewMode.ASPECT_FIT;
                break;
            case "1":
                PublishActivityUI.viewMode = ZegoViewMode.ASPECT_FILL;
                break;
            case "2":
                PublishActivityUI.viewMode = ZegoViewMode.SCALE_TO_FILL;
                break;
            default:
                break;
        }



    }
    public void initVideoCodeIdPreferenceData(){
        SharedPreferences sp = getSharedPreferences(PublishSettingActivityUI.SHARE_PREFERENCE_NAME, MODE_PRIVATE);
        String videoCodeID = sp.getString("video_code_id", "DEFAULT");

        switch (videoCodeID) {
            case "DEFAULT":
                videoConfig.codecID= ZegoVideoCodecID.DEFAULT;
                break;
            case "SVC":
                videoConfig.codecID= ZegoVideoCodecID.SVC;
                break;
            case "VP8":
                videoConfig.codecID= ZegoVideoCodecID.VP8;
                break;
            case "H265":
                videoConfig.codecID= ZegoVideoCodecID.H265;
                break;
            default:
                break;
        }
    }


    private String getQuality(ZegoStreamQualityLevel level) {
        switch (level) {
            case EXCELLENT:
                return getString(R.string.excellent);
            case GOOD:
                return getString(R.string.good);
            case MEDIUM:
                return getString(R.string.medium);
            case BAD:
                return getString(R.string.bad);
            case DIE:
                return getString(R.string.die);
        }

        return getString(R.string.no_data);
    }


    @Override
    protected void onResume() {
        //  ?????????????????????????????????????????????2????????????????????????????????????????????????????????????????????????????????????????????????????????????,
        //  ??????????????????????????????????????????https://blog.csdn.net/Crazy9599/article/details/89842280
        //  ???????????????????????????????????????????????????????????????
        // On some Huawei mobile phones, the application is in the background for more than 2 minutes, the Huawei system will release the camera resources, and may disconnect the network connection of your application,
        // Need to do the front desk service to avoid this situation https://blog.csdn.net/Crazy9599/article/details/89842280
        // So currently close and re-enable the camera to avoid the problem
        if (binding.swCamera.isChecked()) {
            engine.enableCamera(false);
            engine.enableCamera(true);
        }
        if (binding.swMic.isChecked()) {
            engine.enableAudioCaptureDevice(false);
            engine.enableAudioCaptureDevice(true);
        }
        if (engine != null && changeFlag) {
            zegoCanvas.viewMode = viewMode;
            engine.startPreview(zegoCanvas);

        }

        super.onResume();
    }

    public void goSetting(View view) {
        PublishSettingActivityUI.actionStart(this);
    }

    @Override
    protected void onDestroy() {
        // ???????????????????????????????????????????????? logoutRoom
        engine.setAudioConfig(new ZegoAudioConfig());
        engine.setAudioCaptureStereoMode(ZegoAudioCaptureStereoMode.NONE);
        engine.stopPreview();
        engine.setEventHandler(null);
        engine.stopPublishingStream();
        // ??????????????????????????????????????????
        engine.logoutRoom(roomID);
//        engine.stopPerformanceMonitor();
        ZegoExpressEngine.destroyEngine(null);
        changeFlag = false;
        videoConfig.codecID=ZegoVideoCodecID.DEFAULT;
        super.onDestroy();
    }

    /**
     * button ????????????
     * ????????????
     */
    public void onStart(View view) {
        initVideoCodeIdPreferenceData();
        engine.setVideoConfig(videoConfig);
        mStreamID = layoutBinding.edStreamId.getText().toString();
        roomID = layoutBinding.edRoomId.getText().toString();
        streamQuality.setRoomID(String.format("RoomID : %s", roomID));
        ZegoUser user = new ZegoUser(userID, userName);
        engine.loginRoom(roomID, user, null);
        binding.progressBar.setVisibility(View.VISIBLE);
        streamID = layoutBinding.edStreamId.getText().toString();
        // ????????????StreamID??????
        hideInputStreamIDLayout();


        streamQuality.setStreamID(String.format("StreamID : %s", streamID));


    }

    private void hideInputStreamIDLayout() {
        // ??????InputStreamIDLayout??????
        layoutBinding.getRoot().setVisibility(View.GONE);
        binding.publishStateView.setVisibility(View.VISIBLE);
        hideKeyboard(PublishActivityUI.this, layoutBinding.edStreamId);
    }

    private void showInputStreamIDLayout() {
        // ??????InputStreamIDLayout??????
        layoutBinding.getRoot().setVisibility(View.VISIBLE);
        binding.publishStateView.setVisibility(View.GONE);
    }

    //??????????????????
    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);

        }
    }


    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, PublishActivityUI.class);
        activity.startActivity(intent);
    }


}
