package com.example.dataentry.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Persistent representation of one data entry row.
 *
 * <p>The table and column layout follows spec 05 section 4 exactly, including the three indexes on
 * {@code product_name}, {@code entry_date} and {@code type} that the search and listing queries rely
 * on. {@code ddl-auto=update} creates the table on first start, so the schema in the spec and the
 * schema in the database stay in step without a manual {@code CREATE TABLE}.
 *
 * <p>Note the deliberate name difference from the form: the browser submits a field called
 * {@code date}, but the entity field is {@code entryDate} and the column is {@code entry_date}. The
 * translation happens once, in {@code DataEntryService}, so this class never has to deal with the
 * form's naming.
 *
 * <p>The class has no setters for {@code createdAt} / {@code updatedAt} on purpose; those are
 * maintained by the database-visible callbacks below and must not be settable from user input.
 */
@Entity
@Table(
		name = "data_entries",
		indexes = {
				@Index(name = "idx_product_name", columnList = "product_name"),
				@Index(name = "idx_entry_date", columnList = "entry_date"),
				@Index(name = "idx_type", columnList = "type")
		})
public class DataEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_name", nullable = false, length = 150)
	private String productName;

	@Column(name = "description", nullable = false, length = 2000)
	private String description;

	/** Entry time of day, stored as MySQL {@code TIME}. */
	@Column(name = "timing", nullable = false)
	private LocalTime timing;

	/** The entry date, stored as MySQL {@code DATE}. Bound to the form's {@code date} field. */
	@Column(name = "entry_date", nullable = false)
	private LocalDate entryDate;

	/**
	 * Stored as the enum constant name (PURCHASE, SALE, ...) in a {@code varchar(50)} column, as
	 * spec 05 requires. {@link EntryTypeConverter} performs the conversion, so no
	 * {@code @Enumerated} is used here.
	 */
	@Column(name = "type", nullable = false, length = 50)
	private EntryType type;

	@Column(name = "from_user", nullable = false, length = 100)
	private String fromUser;

	@Column(name = "to_user", nullable = false, length = 100)
	private String toUser;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * Stamps {@code createdAt} the first time the row is inserted. Done in Java rather than with a
	 * database {@code DEFAULT CURRENT_TIMESTAMP} so the value always comes from the same clock as
	 * the rest of the application.
	 */
	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	/** Refreshes {@code updatedAt} on every subsequent save. */
	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	/**
	 * Copy constructor used when editing: it copies every user supplied field onto an already
	 * persisted instance, so JPA issues an UPDATE instead of inserting a second row. The id and
	 * both timestamps are carried over from the existing record.
	 */
	public void applyChanges(String productName, String description, LocalTime timing, LocalDate entryDate,
			EntryType type, String fromUser, String toUser) {
		this.productName = productName;
		this.description = description;
		this.timing = timing;
		this.entryDate = entryDate;
		this.type = type;
		this.fromUser = fromUser;
		this.toUser = toUser;
	}

	public Long getId() {
		return id;
	}

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

	public LocalDate getEntryDate() {
		return entryDate;
	}

	public void setEntryDate(LocalDate entryDate) {
		this.entryDate = entryDate;
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

}
