package be.pengo.hibernate_exploration.jointableunique;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "join_unique_owners")
public class JoinUniqueOwner {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "join_unique_owners_seq")
	@SequenceGenerator(name = "join_unique_owners_seq", sequenceName = "join_unique_owners_seq", allocationSize = 1)
	private Long id;

	@Column(nullable = false)
	private String name;

	@OneToMany
	@JoinTable(
			name = "owner_unique_items",
			joinColumns = @JoinColumn(name = "owner_id"),
			inverseJoinColumns = @JoinColumn(name = "item_id", unique = true))
	private Set<JoinUniqueItem> items = new HashSet<>();

	protected JoinUniqueOwner() {
	}

	public JoinUniqueOwner(String name) {
		this.name = name;
	}

	public void addItem(JoinUniqueItem item) {
		items.add(item);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Set<JoinUniqueItem> getItems() {
		return items;
	}

}
