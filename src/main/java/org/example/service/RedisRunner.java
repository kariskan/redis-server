package org.example.service;

import static org.example.constant.CommonType.*;
import static org.example.constant.FirstByteType.*;
import static org.example.error.ErrorType.*;
import static org.example.utils.CommandValidator.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.example.utils.CommandValidator;
import org.example.constant.CommandType;
import org.example.model.Argument;
import org.example.model.RedisInputData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class RedisRunner extends Thread {

	private final Socket socket;

	@Override
	public void run() {
		try (
			var br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			var bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
		) {
			String arrayInfo;
			while ((arrayInfo = br.readLine()) != null) {
				RedisInputData redisInputData;
				try {
					if (isJustSpace(arrayInfo)) {
						continue;
					}
					redisInputData = parseInput(inputArguments(arrayInfo, br));
				} catch (Exception e) {
					writeErrorMessage(e, bw);
					bw.flush();
					log.error("\t* Exception : {} - {}", e.getClass().getName(), e.getMessage());
					continue;
				}
				bw.write(new RedisService(redisInputData).executeCommand());
				bw.flush();
			}
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void writeErrorMessage(Exception e, BufferedWriter bw) throws IOException {
		if (e.getMessage().endsWith(CRLF)) {
			bw.write(e.getMessage());
		} else {
			bw.write(e.getMessage() + CRLF);
		}
	}

	private boolean isJustSpace(String arrayInfo) {
		if (arrayInfo.isEmpty()) {
			return true;
		}
		if (ARRAY_FIRST_BYTE.isInvalidFirstByte(arrayInfo.charAt(0))) {
			SYNTAX_ERROR.throwException();
		}
		return false;
	}

	private List<String> inputArguments(String arrayInfo, BufferedReader br) throws IOException {
		int arraySize = Integer.parseInt(arrayInfo.substring(1));

		var arguments = new ArrayList<String>();
		for (int i = 0; i < arraySize; i++) {
			String argument = br.readLine();
			if (argument.isEmpty()) {
				i--;
				continue;
			}
			if (BULK_STRING_FIRST_BYTE.isInvalidFirstByte(argument.charAt(0))) {
				SYNTAX_ERROR.throwException();
			}
			String part = inputPart(br, argument);
			arguments.add(part);
		}
		return arguments;
	}

	private String inputPart(BufferedReader br, String argument) throws IOException {
		var argumentLength = Integer.parseInt(argument.substring(1));
		String part;
		do {
			part = br.readLine();
		} while (part.isEmpty());
		if (part.length() != argumentLength) {
			SYNTAX_ERROR.throwException();
		}
		return part;
	}

	/**
	 * parsing according to RESP serialization format
	 * first '*'+(number) means size of (array of bulk strings)
	 * each array of bulk string consist of ('$'+string length) and (actual string)
	 * all of different parts seperated by newline(CRLF, \r\n)
	 */
	private static RedisInputData parseInput(List<String> list) throws IOException {
		var commandType = CommandType.findByInput(list.get(0));

		if (Objects.isNull(commandType)) {
			UNKNOWN_COMMAND.throwException(list.get(0));
		}

		var subList = list.subList(1, list.size());
		var arguments = new ArrayList<Argument>();
		for (int i = 0; i < subList.size(); i++) {
			if (containIgnoreCase(NEED_MORE_ARGUMENT, subList.get(i))) {
				arguments.add(new Argument(subList.get(i), subList.get(i + 1)));
				i++;
			} else {
				arguments.add(new Argument(subList.get(i), null));
			}
		}
		var redisCommonData = new RedisInputData(commandType, arguments);
		new CommandValidator(redisCommonData).validateCommandArguments();

		return redisCommonData;
	}
}
