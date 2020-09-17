package com.usaa.sonarqualitygates.test.success;

import java.io.File;
import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SuccessApplication {

	public static void main(String[] args) {
		SpringApplication.run(SuccessApplication.class, args);
		try {
			File tempDir = File.createTempFile("", ".");
			tempDir.delete();
			tempDir.mkdir(); // Noncompliant
		} catch (IOException e) {
		}
	}
}
