package com.demod.factorio.apps;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.Config;
import com.demod.factorio.DataTable;
import com.demod.factorio.FactorioData;
import com.demod.factorio.ModInfo;
import com.demod.factorio.TotalRawCalculator;
import com.demod.factorio.Utils;
import com.demod.factorio.prototype.DataPrototype;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.factorio.prototype.TechPrototype;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;

public class FactorioWikiMain {

	private static class WikiTypeMatch {
		boolean item = false, recipe = false, entity = false, equipment = false, tile = false, fluid = false;
		String entityType;
		String equipmentType;
		String itemType;

		public void setEntity(String type) {
			entity = true;
			entityType = type;
		}

		public void setEquipment(String type) {
			equipment = true;
			equipmentType = type;
		}

		public void setItem(String type) {
			item = true;
			itemType = type;
		}

		@Override
		public String toString() {
			if (!item && !recipe && !fluid) {
				return "N/A";
			} else if (equipment) {
				return equipmentType;
			} else if (fluid) {
				return "fluid";
			} else if (tile) {
				return "tile";
			} else if (entity) {
				return entityType;
			} else if (item) {
				return itemType;
			} else if (recipe) {
				return "recipe";
			}
			return "???";
		}
	}

	public static final Map<String, Integer> wiki_ScienceOrdering = new LinkedHashMap<>();

	static {
		wiki_ScienceOrdering.put("automation-science-pack", 1);
		wiki_ScienceOrdering.put("logistic-science-pack", 2);
		wiki_ScienceOrdering.put("military-science-pack", 3);
		wiki_ScienceOrdering.put("chemical-science-pack", 4);
		wiki_ScienceOrdering.put("production-science-pack", 5);
		wiki_ScienceOrdering.put("utility-science-pack", 6);
		wiki_ScienceOrdering.put("space-science-pack", 7);
	}

	private static Map<String, Function<Double, String>> wiki_EffectModifierFormatter = new LinkedHashMap<>();
	static {
		Function<Double, String> fmtCount = v -> wiki_fmtDouble(v);
		Function<Double, String> fmtPercent = v -> String.format("%.0f%%", v * 100);
		Function<Double, String> fmtSlot = v -> "+" + wiki_fmtDouble(v) + " slots";

		wiki_EffectModifierFormatter.put("ammo-damage", fmtPercent);
		wiki_EffectModifierFormatter.put("character-logistic-slots", fmtSlot);
		wiki_EffectModifierFormatter.put("character-logistic-trash-slots", fmtSlot);
		wiki_EffectModifierFormatter.put("gun-speed", fmtPercent);
		wiki_EffectModifierFormatter.put("laboratory-speed", fmtPercent);
		wiki_EffectModifierFormatter.put("maximum-following-robots-count", fmtCount);
		wiki_EffectModifierFormatter.put("mining-drill-productivity-bonus", fmtPercent);
		wiki_EffectModifierFormatter.put("train-braking-force-bonus", fmtPercent);
		wiki_EffectModifierFormatter.put("turret-attack", fmtPercent);
		wiki_EffectModifierFormatter.put("worker-robot-speed", fmtPercent);
		wiki_EffectModifierFormatter.put("worker-robot-storage", fmtCount);
	}

	private static ModInfo baseInfo;
	private static File folder;

	private static ObjectNode createObjectNode() {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.createObjectNode();
	}

	private static Map<String, WikiTypeMatch> generateWikiTypes(DataTable table) {
		Map<String, WikiTypeMatch> protoMatches = new LinkedHashMap<>();

		table.getItems().entrySet().forEach(e -> protoMatches.computeIfAbsent(e.getKey(), k -> new WikiTypeMatch())
				.setItem(e.getValue().getType()));
		table.getRecipes().keySet()
				.forEach(name -> protoMatches.computeIfAbsent(name, k -> new WikiTypeMatch()).recipe = true);
		table.getTiles().keySet()
				.forEach(name -> protoMatches.computeIfAbsent(name, k -> new WikiTypeMatch()).tile = true);
		table.getFluids().keySet()
				.forEach(name -> protoMatches.computeIfAbsent(name, k -> new WikiTypeMatch()).fluid = true);
		table.getEntities().entrySet().forEach(e -> protoMatches.computeIfAbsent(e.getKey(), k -> new WikiTypeMatch())
				.setEntity(e.getValue().getType()));
		table.getEquipments().entrySet().forEach(e -> protoMatches.computeIfAbsent(e.getKey(), k -> new WikiTypeMatch())
				.setEquipment(e.getValue().getType()));

		return protoMatches;
	}

