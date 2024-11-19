package org.snomed.heathanalytics.server.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;

import java.util.Collection;

public class QueryHelper {

	public static Query termsQuery(String field, Collection<?> values) {
		return (new TermsQuery.Builder()).field(field).terms((tq) -> {
			return tq.value(values.stream().map(JsonData::of).map(FieldValue::of).toList());
		}).build()._toQuery();
	}

}
