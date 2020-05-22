package org.snomed.heathanalytics.datageneration;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ComponentFactoryImpl;
import org.ihtsdo.otf.snomedboot.factory.implementation.standard.ComponentStore;
import org.ihtsdo.otf.sqs.service.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Long.parseLong;

public class SnomedConceptService {

	private final Map<Long, List<Long>> conceptDescendantMap;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public SnomedConceptService(File releaseDirectory) {
		conceptDescendantMap = new Long2ObjectOpenHashMap<>();
		final ComponentStore componentStore = new ComponentStore();
		Map<Long, Node> nodeMap = new Long2ObjectOpenHashMap<>();
		try {
			logger.info("Loading inferred hierarchy from RF2 Snapshot files.");
			new ReleaseImporter().loadSnapshotReleaseFiles(releaseDirectory.getAbsolutePath(), new LoadingProfile(), new ComponentFactoryImpl(componentStore) {
				@Override
				public void addInferredConceptParent(String sourceId, String parentId) {
					long source = parseLong(sourceId);
					long parent = parseLong(parentId);
					nodeMap.computeIfAbsent(parent, id -> new Node(parent))
							.addChild(nodeMap.computeIfAbsent(source, id -> new Node(source)));
				}
			});
		} catch (ReleaseImportException e) {
			throw new RuntimeException("Failed to load SNOMED CT snapshot files from the release directory.", e);
		}
		logger.info("Gathering all inferred descendants.");
		for (Node node : nodeMap.values()) {
			conceptDescendantMap.put(node.id, node.getAncestors());
		}
		logger.info("Inferred descendants of {} active concepts collected.", conceptDescendantMap.size());
	}

	Long selectRandomChildOf(String conceptId) throws ServiceException {
		long conceptIdLong = parseLong(conceptId);
		List<Long> descendants = conceptDescendantMap.get(conceptIdLong);
		if (descendants == null) {
			throw new ServiceException("Concept " + conceptId + " could not be found.");
		}
		if (descendants.isEmpty()) {
			// Concept is a leaf, return self
			return conceptIdLong;
		}
		return descendants.get(ThreadLocalRandom.current().nextInt(0, descendants.size()));
	}

	private static class Node {

		private final Long id;
		private final Set<Node> children;

		private Node(Long id) {
			this.id = id;
			children = new HashSet<>();
		}

		private void addChild(Node child) {
			children.add(child);
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

		public List<Long> getAncestors() {
			return new ArrayList<>(doGetAncestors(new HashSet<>()));
		}

		private Set<Long> doGetAncestors(HashSet<Long> ids) {
			for (Node child : children) {
				ids.add(child.id);
				child.doGetAncestors(ids);
			}
			return ids;
		}
	}

}
