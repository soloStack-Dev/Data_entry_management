package com.example.dataentry.controller;

import com.example.dataentry.dto.DataEntryRequest;
import com.example.dataentry.service.DataEntryService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Handles the create and edit form (spec 06 section 5):
 * {@code GET/POST /data-entry} and {@code GET/POST /data-entry/edit/{id}}.
 *
 * <p>Each GET returns a full page; each POST returns only the form fragment, because the browser
 * reaches them through HTMX. Validation failures re-render that same fragment with the submitted
 * values and the error messages, so the user never loses their typing and no page reload happens.
 */
@Controller
public class DataEntryController {

	/**
	 * View name of the form fragment. Because the fragment file has a single root element, the same
	 * name works both as a Thymeleaf view (for the POST responses) and as an include (for the
	 * full pages).
	 */
	private static final String FORM_VIEW = "fragments/data-entry-form";

	private final DataEntryService service;

	public DataEntryController(DataEntryService service) {
		this.service = service;
	}

	// ------------------------------------------------------------------ create

	/** Renders the blank create form. */
	@GetMapping("/data-entry")
	public String showCreateForm(Model model) {
		prepareFormModel(model, new DataEntryRequest(), "/data-entry", "New data entry", "Save Entry", false);
		return "data-entry";
	}

	/**
	 * Handles the create submission.
	 *
	 * @param bindingResult holds the outcome of the Bean Validation annotations on the DTO.
	 *        Declaring it immediately after the form argument stops Spring from throwing on invalid
	 *        input, which is what allows the form to be re-rendered with the errors attached.
	 */
	@PostMapping("/data-entry")
	public String create(@Valid @ModelAttribute("entryForm") DataEntryRequest form, BindingResult bindingResult,
			Model model, HttpServletResponse response) {
		// Step 1 - field validation. Checked before the service is called, so an invalid payload is
		// never handed to the database. Server-side validation is authoritative (spec 03 section 1):
		// the browser may have let this through, and these are the messages shown beside each field.
		if (bindingResult.hasFieldErrors()) {
			prepareFormModel(model, form, "/data-entry", "New data entry", "Save Entry", false);
			return FORM_VIEW;
		}

		// Step 2 - business rules. A rule spanning two fields cannot be a field-level constraint, so
		// the service reports it as an IllegalArgumentException, which is attached to the
		// BindingResult and rendered at the top of the form with the input intact. The same exception
		// still reaches GlobalExceptionHandler on the routes that have no form to re-render.
		try {
			service.create(form);
		}
		catch (IllegalArgumentException ex) {
			bindingResult.reject("business.rule", ex.getMessage());
			prepareFormModel(model, form, "/data-entry", "New data entry", "Save Entry", false);
			return FORM_VIEW;
		}

		// Step 3 - success. Swap in an empty form plus a confirmation, so the next record can be
		// typed immediately. The HX-Trigger event lets any other part of the page refresh itself.
		response.setHeader("HX-Trigger", "entryCreated");
		prepareFormModel(model, new DataEntryRequest(), "/data-entry", "New data entry", "Save Entry", false);
		model.addAttribute("successMessage", "Entry saved successfully.");
		return FORM_VIEW;
	}

	// ------------------------------------------------------------------ edit

	/** Renders the create form pre-filled with an existing record. */
	@GetMapping("/data-entry/edit/{id}")
	public String showEditForm(@PathVariable Long id, Model model) {
		// toForm performs the entryDate -> date mapping; a missing id raises EntryNotFoundException,
		// which the global handler turns into a friendly 404.
		prepareFormModel(model, service.toForm(service.findById(id)), "/data-entry/edit/" + id,
				"Edit entry #" + id, "Update Entry", true);
		return "data-entry";
	}

	/** Applies the submitted values to an existing record. */
	@PostMapping("/data-entry/edit/{id}")
	public String update(@PathVariable Long id, @Valid @ModelAttribute("entryForm") DataEntryRequest form,
			BindingResult bindingResult, Model model, HttpServletResponse response) {
		String action = "/data-entry/edit/" + id;
		String title = "Edit entry #" + id;

		// Same two-step guard as create(): field validation first, then business rules. Both failure
		// paths re-render the form fragment with the submitted values and the messages attached.
		if (bindingResult.hasFieldErrors()) {
			prepareFormModel(model, form, action, title, "Update Entry", true);
			return FORM_VIEW;
		}

		try {
			service.update(id, form);
		}
		catch (IllegalArgumentException ex) {
			bindingResult.reject("business.rule", ex.getMessage());
			prepareFormModel(model, form, action, title, "Update Entry", true);
			return FORM_VIEW;
		}

		// The record is saved, but the user belongs on the collection page now. HX-Redirect is the
		// HTMX way of saying "perform a real browser navigation to this URL", which keeps the
		// address bar and the back button in sync - a plain redirect would not be followed by HTMX.
		response.setHeader("HX-Redirect", "/collection");
		return null;
	}

	// ------------------------------------------------------------------ shared

	/**
	 * Fills in everything the form fragment needs, whether it is being included in a full page or
	 * returned on its own. Having one method for both cases is what guarantees a page load and a
	 * failed HTMX submit render exactly the same markup.
	 */
	private void prepareFormModel(Model model, DataEntryRequest form, String action, String title,
			String submitLabel, boolean editing) {
		model.addAttribute("entryForm", form);
		model.addAttribute("formAction", action);
		model.addAttribute("formTitle", title);
		model.addAttribute("submitLabel", submitLabel);
		model.addAttribute("editing", editing);
	}

}
