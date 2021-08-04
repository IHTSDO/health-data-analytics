package org.snomed.heathanalytics.util.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Node {

	private final Long id;
	private final Set<Node> parents;
	private Integer frequency;

	public Node(Long id) {
		this.id = id;
		parents = new HashSet<>();
	}

	public Long getId() {
		return id;
	}

	public void addParent(Node parentNode) {
		parents.add(parentNode);
	}

	public Set<Node> getParents() {
		return parents;
	}

	public void setFrequency(Integer frequency) {
		this.frequency = frequency;
	}

	public Integer getFrequency() {
		return frequency;
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
