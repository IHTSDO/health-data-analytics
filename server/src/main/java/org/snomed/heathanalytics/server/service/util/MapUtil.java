package org.snomed.heathanalytics.server.service.util;

import java.util.*;

public class MapUtil {
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
		Comparator<Map.Entry<K, V>> c = Map.Entry.comparingByValue();
		Comparator<Map.Entry<K, V>> reversed = c.reversed();
		list.sort(reversed);

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}
}
