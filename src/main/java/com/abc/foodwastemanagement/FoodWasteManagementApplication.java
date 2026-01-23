package com.abc.foodwastemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.abc.foodwastemanagement.ratelimit.RateLimitingProperties;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
@EnableConfigurationProperties(RateLimitingProperties.class)
public class FoodWasteManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodWasteManagementApplication.class, args);
	}

	// For @Transactional implementation
	@Bean
	public PlatformTransactionManager MongoDBFactory(MongoDatabaseFactory dbFactory) {
		return new MongoTransactionManager(dbFactory);
	}

	@Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
