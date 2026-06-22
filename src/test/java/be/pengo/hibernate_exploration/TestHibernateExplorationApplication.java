package be.pengo.hibernate_exploration;

import org.springframework.boot.SpringApplication;

public class TestHibernateExplorationApplication {

	public static void main(String[] args) {
		SpringApplication.from(HibernateExplorationApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
