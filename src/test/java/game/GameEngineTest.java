package game;

import sample.example.config.GameConfig;
import sample.example.game.GameEngine;
import sample.example.util.ConfigLoader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class GameEngineTest {

	private static GameConfig config;
	private static GameEngine engine;

	@BeforeAll
	public static void setup() throws Exception {
		config = ConfigLoader.loadConfig("src/test/resources/config-test.json");
		engine = new GameEngine(config);
	}

	@Test
	public void testMinMatrixSizeOneByOne() {
		config.setColumns(1);
		config.setRows(1);
		engine = new GameEngine(config);

		String[][] matrix = engine.generateMatrix();
		assertEquals(1, matrix.length);
		assertEquals(1, matrix[0].length);
		assertNotNull(matrix[0][0]);
		assertTrue(config.getSymbols().containsKey(matrix[0][0]));
	}

	@Test
	public void testMaxMatrixSizeFourByFour() {
		config.setColumns(4);
		config.setRows(4);
		engine = new GameEngine(config);

		String[][] matrix = engine.generateMatrix();
		assertEquals(4, matrix.length);
		assertEquals(4, matrix[0].length);
	}

	@Test
	public void testAllStandardSymbolsOccurAtLeastOnceInMultipleRuns() {
		config.setColumns(3);
		config.setRows(3);
		engine = new GameEngine(config);

		Set<String> observedSymbols = new HashSet<>();
		int runs = 1000;

		for (int i = 0; i < runs; i++) {
			String[][] matrix = engine.generateMatrix();
			for (int r = 0; r < matrix.length; r++) {
				for (int c = 0; c < matrix[r].length; c++) {
					String sym = matrix[r][c];
					GameConfig.Symbol symbolConf = config.getSymbols().get(sym);
					if (symbolConf != null && "standard".equalsIgnoreCase(symbolConf.getType())) {
						observedSymbols.add(sym);
					}
				}
			}
		}
		Set<String> standardSymbols = config.getSymbols().entrySet().stream()
				.filter(e -> "standard".equalsIgnoreCase(e.getValue().getType()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		assertTrue(observedSymbols.containsAll(standardSymbols), "All standard symbols should appear in generated matrices after many runs");
	}

	@Test
	public void testRewardZeroIfNoWinningCombinations() {
		config.setColumns(3);
		config.setRows(3);
		engine = new GameEngine(config);

		String[][] matrix = new String[][] {
				{"A", "B", "C"},
				{"D", "E", "F"},
				{"F", "E", "D"}
		};

		BigDecimal reward = engine.calculateReward(matrix, BigDecimal.valueOf(10));
		assertEquals(BigDecimal.ZERO.setScale(2), reward);
	}

	@Test
	public void testMaxRewardWithMaxMultipliers() {
		config.setColumns(3);
		config.setRows(3);
		engine = new GameEngine(config);

		String[][] matrix = new String[3][3];
		for (int r = 0; r < 3; r++) {
			Arrays.fill(matrix[r], "A");
		}

		BigDecimal bet = BigDecimal.valueOf(10);
		BigDecimal reward = engine.calculateReward(matrix, bet);

		BigDecimal symbolMultiplier = BigDecimal.valueOf(5);
		BigDecimal comboMultiplier = BigDecimal.valueOf(20);

		BigDecimal expected = bet.multiply(symbolMultiplier)
				.multiply(comboMultiplier)
				.setScale(2);
		assertEquals(expected, reward);
	}


	@Test
	public void testBonusMultiplyRewardApplied() {
		config.setColumns(3);
		config.setRows(3);
		engine = new GameEngine(config);

		String[][] matrix = new String[3][3];
		for (int r = 0; r < 3; r++) {
			Arrays.fill(matrix[r], "B");
		}
		matrix[0][0] = "10x";

		BigDecimal bet = BigDecimal.valueOf(10);
		BigDecimal reward = engine.calculateReward(matrix, bet);

		BigDecimal expected = bet.multiply(BigDecimal.valueOf(3))
				.multiply(BigDecimal.valueOf(10))
				.multiply(BigDecimal.valueOf(10))
				.setScale(2);

		assertEquals(expected, reward);
	}


	@Test
	public void testBonusExtraBonusApplied() {
		config.setColumns(3);
		config.setRows(3);
		engine = new GameEngine(config);

		String[][] matrix = new String[3][3];
		for (int r = 0; r < 3; r++) {
			Arrays.fill(matrix[r], "C");
		}
		matrix[1][1] = "+1000";

		BigDecimal bet = BigDecimal.valueOf(10);
		BigDecimal reward = engine.calculateReward(matrix, bet);

		BigDecimal expectedMultiplier = BigDecimal.valueOf(10);
		BigDecimal symbolMultiplier = BigDecimal.valueOf(2.5);
		BigDecimal extraBonus = BigDecimal.valueOf(1000);

		BigDecimal expected = bet.multiply(symbolMultiplier)
				.multiply(expectedMultiplier)
				.add(extraBonus)
				.setScale(2);

		assertEquals(expected, reward);
	}


	@Test
	public void testDetectWinningCombinationsReturnsEmptyForNoWin() {
		String[][] matrix = new String[][] {
				{"A", "B", "C"},
				{"D", "E", "F"},
				{"F", "E", "D"}
		};

		Map<String, List<String>> combos = engine.detectWinningCombinations(matrix);
		assertTrue(combos.isEmpty(), "No winning combinations should be detected");
	}

	@Test
	public void testDetectWinningCombinationsDetectsHorizontalWin() {
		String[][] matrix = new String[][] {
				{"A", "A", "A"},
				{"B", "C", "D"},
				{"E", "F", "F"}
		};
		Map<String, List<String>> combos = engine.detectWinningCombinations(matrix);
		assertTrue(combos.containsKey("A"));
		assertTrue(combos.get("A").stream().anyMatch(s -> s.contains("horizontally")));
	}

	@Test
	public void testDetectWinningCombinationsDetectsDiagonalWin() {
		String[][] matrix = new String[][] {
				{"B", "C", "D"},
				{"E", "B", "F"},
				{"A", "E", "B"}
		};
		Map<String, List<String>> combos = engine.detectWinningCombinations(matrix);
		assertTrue(combos.containsKey("B"));
		boolean foundDiagonal = combos.get("B").stream().anyMatch(s -> s.contains("diagonally"));
		assertTrue(foundDiagonal);
	}
}
