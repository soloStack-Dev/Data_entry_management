package com.example.dataentry.controller;

import com.example.dataentry.entity.DataEntry;
import com.example.dataentry.service.DataEntryService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles the collection page and its HTMX interactions (spec 06 section 5):
 * {@code GET /collection}, {@code GET /collection/search?search=} and
 * {@code DELETE /collection/{id}}.
 *
 * <p>Only the first route returns a full page. The other two return the table fragment alone, so
 * HTMX replaces just the table and the search field keeps focus while the user types
 * (spec 01 section 6, spec 04 section 6).
 */
@Controller
public class CollectionController {

	/** Single root element of the table fragment; usable both as a view and as an include. */
	private static final String TABLE_VIEW = "fragments/data-table";

	/** Rows per page. Small enough to stay readable, large enough to need pagination. */
	private static final int PAGE_SIZE = 10;

	private final DataEntryService service;

	public CollectionController(DataEntryService service) {
		this.service = service;
	}

	/** Renders the full collection page with the first page of rows. */
	@GetMapping("/collection")
	public String collection(@RequestParam(name = "search", required = false) String search,
			@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
		prepareTableModel(model, search, page);
		return "collection";
	}

	/**
	 * The HTMX search endpoint. Typing in the search box re-requests the table with the keyword; the
	 * page always restarts at 0 because page 3 of a new result set is rarely what the user means.
	 */
	@GetMapping("/collection/search")
	public String search(@RequestParam(name = "search", required = false) String search,
			@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
		prepareTableModel(model, search, page);
		return TABLE_VIEW;
	}

	/**
	 * Deletes one row and returns the refreshed table.
	 *
	 * <p>The current search term and page number are sent along with the delete request so the
	 * re-rendered table keeps the user in the same view they deleted from - deleting the only row of
	 * page 2 should not silently throw them onto an empty page.
	 */
	@DeleteMapping("/collection/{id}")
	public String delete(@PathVariable Long id, @RequestParam(name = "search", required = false) String search,
			@RequestParam(name = "page", defaultValue = "0") int page, Model model,
			HttpServletResponse response) {
		service.delete(id);

		// HX-Trigger with a JSON payload broadcasts a DOM event that app.js listens for, which is
		// how the confirmation toast is shown without needing a second swap target on the page.
		response.setHeader("HX-Trigger", "{\"entryDeleted\":{\"message\":\"Entry deleted successfully.\"}}");

		prepareTableModel(model, search, page);
		return TABLE_VIEW;
	}

	/**
	 * Shared model for the page, the search fragment and the post-delete refresh. All three must show
	 * identical markup, so they all go through this one method.
	 */
	private void prepareTableModel(Model model, String search, int page) {
		Page<DataEntry> entries = service.findEntries(search, page, PAGE_SIZE);
		model.addAttribute("entries", entries);
		// Normalised so the search input and the pagination links always have a defined value,
		// instead of relying on Thymeleaf's null handling in a dozen places.
		model.addAttribute("search", search == null ? "" : search.trim());
		model.addAttribute("pageSize", PAGE_SIZE);
	}

}
