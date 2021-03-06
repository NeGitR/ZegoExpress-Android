package im.zego.mixer.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import im.zego.common.util.AppLogger;
import im.zego.common.util.SettingDataUtil;
import im.zego.common.widgets.log.FloatingView;
import im.zego.mixer.R;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoLanguage;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MixerMainActivity extends AppCompatActivity {
    public static String roomID = "MixerRoom-1";
    public static String userID;
    public static String userName;
    public static ZegoExpressEngine engine;
    public static ArrayList<String> streamIDList = new ArrayList<>();
    private static IMixerStreamUpdateHandler notifyHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixer_main);
        String randomSuffix = String.valueOf(new Date().getTime()%(new Date().getTime()/1000));
        userID = "user" + randomSuffix;
        userName = "user" + randomSuffix;
        TextView tv_room = findViewById(R.id.tv_room_id);
        tv_room.setText(roomID);

        engine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        if (engine != null) {
            IZegoEventHandler handler = new IZegoEventHandler() {

                @Override
                public void onMixerSoundLevelUpdate(HashMap<Integer, Float> soundLevels) {
                    Log.e("onMixerSoundLevelUpdate", "test");
                }

                @Override
                public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList,JSONObject extendedData) {
                    String streamID;
                    for (int i = 0; i < streamList.size(); i++) {
                        if (updateType == ZegoUpdateType.ADD) {
                            streamID = streamList.get(i).streamID;
                            streamIDList.add(streamID);
                        }
                        else {
                            streamID = streamList.get(i).streamID;
                            streamIDList.remove(streamID);
                        }
                        AppLogger.getInstance().i("onRoomStreamUpdate: roomID = " + roomID + ", updateType =" + updateType + ", streamID = " + streamID);
                    }
                    if (notifyHandler != null) {
                        notifyHandler.onRoomStreamUpdate();
                    }
                }



                @Override
                public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                    AppLogger.getInstance().i("onPublisherStateUpdate???state =" + state + ", streamID = " + streamID + ", errorCode = " + errorCode);
                    if (state == ZegoPublisherState.PUBLISHING) {
                        Toast.makeText(MixerMainActivity.this, getString(R.string.tx_mixer_publish_ok), Toast.LENGTH_SHORT).show();
                    }
                    else if (state == ZegoPublisherState.PUBLISH_REQUESTING) {
                        Toast.makeText(MixerMainActivity.this, getString(R.string.tx_mixer_publish_request), Toast.LENGTH_SHORT).show();
                    }
                    else if (state == ZegoPublisherState.NO_PUBLISH) {
                        if (errorCode == 0) {
                            Toast.makeText(MixerMainActivity.this, getString(R.string.tx_mixer_stop_publish_ok), Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(MixerMainActivity.this, getString(R.string.tx_mixer_publish_fail) + errorCode, Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                    /** ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????SDK???????????????????????? */
                    /** Room status update callback: after logging into the room, when the room connection status changes
                     * (such as room disconnection, login authentication failure, etc.), the SDK will notify through the callback
                     */
                    AppLogger.getInstance().i("onRoomStateUpdate: roomID = " + roomID + ", state = " + state + ", errorCode = " + errorCode);
                    if (errorCode != 0) {
                        Toast.makeText(MixerMainActivity.this, String.format("login room fail, errorCode: %d", errorCode), Toast.LENGTH_LONG).show();
                    }
                }
            };
            engine.setEventHandler(handler);
            ZegoUser user = new ZegoUser(userID, userName);
            engine.loginRoom(roomID, user, null);
            engine.setDebugVerbose(true, ZegoLanguage.CHINESE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZegoExpressEngine.destroyEngine(null);
        streamIDList.clear();
    }

    public void ClickPublishActivity(View view) {
        MixerPublishActivity.actionStart(this);
    }

    public void ClickMixActivity(View view) {
        MixerStartActivity.actionStart(this);
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, MixerMainActivity.class);
        activity.startActivity(intent);
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

    public static void registerStreamUpdateNotify(IMixerStreamUpdateHandler handler) {
        notifyHandler = handler;
    }
}
