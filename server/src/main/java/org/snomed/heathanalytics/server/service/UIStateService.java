package org.snomed.heathanalytics.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UIStateService {

	private final File uiStateStore;

	public UIStateService(@Value("${ui-state.store}") String uiStateStorePath) {
		this.uiStateStore = new File(uiStateStorePath);
	}

	public void put(String panel, String id, String jsonState) throws IOException {
		File jsonFile = getFile(panel, id);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile, StandardCharsets.UTF_8))) {
			writer.write(jsonState);
		}
	}

	public String get(String panel, String id) throws IOException {
		File jsonFile = getFile(panel, id);
		if (!jsonFile.isFile()) {
			return "";
		}
		StringBuilder buffer = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
		}
		return buffer.toString();
	}

	public List<String> list(String panel) {
		String[] list = getDir(panel).list((dir, name) -> name.endsWith(".json"));
		if (list != null) {
			return Arrays.stream(list)
					.map(name -> name.replace(".json", ""))
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	public void delete(String panel, String id) {
		File file = getFile(panel, id);
		file.delete();
	}

	private File getFile(String panel, String id) {
		File dir = getDir(panel);
		dir.mkdirs();
		return new File(dir, id + ".json");
	}

	private File getDir(String panel) {
		return new File(uiStateStore, panel);
	}
}