	public static void main(String[] args) throws IOException {
		DataTable table = FactorioData.getTable();
		baseInfo = new ModInfo(
				Utils.readJsonFromStream(new FileInputStream(new File(FactorioData.factorio, "data/base/info.json"))));

		String outputPath = Config.get().path("output").asText("output");

		folder = new File(outputPath + File.separator + baseInfo.getVersion());
		folder.mkdirs();

		Map<String, WikiTypeMatch> wikiTypes = generateWikiTypes(table);

		write(wiki_Technologies(table), "wiki-technologies");
		write(wiki_FormulaTechnologies(table), "wiki-formula-technologies");
		write(wiki_Recipes(table), "wiki-recipes");
		write(wiki_Types(table, wikiTypes), "wiki-types");
		write(wiki_Items(table), "wiki-items");
		// write(wiki_TypeTree(table), "wiki-type-tree");
		write(wiki_Entities(table, wikiTypes), "wiki-entities");
		write(wiki_DataRawTree(table), "data-raw-tree");

		// wiki_GenerateTintedIcons(table, new File(outputFolder, "icons"));

		Desktop.getDesktop().open(folder);
	}

	/*private static ArrayNode arrayPair(Object a, Object b) {
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode json = objectMapper.createArrayNode();
		json.add(a);
		json.add(b);
		return json;
	}

	private static <T> Collector<T, Object, ArrayNode> toArrayNode() {
		return Collectors.collectingAndThen(Collectors.toList(), ArrayNode::new);
	}*/

	private static ObjectNode wiki_DataRawTree(DataTable table) {
		ObjectNode json = createObjectNode();

		Multimap<String, String> leafs = LinkedHashMultimap.create();

		ObjectNode rawJson = table.getRawJson();
		for (JsonNode v : rawJson) {
			ObjectNode objectNode = (ObjectNode) v;
			for (JsonNode jsonNode : objectNode) {
				String type = jsonNode.path("type").textValue();
				String name = jsonNode.path("name").textValue();
				leafs.put(type, name);
			};
		};

		leafs.keySet().stream().sorted().forEach(type -> {
			ArrayNode jsonNodes = json.putArray(type);
			leafs.get(type).stream().sorted().forEach(jsonNodes::add);
		});

		return json;
	}

