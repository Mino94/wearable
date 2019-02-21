package com.example.keti_jp.wear;

public class Constants {
    protected static final int BUILT_IN_SENSOR_COUNT = 5;
    protected static final int KETIIoT_SENSOR_COUNT = 9;    // 시나리오 버튼 두개 추가


    protected static final String BLUETOOTH_DEVICE_NAME     = "QCOM-BTD";
    // unique UUID string
    protected static final String STRING_UUID_SECURE        = "fa87c0d0-afac-11de-8a39-0800200c9a66";
    protected static final String STRING_UUID_INSECURE      = "8ce255c0-200a-11e0-ac64-0800200c9a66";
    protected static final String STRING_UUID_SPP_SECURE    = "00001101-0000-1000-8000-00805F9B34FB";

    // Constants that indicate the current connection state (using with main handler)
    protected static final int STATE_NONE                       = 0;
    protected static final int STATE_LISTEN                     = 1;
    protected static final int STATE_CONNECTING                 = 2;
    protected static final int STATE_CONNECTION_FAILED          = 3;
    protected static final int STATE_CONNECTED                  = 4;
    protected static final int STATE_CONNECTION_LOST            = 5;
    protected static final int STATE_DISCONNECTED               = 6;
    protected static final int STATE_MESSAGE_READ               = 7;
    protected static final int STATE_MESSAGE_WRITE              = 8;

    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;

    protected static final int STATE_DEVICE_LIST_VIEW           = 9;
    //protected static final int STATE_DETECT_SHAKE_GESTURE       = 10;

    //UI
    protected static final int UI_STATE_CONNECTED                  = 1;
    protected static final int UI_STATE_DEVICE_LIST_VIEW           = 2;
    protected static final int UI_STATE_WAITING_CONNECT_DEVICE     = 3;
    protected static final int UI_STATE_FAILED_BT_CONNECTION       = 4;
    protected static final int UI_STATE_SENSOR_LIST               = 5;
    protected static final int UI_STATE_SENSOR_UPDATE               = 6;
    protected static final int UI_STATE_SENSOR_ADDED               = 7;



    //Shared Preference
    protected  static final String PREFERENCE_OBJECT_KEY        = "PREFERENCE";
    protected  static final String PREFERENCE_BT_MAC_ADDR_KEY   = "BT_MAC";

    // Key name received from the BluetoothService Handler
    protected static final String DEVICE_MAC_ADDR           = "device mac addr";
    protected static final String TOAST                     = "toast";
    protected static final String READ_MESSAGE              = "read message";
    protected static final String WRITE_MESSAGE             = "write message";

    // bluetooth service name
    protected static final String NAME_SECURE               = "BluetoothChatSecure";
    protected static final String NAME_INSECURE             = "BluetoothChatInsecure";


    protected static final String TEXT_BT_DISCOVERABLE      = "Discoverable";
    protected static final String TEXT_BT_DISCONNECTED      = "Disconnected";
    protected static final String TEXT_BT_CONNECTED      = "Connected";

    //Search Bluetooth Device State
    protected static final int SEARCH_BT_STATE_NONE = 0;
    protected static final int SEARCH_BT_STATE_CONNECT_SAVED_ADDR = 1;
    protected static final int SEARCH_BT_STATE_GET_PAIRED_ADDR = 2;
    protected static final int SEARCH_BT_STATE_CONNECT_PAIRED_ADDR = 3;
    protected static final int SEARCH_BT_STATE_SCAN_DEVICE = 4;
    protected static final int SEARCH_BT_STATE_CONNECTED = 5;

    //Messages
    protected static final int KETI_NOTIFY_CONNECT = 0x70;
    protected static final int KETI_REQ_SENSOR_LIST = 0x71;
    protected static final int KETI_RSP_SENSOR_LIST = 0x72;
    protected static final int KETI_UPDATE_SENSOR_VALUE = 0x73;
    protected static final int KETI_SENSOR_ADDED = 0x74;
    protected static final int KETI_SENSOR_DELETED = 0x75;
    protected static final int KETI_NOTIFY_EVENT = 0x76;
    protected static final int KETI_REQ_SENSOR_VALUE = 0x77;
    protected static final int KETI_RSP_SENSOR_VALUE = 0x78;
    protected static final int KETI_CMD_SENSOR_VALUE = 0x79;


    //UI Status Text
    protected static final String UI_STATE_TEXT_ON= "On";
    protected static final String UI_STATE_TEXT_OFF= "Off";

    //GESTURES
    protected static final int HANDLER_SENSOR_SCAN_HAND_UP_GESTURE_DETECT = 10;
    protected static final int HANDLER_SENSOR_SCAN_HAND_DOWN_GESTURE_DETECT = 11;
    protected static final int HANDLER_SENSOR_SCAN_HAND_WATER_LEFT_GESTURE_DETECT = 12;
    protected static final int HANDLER_SENSOR_SCAN_HAND_WATER_RIGHT_GESTURE_DETECT = 13;
    protected static final int HANDLER_SENSOR_SCAN_HAND_GAS_LEFT_GESTURE_DETECT = 14;
    protected static final int HANDLER_SENSOR_SCAN_HAND_GAS_RIGHT_GESTURE_DETECT = 15;
    protected static final int HANDLER_SENSOR_SCAN_HAND_LAMP_SHAKE_GESTURE_DETECT = 16;
    protected static final int HANDLER_SENSOR_SCAN_HAND_RGB_SHAKE_GESTURE_DETECT = 17;
    protected static final int HANDLER_SENSOR_SCAN_HAND_OUT_MODE_ON = 18;
    protected static final int HANDLER_SENSOR_SCAN_HAND_OUT_MODE_OFF = 19;
    protected static final int HANDLER_SENSOR_SCAN_HAND_SMARTHOME_MODE_ON = 20;
    protected static final int HANDLER_SENSOR_SCAN_HAND_SMARTHOME_MODE_OFF = 21;
    protected static final int HANDLER_SENSOR_SCAN_HAND_SCN_FIRE_ON = 22;
    protected static final int HANDLER_SENSOR_SCAN_HAND_SCN_FIRE_OFF = 23;



    public static int unsigned8Bits( byte uByte ) {

        int result = (int)uByte & 0x000000FF;
        return result;
    }

}
