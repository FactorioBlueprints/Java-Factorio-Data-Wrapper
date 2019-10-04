package com.demod.factorio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import com.demod.factorio.prototype.EntityPrototype;
import com.demod.factorio.prototype.EquipmentPrototype;
import com.demod.factorio.prototype.FluidPrototype;
import com.demod.factorio.prototype.ItemPrototype;
import com.demod.factorio.prototype.RecipePrototype;
import com.demod.factorio.prototype.TechPrototype;
import com.demod.factorio.prototype.TilePrototype;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public class DataTable {
	private static Multimap<String, String> entityItemNameMapping = ArrayListMultimap.create();
	static {
		entityItemNameMapping.put("curved-rail", "rail");
		entityItemNameMapping.put("curved-rail", "rail");
		entityItemNameMapping.put("curved-rail", "rail");
		entityItemNameMapping.put("curved-rail", "rail");
		entityItemNameMapping.put("straight-rail", "rail");
	}

	private final TypeHierarchy typeHierarchy;
	private final ObjectNode rawJson;

	private final ObjectNode nameMappingTechnologies;
	private final ObjectNode nameMappingItemsRecipes;

	private final Map<String, EntityPrototype> entities = new LinkedHashMap<>();
	private final Map<String, ItemPrototype> items = new LinkedHashMap<>();
	private final Map<String, RecipePrototype> recipes = new LinkedHashMap<>();
	private final Map<String, RecipePrototype> expensiveRecipes = new LinkedHashMap<>();
	private final Map<String, FluidPrototype> fluids = new LinkedHashMap<>();
	private final Map<String, TechPrototype> technologies = new LinkedHashMap<>();
	private final Map<String, EquipmentPrototype> equipments = new LinkedHashMap<>();
	private final Map<String, TilePrototype> tiles = new LinkedHashMap<>();

	private final Map<String, List<EntityPrototype>> craftingCategories = new LinkedHashMap<>();

	private final Set<String> worldInputs = new LinkedHashSet<>();

	public DataTable(TypeHierarchy typeHierarchy, ObjectNode dataJson, ObjectNode excludeDataJson,
			ObjectNode wikiNamingJson) {
		this.typeHierarchy = typeHierarchy;
		this.rawJson = (ObjectNode) dataJson.path("raw");

		Set<String> excludedRecipesAndItems = asStringSet((ArrayNode) excludeDataJson.path("recipes-and-items"));
		Set<String> excludedTechnologies = asStringSet((ArrayNode) excludeDataJson.path("technologies"));

		nameMappingTechnologies = (ObjectNode) wikiNamingJson.path("technologies");
		nameMappingItemsRecipes = (ObjectNode) wikiNamingJson.path("items and recipes");

		for (JsonNode v : rawJson) {
			if (!(v instanceof ObjectNode)) {
				throw new AssertionError(v.getClass().getSimpleName());
			}
			for (JsonNode protoJson : v) {
				ObjectNode protoObjectNode = (ObjectNode) protoJson;
				String type = protoJson.path("type").textValue();
				String name = protoJson.path("name").textValue();
				if (typeHierarchy.isAssignable("item", type) && !excludedRecipesAndItems.contains(name)) {
					items.put(name, new ItemPrototype(protoObjectNode, name, type));
				} else if (typeHierarchy.isAssignable("recipe", type) && !excludedRecipesAndItems.contains(name)) {
					recipes.put(name, new RecipePrototype(protoObjectNode, name, type, false));
					expensiveRecipes.put(name, new RecipePrototype(protoObjectNode, name, type, true));
				} else if (typeHierarchy.isAssignable("entity", type)) {
					entities.put(name, new EntityPrototype(protoObjectNode, name, type));
				} else if (typeHierarchy.isAssignable("fluid", type)) {
					fluids.put(name, new FluidPrototype(protoObjectNode, name, type));
				} else if (typeHierarchy.isAssignable("technology", type) && !excludedTechnologies.contains(name)) {
					technologies.put(name,
							new TechPrototype(protoObjectNode, name, type, excludedRecipesAndItems));
				} else if (typeHierarchy.isAssignable("equipment", type)) {
					equipments.put(name, new EquipmentPrototype(protoObjectNode, name, type));
				} else if (typeHierarchy.isAssignable("tile", type)) {
					tiles.put(name, new TilePrototype(protoObjectNode, name, type));
				}
			};
		};

		for (RecipePrototype recipe : recipes.values()) {
			for (String input : recipe.getInputs().keySet()) {
				worldInputs.add(input);
			}
		}
		for (RecipePrototype recipe : recipes.values()) {
			for (String output : recipe.getOutputs().keySet()) {
				worldInputs.remove(output);
			}
		}

		technologies.values().stream()
				.filter(TechPrototype::isFirstBonus)
				.forEach(firstBonus -> {
					String firstBonusName = firstBonus.getName();
					String bonusMatch = firstBonusName.substring(0, firstBonusName.length() - 1);
					String bonusName = bonusMatch.substring(0, bonusMatch.length() - 1);

					List<TechPrototype> bonusGroup = technologies.values().stream()
							.filter(bonus -> bonus.getName().startsWith(bonusMatch))
							.peek(b -> b.setBonusLevel(-Integer.parseInt(b.getName().replace(bonusName, ""))))
							.sorted(Comparator.comparingInt(TechPrototype::getBonusLevel))
							.collect(Collectors.toList());

					for (TechPrototype bonus : bonusGroup) {
						bonus.setBonus(true);
						bonus.setBonusName(bonusName);
						bonus.setBonusGroup(bonusGroup);
					}
				});

		this.entities.values().stream()
				.filter(e -> !excludedRecipesAndItems.contains(e.getName()))
				.forEach(e -> e.getCraftingCategories().stream()
						.map(categoryName -> craftingCategories.computeIfAbsent(categoryName, k -> new ArrayList<>()))
						.forEach(categoryNames -> categoryNames.add(e)));
	}

	private Set<String> asStringSet(ArrayNode arrayNode) {
		Set<String> ret = new LinkedHashSet<>();
		Utils.forEach(arrayNode, ret::add);
		return ret;
	}

	public Map<String, EntityPrototype> getEntities() {
		return entities;
	}

	public Optional<EntityPrototype> getEntity(String name) {
		return Optional.ofNullable(entities.get(name));
	}

	public Optional<EquipmentPrototype> getEquipment(String name) {
		return Optional.ofNullable(equipments.get(name));
	}

	public Map<String, EquipmentPrototype> getEquipments() {
		return equipments;
	}

	public Map<String, List<EntityPrototype>> getCraftingCategories() {
		return craftingCategories;
	}

	public Optional<RecipePrototype> getExpensiveRecipe(String name) {
		return Optional.ofNullable(expensiveRecipes.get(name));
	}

	public Map<String, RecipePrototype> getExpensiveRecipes() {
		return expensiveRecipes;
	}

	public Optional<FluidPrototype> getFluid(String name) {
		return Optional.ofNullable(fluids.get(name));
	}

	public Map<String, FluidPrototype> getFluids() {
		return fluids;
	}

	public Optional<ItemPrototype> getItem(String name) {
		return Optional.ofNullable(items.get(name));
	}

	public Map<String, ItemPrototype> getItems() {
		return items;
	}

	public List<ItemPrototype> getItemsForEntity(String entityName) {
		Optional<ItemPrototype> item = getItem(entityName);
		if (item.isPresent()) {
			return ImmutableList.of(item.get());
		}
		return entityItemNameMapping.get(entityName).stream().map(this::getItem).map(Optional::get)
				.collect(Collectors.toList());
	}

	public Optional<JsonNode> getRaw(String... path) {
		JsonNode retJson = rawJson;
		for (String key : path) {
			retJson = retJson.path(key);
			if (retJson.isMissingNode()) {
				return Optional.empty();
			}
		}
		return Optional.of(retJson);
	}

	public ObjectNode getRawJson() {
		return rawJson;
	}

	public Optional<RecipePrototype> getRecipe(String name) {
		return Optional.ofNullable(recipes.get(name));
	}

	public Map<String, RecipePrototype> getRecipes() {
		return recipes;
	}

	public Map<String, TechPrototype> getTechnologies() {
		return technologies;
	}

	public Optional<TechPrototype> getTechnology(String name) {
		return Optional.ofNullable(technologies.get(name));
	}

	public Optional<TilePrototype> getTile(String name) {
		return Optional.ofNullable(tiles.get(name));
	}

	public Map<String, TilePrototype> getTiles() {
		return tiles;
	}

	public TypeHierarchy getTypeHierarchy() {
		return typeHierarchy;
	}

	private String getWikiDefaultName(String name) {
		String[] split = name.split("-|_");
		String formatted = Character.toUpperCase(split[0].charAt(0)) + split[0].substring(1);
		if (formatted.equals("Uranium") && split.length == 2 && split[1].startsWith("2")) {
			return formatted + "-" + split[1];
		}
		for (int i = 1; i < split.length; i++) {
			formatted += " " + split[i];
		}
		return formatted;
	}

	public String getWikiEntityName(String name) {
		return getWikiName(name, nameMappingItemsRecipes);
	}

	public String getWikiItemName(String name) {
		if (name.equals(TotalRawCalculator.RAW_TIME)) {
			return "Time (Seconds)";
		}
		return getWikiName(name, nameMappingItemsRecipes);
	}

	private String getWikiName(String name, ObjectNode nameMappingJson) {
		String ret = nameMappingJson.path(name).asText(null);
		if (ret == null) {
			System.err.println("\"" + name + "\":\"" + getWikiDefaultName(name) + "\",");
			nameMappingJson.put(name, ret = getWikiDefaultName(name));
		}
		return ret;
	}

	public String getWikiRecipeName(String name) {
		return getWikiName(name, nameMappingItemsRecipes);
	}

	public String getWikiTechnologyName(String name) {
		return getWikiName(name, nameMappingTechnologies);
	}

	public Set<String> getWorldInputs() {
		return worldInputs;
	}

	public boolean hasWikiEntityName(String name) {
		return nameMappingItemsRecipes.path(name).asText(null) != null;
	}
}
