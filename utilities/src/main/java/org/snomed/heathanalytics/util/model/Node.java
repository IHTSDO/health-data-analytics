package org.snomed.heathanalytics.util.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class Node {

	private final Long id;
	private final Set<Node> children;
	private Long frequency;
	private boolean covered;

	public Node(Long id) {
		this.id = id;
		children = new HashSet<>();
		frequency = 0L;
	}

	public Long getId() {
		return id;
	}

	public void addParent(Node parentNode) {
		parentNode.getChildren().add(this);
	}

	public Set<Node> getChildren() {
		return children;
	}

	public void setFrequency(Long frequency) {
		this.frequency = frequency;
	}

	public Long getFrequency() {
		return frequency;
	}

	public boolean hasFrequency() {
		return frequency > 0;
	}

	public Long getAggregateFrequency() {
		final AtomicLong aggFrequency = new AtomicLong();
		doGetAggregateFrequency(aggFrequency, child -> true, new HashSet<>());
		return aggFrequency.get();
	}

	public Long getRemainingAggregateFrequency() {
		final AtomicLong aggFrequency = new AtomicLong();
		doGetAggregateFrequency(aggFrequency, child -> !child.isCovered(), new HashSet<>());
		return aggFrequency.get();
	}

	private void doGetAggregateFrequency(AtomicLong aggFrequency, Predicate<Node> predicate, Set<Long> visited) {
		if (!visited.contains(id)) {
			visited.add(id);
			aggFrequency.addAndGet(frequency);
			for (Node child : children) {
				if (predicate.test(child)) {
					child.doGetAggregateFrequency(aggFrequency, predicate, visited);
				}
			}
		}
	}

	public boolean isCovered() {
		return covered;
	}

	public void markAsCoveredIncDescendants() {
		covered = true;
		for (Node child : children) {
			child.markAsCoveredIncDescendants();
		}
	}

	public Set<Node> getDescendants() {
		Set<Node> set = new HashSet<>();
		doGetDescendants(set);
		return set;
	}

	private void doGetDescendants(Set<Node> set) {
		for (Node child : children) {
			set.add(child);
			child.doGetDescendants(set);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Node node = (Node) o;
		return id.equals(node.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