	private static ObjectNode wiki_Entities(DataTable table, Map<String, WikiTypeMatch> wikiTypes) {
		ObjectNode json = createObjectNode();

		Optional<JsonNode> optUtilityConstantsLua = table.getRaw("utility-constants", "default");
		JsonNode utilityConstantsLua = optUtilityConstantsLua.get();

		Color defaultFriendlyColor = Utils.parseColor(utilityConstantsLua.path("chart").path("default_friendly_color"));
		Map<String, Color> defaultFriendlyColorByType = new HashMap<>();
		utilityConstantsLua
				.path("chart")
				.path("default_friendly_color_by_type")
				.fields()
				.forEachRemaining(entry -> {
					String key = entry.getKey();
					Color color = Utils.parseColor(entry.getValue());
					defaultFriendlyColorByType.put(key, color);
				});

		table.getEntities().values().stream().sorted(Comparator.comparing(DataPrototype::getName))
				.filter(e -> !wikiTypes.get(e.getName()).toString().equals("N/A")).forEach(e -> {
					Color mapColor = null;
					JsonNode friendlyMapColorJson = e.getObjectNode().path("friendly_map_color");
					if (!friendlyMapColorJson.isMissingNode()) {
						mapColor = Utils.parseColor(friendlyMapColorJson);
					} else {
						JsonNode mapColorJson = e.getObjectNode().path("map_color");
						if (!mapColorJson.isMissingNode()) {
							mapColor = Utils.parseColor(mapColorJson);
						} else {
							mapColor = defaultFriendlyColorByType.get(e.getObjectNode().path("type").textValue());
							if (mapColor == null && !e.getFlags().contains("not-on-map")) {
								mapColor = defaultFriendlyColor;
							}
						}
					}

					if (e.getType().equals("car") || e.getType().equals("locomotive")
							|| e.getType().contains("wagon") || e.getType().equals("train-stop")) {
						mapColor = null; // these entity types are not drawn on map normally
					}

					double health = e.getObjectNode().path("max_health").doubleValue();
					JsonNode minableLua = e.getObjectNode().path("minable");
					JsonNode resistances = e.getObjectNode().path("resistances");
					JsonNode energySource = e.getObjectNode().path("energy_source");
					if (energySource.isMissingNode() && !e.getObjectNode().path("burner").isMissingNode())
						energySource = e.getObjectNode().path("burner");
					double emissions = 0.0;

					if (!energySource.isMissingNode()) {
						JsonNode prototypeEmissions = energySource.path("emissions_per_minute");
						if (!prototypeEmissions.isMissingNode())
							emissions = prototypeEmissions.doubleValue();
					}

					if (mapColor != null || health > 0 || !minableLua.isMissingNode() || emissions > 0 || !resistances.isMissingNode()) {
						ObjectNode itemJson = createObjectNode();
						json.put(table.getWikiEntityName(e.getName()), itemJson);

						if (mapColor != null)
							itemJson.put("map-color", String.format("%02x%02x%02x", mapColor.getRed(),
									mapColor.getGreen(), mapColor.getBlue()));
						if (health > 0)
							itemJson.put("health", health);
						if (!minableLua.isMissingNode())
							itemJson.put("mining-time", minableLua.path("mining_time").doubleValue());
						if (emissions > 0)
							itemJson.put("pollution", Math.round(emissions * 100) / 100.0);
						if (!resistances.isMissingNode()) {
							ObjectNode resistancesJson = createObjectNode();
							itemJson.put("resistances", resistancesJson);

							for (JsonNode resist : resistances) {
								ObjectNode resistJson = createObjectNode();
								resistancesJson.put(resist.path("type").toString(), resistJson);
								JsonNode percent = resist.path("percent");
								JsonNode decrease = resist.path("decrease");
								resistJson.put("percent", !percent.isMissingNode() ? percent.intValue() : 0);
								resistJson.put("decrease", !decrease.isMissingNode() ? decrease.intValue() : 0);
							};
						}
					}
				});

		// not entities but lets just.. ignore that
		table.getTiles().values().stream().sorted((t1, t2) -> t1.getName().compareTo(t2.getName()))
				.filter(t -> table.hasWikiEntityName(t.getName())).forEach(t -> {
					Color mapColor = null;
					JsonNode mapColorJson = t.getObjectNode().path("map_color");
					if (!mapColorJson.isMissingNode())
						mapColor = Utils.parseColor(mapColorJson);

					if (mapColor != null) {
						ObjectNode itemJson = createObjectNode();
						json.put(table.getWikiEntityName(t.getName()), itemJson);

						itemJson.put("map-color", String.format("%02x%02x%02x", mapColor.getRed(), mapColor.getGreen(),
								mapColor.getBlue()));
					}
				});

		return json;
	}

	public static String wiki_fmtDouble(double value) {
		if (value == (long) value) {
			return String.format("%d", (long) value);
		} else {
			return Double.toString(value);// String.format("%f", value);
		}
	}

	/**
	 * Same as {@link #wiki_fmtName(String, ObjectNode)}, but adds a ", [number]"
	 * when there is a number as the last part of the name. This adds the number to
	 * the icon.
	 */
	public static String wiki_fmtNumberedWikiName(String wikiName) {
		String[] split = wikiName.split("\\s+");
		Integer num = Ints.tryParse(split[split.length - 1]);
		if (num != null) {
			wikiName += ", " + num;
		}
		return wikiName;
	}

