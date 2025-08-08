package sample.example.game;

import sample.example.config.GameConfig;
import sample.example.model.GameResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class GameEngine {
	private final GameConfig config;

	private final Random random = new Random();

	public GameEngine(GameConfig config) {
		this.config = config;
	}

	public String[][] generateMatrix() {
		int rows = config.getRows();
		int cols = config.getColumns();
		String[][] matrix = new String[rows][cols];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				matrix[r][c] = pickStandardSymbolForCell(c, r);
			}
		}

		int totalCells = rows * cols;
		int bonusesToPlace = Math.max(1, totalCells / 6);
		Map<String, Integer> bonusProbs = config.getProbabilities().getBonusSymbols().getSymbols();
		int totalBonusWeight = bonusProbs.values().stream().mapToInt(Integer::intValue).sum();
		for (int i = 0; i < bonusesToPlace; i++) {
			int rIndex = random.nextInt(rows);
			int cIndex = random.nextInt(cols);
			String bonusSymbol = weightedRandomPick(bonusProbs, totalBonusWeight);
			matrix[rIndex][cIndex] = bonusSymbol;
		}

		return matrix;
	}

	private String pickStandardSymbolForCell(int column, int row) {
		List<GameConfig.StandardSymbolProbability> probs = config.getProbabilities().getStandardSymbols();

		Map<String, Integer> symbolsForCell = null;
		for (GameConfig.StandardSymbolProbability p : probs) {
			if (p.getColumn() == column && p.getRow() == row) {
				symbolsForCell = p.getSymbols();
				break;
			}
		}
		if (symbolsForCell == null && !probs.isEmpty()) {
			symbolsForCell = probs.getFirst().getSymbols();
		}
		if (symbolsForCell == null) {
			throw new IllegalStateException("No standard symbols probability for cell " + column + ":" + row);
		}
		int totalProb = symbolsForCell.values().stream().mapToInt(Integer::intValue).sum();

		return weightedRandomPick(symbolsForCell, totalProb);
	}

	private String weightedRandomPick(Map<String, Integer> map, int totalWeight) {
		int val = random.nextInt(totalWeight);
		int sum = 0;
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			sum += entry.getValue();
			if (val < sum) {
				return entry.getKey();
			}
		}
		return map.keySet().iterator().next();
	}

	public Map<String, List<String>> detectWinningCombinations(String[][] matrix) {
		Map<String, List<String>> result = new HashMap<>();
		Map<String, Long> symbolCounts = new HashMap<>();

		int rows = matrix.length;
		int cols = matrix[0].length;
		for (String[] strings : matrix) {
			for (int c = 0; c < cols; c++) {
				String sym = strings[c];
				GameConfig.Symbol symbolConfig = config.getSymbols().get(sym);
				if (symbolConfig != null && "standard".equalsIgnoreCase(symbolConfig.getType())) {
					symbolCounts.put(sym, symbolCounts.getOrDefault(sym, 0L) + 1);
				}
			}
		}
		for (Map.Entry<String, GameConfig.WinCombination> wcEntry : config.getWinCombinations().entrySet()) {
			GameConfig.WinCombination wc = wcEntry.getValue();
			if ("same_symbols".equalsIgnoreCase(wc.getWhen()) && wc.getCount() != null) {
				for (Map.Entry<String, Long> sc : symbolCounts.entrySet()) {
					if (sc.getValue() >= wc.getCount()) {
						result.computeIfAbsent(sc.getKey(), k -> new ArrayList<>()).add(wcEntry.getKey());
					}
				}
			}
		}

		for (Map.Entry<String, GameConfig.WinCombination> wcEntry : config.getWinCombinations().entrySet()) {
			GameConfig.WinCombination wc = wcEntry.getValue();
			if ("linear_symbols".equalsIgnoreCase(wc.getWhen()) && wc.getCoveredAreas() != null) {
				for (List<String> area : wc.getCoveredAreas()) {
					if (area.isEmpty()) continue;
					String firstSymbol = null;
					boolean allSame = true;

					for (String cell : area) {
						String[] parts = cell.split(":");
						int row = Integer.parseInt(parts[0]);
						int col = Integer.parseInt(parts[1]);

						if (row >= rows || col >= cols) {
							allSame = false;
							break;
						}

						String sym = matrix[row][col];
						GameConfig.Symbol symConf = config.getSymbols().get(sym);

						if (symConf == null || !"standard".equalsIgnoreCase(symConf.getType())) {
							allSame = false;
							break;
						}

						if (firstSymbol == null) {
							firstSymbol = sym;
						}
						else if (!firstSymbol.equals(sym)) {
							allSame = false;
							break;
						}
					}
					if (allSame) {
						result.computeIfAbsent(firstSymbol, k -> new ArrayList<>()).add(wcEntry.getKey());
					}
				}
			}
		}

		return result;
	}
	public BigDecimal calculateReward(String[][] matrix, BigDecimal betAmount) {
		Map<String, List<String>> winningCombos = detectWinningCombinations(matrix);

		if (winningCombos.isEmpty()) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		BigDecimal totalReward = BigDecimal.ZERO;
		for (Map.Entry<String, List<String>> entry : winningCombos.entrySet()) {
			String symbol = entry.getKey();
			List<String> combos = entry.getValue();
			GameConfig.Symbol symbolConfig = config.getSymbols().get(symbol);
			if (symbolConfig == null) continue;

			double symbolRewardMultiplier = symbolConfig.getRewardMultiplier();

			double maxComboMultiplier = combos.stream()
					.mapToDouble(c -> config.getWinCombinations().get(c).getRewardMultiplier())
					.max()
					.orElse(1.0);

			BigDecimal rewardForSymbol = betAmount
					.multiply(BigDecimal.valueOf(symbolRewardMultiplier))
					.multiply(BigDecimal.valueOf(maxComboMultiplier));

			totalReward = totalReward.add(rewardForSymbol);
		}

		BonusEffect bonusEffect = findBonusEffect(matrix);

		switch (bonusEffect.type) {
			case MULTIPLY_REWARD -> totalReward = totalReward.multiply(BigDecimal.valueOf(bonusEffect.value));
			case EXTRA_BONUS -> totalReward = totalReward.add(BigDecimal.valueOf(bonusEffect.value));
			case MISS -> {
				// not today
			}
		}

		return totalReward.setScale(2, RoundingMode.HALF_UP);
	}


	private enum BonusType {
		MULTIPLY_REWARD,
		EXTRA_BONUS,
		MISS
	}

	private record BonusEffect(BonusType type, double value) {
	}

	private BonusEffect findBonusEffect(String[][] matrix) {
		double maxMultiplier = 1.0;
		double maxExtra = 0.0;

		for (String[] strings : matrix) {
			for (String sym : strings) {
				GameConfig.Symbol conf = config.getSymbols().get(sym);
				if (conf != null && "bonus".equalsIgnoreCase(conf.getType())) {
					String impact = conf.getImpact();
					if ("multiply_reward".equalsIgnoreCase(impact)) {
						if (conf.getRewardMultiplier() > maxMultiplier) {
							maxMultiplier = conf.getRewardMultiplier();
						}
					}
					else if ("extra_bonus".equalsIgnoreCase(impact)) {
						if (conf.getExtra() > maxExtra) {
							maxExtra = conf.getExtra();
						}
					}
				}
			}
		}

		if (maxMultiplier > 1.0) {
			return new BonusEffect(BonusType.MULTIPLY_REWARD, maxMultiplier);
		}
		if (maxExtra > 0.0) {
			return new BonusEffect(BonusType.EXTRA_BONUS, maxExtra);
		}
		return new BonusEffect(BonusType.MISS, 0);
	}



	public GameResult playRound(BigDecimal betAmount) {
		String[][] matrix = generateMatrix();
		Map<String, List<String>> winningCombos = detectWinningCombinations(matrix);
		BigDecimal reward = calculateReward(matrix, betAmount);

		GameResult result = new GameResult();
		result.setMatrix(matrix);
		result.setReward(reward);
		result.setAppliedWinningCombinations(winningCombos);

		return result;
	}
}
