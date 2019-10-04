package com.demod.factorio.apps;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModInfo;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.RecipePrototype;

public class FactorioJSONMain {

	private static void json_Recipes(DataTable table, PrintWriter pw) {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode json = objectMapper.createObjectNode();

		table.getRecipes().entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).forEach(e -> {
			RecipePrototype recipe = e.getValue();
			ObjectNode recipeJson = json.putObject(recipe.getName());

			recipeJson.put("wiki-name", table.getWikiRecipeName(recipe.getName()));
			recipeJson.put("type", recipe.getType());
			recipeJson.put("energy-required", recipe.getEnergyRequired());

			ObjectNode inputsJson = recipeJson.putObject("inputs");
			recipe.getInputs().entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
					.forEach(e2 -> {
						inputsJson.put(e2.getKey(), e2.getValue());
					});

			ObjectNode outputsJson = recipeJson.putObject("outputs");
			recipe.getOutputs().entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
					.forEach(e2 -> {
						outputsJson.put(e2.getKey(), e2.getValue());
					});
		});

		ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
		try {
			String string = objectWriter.writeValueAsString(json);
			pw.println(string);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws IOException {
		DataTable table = FactorioData.getTable();
		ModInfo baseInfo = new ModInfo(
				Utils.readJsonFromStream(new FileInputStream(new File(FactorioData.factorio, "data/base/info.json"))));

		File outputFolder = new File("output/" + baseInfo.getVersion());
		outputFolder.mkdirs();

		try (PrintWriter pw = new PrintWriter(
				new File(outputFolder, "json-recipes-" + baseInfo.getVersion() + ".txt"))) {
			json_Recipes(table, pw);
		}

		Desktop.getDesktop().open(outputFolder);
	}

}
