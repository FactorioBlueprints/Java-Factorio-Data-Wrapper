package com.demod.factorio;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.demod.factorio.prototype.RecipePrototype;

public class TotalRawCalculator {
	public static final String RAW_TIME = "_TIME_";
	private final Map<String, RecipePrototype> recipes;
	private final Set<String> characterCraftingCategories;

	public TotalRawCalculator(Map<String, RecipePrototype> recipes, Set<String> characterCraftingCategories) {
		this.recipes = recipes;
		this.characterCraftingCategories = characterCraftingCategories;
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
				Optional<RecipePrototype> findRecipe = findRecipe(input, expandedRecipeNames);
				if (findRecipe.isPresent()) {
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

	private Optional<RecipePrototype> findRecipe(String input, Set<String> expandedRecipeNames) {
		// Nutrients have several context-dependent production recipes, so no one recipe is canonical.
		if (input.equals("nutrients")) {
			return Optional.empty();
		}
		return recipes.values().stream()
				// Factorio's allow_decomposition controls whether a recipe is expanded for the tooltip's
				// "Total raw" calculation:
				// https://lua-api.factorio.com/latest/prototypes/RecipePrototype.html#allow_decomposition
				.filter(RecipePrototype::isDecomposable)
				.filter(r -> r.isHandCraftable(characterCraftingCategories))
				.filter(r -> !r.isRecycling())
				.filter(r -> r.getOutputs().containsKey(input))
				.filter(r -> !expandedRecipeNames.contains(r.getName())).findFirst();
	}
}
