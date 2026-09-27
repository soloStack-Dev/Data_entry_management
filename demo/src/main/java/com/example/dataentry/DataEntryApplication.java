package com.example.dataentry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the Data Entry Management Platform.
 *
 * <p>{@code @SpringBootApplication} bundles three annotations:
 * <ul>
 *   <li>{@code @Configuration} - this class can declare beans.</li>
 *   <li>{@code @EnableAutoConfiguration} - lets Spring Boot configure JPA, Thymeleaf, MVC and the
 *       datasource automatically based on the classpath and {@code application.properties}.</li>
 *   <li>{@code @ComponentScan} - scans this package and all sub-packages for
 *       {@code @Controller}, {@code @Service}, {@code @Repository} and {@code @Component}
 *       classes. This is why every layer of the app must live under
 *       {@code com.example.dataentry} - anything placed elsewhere is invisible to Spring.</li>
 * </ul>
 */
@SpringBootApplication
public class DataEntryApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataEntryApplication.class, args);
	}

}
