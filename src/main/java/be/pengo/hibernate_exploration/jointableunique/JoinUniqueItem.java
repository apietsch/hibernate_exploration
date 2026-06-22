package be.pengo.hibernate_exploration.jointableunique;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "join_unique_items")
public class JoinUniqueItem {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "join_unique_items_seq")
	@SequenceGenerator(name = "join_unique_items_seq", sequenceName = "join_unique_items_seq", allocationSize = 1)
	private Long id;

	@Column(nullable = false)
	private String name;

	protected JoinUniqueItem() {
	}

	public JoinUniqueItem(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

}
