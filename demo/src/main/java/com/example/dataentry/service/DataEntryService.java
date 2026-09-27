package com.example.dataentry.service;

import java.time.LocalDate;

import com.example.dataentry.dto.DataEntryRequest;
import com.example.dataentry.entity.DataEntry;
import com.example.dataentry.exception.EntryNotFoundException;
import com.example.dataentry.repository.DataEntryRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business layer. Every database read and write in the application happens here, which is what
 * keeps the controllers free of persistence logic (spec 00 section 4 and spec 04 section 2).
 *
 * <p>Two responsibilities are worth calling out:
 * <ul>
 *   <li><b>Name mapping.</b> The form speaks {@code date}, the database speaks {@code entry_date}.
 *       Converting between the {@link DataEntryRequest} DTO and the {@link DataEntry} entity is
 *       centralised in the private {@code toEntity} helper, so the mismatch is handled in exactly
 *       one place.</li>
 *   <li><b>Trimming.</b> {@code @NotBlank} rejects whitespace-only input, but a value like
 *       {@code "  Keyboard  "} still passes. Trimming here means the stored value is what the user
 *       meant, which keeps the search index useful.</li>
 * </ul>
 *
 * <p>{@code open-in-view} is disabled (spec 06), so every method that hands data to a view loads
 * what it needs inside the transaction rather than relying on lazy loading during rendering.
 */
@Service
@Transactional(readOnly = true)
public class DataEntryService {

	/** Number of rows shown per page on the collection table. */
	private static final int DEFAULT_PAGE_SIZE = 10;

	private final DataEntryRepository repository;

	/** Constructor injection: the dependency is explicit and the class stays unit-testable. */
	public DataEntryService(DataEntryRepository repository) {
		this.repository = repository;
	}

	// ---------------------------------------------------------------- create

	/**
	 * Validates and stores a new entry. Bean Validation has already run on the DTO by the time this
	 * is called (the controller is annotated with {@code @Valid}), so this method only has to apply
	 * the business rules and trim the input.
	 */
	@Transactional
	public DataEntry create(DataEntryRequest request) {
		requireDistinctUsers(request);

		DataEntry entry = new DataEntry();
		applyRequest(entry, request);
		return repository.save(entry);
	}

	// ---------------------------------------------------------------- update

	/**
	 * Applies the submitted form to an existing row.
	 *
	 * <p>The record is loaded first and then mutated in place. Reusing the managed instance is what
	 * makes JPA issue an {@code UPDATE} rather than an {@code INSERT}, and it guarantees the id and
	 * {@code created_at} of the original row are preserved.
	 *
	 * @throws EntryNotFoundException if no row has the given id
	 */
	@Transactional
	public DataEntry update(Long id, DataEntryRequest request) {
		requireDistinctUsers(request);

		DataEntry entry = findById(id);
		applyRequest(entry, request);
		return repository.save(entry);
	}

	// ---------------------------------------------------------------- delete

	/**
	 * Removes a row, and treats "already gone" as an error so the user is not told a delete
	 * succeeded when nothing changed.
	 */
	@Transactional
	public void delete(Long id) {
		if (!repository.existsById(id)) {
			throw new EntryNotFoundException("No entry exists with id " + id + ".");
		}
		repository.deleteById(id);
	}

	// ---------------------------------------------------------------- read

	/** Loads one entry or fails loudly; used by the edit form. */
	public DataEntry findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntryNotFoundException("No entry exists with id " + id + "."));
	}

	/**
	 * Returns one page of entries, newest first.
	 *
	 * <p>A blank keyword is a valid and common case - it is simply "no filter" - so it is treated as
	 * {@code findAll} rather than as an error. The caller never has to build two different queries.
	 */
	public Page<DataEntry> findEntries(String search, int page, int size) {
		// Guard against a negative or oversized page coming in from a hand-crafted URL, which would
		// otherwise be passed straight to the SQL LIMIT clause.
		Pageable pageable = PageRequest.of(
				Math.max(page, 0),
				size > 0 ? size : DEFAULT_PAGE_SIZE,
				Sort.by(Sort.Direction.DESC, "id"));

		String keyword = search == null ? "" : search.trim();
		if (keyword.isEmpty()) {
			return repository.findAll(pageable);
		}
		return repository.search(keyword, pageable);
	}

	/** Builds the numbers for the dashboard cards in a single pass. */
	public DashboardStats loadDashboardStats() {
		// LocalDate.now() is evaluated once and reused, so the count query and the "recent" query
		// cannot straddle midnight and disagree with each other.
		LocalDate today = LocalDate.now();
		return new DashboardStats(
				repository.count(),
				repository.countByEntryDate(today),
				repository.findTop5ByOrderByCreatedAtDesc());
	}

	// ---------------------------------------------------------------- mapping helpers

	/**
	 * Business rule from spec 03 section 8: an entry cannot go from a user to that same user.
	 *
	 * <p>It lives in the service rather than as a field-level annotation because it compares two
	 * fields at once, which Bean Validation cannot express on either field alone. The comparison
	 * ignores case and surrounding spaces, so "Bob" and " bob " are treated as the same person.
	 */
	private void requireDistinctUsers(DataEntryRequest request) {
		String from = request.getFromUser() == null ? "" : request.getFromUser().trim();
		String to = request.getToUser() == null ? "" : request.getToUser().trim();
		if (from.equalsIgnoreCase(to)) {
			throw new IllegalArgumentException("From user and to user must be different.");
		}
	}

	/**
	 * Builds a pre-filled form from a stored entity, so the edit page can show existing values.
	 *
	 * <p>This is the reverse of {@link #applyRequest} and, like it, the only place that knows
	 * {@code entryDate} is called {@code date} on the way out. Keeping both directions here is what
	 * makes the column name and the field name free to change independently.
	 */
	public DataEntryRequest toForm(DataEntry entry) {
		DataEntryRequest request = new DataEntryRequest();
		request.setProductName(entry.getProductName());
		request.setDescription(entry.getDescription());
		request.setTiming(entry.getTiming());
		request.setDate(entry.getEntryDate());
		request.setType(entry.getType());
		request.setFromUser(entry.getFromUser());
		request.setToUser(entry.getToUser());
		return request;
	}

	/**
	 * Copies the validated form values onto the entity, normalising whitespace as it goes.
	 *
	 * <p>This is the single place where {@code request.date} becomes {@code entry.entryDate}.
	 */
	private void applyRequest(DataEntry entry, DataEntryRequest request) {
		entry.applyChanges(
				trim(request.getProductName()),
				trim(request.getDescription()),
				request.getTiming(),
				request.getDate(),
				request.getType(),
				trim(request.getFromUser()),
				trim(request.getToUser()));
	}

	/** Null-safe trim; {@code @NotBlank} has already rejected null and blank values. */
	private String trim(String value) {
		return value == null ? null : value.trim();
	}

}
