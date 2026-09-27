package com.example.dataentry.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.dataentry.entity.EntryType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Form backing object for both the create and the edit form.
 *
 * <p>A DTO is used instead of binding straight onto the entity for two reasons: the browser field
 * is called {@code date} while the entity column is {@code entry_date} (spec 05), and validation
 * annotations belong to the input contract rather than to the database model.
 *
 * <p>Every constraint here duplicates a browser constraint ({@code required}, {@code minlength}, ...)
 * on purpose. Spec 03 section 1 states that server side validation is authoritative, so these
 * annotations - not the HTML - are what actually protects the database.
 */
public class DataEntryRequest {

	@NotBlank(message = "Product name is required.")
	@Size(min = 2, max = 150, message = "Product name must be between 2 and 150 characters.")
	private String productName;

	@NotBlank(message = "Description is required.")
	@Size(max = 2000, message = "Description must not exceed 2000 characters.")
	private String description;

	@NotNull(message = "Timing is required.")
	// An HTML <input type="time"> submits "HH:mm" or "HH:mm:ss"; this tells the binder which
	// format to expect. Without it, binding a bare "09:30" to LocalTime is ambiguous.
	@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
	private LocalTime timing;

	@NotNull(message = "Date is required.")
	// Binds the submitted "date" parameter to this field, and to entity field entryDate.
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate date;

	@NotNull(message = "Type is required.")
	private EntryType type;

	@NotBlank(message = "From user is required.")
	@Size(max = 100, message = "From user must not exceed 100 characters.")
	private String fromUser;

	@NotBlank(message = "To user is required.")
	@Size(max = 100, message = "To user must not exceed 100 characters.")
	private String toUser;

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalTime getTiming() {
		return timing;
	}

	public void setTiming(LocalTime timing) {
		this.timing = timing;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public EntryType getType() {
		return type;
	}

	public void setType(EntryType type) {
		this.type = type;
	}

	public String getFromUser() {
		return fromUser;
	}

	public void setFromUser(String fromUser) {
		this.fromUser = fromUser;
	}

	public String getToUser() {
		return toUser;
	}

	public void setToUser(String toUser) {
		this.toUser = toUser;
	}

}
