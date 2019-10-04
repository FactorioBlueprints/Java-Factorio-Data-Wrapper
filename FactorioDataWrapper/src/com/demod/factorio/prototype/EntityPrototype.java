package com.demod.factorio.prototype;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.demod.factorio.Utils;

public class EntityPrototype extends DataPrototype {

	private final Rectangle2D.Double selectionBox;
	private final List<String> flags = new ArrayList<>();
	private final List<String> craftingCategories = new ArrayList<>();

	public EntityPrototype(ObjectNode json, String name, String type) {
		super(json, name, type);
		JsonNode selectionBoxJson = json.path("selection_box");
		if (!selectionBoxJson.isMissingNode()) {
			selectionBox = Utils.parseRectangle(selectionBoxJson);
		} else {
			selectionBox = new Rectangle2D.Double();
		}

		JsonNode flags = json.path("flags");
		for (JsonNode flag : flags) {
			this.flags.add(flag.textValue());
		}

		JsonNode categories = json.path("crafting_categories");
		for (JsonNode category : categories) {
			String categoryName = category.textValue();
			this.craftingCategories.add(categoryName);
		}
	}

	public List<String> getFlags() {
		return flags;
	}

	public Rectangle2D.Double getSelectionBox() {
		return selectionBox;
	}

	public List<String> getCraftingCategories() {
		return Collections.unmodifiableList(this.craftingCategories);
	}
}
