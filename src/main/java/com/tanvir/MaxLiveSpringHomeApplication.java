package com.tanvir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MaxLiveSpringHomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(MaxLiveSpringHomeApplication.class, args);
	}

}


