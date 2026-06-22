package be.pengo.hibernate_exploration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class HibernateExplorationApplicationTests {

	@Test
	void contextLoads() {
	}

}
