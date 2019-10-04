package com.demod.factorio.prototype;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.FactorioData;
import com.demod.factorio.Utils;

public class TechPrototype extends DataPrototype {

	public static class Effect {
		private final JsonNode json;
		private final String type;
		private final double modifier;
		private final Optional<String> recipe;
		private final String key;

		public Effect(JsonNode json) {
			this.json = json;
			type = json.path("type").textValue();
			JsonNode modifierJson = json.path("modifier");
			modifier = modifierJson.isNumber()
					? modifierJson.doubleValue()
					: 0;
			recipe = this.type.equals("unlock-recipe")
					? Optional.of(json.path("recipe").textValue())
					: Optional.empty();
			key = type.equals("ammo-damage")
					? type + "|" + json.path("ammo_category").textValue()
					: type;
		}

		public double getModifier() {
			return modifier;
		}

		public String getType() {
			return type;
		}

		private Optional<String> getRecipe() {
			return recipe;
		}

		public String getKey() {
			return key;
		}

		public JsonNode json() {
			return json;
		}
	}

	private final boolean upgrade;
	private final List<String> prerequisites = new ArrayList<>();
	private final List<Effect> effects = new ArrayList<>();
	private final LinkedHashMap<String, Integer> ingredients = new LinkedHashMap<>();
	private int count;
	private final double time;
	private final String order;
	private final List<String> recipeUnlocks = new ArrayList<>();
	private final Optional<String> maxLevel;
	private final boolean maxLevelInfinite;
	private final Optional<IntUnaryOperator> bonusCountFormula;
	private final Optional<String> bonusCountFormulaVisual;

	private final boolean firstBonus;

	private boolean bonus;
	private List<TechPrototype> bonusGroup;
	private String bonusName;
	private int bonusLevel;

	public TechPrototype(ObjectNode json, String name, String type, Set<String> excludedRecipesAndItems) {
		super(json, name, type);

		upgrade = json.path("upgrade").booleanValue();
		order = json.path("order").textValue();

		JsonNode prerequisites = json.path("prerequisites");
		for (JsonNode prerequisite : prerequisites) {
			this.prerequisites.add(prerequisite.textValue());
		};

		JsonNode effects = json.path("effects");
		for (JsonNode effect : effects) {
			this.effects.add(new Effect(effect));
		};

		JsonNode unitJson = json.path("unit");
		JsonNode ingredients = unitJson.path("ingredients");
		for (JsonNode ingredient : ingredients) {
			this.ingredients.put(ingredient.path(1).textValue(), ingredient.path(2).intValue());
		};
		count = unitJson.path("count").intValue();
		time = unitJson.path("time").doubleValue();

		for (Effect effect : getEffects()) {
			Optional<String> recipe = effect.getRecipe();
			recipe.ifPresent(recipeName -> {
				if (!excludedRecipesAndItems.contains(recipeName)) {
					recipeUnlocks.add(recipeName);
				}
			});
		}

		JsonNode maxLevelJson = json.path("max_level");
		if (!maxLevelJson.isMissingNode()) {
			String value = maxLevelJson.textValue();
			maxLevel = Optional.of(value);
			maxLevelInfinite = value.equals("infinite");
		} else {
			maxLevel = Optional.empty();
			maxLevelInfinite = false;
		}
		bonusCountFormulaVisual = calculateCountFormula(json);
		bonusCountFormula = bonusCountFormulaVisual.map(FactorioData::parseCountFormula);

		firstBonus = calculateFirstBonus(name);
	}

	private Optional<String> calculateCountFormula(ObjectNode json) {
		JsonNode countFormulaJson = json.path("unit").path("count_formula");
		if (!countFormulaJson.isMissingNode()) {
			String countFormulaString = countFormulaJson.textValue();
			return Optional.of(countFormulaString);
		}

		return Optional.empty();
	}

	private boolean calculateFirstBonus(String name) {
		return (upgrade || maxLevelInfinite) && name.endsWith("-1");
	}


	public Optional<IntUnaryOperator> getBonusCountFormula() {
		return bonusCountFormula;
	}

	public Optional<String> getBonusCountFormulaVisual() {
		return bonusCountFormulaVisual;
	}

	public List<TechPrototype> getBonusGroup() {
		return bonusGroup;
	}

	public int getBonusLevel() {
		return bonusLevel;
	}

	public String getBonusName() {
		return bonusName;
	}

	public int getCount() {
		return count;
	}

	public int getEffectiveCount() {
		return (isBonus() && getBonusCountFormula().isPresent())
				? getBonusCountFormula().get().applyAsInt(getBonusLevel()) : getCount();
	}

	public List<Effect> getEffects() {
		return effects;
	}

	public LinkedHashMap<String, Integer> getIngredients() {
		return ingredients;
	}

	public Optional<String> getMaxLevel() {
		return maxLevel;
	}

	public String getOrder() {
		return order;
	}

	public List<String> getPrerequisites() {
		return prerequisites;
	}

	public List<String> getRecipeUnlocks() {
		return recipeUnlocks;
	}

	public double getTime() {
		return time;
	}

	public boolean isBonus() {
		return bonus;
	}

	public boolean isFirstBonus() {
		return firstBonus;
	}

	public boolean isMaxLevelInfinite() {
		return maxLevelInfinite;
	}

	public boolean isUpgrade() {
		return upgrade;
	}

	public void setBonus(boolean bonus) {
		this.bonus = bonus;
	}

	public void setBonusGroup(List<TechPrototype> bonusGroup) {
		this.bonusGroup = bonusGroup;
	}

	public void setBonusLevel(int bonusLevel) {
		this.bonusLevel = bonusLevel;
	}

	public void setBonusName(String bonusName) {
		this.bonusName = bonusName;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
