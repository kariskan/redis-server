package org.example.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FirstByteType {

	LENGTH_FIRST_BYTE('$'),
	NULL_FIRST_BYTE('_'),
	SIMPLE_FIRST_BYTE('+'),
	ARRAY_FIRST_BYTE('*'),
	BULK_STRING_FIRST_BYTE('$');

	private final char value;

	public boolean isInvalidFirstByte(char value) {
		return this.value != value;
	}
}
