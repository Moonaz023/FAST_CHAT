package com.example.fast_chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.example.fast_chat.repository.conversation")
@EnableReactiveMongoRepositories(basePackages = "com.example.fast_chat.repository.conversation")
public class FastChatApplication {

	public static void main(String[] args) {
		SpringApplication.run(FastChatApplication.class, args);
	}

}
