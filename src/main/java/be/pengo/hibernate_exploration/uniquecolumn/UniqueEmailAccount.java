package be.pengo.hibernate_exploration.uniquecolumn;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "unique_email_accounts")
public class UniqueEmailAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unique_email_accounts_seq")
	@SequenceGenerator(name = "unique_email_accounts_seq", sequenceName = "unique_email_accounts_seq", allocationSize = 1)
	private Long id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	protected UniqueEmailAccount() {
	}

	public UniqueEmailAccount(String email) {
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

}
