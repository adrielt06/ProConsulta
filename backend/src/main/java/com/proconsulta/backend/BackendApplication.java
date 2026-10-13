package com.proconsulta.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// El servidor corre en UTC; la zona de cada consultorio vive en tenant.timezone (R2).
		// Los tests no pasan por main(): ahí lo fija el argLine de surefire en pom.xml.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(BackendApplication.class, args);
	}

}
