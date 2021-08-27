package org.snomed.heathanalytics.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.util.model.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EncounterCluster {

	public static final long ROOT_CONCEPT = 138875005L;
	public static final String TERM_TO_CONCEPT_MAP = "-term-to-concept-map";
	public static final String ENCOUNTER_FREQUENCY = "-encounter-frequency-file";
	public static final String RELATIONSHIPS = "-relationship-file";
	public static final String MIN_ENCOUNTER_FREQUENCY = "-min-encounter-frequency";
	public static final String MIN_FREQUENCY_DEFAULT = "100";
	public static final String HELP = "-help";

	// -term-to-concept-map barts-core-problem-list-map.txt
	// -encounter-frequency-file "Criteria Frequency - everyone_social.150621-problems.csv"
	// -relationship-file ../release/Snapshot/Terminology/sct2_Relationship_Snapshot_INT_20210731.txt
	// -min-encounter-frequency 100
	public static void main(String[] args) throws IOException {
		new EncounterCluster().run(Arrays.asList(args));
	}

	final Logger logger = LoggerFactory.getLogger(getClass());
	private void run(List<String> args) throws IOException {
		if (args.isEmpty() || args.contains(HELP)) {
			printHelp();
			return;
		}

		String termToConceptMapFile = getArgValue(TERM_TO_CONCEPT_MAP, args);
		String encounterFrequencyFile = getArgValue(ENCOUNTER_FREQUENCY, args);
		String relationshipFile = getArgValue(RELATIONSHIPS, args);
		int minEncounterFrequency = Integer.parseInt(getArgValue(MIN_ENCOUNTER_FREQUENCY, args, MIN_FREQUENCY_DEFAULT));

		Map<String, Long> termToConceptMap = readTermToConceptMap(termToConceptMapFile);
		Map<Long, Integer> encounterFrequency = readEncounterFrequency(encounterFrequencyFile, termToConceptMap);
		final Map<Long, Node> nodeMap = buildHierarchy(relationshipFile);

		addEncountersToNodes(encounterFrequency, nodeMap);

		final Node rootConcept = nodeMap.get(ROOT_CONCEPT);
		clusterEncounters(rootConcept, minEncounterFrequency, new HashSet<>());

		// List of concepts with no clinical meaning - do not cluster here
		// Ask for number of categories up front
		// List of concepts to leave alone / not cluster
		// Output all encounters and clusters (new list)
	}

	private String getArgValue(String key, List<String> args) {
		return getArgValue(key, args, null);
	}

	private String getArgValue(String key, List<String> args, String defaultValue) {
		final int index = args.indexOf(key);
		if (index == -1 || index == args.size() - 1) {
			return defaultValue;
		}
		return args.get(index + 1);
	}

	private void printHelp() {
		System.out.printf("Usage: %s \"file-path\" %s \"file-path\" %s \"file-path\" [%s 100]%n",
				TERM_TO_CONCEPT_MAP, ENCOUNTER_FREQUENCY, RELATIONSHIPS, MIN_ENCOUNTER_FREQUENCY);
	}

	private boolean clusterEncounters(Node conceptNode, int minEncounterFrequency, Set<Long> clusterPoints) {
		final Long conceptId = conceptNode.getId();
		if (clusterPoints.contains(conceptId)) {
			return false;
		}
		final Integer frequency = conceptNode.getAggregateFrequency();
		if (frequency > 0 && frequency < minEncounterFrequency) {
			return true;
		}

		boolean clusteringRequired = false;
		for (Node child : conceptNode.getChildren()) {
			if (clusterEncounters(child, minEncounterFrequency, clusterPoints)) {
				clusteringRequired = true;
			}
		}
		if (clusteringRequired) {
			Map<Long, Integer> insufficientChildFrequencies = conceptNode.getChildren().stream()
					.filter(node -> node.getAggregateFrequency() > 0 && node.getAggregateFrequency() < minEncounterFrequency)
					.collect(Collectors.toMap(Node::getId, Node::getAggregateFrequency));
			Map<Long, Integer> sufficientChildFrequencies = conceptNode.getChildren().stream()
					.filter(node -> node.getAggregateFrequency() > 0 && node.getAggregateFrequency() >= minEncounterFrequency)
					.collect(Collectors.toMap(Node::getId, Node::getAggregateFrequency));

			String childFrequenciesMessage = sufficientChildFrequencies.isEmpty() ? "" : String.format(", and those with sufficient frequency:%s", sufficientChildFrequencies);
			final Integer ownFrequency = conceptNode.getFrequency();
			String ownFrequencyMessage = ownFrequency != null ? String.format(", own frequency:%s", ownFrequency) : "";

			logger.info("Concept {}, with aggregated frequency {}, should be used to summarise concepts without sufficient frequency:{}{}{}.",
					conceptId, conceptNode.getAggregateFrequency(), insufficientChildFrequencies, childFrequenciesMessage, ownFrequencyMessage);

			clusterPoints.add(conceptId);
		}

		return false;
	}

	private Map<String, Long> readTermToConceptMap(String termToConceptMapFile) {
		Map<String, Long> termToConceptMap = new HashMap<>();
		try (BufferedReader mapReader = new BufferedReader(new FileReader(termToConceptMapFile))) {
			final String header = mapReader.readLine();
			logger.info("Reading term to concept map with header line: {}", header);

			String line;
			int lineNum = 1;
			while ((line = mapReader.readLine()) != null) {
				lineNum++;
				String[] values = line.split("\\t");
				if (values.length > 1 && !values[1].isBlank()) {
					termToConceptMap.put(values[0], Long.parseLong(values[1]));
				} else {
					logger.debug("Skipped line {} of mapping file because found {} values: {}", lineNum, values.length, values);
				}
			}
			logger.info("Read {} map entries.", termToConceptMap.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return termToConceptMap;
	}

	private Map<Long, Integer> readEncounterFrequency(String encounterFrequencyFile, Map<String, Long> termToConceptMap) {
		Map<Long, Integer> encounterFrequencyMap = new HashMap<>();
		try (BufferedReader mapReader = new BufferedReader(new FileReader(encounterFrequencyFile))) {
			final String header = mapReader.readLine();
			logger.info("Reading encounter frequency map with header line: {}", header);

			String line;
			int lineNum = 1;
			while ((line = mapReader.readLine()) != null) {
				lineNum++;
				String[] values = line.split(",");
				if (values.length > 1 && values[1].matches("[0-9]+")) {

					final String term = values[0].toLowerCase().replace("_", " ");
					final Long concept = termToConceptMap.get(term);
					if (concept != null) {
						encounterFrequencyMap.put(concept, Integer.parseInt(values[1]));
					} else {
						logger.warn("Term {} not found in map. Line {} of encounter file not used.", term, line);
					}
				} else {
					logger.debug("Skipped line {} of encounter frequency file because found {} values: {}", lineNum, values.length, values);
				}
			}
			logger.info("Read {} map entries.", encounterFrequencyMap.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return encounterFrequencyMap;

	}

	private Map<Long, Node> buildHierarchy(String relationshipFile) throws IOException {
		Map<Long, Node> nodeMap = new HashMap<>();
		try (BufferedReader mapReader = new BufferedReader(new FileReader(relationshipFile))) {
			final String header = mapReader.readLine();
			logger.info("Reading relationship file with header line: {}", header);

			String line;
			while ((line = mapReader.readLine()) != null) {
				String[] values = line.split("\\t");
				// id	effectiveTime	active	moduleId	sourceId	destinationId	relationshipGroup	typeId	characteristicTypeId	modifierId
				// 0	1				2		3			4			5				6					7		8						9

				// if active is-a relationships
				if (values[2].equals("1") && values[7].equals("116680003")) {
					final Long childConcept = Long.parseLong(values[4]);
					final Long parentConcept = Long.parseLong(values[5]);
					nodeMap.computeIfAbsent(childConcept, Node::new)
							.addParent(nodeMap.computeIfAbsent(parentConcept, Node::new));
				}
			}
			logger.info("Loaded {} active concepts into hierarchy.", nodeMap.size());
		}
		return nodeMap;

	}

	private void addEncountersToNodes(Map<Long, Integer> encounterFrequency, Map<Long, Node> nodeMap) {
		int added = 0;
		for (Map.Entry<Long, Integer> entry : encounterFrequency.entrySet()) {
			final Node node = nodeMap.get(entry.getKey());
			if (node != null) {
				node.setFrequency(entry.getValue());
				added++;
			} else {
				logger.warn("Concept {} not found in set of active concepts loaded.", entry.getKey());
			}
		}
		logger.info("Added frequency data for {} encounters.", added);
	}
}
