package org.example.service;

import static java.lang.Long.*;
import static java.time.Instant.ofEpochSecond;
import static java.time.Instant.*;
import static java.time.LocalDateTime.*;
import static java.time.ZoneId.*;
import static org.example.constant.CommonType.*;
import static org.example.constant.FirstByteType.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.example.model.Argument;
import org.example.model.RedisInputData;
import org.example.model.RedisValue;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RedisService {

	public static final Map<String, RedisValue> redisCommonMap = new HashMap<>();

	private final RedisInputData redisInputData;

	public String executeCommand() {
		return switch (redisInputData.commandType()) {
			case PING -> executePing(redisInputData);
			case ECHO -> executeEcho(redisInputData);
			case GET -> executeGet(redisInputData);
			case SET -> executeSet(redisInputData);
		};
	}

	private String executePing(RedisInputData redisInputData) {
		if (redisInputData.arguments().isEmpty()) {
			return simpleString(PONG);
		}
		return bulkString(redisInputData.arguments().get(0).name());
	}

	private String executeEcho(RedisInputData redisInputData) {
		return bulkString(redisInputData.arguments().get(0).name());
	}

	private String executeGet(RedisInputData redisInputData) {
		var redisValue = redisCommonMap.get(redisInputData.arguments().get(0).name());
		if (isExpiredValue(redisValue)) {
			redisCommonMap.remove(redisInputData.arguments().get(0).name());
			return nullString();
		}
		return bulkString(redisValue.value());
	}

	private String executeSet(RedisInputData redisInputData) {
		var key = redisInputData.arguments().get(0).name();
		var value = redisInputData.arguments().get(1).name();
		var args = redisInputData.arguments().subList(2, redisInputData.arguments().size());
		String response = null;
		boolean get = false;

		if (needPreviousValue(args, key)) {
			get = true;
			response = redisCommonMap.get(key).value();
		}
		if (isConflict(args, key)) {
			return null;
		}
		redisCommonMap.put(key, generateRedisValue(value, args, key));

		if (response == null && get) {
			return nullString();
		} else if (response == null) {
			return simpleString("OK");
		}
		return bulkString(response);
	}

	private RedisValue generateRedisValue(String value, List<Argument> args, String key) {
		var redisValue = new RedisValue(value, LocalDateTime.now(), null, null);

		Argument argument = args.stream()
			.filter(arg -> StringUtils.equalsAnyIgnoreCase(arg.name(), TTL_WHITELIST))
			.findFirst()
			.orElse(null);
		if (argument == null) {
			return redisValue;
		}
		return getRedisValueAppliedTTL(key, value, argument);
	}

	/**
	 * EX seconds - Set the specified expire time, in seconds (a positive integer).
	 * PX milliseconds - Set the specified expire time, in milliseconds (a positive integer).
	 * EXAT timestamp-seconds - Set the specified Unix time at which the key will expire, in seconds (a positive integer).
	 * PXAT timestamp-milliseconds - Set the specified Unix time at which the key will expire, in milliseconds (a positive integer).
	 * KEEPTTL -- Retain the time to live associated with the key.
	 * GET -- Return the old string stored at key, or nil if key did not exist.
	 * 		  An error is returned and SET aborted if the value stored at key is not a string.
	 */
	private RedisValue getRedisValueAppliedTTL(String key, String value, Argument argument) {
		var now = LocalDateTime.now();
		return switch (argument.name()) {
			case "EX" -> new RedisValue(value, now, parseLong(argument.value()) * 1000L, null);
			case "PX" -> new RedisValue(value, now, parseLong(argument.value()), null);
			case "EXAT" -> {
				LocalDateTime ttl = ofInstant(ofEpochSecond(parseLong(argument.value())), systemDefault());
				yield new RedisValue(value, now, null, ttl);
			}
			case "PXAT" -> {
				LocalDateTime ttl = ofInstant(ofEpochMilli(parseLong(argument.value())), systemDefault());
				yield new RedisValue(value, now, null, ttl);
			}
			// KEEPTTL
			default -> {
				var previousValue = redisCommonMap.get(key);
				if (isExpiredValue(previousValue)) {
					yield new RedisValue(value, now, null, null);
				}
				yield new RedisValue(value, now, previousValue.epochTTL(), previousValue.dueDateTTL());
			}
		};
	}

	private boolean needPreviousValue(List<Argument> args, String key) {
		return isArgumentContains(args, "GET") && !isExpiredValue(redisCommonMap.get(key));
	}

	/**
	 * NX -> Only set the key if it does not already exist.
	 * XX -> Only set the key if it already exists.
	 */
	private boolean isConflict(List<Argument> args, String key) {
		return (isArgumentContains(args, "NX") && redisCommonMap.containsKey(key))
			   || ((isArgumentContains(args, "XX") && !redisCommonMap.containsKey(key)));
	}

	/**
	 * case epochTTL -> if (createdAt + epochTTL < now) then key was expired
	 * case dueDateTTL -> if (dueDateTTL < now) then key was expired
	 */
	private boolean isExpiredValue(RedisValue redisValue) {
		if (redisValue == null) {
			return true;
		}
		var now = LocalDateTime.now();
		var dueDate = redisValue.dueDateTTL();
		if (redisValue.epochTTL() != null) {
			return redisValue.createdAt().plus(redisValue.epochTTL(), ChronoUnit.MILLIS).isAfter(now);
		}
		if (redisValue.dueDateTTL() != null) {
			return dueDate.isAfter(now);
		}
		return false;
	}

	/**
	 * first byte: +
	 * is terminated by CRLF
	 */
	private String simpleString(String response) {
		return SIMPLE_FIRST_BYTE.getValue() + response + CRLF;
	}

	/**
	 * case common bulk string
	 * first byte: $
	 * then one or more decimal digits as the string's length
	 * then CRLF
	 * then string(data)
	 * then CRLF
	 * -----------------
	 * case null bulk string
	 * first byte: $
	 * then -1
	 * then CRLF
	 */
	private String bulkString(String response) {
		if (response == null) {
			return NULL_BULK_STRING + CRLF;
		}
		return String.valueOf(LENGTH_FIRST_BYTE.getValue()) + response.length() + CRLF + response + CRLF;
	}

	/**
	 * represents non-existent values
	 * first byte: _
	 * then CRLF
	 */
	private String nullString() {
		return NULL_FIRST_BYTE.getValue() + CRLF;
	}

	private boolean isArgumentContains(List<Argument> args, String target) {
		return args.stream()
			.anyMatch(argument -> StringUtils.equalsAnyIgnoreCase(argument.name(), target));
	}
}
