package org.example.utils;

import static org.example.constant.CommonType.*;
import static org.example.error.ErrorType.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.example.model.Argument;
import org.example.model.RedisInputData;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommandValidator {

	private final RedisInputData redisInputData;

	public void validateCommandArguments() {
		switch (redisInputData.commandType()) {
			case PING -> validatePing(redisInputData);
			case ECHO -> validateEcho(redisInputData);
			case GET -> validateGet(redisInputData);
			case SET -> validateSet(redisInputData);
		}
	}

	/**
	 * PING [bulk strings]
	 */
	private void validatePing(RedisInputData redisInputData) {
		if (redisInputData.arguments().size() <= 1) {
			return;
		}
		WRONG_NUMBER_OF_ARGUMENTS.throwException(redisInputData.commandType());
	}

	/**
	 * ECHO bulk strings
	 */
	private void validateEcho(RedisInputData redisInputData) {
		if (redisInputData.arguments().size() == 1) {
			return;
		}
		WRONG_NUMBER_OF_ARGUMENTS.throwException(redisInputData.commandType());
	}

	/**
	 * GET key
	 */
	private void validateGet(RedisInputData redisInputData) {
		if (redisInputData.arguments().size() == 1) {
			return;
		}
		WRONG_NUMBER_OF_ARGUMENTS.throwException(redisInputData.commandType());
	}

	/**
	 * SET key value
	 * [NX | XX]
	 * [GET]
	 * [EX seconds | PX milliseconds | EXAT unix-time-seconds | PXAT unix-time-milliseconds | KEEPTTL]
	 */
	private void validateSet(RedisInputData redisInputData) {
		if (redisInputData.arguments().size() <= 2) {
			return;
		}

		var arguments = redisInputData.arguments().subList(2, redisInputData.arguments().size());
		if (isUnknownArgumentExists(arguments) || getGetCount(arguments) > 1
			|| getExistArgument(arguments) > 1 || getTTlArgument(arguments) > 1) {
			SYNTAX_ERROR.throwException();
		}
		if (arguments.stream().anyMatch(argument ->
			containIgnoreCase(NEED_MORE_ARGUMENT, argument.name()) && !StringUtils.isNumeric(argument.value()))) {
			INVALID_VALUE_TYPE.throwException();
		}
	}

	/**
	 * @return number of arguments that require ttl value
	 */
	private long getTTlArgument(List<Argument> arguments) {
		return arguments.stream()
			.filter(argument -> StringUtils.equalsAnyIgnoreCase(argument.name(), TTL_WHITELIST))
			.count();
	}

	/**
	 * @return NX or XX count
	 */
	private long getExistArgument(List<Argument> arguments) {
		return arguments.stream()
			.filter(argument -> StringUtils.equalsAnyIgnoreCase(argument.name(), EXIST_WHITELIST))
			.count();
	}

	/**
	 * @return GET count
	 */
	private long getGetCount(List<Argument> arguments) {
		return arguments.stream()
			.filter(argument -> StringUtils.equalsAnyIgnoreCase(argument.name(), GET_WHITELIST))
			.count();
	}

	private boolean isUnknownArgumentExists(List<Argument> arguments) {
		String[] args = Stream.of(new String[] {GET_WHITELIST}, TTL_WHITELIST, EXIST_WHITELIST)
			.flatMap(Arrays::stream)
			.toArray(String[]::new);
		return arguments.stream()
			.anyMatch(argument -> !StringUtils.equalsAnyIgnoreCase(argument.name(), args));
	}

	public static boolean containIgnoreCase(String[] whiteList, String argument) {
		return Arrays.stream(whiteList)
			.anyMatch(s -> StringUtils.equalsIgnoreCase(s, argument));
	}
}
