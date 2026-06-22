package be.pengo.hibernate_exploration.uniquecolumn;

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
import org.springframework.transaction.annotation.Transactional;

import be.pengo.hibernate_exploration.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.format_sql=true",
		"logging.level.org.hibernate.SQL=DEBUG",
		"logging.level.org.hibernate.orm.jdbc.bind=TRACE"
})
@Transactional
class UniqueColumnAnnotationTests {

	@Autowired
	private EntityManager entityManager;

	@Test
	void columnUniqueTrueIsEnforcedByTheDatabaseOnFlush() {
		entityManager.persist(new UniqueEmailAccount("same-address@example.test"));
		entityManager.flush();
		entityManager.clear();

		entityManager.persist(new UniqueEmailAccount("same-address@example.test"));

		Throwable thrown = catchThrowable(entityManager::flush);

		assertThat(thrown).isNotNull();

		ConstraintViolationException hibernateException = findCause(thrown, ConstraintViolationException.class)
			.orElseThrow();
		assertThat(hibernateException.getConstraintName()).isNotBlank();

		SQLException sqlException = findCause(thrown, SQLException.class).orElseThrow();
		assertThat(sqlException.getErrorCode()).isEqualTo(1);
		assertThat(sqlException.getMessage()).contains("ORA-00001");
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

}
