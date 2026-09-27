package com.example.dataentry.controller;

import com.example.dataentry.service.DataEntryService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the dashboard at {@code /} (spec 00 section 2).
 *
 * <p>Kept separate from the other two controllers because it maps exactly one route and has no
 * form handling. It still follows the same rule: the controller gathers what the view needs and
 * delegates every query to the service.
 */
@Controller
public class DashboardController {

	private final DataEntryService service;

	public DashboardController(DataEntryService service) {
		this.service = service;
	}

	/**
	 * @return the {@code index} template, with the three dashboard counters already resolved
	 */
	@GetMapping("/")
	public String home(Model model) {
		// loadDashboardStats issues its queries in one service call, so the view never triggers
		// lazy loading and open-in-view can stay disabled.
		model.addAttribute("stats", service.loadDashboardStats());
		return "index";
	}

}
