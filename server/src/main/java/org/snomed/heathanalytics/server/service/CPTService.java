package org.snomed.heathanalytics.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.heathanalytics.server.model.CPTCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static java.lang.String.format;

@Service
public class CPTService {

	private static final String TAB = "\t";
	private static final int COLUMN_COUNT = 18;

	private final String dataDirectory;
	private Set<CPTCode> cptCodes;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public CPTService(@Value("${cpt.data.directory}") String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	public void attemptToLoadCPTDataFiles() throws IOException {
		if (!StringUtils.isEmpty(dataDirectory)) {
			File dataDirectoryFile = new File(dataDirectory);
			if (dataDirectoryFile.isDirectory()) {
				File cptCodesFile = new File(dataDirectoryFile, "cpt-codes.txt");
				if (cptCodesFile.isFile()) {
					loadCPTCodes(new FileInputStream(cptCodesFile));
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
			if (columns.length != COLUMN_COUNT) {
				throw new IllegalArgumentException(format("Header row contains %s columns but expected %s.", columns.length, COLUMN_COUNT));
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
			Set<CPTCode> cptCodes = new HashSet<>();
			while ((row = reader.readLine()) != null) {
				line++;
				String[] values = row.split(TAB);
				if (values.length == 18) {
					String cptCode = values[0];
					String workRVU = values[10];
					String facilityPracticeExpenseRVU = values[11];
					String nonfacilityPracticeExpenseRVU = values[12];
					String pliRVU = values[13];
					String totalFacilityRVU = values[14];
					String totalMedicarePhysicianFeeScheduleFacilityPayment = values[15];
					String totalNonfacilityRVU = values[16];
					String totalMedicarePhysicianFeeScheduleNonFacilityPayment = values[17];
					cptCodes.add(new CPTCode(cptCode, workRVU, facilityPracticeExpenseRVU, nonfacilityPracticeExpenseRVU, pliRVU,
							totalFacilityRVU, totalMedicarePhysicianFeeScheduleFacilityPayment, totalNonfacilityRVU, totalMedicarePhysicianFeeScheduleNonFacilityPayment));
				} else {
					logger.info("Skipping line {}, because it does not contain enough values.", line);
				}
			}
			logger.info("Loaded {} CTP codes.", cptCodes.size());
			this.cptCodes = cptCodes;
		}
	}

	public Set<CPTCode> getCPTCodes() {
		return cptCodes;
	}
}
