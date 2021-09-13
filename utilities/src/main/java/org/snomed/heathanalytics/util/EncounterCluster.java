package org.snomed.heathanalytics.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.util.model.Feature;
import org.snomed.heathanalytics.util.model.Node;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class EncounterCluster {

	// Development roadmap:
	// Output complete list of features with: conceptId, overall frequency, included weak encounters, included sub-features - DONE
	// Output list of original encounters with: conceptId, count of times included in a feature - DONE
	// Input list of concepts to leave alone / not cluster
	// Input list of concepts with no clinical meaning - do not cluster here
	// Input number of categories required

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
		// Output help if no input arguments or help requested
		if (args.isEmpty() || args.contains(HELP)) {
			printHelp();
			return;
		}

		// Read input arguments
		String termToConceptMapFile = getArgValue(TERM_TO_CONCEPT_MAP, args);
		String encounterFrequencyFile = getArgValue(ENCOUNTER_FREQUENCY, args);
		String relationshipFile = getArgValue(RELATIONSHIPS, args);
		int minEncounterFrequency = Integer.parseInt(getArgValue(MIN_ENCOUNTER_FREQUENCY, args, MIN_FREQUENCY_DEFAULT));

		// Read input files
		Map<String, Long> termToConceptMap = readTermToConceptMap(termToConceptMapFile);
		Map<Long, Long> encounterFrequencyMap = readEncounterFrequency(encounterFrequencyFile, termToConceptMap);
		final Map<Long, Node> nodeMap = buildHierarchy(relationshipFile);

		// Map encounters and frequencies into concept hierarchy
		addEncountersToNodes(encounterFrequencyMap, nodeMap);

		// Traverse hierarchy and perform clustering where needed
		final Node rootConcept = nodeMap.get(ROOT_CONCEPT);
		final Map<Long, Feature> clusterPoints = new HashMap<>();
		clusterEncounters(rootConcept, minEncounterFrequency, clusterPoints);
		logger.info("Clustering complete.");

		// Build list of all features including clusters and survivors for output
		final Map<Long, Feature> features = new HashMap<>(clusterPoints);
		// Add surviving encounters to feature list
		for (Map.Entry<Long, Long> encounterFrequency : encounterFrequencyMap.entrySet()) {
			final Long conceptId = encounterFrequency.getKey();
			if (!features.containsKey(conceptId) && encounterFrequency.getValue() >= minEncounterFrequency) {
				features.put(conceptId, Feature.newSurvivingFeature(conceptId, encounterFrequency.getValue()));
			}
		}

		// Output complete list of features
		final String outputFeaturesFilename = "features.tsv";
		try (BufferedWriter featureListWriter = new BufferedWriter(new FileWriter(outputFeaturesFilename))) {
			featureListWriter.write("featureConceptId\toverallFrequency\tincludesWeakEncounters\tincludesSubFeatures");
			featureListWriter.newLine();
			for (Feature feature : features.values()) {
				featureListWriter.write(String.format("%s\t%s\t%s\t%s", feature.getConceptId(), feature.getAggregatedFrequency(),
						stringWithoutBraces(feature.getWeakConcepts()),
						stringWithoutBraces(feature.getSubFeatures())));
				featureListWriter.newLine();
			}
		}
		logger.info("Written complete feature list to \"{}\". {} features including {} clusters.", outputFeaturesFilename,
				features.size(), features.values().stream().filter(Feature::isCluster).count());

		// Output list of original encounters with feature inclusion count
		final String outputEncounterFeatureInclusionFilename = "encounter_feature_inclusion.tsv";
		int encounterIncludedInAFeature = 0;
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputEncounterFeatureInclusionFilename))) {
			writer.write("encounterConceptId\tsufficientFrequency\tfeatureInclusionCount");
			writer.newLine();
			for (Long encounterConceptId : encounterFrequencyMap.keySet()) {
				int inclusions = 0;
				for (Feature feature : features.values()) {
					if (feature.getConceptId().equals(encounterConceptId) ||
							feature.getWeakConcepts().contains(encounterConceptId) ||
							feature.getSubFeatures().contains(encounterConceptId)) {
						inclusions++;
					}
				}
				if (inclusions > 0) {
					encounterIncludedInAFeature++;
				}
				writer.write(encounterConceptId.toString());
				writer.write("\t");
				writer.write(String.valueOf(encounterFrequencyMap.get(encounterConceptId) >= minEncounterFrequency));
				writer.write("\t");
				writer.write(String.valueOf(inclusions));
				writer.newLine();
			}
		}
		logger.info("Written encounter feature inclusion list to \"{}\". Out of {} encounter concepts {} were included in one or more features, {} were included in none.",
				outputEncounterFeatureInclusionFilename, encounterFrequencyMap.size(),
				encounterIncludedInAFeature, encounterFrequencyMap.size() - encounterIncludedInAFeature);

	}

	private String stringWithoutBraces(Collection<?> collection) {
		return collection.toString().replace("[", "").replace("]", "");
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

	private boolean clusterEncounters(Node conceptNode, int minEncounterFrequency, Map<Long, Feature> clusterPoints) {
		final Long conceptId = conceptNode.getId();
		if (clusterPoints.containsKey(conceptId)) {
			return false;
		}
		final Long frequency = conceptNode.getAggregateFrequency();
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
			Map<Long, Long> insufficientChildFrequencies = conceptNode.getChildren().stream()
					.filter(node -> node.getAggregateFrequency() > 0 && node.getAggregateFrequency() < minEncounterFrequency)
					.collect(Collectors.toMap(Node::getId, Node::getAggregateFrequency));
			Map<Long, Long> sufficientChildFrequencies = conceptNode.getChildren().stream()
					.filter(node -> node.getAggregateFrequency() > 0 && node.getAggregateFrequency() >= minEncounterFrequency)
					.collect(Collectors.toMap(Node::getId, Node::getAggregateFrequency));

			String childFrequenciesMessage = sufficientChildFrequencies.isEmpty() ? "" : String.format(", and those with sufficient frequency:%s", sufficientChildFrequencies);
			final Long ownFrequency = conceptNode.getFrequency();
			String ownFrequencyMessage = ownFrequency != null ? String.format(", own frequency:%s", ownFrequency) : "";

			logger.info("Concept {}, with aggregated frequency {}, should be used to summarise concepts without sufficient frequency:{}{}{}.",
					conceptId, conceptNode.getAggregateFrequency(), insufficientChildFrequencies, childFrequenciesMessage, ownFrequencyMessage);

			clusterPoints.put(conceptId, Feature.newCluster(conceptId, conceptNode.getAggregateFrequency(), insufficientChildFrequencies.keySet(), sufficientChildFrequencies.keySet()));
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

	private Map<Long, Long> readEncounterFrequency(String encounterFrequencyFile, Map<String, Long> termToConceptMap) {
		Map<Long, Long> encounterFrequencyMap = new HashMap<>();
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
						encounterFrequencyMap.put(concept, Long.parseLong(values[1]));
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

	private void addEncountersToNodes(Map<Long, Long> encounterFrequency, Map<Long, Node> nodeMap) {
		int added = 0;
		for (Map.Entry<Long, Long> entry : encounterFrequency.entrySet()) {
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
