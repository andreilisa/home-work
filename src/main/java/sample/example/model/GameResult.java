package sample.example.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameResult {
	private String[][] matrix;
	private BigDecimal reward;
	private Map<String, List<String>> appliedWinningCombinations;
	private Set<String> appliedBonusSymbols;

	public String[][] getMatrix() {
		return matrix;
	}

	public void setMatrix(String[][] matrix) {
		this.matrix = matrix;
	}

	public BigDecimal getReward() {
		return reward;
	}

	public void setReward(BigDecimal reward) {
		this.reward = reward;
	}

	public Map<String, List<String>> getAppliedWinningCombinations() {
		return appliedWinningCombinations;
	}

	public void setAppliedWinningCombinations(Map<String, List<String>> appliedWinningCombinations) {
		this.appliedWinningCombinations = appliedWinningCombinations;
	}

	public Set<String> getAppliedBonusSymbols() {
		return appliedBonusSymbols;
	}

	public void setAppliedBonusSymbols(Set<String> appliedBonusSymbols) {
		this.appliedBonusSymbols = appliedBonusSymbols;
	}
}
