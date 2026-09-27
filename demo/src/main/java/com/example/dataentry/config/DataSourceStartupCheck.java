package com.example.dataentry.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Fails the startup with a readable explanation when the database cannot be reached.
 *
 * <p>Without this check the application starts, Hibernate asks the DataSource for a connection,
 * the connection attempt fails, and Hibernate then reports:
 *
 * <pre>Unable to determine Dialect without JDBC metadata</pre>
 *
 * <p>That message is technically accurate but points at Hibernate, which is not the problem. The
 * real cause - a wrong port, a wrong password, or a database that is not running - appears only as
 * a {@code WARN} line far above the stack trace, and it is easy to miss. This check opens and
 * immediately closes one connection while the DataSource is being created, so the actual reason is
 * reported as the primary cause of a startup failure.
 *
 * <p>It is registered as a {@link BeanPostProcessor} because that guarantees it runs at the right
 * moment: a bean post processor finishes before the bean is handed to whoever requested it, so the
 * DataSource is always validated before JPA is able to use it, no matter which bean triggers its
 * creation. The {@link Bean} method is {@code static} so that this configuration class itself does
 * not have to be instantiated early, which Spring warns about for post processors.
 */
@Configuration
public class DataSourceStartupCheck {

	/**
	 * Registers the check. Passing the {@link Environment} lets the failure message quote the exact
	 * URL and username that were configured, instead of guessing at them.
	 *
	 * <p>The method name must differ from the class name: a {@code @Configuration} class is itself
	 * registered under its own decapitalised name, so a {@code @Bean} method called
	 * {@code dataSourceStartupCheck} would collide with it and fail with "a bean with that name has
	 * already been defined".
	 */
	@Bean
	static BeanPostProcessor dataSourceConnectionVerifier(Environment environment) {
		return new ConnectionVerifyingPostProcessor(environment);
	}

	/** Validates every {@link DataSource} bean once, as it is created. */
	private static final class ConnectionVerifyingPostProcessor implements BeanPostProcessor {

		private static final Logger log = LoggerFactory.getLogger(DataSourceStartupCheck.class);

		private final Environment environment;

		private ConnectionVerifyingPostProcessor(Environment environment) {
			this.environment = environment;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof DataSource dataSource) {
				verify(dataSource);
			}
			return bean;
		}

		/**
		 * Opens one connection to prove the database is reachable and the credentials are accepted.
		 * The connection is closed again straight away; the pool itself is left initialised and
		 * reusable, so this costs one connection rather than a second pool.
		 */
		private void verify(DataSource dataSource) {
			try (Connection connection = dataSource.getConnection()) {
				// Logging is fully initialised by now, unlike during environment preparation, so
				// this is the reliable place to tell the operator which database is in use. The
				// password is never included.
				log.info("Connected to {} as {}", connection.getMetaData().getURL(),
						environment.getProperty("spring.datasource.username", "<not set>"));
			} catch (SQLException ex) {
				throw new IllegalStateException(failureMessage(ex), ex);
			}
		}

		/**
		 * Builds the message shown to the operator. The password is deliberately never included, and
		 * neither is any other value that could be a secret - only the URL, the user name and the
		 * server's own reply.
		 */
		private String failureMessage(SQLException ex) {
			String url = environment.getProperty("spring.datasource.url", "<not set>");
			String username = environment.getProperty("spring.datasource.username", "<not set>");

			return """
					Cannot connect to the database.

					  JDBC URL : %s
					  Username : %s
					  Reason   : %s

					Where the settings come from, highest priority first:

					  1. Environment variables (DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD)
					  2. The .env file next to the application or one directory above it
					  3. The portable defaults in application.properties (host localhost, port 3306)

					The most common cause is a port mismatch. If .env publishes MySQL on
					DB_HOST_PORT, that value is what the application must use on the host. The URL
					above already shows the port that was actually tried, so compare it with .env:

					  docker compose ps        shows the published database port
					  docker compose logs app  shows what the container was given

					Both of these override .env if you need a one-off change:

					  $env:DB_PORT="3310"
					  $env:SERVER_PORT="8082"

					See RUN_COMMANDS.txt section 6, or start everything through Docker instead:

					  docker compose up -d
					""".formatted(url, username, rootReason(ex));
		}

		/**
		 * Uses the deepest message in the chain. Connector/J wraps the server's reply, so the useful
		 * text ("Access denied for user ...") sits a few levels down rather than at the top.
		 */
		private String rootReason(Throwable ex) {
			Throwable cause = ex;
			while (cause.getCause() != null && cause.getCause() != cause) {
				cause = cause.getCause();
			}
			return cause.getMessage() == null ? ex.getMessage() : cause.getMessage();
		}

	}

}
