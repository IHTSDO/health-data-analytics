package org.snomed.heathanalytics.server.service.stats;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class EncounterRatioFrequencyDistributionTest {

	@Test
	public void testRatiosWithZeroOrOnePatient() {
		EncounterFrequencyDistribution leftMatrix = new EncounterFrequencyDistribution();
		EncounterFrequencyDistribution rightMatrix = new EncounterFrequencyDistribution();
		EncounterRatioFrequencyDistribution relevanceMatrix = new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
		assertTrue(relevanceMatrix.getConceptFractions().isEmpty());

		leftMatrix.addPatient();
		leftMatrix.addEncounter(100L);
		relevanceMatrix = new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
		assertEquals(0, relevanceMatrix.getConceptFractions().size());

		rightMatrix.addPatient();
		rightMatrix.addEncounter(200L);
		relevanceMatrix = new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
		assertEquals(2, relevanceMatrix.getConceptFractions().size());
		assertEquals(1F, relevanceMatrix.getConceptFractions().get(100L), 0);
		assertEquals(-1F, relevanceMatrix.getConceptFractions().get(200L), 0);

		rightMatrix.addEncounter(100L);
		relevanceMatrix = new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
		assertEquals(2, relevanceMatrix.getConceptFractions().size());
		assertEquals(0F, relevanceMatrix.getConceptFractions().get(100L), 0);
	}

	@Test
	public void testRatiosWithManyPatients() {
		EncounterFrequencyDistribution leftMatrix = new EncounterFrequencyDistribution();
		EncounterFrequencyDistribution rightMatrix = new EncounterFrequencyDistribution();
		EncounterRatioFrequencyDistribution relevanceMatrix = new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
		assertTrue(relevanceMatrix.getConceptFractions().isEmpty());

		for (int i = 0; i < 10; i++) {
			leftMatrix.addPatient();
		}
		for (int i = 0; i < 100; i++) {
			rightMatrix.addPatient();
		}

		double delta = 0.01;
		leftMatrix.addEncounter(100L);
		relevanceMatrix = new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
		assertEquals(1, relevanceMatrix.getConceptFractions().size());
		assertEquals(0.1, relevanceMatrix.getConceptFractions().get(100L), delta);

		rightMatrix.addEncounter(200L);
		relevanceMatrix = new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
		assertEquals(2, relevanceMatrix.getConceptFractions().size());
		assertEquals(0.1, relevanceMatrix.getConceptFractions().get(100L), delta);
		assertEquals(-0.01, relevanceMatrix.getConceptFractions().get(200L), delta);

		rightMatrix.addEncounter(100L);
		relevanceMatrix = new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
		assertEquals(2, relevanceMatrix.getConceptFractions().size());
		assertEquals(0.09F, relevanceMatrix.getConceptFractions().get(100L), delta);

		rightMatrix.addEncounter(100L);
		relevanceMatrix = new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
		assertEquals(2, relevanceMatrix.getConceptFractions().size());
		assertEquals(0.08F, relevanceMatrix.getConceptFractions().get(100L), delta);

		rightMatrix.addEncounter(100L);
		relevanceMatrix = new EncounterRatioFrequencyDistribution(leftMatrix, rightMatrix);
		assertEquals(2, relevanceMatrix.getConceptFractions().size());
		assertEquals(0.07F, relevanceMatrix.getConceptFractions().get(100L), delta);
		System.out.println(relevanceMatrix);
	}

}
