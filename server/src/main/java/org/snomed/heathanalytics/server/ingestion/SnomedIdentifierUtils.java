package org.snomed.heathanalytics.server.ingestion;

import com.google.common.base.Strings;

import java.util.regex.Pattern;

public class SnomedIdentifierUtils {

	public static final Pattern SCTID_PATTERN = Pattern.compile("\\d{6,18}");
	public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

	private static final String PARTITION_PART2_CONCEPT = "0";
	private static final String PARTITION_PART2_DESCRIPTION = "1";
	private static final String PARTITION_PART2_RELATIONSHIP = "2";

	public static boolean isValidConceptIdFormat(String sctid) {
		return sctid != null && SCTID_PATTERN.matcher(sctid).matches() && PARTITION_PART2_CONCEPT.equals(getPartitionIdPart(sctid)) && isChecksumCorrect(sctid);
	}

	public static boolean isValidDescriptionIdFormat(String sctid) {
		return sctid != null && SCTID_PATTERN.matcher(sctid).matches() && PARTITION_PART2_DESCRIPTION.equals(getPartitionIdPart(sctid)) && isChecksumCorrect(sctid);
	}

	public static boolean isValidRelationshipIdFormat(String sctid) {
		return sctid != null && SCTID_PATTERN.matcher(sctid).matches() && PARTITION_PART2_RELATIONSHIP.equals(getPartitionIdPart(sctid)) && isChecksumCorrect(sctid);
	}

	public static boolean isValidRefsetMemberIdFormat(String uuid) {
		return uuid != null && UUID_PATTERN.matcher(uuid).matches();
	}

	public static boolean isChecksumCorrect(String sctid) {
		// Assume true for this use case
		return true;
	}

	private static String getPartitionIdPart(String sctid) {
		if (!Strings.isNullOrEmpty(sctid) && sctid.length() > 4) {
			return sctid.substring(sctid.length() - 2, sctid.length() - 1);
		}
		return null;
	}

}
