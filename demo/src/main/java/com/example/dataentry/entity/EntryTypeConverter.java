package com.example.dataentry.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores an {@link EntryType} as a plain string.
 *
 * <p>Without this, {@code @Enumerated(EnumType.STRING)} makes Hibernate generate a native MySQL
 * {@code ENUM('PURCHASE', ...)} column. That would work, but it is not the schema in spec 05,
 * which asks for {@code type VARCHAR(50)}, and a native enum needs a DDL change whenever a value is
 * added. Converting to a string instead keeps the column a {@code varchar(50)} that matches the
 * specification and can hold any new value without altering the table.
 *
 * <p>{@code autoApply = true} means the converter is used for every {@code EntryType} attribute
 * without any further annotation.
 */
@Converter(autoApply = true)
public class EntryTypeConverter implements AttributeConverter<EntryType, String> {

	/**
	 * Entity to database. {@code name()} is used rather than {@code toString()} because the stored
	 * value must stay a valid identifier, matching the {@code label} only for display.
	 */
	@Override
	public String convertToDatabaseColumn(EntryType attribute) {
		return attribute == null ? null : attribute.name();
	}

	/**
	 * Database to entity. {@code valueOf} restores the constant, and throws if the database holds a
	 * value this build of the application does not know - which is the correct outcome, because
	 * silently defaulting to {@code OTHER} would hide corrupt data.
	 */
	@Override
	public EntryType convertToEntityAttribute(String dbData) {
		return dbData == null ? null : EntryType.valueOf(dbData);
	}

}
