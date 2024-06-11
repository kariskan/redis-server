package org.example.model;

import java.time.LocalDateTime;

public record RedisValue(
	String value,
	LocalDateTime createdAt,
	Long epochTTL,
	LocalDateTime dueDateTTL
) {
}
