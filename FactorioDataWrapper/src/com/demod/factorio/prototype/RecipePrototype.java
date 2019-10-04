package com.demod.factorio.prototype;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.Utils;

public class RecipePrototype extends DataPrototype {

	private final String category;
	private final Map<String, Integer> inputs = new LinkedHashMap<>();
	private final Map<String, Double> outputs = new LinkedHashMap<>();
	private final double energyRequired;
	private final boolean handCraftable;
	private final boolean hasFluid;

	public RecipePrototype(ObjectNode objectNode, String name, String type, boolean expensive) {
		super(objectNode, name, type);
		JsonNode difficultyJson = objectNode.path(expensive ? "expensive" : "normal");
		if (!difficultyJson.isMissingNode()) {
			difficultyJson.fields().forEachRemaining(entry -> {
				String k = entry.getKey();
				JsonNode v = entry.getValue();
				objectNode.set(k, v);
			});
		}

		JsonNode ingredientsJson = objectNode.path("ingredients");
		for (JsonNode lv : ingredientsJson) {
			JsonNode nameNode = lv.path("name");
			if (nameNode.isMissingNode()) {
				inputs.put(lv.get(0).textValue(), lv.get(1).intValue());
			} else {
				inputs.put(nameNode.textValue(), lv.path("amount").intValue());
			}
		};

		JsonNode resultJson = objectNode.path("result");
		if (resultJson.isMissingNode()) {
			resultJson = objectNode.path("results");
		}
		if (resultJson instanceof ArrayNode) {
			for (JsonNode lv : resultJson) {
				JsonNode nameNode = lv.path("name");
				if (nameNode.isMissingNode()) {
					outputs.put(lv.get(0).textValue(), lv.get(1).doubleValue());
				} else {
					JsonNode probabilityJson = lv.path("probability");
					if (probabilityJson.isMissingNode()) {
						outputs.put(lv.path("name").textValue(), lv.path("amount").doubleValue());
					} else {
						outputs.put(lv.path("name").textValue(), probabilityJson.doubleValue());
					}
				}
			};
		} else {
			outputs.put(resultJson.textValue(), objectNode.path("result_count").asDouble(1));
		}

		energyRequired = objectNode.path("energy_required").asDouble(0.5);
		category = objectNode.path("category").asText("crafting");
		handCraftable = category.equals("crafting");
		hasFluid = this.calculateHasFluid(objectNode);
	}

	private boolean calculateHasFluid(ObjectNode json) {
		List<JsonNode> items = new ArrayList<>();
		JsonNode ingredientsJson = json.path("ingredients");
		ingredientsJson.forEach(items::add);
		JsonNode resultsJson = json.path("results");
		if (!resultsJson.isMissingNode()) {
			items.add(resultsJson);
		}

		return items.stream()
				.map(item -> item.path("type"))
				.anyMatch(typeJson -> !typeJson.isMissingNode() && typeJson.textValue().equals("fluid"));
	}


	public String getCategory() {
		return category;
	}

	public double getEnergyRequired() {
		return energyRequired;
	}

	public Map<String, Integer> getInputs() {
		return inputs;
	}

	public Map<String, Double> getOutputs() {
		return outputs;
	}

	public boolean isHandCraftable() {
		return handCraftable;
	}

	public boolean hasFluid() {
		return hasFluid;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Recipe: " + getName() + (!isHandCraftable() ? " (MACHINE ONLY)" : "") + "\n");
		sb.append("\tTIME " + getEnergyRequired() + "\n");
		getInputs().forEach((k, v) -> {
			sb.append("\tIN " + k + " " + v + "\n");
		});
		getOutputs().forEach((k, v) -> {
			sb.append("\tOUT " + k + " " + v + "\n");
		});
		return sb.toString();
	}
}
