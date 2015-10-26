package com.app2.proxy.net;

public class Data {

    private byte[] bytes = null;
    private String string = null;

    public Data() {

    }

    public Data(byte[] bytes, String string) {
        super();
        this.bytes = bytes;
        this.string = string;
    }

    public Data(byte[] bytes) {
        super();
        this.bytes = bytes;
    }

    public Data(byte[] bytes, int offset, int length) {
        super();
        byte[] byteArray = new byte[length];
        System.arraycopy(bytes, offset, byteArray, 0, length);
        this.bytes = byteArray;
    }

    public Data(String string) {
        super();
        this.string = string;
    }

    public byte[] getBytes() {
        if (bytes == null) {
            if (string == null) {
                return null;
            } else {
                return string.getBytes();
            }
        } else {
            return bytes;
        }
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getString() {
        if (string == null) {
            if (bytes == null) {
                return null;
            } else {
                return new String(bytes);
            }
        } else {
            return string;
        }
    }

    public void setString(String string) {
        this.string = string;
    }

}
