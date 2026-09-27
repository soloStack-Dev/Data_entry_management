package com.example.dataentry.config;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Contributes values that every page needs, so the individual controllers stay free of them.
 *
 * <p>Today this is only {@code currentPath}, the URI of the incoming request, which the navbar uses
 * to mark the active link. Doing it once here means no page has to declare the attribute, and no
 * controller has to remember to add it.
 */
@ControllerAdvice
public class NavigationModelAdvice {

	/**
	 * Runs before every controller handler. {@code @ModelAttribute} on a {@code @ControllerAdvice}
	 * method applies globally, and the returned value is available in every Thymeleaf template.
	 */
	@ModelAttribute("currentPath")
	public String currentPath(HttpServletRequest request) {
		// The plain request URI, e.g. "/" or "/data-entry". Query parameters are excluded, which is
		// what the navbar wants: /collection?page=2 should still highlight "Collection".
		return request.getRequestURI();
	}

}
