package org.example.constant;

public class CommonType {

	public static final String CRLF = "\r\n";
	public static final String ERROR_PREFIX = "-ERR ";
	public static final String PONG = "PONG";
	public static final String NULL_BULK_STRING = "$-1";

	public static final String[] NEED_MORE_ARGUMENT = {"EX", "PX", "EXAT", "PXAT"};
	public static final String[] TTL_WHITELIST = {"EX", "PX", "EXAT", "PXAT", "KEEPTTL"};
	public static final String[] EXIST_WHITELIST = {"XX", "NX"};
	public static final String GET_WHITELIST = "GET";
}
