package be.pengo.hibernate_exploration.jointableunique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import be.pengo.hibernate_exploration.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NonUniqueResultException;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.format_sql=true",
		"logging.level.org.hibernate.SQL=DEBUG",
		"logging.level.org.hibernate.orm.jdbc.bind=TRACE"
})
class JoinTableUniqueJoinColumnTests {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Test
	void uniqueInverseJoinColumnIsEnforcedByTheDatabaseOnFlush() {
		ScenarioIds ids = createValidScenario();

		Throwable thrown = catchThrowable(() -> transactionTemplate.executeWithoutResult(status -> {
			entityManager.clear();

			JoinUniqueOwner secondOwner = entityManager.find(JoinUniqueOwner.class, ids.secondOwnerId());
			JoinUniqueItem sharedItem = entityManager.find(JoinUniqueItem.class, ids.sharedItemId());

			secondOwner.addItem(sharedItem);
			entityManager.flush();
		}));

		assertThat(thrown).isNotNull();

		ConstraintViolationException hibernateException = findCause(thrown, ConstraintViolationException.class)
			.orElseThrow();
		assertThat(hibernateException.getConstraintName()).isNotBlank();

		SQLException sqlException = findCause(thrown, SQLException.class).orElseThrow();
		assertThat(sqlException.getErrorCode()).isEqualTo(1);
		assertThat(sqlException.getMessage()).contains("ORA-00001");
	}

	@Test
	@DirtiesContext
	void singleResultQueryThrowsWhenCorruptedJoinTableReturnsTwoOwnersForOneItem() {
		ScenarioIds ids = createValidScenario();

		addDuplicateJoinTableRowAfterDroppingGeneratedUniqueConstraint(ids);

		Throwable thrown = catchThrowable(() -> transactionTemplate.execute(status -> {
			entityManager.clear();

			return entityManager.createQuery("""
					select owner
					from JoinUniqueOwner owner
					join owner.items item
					where item.id = :itemId
					""", JoinUniqueOwner.class)
				.setParameter("itemId", ids.sharedItemId())
				.getSingleResult();
		}));

		assertThat(thrown).isInstanceOf(NonUniqueResultException.class);
		assertThat(thrown).hasMessage("Query did not return a unique result: 2 results were returned");
	}

	@Test
	@DirtiesContext
	void corruptedJoinTableRowsAreNotDetectedWhenReadingTheCollections() {
		ScenarioIds ids = createValidScenario();

		addDuplicateJoinTableRowAfterDroppingGeneratedUniqueConstraint(ids);

		ReadResult result = transactionTemplate.execute(status -> {
			entityManager.clear();

			JoinUniqueOwner firstOwner = entityManager.find(JoinUniqueOwner.class, ids.firstOwnerId());
			JoinUniqueOwner secondOwner = entityManager.find(JoinUniqueOwner.class, ids.secondOwnerId());

			JoinUniqueItem firstOwnerItem = firstOwner.getItems().iterator().next();
			JoinUniqueItem secondOwnerItem = secondOwner.getItems().iterator().next();

			return new ReadResult(
					firstOwner.getItems().size(),
					secondOwner.getItems().size(),
					firstOwnerItem.getId(),
					secondOwnerItem.getId());
		});

		assertThat(result.firstOwnerItemCount()).isEqualTo(1);
		assertThat(result.secondOwnerItemCount()).isEqualTo(1);
		assertThat(result.firstOwnerItemId()).isEqualTo(ids.sharedItemId());
		assertThat(result.secondOwnerItemId()).isEqualTo(ids.sharedItemId());
	}

	private void addDuplicateJoinTableRowAfterDroppingGeneratedUniqueConstraint(ScenarioIds ids) {
		dropGeneratedUniqueConstraintOnItemId();
		jdbcTemplate.update(
				"insert into owner_unique_items (owner_id, item_id) values (?, ?)",
				ids.secondOwnerId(),
				ids.sharedItemId());
	}

	private ScenarioIds createValidScenario() {
		return transactionTemplate.execute(status -> {
			JoinUniqueItem sharedItem = new JoinUniqueItem("shared item");
			JoinUniqueOwner firstOwner = new JoinUniqueOwner("first owner");
			JoinUniqueOwner secondOwner = new JoinUniqueOwner("second owner");

			firstOwner.addItem(sharedItem);

			entityManager.persist(sharedItem);
			entityManager.persist(firstOwner);
			entityManager.persist(secondOwner);
			entityManager.flush();

			return new ScenarioIds(firstOwner.getId(), secondOwner.getId(), sharedItem.getId());
		});
	}

	private void dropGeneratedUniqueConstraintOnItemId() {
		String constraintName = findUniqueConstraintOnItemId()
			.orElseThrow(() -> new IllegalStateException("No generated unique/primary key constraint found on OWNER_UNIQUE_ITEMS.ITEM_ID"));

		jdbcTemplate.execute("alter table owner_unique_items drop constraint " + constraintName);
	}

	private Optional<String> findUniqueConstraintOnItemId() {
		List<String> constraintNames = jdbcTemplate.queryForList("""
				select uc.constraint_name
				from user_constraints uc
				join user_cons_columns ucc
					on uc.constraint_name = ucc.constraint_name
				where uc.table_name = 'OWNER_UNIQUE_ITEMS'
				  and ucc.column_name = 'ITEM_ID'
				  and uc.constraint_type in ('P', 'U')
				""", String.class);

		return constraintNames.stream().findFirst();
	}

	private static <T extends Throwable> Optional<T> findCause(Throwable throwable, Class<T> type) {
		return exceptionChain(throwable).stream()
			.filter(type::isInstance)
			.map(type::cast)
			.findFirst();
	}

	private static List<Throwable> exceptionChain(Throwable throwable) {
		List<Throwable> chain = new ArrayList<>();
		Throwable current = throwable;
		while (current != null) {
			chain.add(current);
			current = current.getCause();
		}
		return chain;
	}

	private record ScenarioIds(Long firstOwnerId, Long secondOwnerId, Long sharedItemId) {
	}

	private record ReadResult(int firstOwnerItemCount, int secondOwnerItemCount, Long firstOwnerItemId, Long secondOwnerItemId) {
	}

}
