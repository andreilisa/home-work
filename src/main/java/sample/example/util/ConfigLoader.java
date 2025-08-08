package sample.example.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import sample.example.config.GameConfig;

import java.io.File;

public class ConfigLoader {
	public static GameConfig loadConfig(String path) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper.readValue(new File(path), GameConfig.class);
	}
}
