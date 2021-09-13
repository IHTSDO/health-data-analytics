package org.snomed.heathanalytics.util.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Node {

	private final Long id;
	private final Set<Node> parents;
	private final Set<Node> children;
	private Long frequency;
	private Long aggregateFrequency;

	public Node(Long id) {
		this.id = id;
		parents = new HashSet<>();
		children = new HashSet<>();
		aggregateFrequency = 0L;
	}

	public Long getId() {
		return id;
	}

	public void addParent(Node parentNode) {
		parents.add(parentNode);
		parentNode.getChildren().add(this);
	}

	public Set<Node> getChildren() {
		return children;
	}

	public void setFrequency(Long frequency) {
		this.frequency = frequency;
		addToAggregateFrequency(frequency);
	}

	private void addToAggregateFrequency(Long frequency) {
		aggregateFrequency += frequency;
		for (Node parent : parents) {
			parent.addToAggregateFrequency(frequency);
		}
	}

	public Long getFrequency() {
		return frequency;
	}

	public Long getAggregateFrequency() {
		return aggregateFrequency;
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
