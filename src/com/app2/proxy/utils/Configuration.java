package com.app2.proxy.utils;

public class Configuration {
    public static final String MANAGER_SERVER_IP = "10.128.180.43";

    public static final int MANAGER_SERVER_PORT = 6666;
    public static final int ADB_SERVER_PORT = 9999;

    public static final String CMD_RETURN_TAG = "RETURN-";
    public static final String CMD_CONNECT_ADB = "cmd_connect_adb";
    public static final String CMD_LIST_DEVICES = "cmd_list_devices";
    public static final String CMD_SET_DEVICE = "cmd_set_device";
    
    public static final String CMD_RETRUN_LIST_DEVICES = CMD_RETURN_TAG + CMD_LIST_DEVICES;
    public static final String CMD_RETRUN_SET_DEVICE = CMD_RETURN_TAG + CMD_SET_DEVICE;
    
    public static final String SEG = "#@#";

    public static final String TYPE_PC_TERMINAL = "type_pc_terminal";
    public static final String TYPE_PHONE_CLIENT = "type_phone_client";

}
