package sample.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import sample.example.config.GameConfig;
import sample.example.game.GameEngine;
import sample.example.model.GameResult;
import sample.example.util.ConfigLoader;

import java.math.BigDecimal;

public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println("Usage: java -jar scratchgame.jar <config.json path> <bet amount>");
			return;
		}

		String configPath = args[0];
		BigDecimal betAmount = new BigDecimal(args[1]);

		GameConfig config = ConfigLoader.loadConfig(configPath);
		GameEngine engine = new GameEngine(config);

		GameResult result = engine.playRound(betAmount);

		ObjectMapper mapper = new ObjectMapper();
		String jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
		System.out.println(jsonResult);
	}
}
