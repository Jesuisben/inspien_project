package com.inspien_project;

import com.inspien_project.common.EnvSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// DB 일시적 통제
@EnableConfigurationProperties(EnvSettings.class)
@SpringBootApplication(excludeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
public class InspienProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(InspienProjectApplication.class, args);
	}

}