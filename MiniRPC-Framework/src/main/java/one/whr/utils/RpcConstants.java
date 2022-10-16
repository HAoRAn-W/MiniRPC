package one.whr.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RpcConstants {
    // magic number is used to verify RpcMessages
    public static final byte[] MAGIC_NUMBER = {(byte) 'g', (byte) 'r', (byte) 'p', (byte) 'c'};

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static final byte VERSION = 1;

    public static final byte TOTAL_LENGTH = 16;

    public static final byte REQUEST_TYPE = 1;

    public static final byte RESPONSE_TYPE = 2;

    public static final byte HEARTBEAT_PING_TYPE = 3;

    public static final byte HEARTBEAT_PONG_TYPE = 4;

    public static final int HEAD_LENGTH = 16;

    public static final String PING = "PING";

    public static final String PONG = "PONG";

    public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024;

}
