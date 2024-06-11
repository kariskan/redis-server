package org.example;

import java.io.IOException;
import java.net.ServerSocket;

import org.example.service.RedisRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisApplication {

	public static final int PORT = 8888;

	public static void main(String[] args) {
		log.info("Starting Redis application");
		try (var server = new ServerSocket(PORT)) {
			while (true) {
				var listen = server.accept();
				var redisExecutorThread = new RedisRunner(listen);
				redisExecutorThread.start();
			}
		} catch (IOException ex) {
			log.error("\t* Exception : {} - {}", ex.getClass().getName(), ex.getMessage());
		}
		log.info("client disconnected.");
	}
}
