package com.example.dataentry.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Makes {@code .mvnw spring-boot:run} behave like {@code docker compose up} by loading the
 * repository's root {@code .env} file into the Spring {@link org.springframework.core.env.Environment}.
 *
 * <p>Docker Compose reads {@code .env} automatically; the application did not, which meant a
 * developer running the app from Maven had to export every value by hand. The most expensive
 * consequence was silent: the portable default port 3306 was reached instead of the port the
 * database is actually published on, the native MySQL server answered, and the failure surfaced as
 * an authentication error against the wrong instance rather than as a configuration mistake.
 *
 * <h2>Precedence</h2>
 *
 * The {@code .env} file is added with {@link ConfigurableEnvironment#getPropertySources() addLast},
 * which gives it the <em>lowest</em> priority. Real environment variables always win, so:
 *
 * <ul>
 *   <li>Docker Compose keeps working, because it passes {@code DB_HOST} and {@code DB_PORT} as real
 *       environment variables for the application container.</li>
 *   <li>A production deployment that supplies normal environment variables is unaffected.</li>
 *   <li>CI without a {@code .env} file is unaffected - the lookup simply finds nothing.</li>
 * </ul>
 *
 * <p>Nothing is written to the repository and no secret is ever logged: the file is read at runtime
 * only, and {@code .env} remains git-ignored.
 *
 * <h2>Name mapping</h2>
 *
 * {@code .env} describes the <em>host side</em> of the Docker stack, so it uses
 * {@code DB_HOST_PORT} and {@code APP_PORT} where the application expects {@code DB_PORT} and
 * {@code SERVER_PORT}. Those are mapped here rather than by duplicating the value in the file,
 * because a second copy in {@code .env} would be free to drift out of sync.
 *
 * <h2>Switching it off</h2>
 *
 * Set {@code app.dotenv.enabled=false} to disable the whole mechanism, or
 * {@code app.dotenv.file=/some/path} to load a different file.
 *
 * <p>Registered in {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/** Set to {@code false} to ignore the {@code .env} file entirely. */
	public static final String ENABLED_PROPERTY = "app.dotenv.enabled";

	/** Overrides the location of the {@code .env} file. */
	public static final String FILE_PROPERTY = "app.dotenv.file";

	/**
	 * Runs last so that {@code application.properties} is already loaded. That matters for two
	 * reasons: the {@code app.dotenv.*} switches above can be read, and the property source is
	 * appended to a fully built list rather than being pre-empted by config data.
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!environment.getProperty(ENABLED_PROPERTY, Boolean.class, Boolean.TRUE)) {
			log().debug("{} is false - not loading any .env file", ENABLED_PROPERTY);
			return;
		}

		Path file = locate(environment);
		if (file == null) {
			// Expected on CI and in production. Not a problem, so it is not logged at INFO.
			log().debug("No .env file found next to the working directory - using environment "
					+ "variables and the defaults in application.properties only");
			return;
		}

		Map<String, Object> values = withAliases(parse(file));
		if (values.isEmpty()) {
			return;
		}

		// addLast() is what makes real environment variables win. Do not change this to addFirst().
		environment.getPropertySources()
				.addLast(new MapPropertySource(SOURCE_NAME + ":" + file.getFileName(), values));

		// DEBUG, not INFO: this runs while the environment is being prepared, which is before the
		// logging system is initialised, so an INFO message here is silently dropped. The effective
		// configuration is reported instead by DataSourceStartupCheck, once logging is up.
		// Names only, never values, so that a password in .env cannot reach the log.
		log().debug("Loaded {} setting(s) from {}: {}", values.size(), file, new ArrayList<>(values.keySet()));
	}

	// ------------------------------------------------------------------ locating the file

	/**
	 * Looks for {@code .env} in the working directory and then one level up.
	 *
	 * <p>The parent lookup is what makes Maven work: {@code ./mvnw.cmd spring-boot:run} is executed
	 * from {@code demo/}, but {@code .env} lives at the repository root next to
	 * {@code docker-compose.yml}. Running the jar from the repository root finds it directly.
	 */
	private Path locate(ConfigurableEnvironment environment) {
		String configured = environment.getProperty(FILE_PROPERTY);
		if (StringUtils.hasText(configured)) {
			Path explicit = Paths.get(configured).toAbsolutePath();
			return Files.isRegularFile(explicit) ? explicit : null;
		}

		Path working = Paths.get(environment.getProperty("user.dir", System.getProperty("user.dir", ".")));
		for (Path directory : List.of(working, parentOf(working))) {
			if (directory == null) {
				continue;
			}
			Path candidate = directory.resolve(".env");
			if (Files.isRegularFile(candidate)) {
				return candidate.toAbsolutePath();
			}
		}
		return null;
	}

	private Path parentOf(Path path) {
		Path parent = path.toAbsolutePath().getParent();
		return parent == null ? null : parent.normalize();
	}

	// ------------------------------------------------------------------ parsing

	/**
	 * Parses the small subset of the {@code .env} format that this project uses: {@code KEY=VALUE}
	 * one per line, {@code #} comments, optional {@code export}, and optional quoting.
	 *
	 * <p>Values are intentionally <em>not</em> unescaped or interpolated. A password is taken
	 * literally so that a value containing {@code $} cannot be expanded.
	 */
	private Map<String, Object> parse(Path file) {
		Map<String, Object> values = new LinkedHashMap<>();
		List<String> lines;
		try {
			lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new UncheckedIOException("Cannot read " + file, ex);
		}

		for (String raw : lines) {
			String line = stripByteOrderMark(raw).trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			if (line.startsWith("export ")) {
				line = line.substring("export ".length()).trim();
			}

			int separator = line.indexOf('=');
			if (separator <= 0) {
				continue;
			}

			String key = line.substring(0, separator).trim();
			String value = unquote(line.substring(separator + 1).trim());
			if (isValidKey(key)) {
				values.put(key, value);
			}
		}
		return values;
	}

	/** Removes a UTF-8 BOM, which some Windows editors add to the first line of a file. */
	private String stripByteOrderMark(String line) {
		return line.startsWith("\uFEFF") ? line.substring(1) : line;
	}

	private String unquote(String value) {
		if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
			return value.substring(1, value.length() - 1);
		}
		if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
			return value.substring(1, value.length() - 1);
		}
		// An unquoted value may carry a trailing "# comment", which Docker Compose strips.
		// Matching that rule keeps the application and Compose reading the same password.
		int comment = value.indexOf(" #");
		return comment >= 0 ? value.substring(0, comment).trim() : value;
	}

	/** Rejects anything that cannot be a property name, rather than failing the whole file. */
	private boolean isValidKey(String key) {
		if (!Character.isLetterOrDigit(key.charAt(0)) && key.charAt(0) != '_') {
			return false;
		}
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '_' && c != '.') {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ name mapping

	/**
	 * Each rule lists the property the application reads, followed by the {@code .env} names that
	 * may supply it in order of preference. The first name actually present wins.
	 */
	private static final List<String[]> ALIASES = List.of(
			new String[] { "DB_PORT", "DB_HOST_PORT" },
			new String[] { "SERVER_PORT", "MAVEN_APP_PORT", "APP_PORT" });

	/**
	 * Copies the host-side names onto the names the application reads, without ever overwriting a
	 * value the file already defines itself.
	 */
	private Map<String, Object> withAliases(Map<String, Object> values) {
		Map<String, Object> result = new LinkedHashMap<>(values);
		for (String[] rule : ALIASES) {
			String target = rule[0];
			if (result.containsKey(target)) {
				continue;
			}
			for (int i = 1; i < rule.length; i++) {
				String source = rule[i];
				if (result.containsKey(source)) {
					result.put(target, result.get(source));
					break;
				}
			}
		}
		return result;
	}

	private static final String SOURCE_NAME = "dotEnvFile";

	private static Logger log() {
		return LoggerFactory.getLogger(DotEnvEnvironmentPostProcessor.class);
	}

}
