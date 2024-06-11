package org.example.constant;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public enum CommandType {

	PING,
	ECHO,
	GET,
	SET;

	public static CommandType findByInput(String input) {
		return Arrays.stream(CommandType.values())
			.filter(commandType -> StringUtils.equalsIgnoreCase(commandType.name(), input))
			.findFirst()
			.orElse(null);
	}
}
