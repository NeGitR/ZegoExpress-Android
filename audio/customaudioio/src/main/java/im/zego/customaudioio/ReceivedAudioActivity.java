package im.zego.customaudioio;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;

import im.zego.common.GetAppIDConfig;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoAudioDataHandler;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoAudioChannel;
import im.zego.zegoexpress.constants.ZegoAudioDataCallbackBitMask;
import im.zego.zegoexpress.constants.ZegoAudioSampleRate;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.entity.ZegoAudioFrameParam;
import im.zego.zegoexpress.entity.ZegoEngineConfig;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;

public class ReceivedAudioActivity extends Activity {
    private Button stop,loginRoom;
    private ZegoExpressEngine engine;
    private String roomId="NeTest-3";
    private String userId;
    private Integer mRecordBufferSize;
    private int captureSampleRate = 44100;
    private int captureChannel = AudioFormat.CHANNEL_IN_MONO;

    ByteBuffer mPcmBuffer;
    private AudioRecord mAudioRecord;
    private TextView publishState, publishStreamIdEditText;
    private ZegoAudioFrameParam audioFrameParam=new ZegoAudioFrameParam();

    private ZegoEngineConfig engineConfig=new ZegoEngineConfig();

    // Recording  Macro
    private static final boolean BEDEBUG = true;
    private File parent = null;//文件目录
    private File  recordingFile = null;
    private FileOutputStream dos = null;
    private byte[] bytes ;

    private enum Status {
        STATUS_NO_READY,
        STATUS_READY,
        STATUS_START,
        STATUS_STOP
    }
    private Status mStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.received_audio);
        checkPermissions();
        initView();

        audioFrameParam.sampleRate = ZegoAudioSampleRate.ZEGO_AUDIO_SAMPLE_RATE_32K;
        audioFrameParam.channel = ZegoAudioChannel.MONO;

        String fileName =  "onPlaybackAudioData-" + Integer.toString(audioFrameParam.sampleRate.value())
                + "_" + (ZegoAudioChannel.MONO.value() == 1 ? "MONO" : "STEREO")
                +"_" + ".pcm";
        parent = new File(this.getExternalFilesDir("") + "/debugTest");
        Log.e("NeDebug", "the Debug path: " + parent);
        if (!parent.exists()) {
            parent.mkdirs();//创建文件夹
        }
        if(BEDEBUG) {
            recordingFile = new File(parent, fileName);
            if (recordingFile.exists()) {
                recordingFile.delete();
            }

            try {
                recordingFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("NeDebug", "create Audio Saved File Error !");
            }
            if(BEDEBUG) {
                try {
                    dos = new FileOutputStream(recordingFile);
                } catch (IOException e){
                    e.printStackTrace();
                    Log.e("NeDebug", "create FileOutputStream Error !");
                }
            }
        }
        createZegoExpressEngine();
    }

    private void initView() {
        loginRoom = findViewById(R.id.login_room3);
        stop = findViewById(R.id.logoutRoom3);

        publishState =findViewById(R.id.play_state2);
        publishStreamIdEditText=findViewById(R.id.received_stream_id2);

        loginRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userId = String.valueOf(new Date().getTime()%(new Date().getTime()/1000));

                Log.e("[ZEGO]", "Publish :" + publishStreamIdEditText.getText().toString().trim() + " userId:" + userId);

                int bytesLen = (audioFrameParam.sampleRate.value()/100) * audioFrameParam.channel.value() * 2;
                bytes = new byte[bytesLen];


                int bitmask = ZegoAudioDataCallbackBitMask.CAPTURED.value();
                bitmask |= ZegoAudioDataCallbackBitMask.MIXED.value();
                bitmask |= ZegoAudioDataCallbackBitMask.PLAYBACK.value();

                engine.enableAudioDataCallback(true, bitmask, audioFrameParam);

                engine.loginRoom(roomId,new ZegoUser(userId));

                engine.startPlayingStream(publishStreamIdEditText.getText().toString().trim());

            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord();
            }
        });

    }
    public void reset(){
        destroy();
        createZegoExpressEngine();
    }

    private void createZegoExpressEngine() {
        engine = ZegoExpressEngine.createEngine(GetAppIDConfig.appID, GetAppIDConfig.appSign, true, ZegoScenario.GENERAL, getApplication(), null);

        engine.setEventHandler(new IZegoEventHandler() {
            @Override
            public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList,JSONObject extendedData) {
                for(ZegoStream stream:streamList) {
                    Log.i("[ZEGO]", "onRoomStreamUpdate roomID:" + roomID + " updateType:" + updateType+" streamId:"+stream.streamID);
                }
            }

            @Override
            public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                Log.i("[ZEGO]", "onRoomStateUpdate roomID:"+roomID+" state:"+state);
            }

            @Override
            public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                Log.i("[ZEGO]", "onPublisherStateUpdate streamID:"+streamID+" state:"+state);
                publishState.setText("publish state:" +state+"   streamId:"+streamID);
            }

            @Override
            public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
            }
        });

        engine.setAudioDataHandler(new IZegoAudioDataHandler() {
            @Override
            public void onCapturedAudioData(ByteBuffer data, int dataLength, ZegoAudioFrameParam param) {
                Log.e("[RawData]", "onCapturedAudioData dataLength:"+dataLength+ " channel:"+param.channel + " sampleRate:"+param.sampleRate);
            }

            @Override
            public void onMixedAudioData(ByteBuffer data, int dataLength, ZegoAudioFrameParam param) {
                Log.e("[RawData]", "onMixedAudioData dataLength:"+dataLength+ " channel:"+param.channel + " sampleRate:"+param.sampleRate);
            }

            @Override
            public void onPlaybackAudioData(ByteBuffer data, int dataLength, ZegoAudioFrameParam param) {
                Log.e("[RawData]", "onPlaybackAudioData dataLength:"+dataLength+ " channel:"+param.channel + " sampleRate:"+param.sampleRate);
                if(BEDEBUG) {
                    data.get(bytes,0,dataLength);
                    try {
                        dos.write(bytes, 0, dataLength);
                    } catch (IOException e){
                        e.printStackTrace();
                        Log.e("NeDebug", "create write Error !");
                    }
                }
            }

            @Override
            public void onPlayerAudioData(ByteBuffer data, int dataLength, ZegoAudioFrameParam param, String streamID) {
                Log.e("[RawData]", "onPlayerAudioData dataLength:"+dataLength+ " channel:"+param.channel + " sampleRate:"+param.sampleRate);
            }
        });
    }

    private void stopRecord() {
        if(mStatus== Status.STATUS_STOP) {
            Log.i("[ZEGO]","The custom audio capture has been disabled, please do not click repeatedly");
            return;
        }
        engine.stopPlayingStream(publishStreamIdEditText.getText().toString().trim());

        engine.logoutRoom(roomId);

        mStatus= Status.STATUS_STOP;

        if(BEDEBUG) {
            try {
                dos.flush();
                dos.close();
            } catch (IOException e){

            }
        }
    }

    // Check and request permission
    private boolean checkPermissions() {
        String[] PERMISSIONS_STORAGE = {
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.RECORD_AUDIO"};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_STORAGE, 101);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroy();
    }
    public void destroy(){
        if(mAudioRecord!=null) {
            mAudioRecord.release();
        }
        ZegoExpressEngine.destroyEngine(null);
        mStatus= Status.STATUS_NO_READY;
        publishState.setText("");
    };
}
