package org.example.model;

import java.util.List;

import org.example.constant.CommandType;

public record RedisInputData(CommandType commandType, List<Argument> arguments) {
}