	private static ObjectNode wiki_FormulaTechnologies(DataTable table) {
		ObjectNode json = createObjectNode();
		table.getTechnologies().values().stream().filter(t -> t.isBonus()).map(t -> t.getBonusName()).distinct()
				.sorted().forEach(bonusName -> {
					ArrayNode itemJson = json.putArray(table.getWikiTechnologyName(bonusName));

					TechPrototype firstTech = table.getTechnology(bonusName + "-1").get();
					int maxBonus = firstTech.getBonusGroup().stream().mapToInt(TechPrototype::getBonusLevel).max()
							.getAsInt();

					double time = 0;
					Optional<IntUnaryOperator> countFormula = Optional.empty();
					LinkedHashMap<String, Integer> ingredients = null;
					List<TechPrototype.Effect> effects = null;
					Map<String, Double> effectTypeSum = new LinkedHashMap<>();

					for (int i = 1; i <= maxBonus; i++) {
						Optional<TechPrototype> optTech = table.getTechnology(bonusName + "-" + i);
						int count;
						boolean showFormula = false;
						String formula = null;
						if (optTech.isPresent()) {
							TechPrototype tech = optTech.get();

							time = tech.getTime();
							ingredients = tech.getIngredients();
							effects = tech.getEffects();

							if (tech.getBonusCountFormula().isPresent()) {
								countFormula = tech.getBonusCountFormula();
							}
							count = tech.getEffectiveCount();

							if (tech.isMaxLevelInfinite()) {
								showFormula = true;
								formula = tech.getBonusCountFormulaVisual().get().replace("L", "Level");
							}
						} else {
							count = countFormula.get().applyAsInt(i);
						}

						effects.forEach(e -> {
							double sum = effectTypeSum.getOrDefault(e.getKey(), 0.0);
							sum += e.getModifier();
							effectTypeSum.put(e.getKey(), sum);
						});

						// TODO convert markup to json
						String markup = "| {{Icontech|" + table.getWikiTechnologyName(bonusName) + " (research)|" + i
								+ "}} " + table.getWikiTechnologyName(bonusName) + " " + i + " || {{Icon|Time|"
								+ wiki_fmtDouble(time) + "}} "
								+ ingredients.entrySet().stream()
										.sorted((e1, e2) -> Integer.compare(wiki_ScienceOrdering.get(e1.getKey()),
												wiki_ScienceOrdering.get(e2.getKey())))
										.map(e -> "{{Icon|" + table.getWikiItemName(e.getKey()) + "|" + e.getValue()
												+ "}}")
										.collect(Collectors.joining(" "))
								+ " <big>X " + count + "</big>" + (showFormula ? (" " + formula)
										: "")
								+ " || "
								+ effects.stream()
										.map(e -> wiki_EffectModifierFormatter.getOrDefault(e.getType(), v -> "")
												.apply(e.getModifier()))
										.filter(s -> !s.isEmpty()).distinct().collect(Collectors.joining(" "))
								+ " || "
								+ effectTypeSum.entrySet().stream()
										.map(e -> wiki_EffectModifierFormatter
												.getOrDefault(e.getKey().split("\\|")[0], v -> "").apply(e.getValue()))
										.filter(s -> !s.isEmpty()).distinct().collect(Collectors.joining(" "));
						itemJson.add(markup);
					}
				});
		return json;
	}

