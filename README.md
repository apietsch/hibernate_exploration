# Hibernate Unique Constraint Exploration

This project is a small Spring Boot and Hibernate/JPA playground for understanding how unique constraints behave in practice.

The goal is not only to define constraints, but to deliberately violate them in tests and inspect the actual exceptions produced by Hibernate, Spring Data/JPA, JDBC, and the database. The project uses Testcontainers with a real Oracle database so the behavior being tested is database-backed instead of relying on an in-memory substitute.

## Goals

- Explore uniqueness declared with JPA annotations.
- Explore uniqueness declared directly at the database/schema level.
- Compare the exceptions raised for different kinds of uniqueness violations.
- Understand when violations appear: during persist, flush, transaction commit, or later access.
- Observe how Hibernate wraps database constraint failures.
- Document the difference between JPA metadata, generated DDL, and real database constraints.
- Investigate uniqueness behavior in association tables, especially collection mappings using join tables.

## Planned Experiments

### 1. Unique Column Annotation

Create an entity with a property marked as unique through JPA metadata, for example:

```java
@Column(unique = true)
private String email;
```

The test should insert two rows with the same value and verify the actual exception chain.

Questions to answer:

- Does the constraint exist in the generated database schema?
- Which exception type is seen at the test boundary?
- What is the root cause from Oracle?
- Does the failure happen on `persist`, `save`, `flush`, or commit?

### 2. Table-Level Unique Constraint Annotation

Create an entity using a table-level unique constraint, for example:

```java
@Table(uniqueConstraints = {
		@UniqueConstraint(name = "uk_person_username", columnNames = "username")
})
```

The test should compare this behavior with `@Column(unique = true)`.

Questions to answer:

- Is the generated DDL different?
- Is naming the constraint useful for debugging exceptions?
- Does Hibernate expose the violated constraint name?

### 3. Composite Unique Constraint

Create an entity where uniqueness depends on multiple columns.

Example idea:

```java
@Table(uniqueConstraints = {
		@UniqueConstraint(name = "uk_booking_room_day", columnNames = {"room_id", "booking_day"})
})
```

The test should prove that duplicate values are allowed individually, but not in the same combination.

Questions to answer:

- How does the exception differ from a single-column unique violation?
- Is the constraint name available in the exception chain?
- How should the test assert the failure without becoming too database-version-specific?

### 4. Database-Level Unique Constraint

Create an example where uniqueness is not declared with JPA annotations, but exists only in the database schema.

This can be done with schema SQL or a migration tool later. The important part is that the entity itself does not advertise the uniqueness rule.

Questions to answer:

- Does Hibernate know about the constraint before the database rejects the insert?
- Is the resulting exception different from annotation-generated constraints?
- What happens when application-level assumptions and database-level constraints diverge?

### 5. Join Table Collection With Unique Join Column

Create an entity with a collection association using a join table where one side has a unique constraint.

Example idea:

```java
@OneToMany
@JoinTable(
		name = "owner_items",
		joinColumns = @JoinColumn(name = "owner_id"),
		inverseJoinColumns = @JoinColumn(name = "item_id", unique = true)
)
private Set<Item> items = new HashSet<>();
```

This should show the impact of putting `unique = true` on the joined entity column. In practice, this can turn a collection association into something closer to exclusive ownership, because the same item cannot appear for multiple owners through the join table.

Questions to answer:

- Can the same child/entity be linked to two different parent entities?
- Does the unique constraint apply to the join table column rather than the child table?
- How does Hibernate order inserts into the entity tables and join table?
- Does using `List`, `Set`, `@OneToMany`, or `@ManyToMany` change the observed behavior?

## Testing Approach

Each experiment should have focused tests that:

1. Persist valid baseline data.
2. Attempt a specific uniqueness violation.
3. Force database interaction with `flush()` when needed.
4. Capture the thrown exception.
5. Inspect the exception chain and database constraint information.

The tests should prefer real behavior over assumptions. If the database raises an Oracle-specific error, the test should document it clearly without hiding it behind a generic assertion too early.

## Database

The project uses Oracle through Testcontainers:

```yaml
services:
  oracle:
    image: 'gvenzl/oracle-free:latest'
```

Using a real database matters here because uniqueness violations are ultimately enforced by the database. Hibernate and JPA can describe constraints, but the most important behavior for this project is the actual failure produced by Oracle and how that failure travels back through Hibernate and Spring.

## Useful Things To Observe

- The top-level exception type seen by the test.
- The Hibernate exception type.
- The JDBC exception type.
- The Oracle error code and message.
- The violated constraint name, when available.
- Whether the failure happens before or after transaction commit.
- The SQL statements emitted around the failure.
- The generated schema for annotation-based constraints.

## Current State

The project currently contains the Spring Boot scaffold, Oracle Testcontainers setup, and a basic context-load test. The next step is to add small entities and tests for each uniqueness scenario above.
