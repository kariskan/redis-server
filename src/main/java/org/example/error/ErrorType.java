package org.example.error;

import static org.example.constant.CommonType.*;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorType {

	UNKNOWN_COMMAND("unknown command '%s'"),
	WRONG_NUMBER_OF_ARGUMENTS("wrong number of arguments for '%s' command"),
	SYNTAX_ERROR("syntax error"),
	INVALID_VALUE_TYPE("value is not an integer or out of range"),
	INVALID_RESP_SERIALIZATION_FORMAT("invalid RESP serialization format"),
	;

	private final String message;

	public String getMessage(Object... args) {
		return ERROR_PREFIX + String.format(message, args) + CRLF;
	}

	public void throwException(Object... args) {
		throw new RuntimeException(getMessage(args));
	}
}
