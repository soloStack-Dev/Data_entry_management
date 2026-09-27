package com.example.dataentry.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Turns exceptions into user-facing responses (spec 04 section 10).
 *
 * <p>Two rules shape this class:
 * <ul>
 *   <li>The browser must never see a stack trace, a SQL message or a credential. Details go to the
 *       application log; the user gets a short, friendly sentence.</li>
 *   <li>The response shape depends on who is asking. An HTMX request expects a small fragment that
 *       can be swapped into the page, while a normal browser request expects a full page. Detecting
 *       the {@code HX-Request} header is the standard way to tell them apart, and branching here
 *       means no controller has to duplicate that logic.</li>
 * </ul>
 *
 * <p>{@code basePackages} deliberately restricts this advice to the application's own controllers.
 * Without it the catch-all below would also intercept Spring Boot's internal {@code /error} dispatch
 * and try to render the error view with the framework's own attributes, which fails.
 */
@ControllerAdvice(basePackages = "com.example.dataentry.controller")
public class GlobalExceptionHandler {

	/** Logged, never shown to the user - this is the audit trail for unexpected failures. */
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * Response header carrying the friendly message to the browser. Read by app.js when HTMX reports
	 * a failed request, so the reason can be shown as a toast without replacing the page content.
	 */
	public static final String HEADER_CLIENT_MESSAGE = "X-App-Error";

	/** A record was addressed by an id that does not exist. */
	@ExceptionHandler(EntryNotFoundException.class)
	public ModelAndView handleEntryNotFound(EntryNotFoundException ex, HttpServletRequest request,
			HttpServletResponse response) {
		// Expected, user-caused condition: log at warn, return 404.
		log.warn("Entry not found: {}", ex.getMessage());
		return respond(request, response, HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/** A business rule was violated, e.g. from-user equal to to-user. */
	@ExceptionHandler(IllegalArgumentException.class)
	public ModelAndView handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request,
			HttpServletResponse response) {
		log.warn("Rejected request: {}", ex.getMessage());
		return respond(request, response, HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	/**
	 * Anything not handled above - for example a database outage. The full exception, including the
	 * stack trace, is logged so it can be diagnosed, but only a neutral message reaches the browser.
	 */
	@ExceptionHandler(Exception.class)
	public ModelAndView handleUnexpected(Exception ex, HttpServletRequest request,
			HttpServletResponse response) {
		log.error("Unexpected failure while handling {} {}", request.getMethod(), request.getRequestURI(), ex);
		return respond(request, response, HttpStatus.INTERNAL_SERVER_ERROR,
				"Something went wrong on the server. Please try again.");
	}

	/**
	 * Builds the response: an alert fragment for HTMX, a full error page otherwise. The status code
	 * is written to the response because {@code ModelAndView} alone cannot carry it.
	 *
	 * <p>The friendly message is repeated in an {@code X-App-Error} header so that app.js can show
	 * it as a toast. That is necessary because HTMX ignores the body of a 4xx/5xx response by
	 * default, which is exactly the behaviour wanted here: a failed delete must not wipe out the
	 * table, so the message is surfaced next to it instead of replacing it.
	 */
	private ModelAndView respond(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
			String message) {
		response.setStatus(status.value());
		response.setHeader(HEADER_CLIENT_MESSAGE, message);

		if (isHtmx(request)) {
			// HTMX swaps this fragment into whatever target declared the request.
			ModelAndView fragment = new ModelAndView("fragments/alerts");
			fragment.addObject("errorMessage", message);
			return fragment;
		}

		// A full browser navigation gets a complete page.
		ModelAndView page = new ModelAndView("error");
		page.addObject("status", status.value());
		page.addObject("message", message);
		return page;
	}

	/**
	 * HTMX sets {@code HX-Request: true} on every request it issues. It cannot be spoofed by a form
	 * post, which is exactly what is wanted here.
	 */
	private boolean isHtmx(HttpServletRequest request) {
		return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
	}

}
