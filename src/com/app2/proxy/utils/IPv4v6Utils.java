package com.app2.proxy.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Pattern;


public class IPv4v6Utils {

    /**
     * 获取本地IP地址 * @author YOLANDA * @return
     */
    public static String getLocalIPAddress() {
        String ipAddress = "";

        try {
            Enumeration<NetworkInterface> netfaces = NetworkInterface.getNetworkInterfaces(); // 遍历所用的网络接口
            while (netfaces.hasMoreElements()) {
                NetworkInterface nif = netfaces.nextElement();// 得到每一个网络接口绑定的地址
                Enumeration<InetAddress> inetAddresses = nif.getInetAddresses(); // 遍历每一个接口绑定的所有ip
                while (inetAddresses.hasMoreElements()) {
                    InetAddress ip = inetAddresses.nextElement();

                    if (!ip.isLoopbackAddress() && isIPv4Address(ip.getHostAddress())) {
                        ipAddress = ip.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipAddress;
    }

    /*
     * 这里要加上一点说明，因为在21开始，HTTPClient被抛弃了，Google推荐使用URLConnect，这里的Ipv4也被抛弃了，为了兼容以后的版本
     * ，我把HTTPClient的一些源码直接拿到项目中来用，所以这里出现了Ipv4的检查的源码
     */
    /**
     * Ipv4地址检查
     */
    private static final Pattern IPV4_PATTERN = Pattern
            .compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

    // public static String matchIPV4(String in) {
    // Matcher temp = IPV4_PATTERN.matcher(in);
    // if(temp.find()){
    // return temp.group();
    // }
    // return null;
    // }

    /**
     * * 检查是否是有效的IPV4地址 *
     * 
     * @param input
     *            the address string to check for validity *
     * @return true if the input parameter is a valid IPv4 address
     */
    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

    /* ===========以下是IPv6的检查，，暂时用不到========== */
    // 未压缩过的IPv6地址检查
    private static final Pattern IPV6_STD_PATTERN = Pattern.compile("^[0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4}){7}$");

    // 检查参数是否有效的标准(未压缩的)IPv6地址
    public static boolean isIPv6StdAddress(final String input) {
        return IPV6_STD_PATTERN.matcher(input).matches();
    }

    // 压缩过的IPv6地址检查
    private static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile("^(([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,5})?)" + "::"
            + "(([0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4}){0,5})?)$");

    // 检查参数是否有效压缩IPv6地址
    public static boolean isIPv6HexCompressedAddress(final String input) {
        int colonCount = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == ':') {
                colonCount++;
            }
        }
        return colonCount <= 7 && IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
    }

    // 检查是否是压缩或者未压缩过的IPV6地址
    public static boolean isIPv6Address(final String input) {
        return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
    }

    /**
     * deviceID的组成为：渠道标志+识别符来源标志+hash后的终端识别符
     * <p/>
     * 渠道标志为： 1，andriod（a）
     * <p/>
     * 识别符来源标志： 1， wifi mac地址（wifi）； 2， IMEI（imei）； 3， 序列号（sn）； 4，
     * id：随机码。若前面的都取不到时，则随机生成一个随机码，需要缓存。
     * 
     * @param context
     * @return
     */
    private static String getDeviceId(Context context) {

        StringBuilder deviceId = new StringBuilder();
        // 渠道标志
        deviceId.append("a");

        try {

            // wifi mac地址
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            String wifiMac = info.getMacAddress();
            if (!TextUtils.isEmpty(wifiMac)) {
                deviceId.append("wifi");
                deviceId.append(wifiMac);
                LogExt.e("getDeviceId : ", deviceId.toString());
                return deviceId.toString();
            }

            // IMEI（imei）
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = tm.getDeviceId();
            if (!TextUtils.isEmpty(imei)) {
                deviceId.append("imei");
                deviceId.append(imei);
                LogExt.e("getDeviceId : ", deviceId.toString());
                return deviceId.toString();
            }

            // 序列号（sn）
            String sn = tm.getSimSerialNumber();
            if (!TextUtils.isEmpty(sn)) {
                deviceId.append("sn");
                deviceId.append(sn);
                LogExt.e("getDeviceId : ", deviceId.toString());
                return deviceId.toString();
            }

            // 如果上面都没有， 则生成一个id：随机码
            String uuid = getUUID(context);
            if (!TextUtils.isEmpty(uuid)) {
                deviceId.append("id");
                deviceId.append(uuid);
                LogExt.e("getDeviceId : ", deviceId.toString());
                return deviceId.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            deviceId.append("id").append(getUUID(context));
        }

        LogExt.e("getDeviceId : ", deviceId.toString());

        return deviceId.toString();

    }

    /**
     * 得到全局唯一UUID
     */
    public static String getUUID(Context context) {
        SharedPreferences mShare = context.getSharedPreferences("sysCacheMap", Context.MODE_PRIVATE);
        String uuid = mShare.getString("uuid", "");

        if (TextUtils.isEmpty(uuid)) {
            uuid = getDeviceId(context);
            mShare.edit().putString("uuid", uuid).apply();
        }

        return uuid;
    }
}
