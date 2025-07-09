package org.stir.shrinkurl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class ShrinkurlApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShrinkurlApplication.class, args);
	}
}
