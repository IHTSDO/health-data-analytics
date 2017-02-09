package org.snomed.heathanalytics.domain;

import java.util.Comparator;
import java.util.Date;

public interface Act {

	Comparator<Act> ACT_DATE_COMPARATOR = Comparator.comparing(Act::getDate);

	Date getDate();

}
