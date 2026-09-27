package com.example.dataentry.repository;

import java.time.LocalDate;
import java.util.List;

import com.example.dataentry.entity.DataEntry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link DataEntry}.
 *
 * <p>Extending {@code JpaRepository} supplies the whole CRUD surface plus {@code findAll(Pageable)}
 * for free. Every query below is either a derived method name or a JPQL string, so there is no
 * concatenated SQL anywhere in the application (spec 03 section 11): the keyword is always bound as
 * a prepared-statement parameter, so user input can never be interpreted as SQL.
 */
public interface DataEntryRepository extends JpaRepository<DataEntry, Long> {

	/**
	 * Free-text search for the collection page, which spec 01 section 6 requires to filter by
	 * product name and the other text fields. Written as JPQL so the {@code OR} grouping is
	 * explicit and the ordering is defined in one visible place (spec 04 section 5 recommends a
	 * query for multi-field search).
	 *
	 * <p>Passing a {@link Pageable} returns one slice of the matches plus the total number of
	 * matches, which is what the pagination controls need in order to render page numbers.
	 */
	@Query("""
			select e from DataEntry e
			where lower(e.productName) like lower(concat('%', :keyword, '%'))
			   or lower(e.description) like lower(concat('%', :keyword, '%'))
			""")
	Page<DataEntry> search(@Param("keyword") String keyword, Pageable pageable);

	/** Dashboard card: how many entries exist for a given day. */
	long countByEntryDate(LocalDate date);

	/** Dashboard card: the newest records, shown as "Recent entries". */
	List<DataEntry> findTop5ByOrderByCreatedAtDesc();

}
