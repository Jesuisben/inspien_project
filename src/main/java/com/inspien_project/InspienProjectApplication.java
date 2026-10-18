package com.inspien_project;

import org.springframework.scheduling.annotation.EnableScheduling;
import com.inspien_project.common.EnvSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableScheduling
@EnableConfigurationProperties(EnvSettings.class)
@SpringBootApplication(excludeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
public class InspienProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(InspienProjectApplication.class, args);
	}

}