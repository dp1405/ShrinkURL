package org.stir.shrinkurl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.stir.shrinkurl.repository")
@EnableMongoRepositories(basePackages = "org.stir.shrinkurl.repository")
public class ShrinkurlApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShrinkurlApplication.class, args);
	}
}