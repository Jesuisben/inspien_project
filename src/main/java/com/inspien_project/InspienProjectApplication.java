package com.inspien_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// DB 일시적 통제
@SpringBootApplication(excludeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
public class InspienProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(InspienProjectApplication.class, args);
	}

}