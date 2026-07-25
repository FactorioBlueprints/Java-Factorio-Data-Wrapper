package com.demod.factorio;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.demod.factorio.prototype.RecipePrototype;

public class TotalRawCalculator {
	public static final String RAW_TIME = "_TIME_";
	private final Map<String, RecipePrototype> recipes;

	public TotalRawCalculator(Map<String, RecipePrototype> recipes) {
		this.recipes = recipes;
	}

	public Map<String, Double> compute(RecipePrototype recipe) {
		return compute(recipe, new LinkedHashSet<>());
	}

	private Map<String, Double> compute(RecipePrototype recipe, Set<String> expandedRecipeNames) {
		Map<String, Double> totalRaw = new LinkedHashMap<>();
		totalRaw.put(TotalRawCalculator.RAW_TIME, recipe.getEnergyRequired());
		expandedRecipeNames.add(recipe.getName());

		try {
			for (Entry<String, Integer> entry : recipe.getInputs().entrySet()) {
				String input = entry.getKey();
				Optional<RecipePrototype> findRecipe = recipes.values().stream()
						// XXX the nutrients-from-fish filter is here to match the bad Factorio behavior
						// of picking nutrients from biter eggs
						.filter(r -> !r.getName().equals("nutrients-from-fish"))
						.filter(RecipePrototype::isHandCraftable)
						.filter(r -> r.getOutputs().keySet().stream().anyMatch(i -> {
							return i.equals(input);
						})).findFirst();
				if (findRecipe.isPresent() && !expandedRecipeNames.contains(findRecipe.get().getName())) {
					RecipePrototype inputRecipe = findRecipe.get();
					Map<String, Double> inputTotalRaw = compute(inputRecipe, expandedRecipeNames);
					Double inputRunYield = inputRecipe.getOutputs().get(input);
					double inputRunCount = entry.getValue() / inputRunYield;
					inputTotalRaw.forEach((k, v) -> {
						totalRaw.put(k, totalRaw.getOrDefault(k, 0.0) + v * inputRunCount);
					});
				} else {
					totalRaw.put(input, totalRaw.getOrDefault(input, 0.0) + entry.getValue());
				}
			}
		} finally {
			expandedRecipeNames.remove(recipe.getName());
		}

		return totalRaw;
	}
}