	@SuppressWarnings("unused")
	private static void wiki_GenerateTintedIcons(DataTable table, File folder) {
		folder.mkdirs();

		table.getRecipes().values().stream().forEach(recipe -> {
			if (!recipe.getObjectNode().path("icons").isMissingNode()) {
				System.out.println();
				System.out.println(recipe.getName());
				// Utils.debugPrintJson(recipe.getObjectNode().path("icons"));
				try {
					ImageIO.write(FactorioData.getIcon(recipe), "PNG", new File(folder, recipe.getName() + ".png"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		table.getItems().values().stream().forEach(item -> {
			if (!item.getObjectNode().path("icons").isMissingNode()) {
				System.out.println();
				System.out.println(item.getName());
				// Utils.debugPrintJson(item.getObjectNode().path("icons"));
				try {
					ImageIO.write(FactorioData.getIcon(item), "PNG", new File(folder, item.getName() + ".png"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private static ObjectNode wiki_Items(DataTable table) {
		ObjectNode json = createObjectNode();

		Multimap<String, String> requiredTechnologies = LinkedHashMultimap.create();
		table.getTechnologies().values()
				.forEach(tech -> tech.getRecipeUnlocks().stream().map(table.getRecipes()::get)
						.flatMap(r -> r.getOutputs().keySet().stream())
						.forEach(name -> requiredTechnologies.put(name, tech.getName())));

		table.getItems().values().stream().sorted((i1, i2) -> i1.getName().compareTo(i2.getName())).forEach(item -> {
			ObjectNode itemJson = createObjectNode();
			json.put(table.getWikiItemName(item.getName()), itemJson);

			List<String> names = table.getRecipes().values().stream()
					.filter(r -> r.getInputs().containsKey(item.getName())).map(RecipePrototype::getName).sorted()
					.collect(Collectors.toList());
			if (!names.isEmpty()) {
				ArrayNode consumers = itemJson.putArray("consumers");
				names.stream().map(n -> table.getWikiRecipeName(n)).forEach(consumers::add);
			}

			itemJson.put("stack-size", item.getObjectNode().path("stack_size").intValue());

			Collection<String> reqTech = requiredTechnologies.get(item.getName());
			if (!reqTech.isEmpty()) {
				ArrayNode technologies = itemJson.putArray("required-technologies");
				reqTech.stream().sorted().map(n -> table.getWikiTechnologyName(n)).forEach(technologies::add);
			}
		});

		return json;
	}

	/**
	 *
	 * List all hand-craftable recipes.
	 *
	 * <pre>
	 * |recipe = Time, [ENERGY] + [Ingredient Name], [Ingredient Count] + ...  = [Ingredient Name], [Ingredient Count] + ...
	 * </pre>
	 *
	 * If there is only one output ingredient with just 1 count, do not include the
	 * = part
	 *
	 * <pre>
	 * |total-raw = Time, [ENERGY] + [Ingredient Name], [Ingredient Count] + ...
	 * </pre>
	 *
	 */
	private static ObjectNode wiki_Recipes(DataTable table) {
		ObjectNode json = createObjectNode();

		Map<String, RecipePrototype> normalRecipes = table.getRecipes();
		Map<String, RecipePrototype> expensiveRecipes = table.getExpensiveRecipes();

		TotalRawCalculator normalTotalRawCalculator = new TotalRawCalculator(normalRecipes);
		TotalRawCalculator expensiveTotalRawCalculator = new TotalRawCalculator(expensiveRecipes);

		Sets.union(normalRecipes.keySet(), expensiveRecipes.keySet()).stream().sorted().forEach(name -> {
			ObjectNode item = createObjectNode();
			json.put(table.getWikiRecipeName(name), item);

			{
				RecipePrototype recipe = normalRecipes.get(name);
				if (recipe != null) {
					ArrayNode recipeJson = item.putArray("recipe");
					ArrayNode time = recipeJson.addArray();
					time.add("Time") ;
					time.add(recipe.getEnergyRequired());

					recipe.getInputs().entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
							.forEach(entry -> {
								ArrayNode pair = recipeJson.addArray();
								pair.add(table.getWikiItemName(entry.getKey()));
								pair.add(entry.getValue());
							});
					if (recipe.getOutputs().size() > 1
							|| recipe.getOutputs().values().stream().findFirst().get() != 1) {
						ArrayNode recipeOutputJson = item.putArray("recipe-output");
						recipe.getOutputs().entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
								.forEach(entry -> {
									ArrayNode pair = recipeOutputJson.addArray();
									pair.add(table.getWikiItemName(entry.getKey()));
									pair.add(entry.getValue());
								});
					}

					Map<String, Double> totalRaw = normalTotalRawCalculator.compute(recipe);

					ArrayNode totalRawJson = item.putArray("total-raw");
					ArrayNode pair = totalRawJson.addArray();
					pair.add("Time");
					pair.add(totalRaw.get(TotalRawCalculator.RAW_TIME));
					totalRaw.entrySet().stream().filter(e -> !e.getKey().equals(TotalRawCalculator.RAW_TIME))
							.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).forEach(entry -> {
								ArrayNode innerPair = totalRawJson.addArray();
								innerPair.add(table.getWikiItemName(entry.getKey()));
								innerPair.add(entry.getValue());
							});
				}
			}
			{
				RecipePrototype recipe = expensiveRecipes.get(name);
				if (recipe != null) {
					ArrayNode recipeJson = item.putArray("expensive-recipe");
					ArrayNode pair = recipeJson.addArray();
					pair.add("Time");
					pair.add(recipe.getEnergyRequired());
					recipe.getInputs().entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
							.forEach(entry -> {
								ArrayNode innerPair = recipeJson.addArray();
								innerPair.add(table.getWikiItemName(entry.getKey()));
								innerPair.add(entry.getValue());
							});
					if (recipe.getOutputs().size() > 1
							|| recipe.getOutputs().values().stream().findFirst().get() != 1) {
						ArrayNode recipeOutputJson = item.putArray("expensive-recipe-output");
						recipe.getOutputs().entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
								.forEach(entry -> {
									ArrayNode innerPair = recipeOutputJson.addArray();
									innerPair.add(table.getWikiItemName(entry.getKey()));
									innerPair.add(entry.getValue());
								});
					}

					Map<String, Double> totalRaw = expensiveTotalRawCalculator.compute(recipe);

					ArrayNode totalRawJson = item.putArray("expensive-total-raw");
					ArrayNode innerPair = totalRawJson.addArray();
					innerPair.add("Time");
					innerPair.add(totalRaw.get(TotalRawCalculator.RAW_TIME));
					totalRaw.entrySet().stream().filter(e -> !e.getKey().equals(TotalRawCalculator.RAW_TIME))
							.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).forEach(entry -> {
								ArrayNode innerInnerPair = totalRawJson.addArray();
								innerInnerPair.add(table.getWikiItemName(entry.getKey()));
								innerInnerPair.add(entry.getValue());
							});
				}
			}

			// category must be same for expensive and normal
			String category = normalRecipes.get(name).getCategory();
			Map<String, List<EntityPrototype>> craftingCategories = table.getCraftingCategories();
			ArrayNode producers = item.putArray("producers");
			craftingCategories
					.get(category)
					.stream()
					.sorted((e1, e2) -> e1.getName().compareTo(e2.getName()))
					.map(e -> table.getWikiEntityName(e.getName()))
					.forEach(producers::add);

		});

		return json;
	}

	/**
	 * | cost = Time,30 + Science pack 1,1 + Science pack 2,1 + Science pack 3,1<br>
	 * |cost-multiplier = 1000 <br>
	 * |expensive-cost-multiplier = 4000<br>
	 * |required-technologies = Advanced electronics + Concrete <br>
	 * |allows = Atomic bomb + Uranium ammo + Kovarex enrichment process + Nuclear
	 * fuel reprocessing <br>
	 * |effects = Nuclear reactor + Centrifuge + Uranium processing + Uranium fuel
	 * cell + Heat exchanger + Heat pipe + Steam turbine <br>
	 * <br>
	 * allows are the techs it unlocks, effects are the items it unlocks. <br>
	 * bonuses are handled weirdly, we do one infobox per kind of bonus that gives
	 * the required technologies for the first tier of the bonus, no effect and the
	 * other bonus research as the allows, like this: <br>
	 * | cost = time, 60 + science pack 1,1 + science pack 2,1 + science pack 3,1 +
	 * military science pack,1 <br>
	 * | cost-multiplier = 100 <br>
	 * | required-technologies = tanks <br>
	 * | allows = Cannon shell damage (research), 2-5<br>
	 * <br>
	 * - Bilka
	 */
	private static ObjectNode wiki_Technologies(DataTable table) {
		ObjectNode json = createObjectNode();

		Multimap<String, String> allowsMap = LinkedHashMultimap.create();
		table.getTechnologies().values().forEach(tech -> tech.getPrerequisites()
				.forEach(n -> allowsMap.put(n, tech.isBonus() ? tech.getBonusName() : tech.getName())));

		table.getTechnologies().values().stream().sorted((t1, t2) -> t1.getName().compareTo(t2.getName()))
				.filter(t -> !t.isBonus() || t.isFirstBonus()).forEach(tech -> {
					ObjectNode itemJson = createObjectNode();
					json.put(table.getWikiTechnologyName(tech.isBonus() ? tech.getBonusName() : tech.getName()),
							itemJson);

					itemJson.put("internal-name", tech.getName());
					ArrayNode costJson = itemJson.putArray("cost");

					ArrayNode pair = costJson.addArray();
					pair.add("Time");
					pair.add( tech.getTime());
					tech.getIngredients().entrySet().stream().sorted((e1, e2) -> Integer
							.compare(wiki_ScienceOrdering.get(e1.getKey()), wiki_ScienceOrdering.get(e2.getKey())))
							.forEach(entry -> {
								ArrayNode innerPair = costJson.addArray();
								innerPair.add(table.getWikiItemName(entry.getKey()));
								innerPair.add(entry.getValue());
							});

					int count = tech.getEffectiveCount();
					itemJson.put("cost-multiplier", count);
					itemJson.put("expensive-cost-multiplier", (count * 4));

					if (!tech.getPrerequisites().isEmpty()) {
						ArrayNode requiredTechnologies = itemJson.putArray("required-technologies");
						tech.getPrerequisites().stream().sorted()
								.map(n -> wiki_fmtNumberedWikiName(table.getWikiTechnologyName(n)))
								.forEach(requiredTechnologies::add);
					}

					if (!tech.isFirstBonus()) {
						Collection<String> allows = allowsMap.get(tech.getName());
						if (!allows.isEmpty()) {
							ArrayNode allowsArray = itemJson.putArray("allows");
							allows.stream().sorted()
									.map(n -> wiki_fmtNumberedWikiName(table.getWikiTechnologyName(n)))
									.forEach(allowsArray::add);
						}
					} else {
						if (!tech.isMaxLevelInfinite() && tech.getBonusGroup().size() == 2) {
							ArrayNode allows = itemJson.putArray("allows");
							allows.add(table.getWikiTechnologyName(tech.getBonusName()) + ", 2");
						} else {
							String lastLevel;
							if (tech.isMaxLevelInfinite() || tech.getBonusGroup().get(tech.getBonusGroup().size() -1).isMaxLevelInfinite() )
								lastLevel = "&infin;";
							else
								lastLevel = String.valueOf(tech.getBonusGroup().size());
							ArrayNode allows = itemJson.putArray("allows");
							allows.add(table.getWikiTechnologyName(tech.getBonusName()) + ", 2-" + lastLevel);
						}
					}

					if (!tech.getRecipeUnlocks().isEmpty()) {
						ArrayNode effects = itemJson.putArray("effects");
						tech.getRecipeUnlocks().stream().sorted()
								.map(n -> table.getWikiRecipeName(n)).forEach(effects::add);
					}
				});
		return json;
	}

	private static ObjectNode wiki_Types(DataTable table, Map<String, WikiTypeMatch> wikiTypes) {
		ObjectNode json = createObjectNode();

		wikiTypes.entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).forEach(e -> {
			String name = e.getKey();
			WikiTypeMatch m = e.getValue();
			String type = m.toString();

			if (!m.item && !m.recipe && !m.fluid) {
				return;
			}

			ObjectNode item = createObjectNode();
			json.put(m.item ? table.getWikiItemName(name) : table.getWikiRecipeName(name), item);
			item.put("internal-name", name);
			item.put("prototype-type", type);
		});

		return json;
	}

	@SuppressWarnings("unused")
	private static ObjectNode wiki_TypeTree(DataTable table) {
		ObjectNode json = createObjectNode();

		Multimap<String, String> links = LinkedHashMultimap.create();
		Multimap<String, String> leafs = LinkedHashMultimap.create();

		table.getTypeHierarchy().getParents().forEach((n, p) -> {
			links.put(p, n);
		});
		table.getTypeHierarchy().getRoots().forEach(n -> {
			links.put("__ROOT__", n);
		});

		ObjectNode rawJson = table.getRawJson();
		for (JsonNode v : rawJson) {
			for (JsonNode protoLua : v) {
				String type = protoLua.path("type").textValue();
				String name = protoLua.path("name").textValue();
				leafs.put(type, name);
				if (!table.getTypeHierarchy().getParents().containsKey(type)
						&& !table.getTypeHierarchy().getRoots().contains(type)) {
					System.err.println("MISSING PARENT FOR TYPE: " + type + " (" + name + ")");
				}
			};
		};

		Collection<String> rootTypes = links.get("__ROOT__");
		rootTypes.stream().sorted().forEach(n -> {
			json.put(n, wiki_TypeTree_GenerateNode(links, leafs, n));
		});

		return json;
	}

	private static ObjectNode wiki_TypeTree_GenerateNode(Multimap<String, String> links, Multimap<String, String> leafs,
			String parent) {
		Collection<String> types = links.get(parent);
		Collection<String> names = leafs.get(parent);

		ObjectNode nodeJson = createObjectNode();
		Streams.concat(types.stream(), names.stream()).sorted().forEach(n -> {
			if (types.contains(n)) {
				nodeJson.put(n, wiki_TypeTree_GenerateNode(links, leafs, n));
			} else {
				nodeJson.putObject(n);
			}
		});
		return nodeJson;
	}

	private static void write(ObjectNode json, String name) throws IOException {
		ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
		String string = objectWriter.writeValueAsString(json);
		Files.write(string, new File(folder, name + "-" + baseInfo.getVersion() + ".json"),
				StandardCharsets.UTF_8);
	}
}
