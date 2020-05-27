package org.snomed.heathanalytics.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.server.model.CPTCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@Service
public class CPTService {

	private static final String TAB = "\t";
	private static final String CPT_CODES_TXT = "cpt-codes.txt";
	private static final int CPT_COLUMN_COUNT = 18;
	private static final String SNOMED_CPT_MAP_TXT = "snomed-cpt-map.txt";
	private static final int MAP_COLUMN_COUNT = 9;

	private final String dataDirectory;
	private final Map<String, CPTCode> cptCodeMap = new HashMap<>();
	private final Map<String, CPTCode> snomedToCptMap = new HashMap<>();
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public CPTService(@Value("${cpt.data.directory}") String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	@PostConstruct
	public void attemptToLoadCPTDataFiles() throws IOException {
		if (!StringUtils.isEmpty(dataDirectory)) {
			File dataDirectoryFile = new File(dataDirectory);
			if (dataDirectoryFile.isDirectory()) {
				File cptCodesFile = new File(dataDirectoryFile, CPT_CODES_TXT);
				if (cptCodesFile.isFile()) {
					logger.info("Loading {}", cptCodesFile.getName());
					loadCPTCodes(new FileInputStream(cptCodesFile));

					File snomedCptMapFile = new File(dataDirectoryFile, SNOMED_CPT_MAP_TXT);
					if (snomedCptMapFile.isFile()) {
						logger.info("Loading {}", snomedCptMapFile.getName());
						loadSnomedCPTMap(new FileInputStream(snomedCptMapFile));
					} else {
						logger.warn("{} file was found but {} was not. " +
								"Health records with SNOMED CT codes will not be mapped to CPT.", CPT_CODES_TXT, SNOMED_CPT_MAP_TXT);
					}
					return;
				}
			} else {
				logger.info("No directory at {}", dataDirectoryFile.getAbsolutePath());
			}
		}
		logger.info("No CPT Code files found to load.");
	}

	public void loadCPTCodes(InputStream cptCodesInputStream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(cptCodesInputStream))) {
			String header = reader.readLine();
			if (header == null) {
				throw new IllegalArgumentException("No rows found.");
			}
			String[] columns = header.split(TAB);
			if (columns.length != CPT_COLUMN_COUNT) {
				throw new IllegalArgumentException(format("Header row contains %s columns but expected %s.", columns.length, CPT_COLUMN_COUNT));
			}
			String headerFirstName = columns[0];
			if (!StringUtils.isEmpty(headerFirstName) && headerFirstName.matches("[0-9]+")) {
				throw new IllegalArgumentException("The first row of the file should be just column headers but numbers found.");
			}

			/*
			Columns:
			0 CPT Code
			1 Short Descriptor
			2 Medium Descriptor
			3 Descriptor
			4 H1 Descriptor
			5 H2 Descriptor
			6 H3 Descriptor
			7 H4 Descriptor
			8 H5 Descriptor
			9 H6 Descriptor
			10 Work RVU
			11 Facility Practice Expense RVU
			12 Nonfacility Practice Expense RVU
			13 PLI RVU
			14 Total Facility RVU
			15 Total Medicare Physician Fee Schedule Facility Payment
			16 Total Nonfacility RVU
			17 Total Medicare Physician Fee Schedule Non-Facility Payment
			 */
			// Read values
			String row;
			int line = 1;
			cptCodeMap.clear();
			while ((row = reader.readLine()) != null) {
				line++;
				String[] values = row.split(TAB);
				if (values.length == CPT_COLUMN_COUNT) {
					String cptCode = values[0];
					String h1Descriptor = values[4];
					String h2Descriptor = values[5];
					String h3Descriptor = values[6];
					Float workRVU = toFloat(values[10]);
					Float facilityPracticeExpenseRVU = toFloat(values[11]);
					Float nonfacilityPracticeExpenseRVU = toFloat(values[12]);
					Float pliRVU = toFloat(values[13]);
					Float totalFacilityRVU = toFloat(values[14]);
					Float totalMedicarePhysicianFeeScheduleFacilityPayment = toFloat(values[15]);
					Float totalNonfacilityRVU = toFloat(values[16]);
					Float totalMedicarePhysicianFeeScheduleNonFacilityPayment = toFloat(values[17]);
					cptCodeMap.put(cptCode, new CPTCode(cptCode, h1Descriptor, h2Descriptor, h3Descriptor, workRVU, facilityPracticeExpenseRVU, nonfacilityPracticeExpenseRVU, pliRVU,
							totalFacilityRVU, totalMedicarePhysicianFeeScheduleFacilityPayment, totalNonfacilityRVU, totalMedicarePhysicianFeeScheduleNonFacilityPayment));
				} else {
					logger.info("Skipping line {}, because it does not contain enough values.", line);
				}
			}
			logger.info("Loaded {} CTP codes{}.", cptCodeMap.size(), cptCodeMap.isEmpty() ? "" : format(", e.g. %s", cptCodeMap.values().iterator().next()));
		}
	}

	private Float toFloat(String value) {
		if (value != null && !value.isEmpty()) {
			value = value.trim();
			if (value.startsWith("$")) {
				value = value.substring(1);
			}
			if (value.matches("^[0-9]+\\.[0-9]+")) {
				return Float.parseFloat(value);
			}
		}
		return null;
	}

	private void loadSnomedCPTMap(FileInputStream mapInputStream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(mapInputStream))) {
			String header = reader.readLine();
			if (header == null) {
				throw new IllegalArgumentException("No rows found.");
			}
			String[] columns = header.split(TAB);
			if (columns.length != MAP_COLUMN_COUNT) {
				throw new IllegalArgumentException(format("Header row contains %s columns but expected %s.", columns.length, MAP_COLUMN_COUNT));
			}
			String headerFirstName = columns[0];
			if (!StringUtils.isEmpty(headerFirstName) && headerFirstName.matches("[0-9]+")) {
				throw new IllegalArgumentException("The first row of the file should be just column headers but numbers found.");
			}

			/*
			Columns:
			0 X
			1 SNOMED CT Code
			2 SNOMED FSN Term
			3 X
			4 X
			5 X
			6 X
			7 CPT Code
			8 CPT Term
			 */
			// Read values
			String row;
			int line = 1;
			snomedToCptMap.clear();
			while ((row = reader.readLine()) != null) {
				line++;
				String[] values = row.split(TAB);
				if (values.length == MAP_COLUMN_COUNT) {
					String snomedCode = values[1];
					String cptCode = values[7];

					CPTCode cptCodeObject = cptCodeMap.get(cptCode);
					if (cptCodeObject != null) {
						snomedToCptMap.put(snomedCode, cptCodeObject);
					} else {
						logger.warn("CTP code '{}' found in mapping file but the entry must be ignored because the code was not successfully loaded from the {} file.",
								cptCode, CPT_CODES_TXT);
					}
				} else {
					logger.info("Skipping line {}, because it does not contain enough values.", line);
				}
			}
			logger.info("Loaded {} SNOMED CT to CTP map entries.", snomedToCptMap.size());
		}

	}

	public Map<String, CPTCode> getCptCodeMap() {
		return cptCodeMap;
	}

	public Map<String, CPTCode> getSnomedToCptMap() {
		return snomedToCptMap;
	}
}
