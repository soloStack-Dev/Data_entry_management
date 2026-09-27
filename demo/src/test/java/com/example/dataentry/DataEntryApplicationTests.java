package com.example.dataentry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Context load smoke test.
 *
 * <p>Because {@code mysql-connector-j} and {@code spring-boot-starter-data-jpa} are on the
 * classpath, starting the application context opens a real datasource. There is no embedded
 * database in this project, so the MySQL container has to be reachable first:
 *
 * <pre>
 *   docker compose up -d mysql
 *   .\mvnw.cmd test
 * </pre>
 *
 * <p>With the container stopped this test fails with
 * {@code DataSourceBeanCreationException: Failed to determine a suitable driver class} or a
 * connection refused error. That is an environment problem, not a code problem.
 */
@SpringBootTest
class DataEntryApplicationTests {

	@Test
	void contextLoads() {
	}

}
