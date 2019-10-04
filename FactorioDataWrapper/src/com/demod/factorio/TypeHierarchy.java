package com.demod.factorio;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class TypeHierarchy {
	private final Map<String, String> parents = new HashMap<>();
	private final Set<String> roots = new LinkedHashSet<>();

	public TypeHierarchy(ObjectNode json) {
		json.fields().forEachRemaining(entry -> {
			String t = entry.getKey();
			JsonNode p = entry.getValue();
			if (p instanceof TextNode) {
				parents.put(t, p.textValue());
			} else {
				roots.add(t);
			}
		});
	}

	public Map<String, String> getParents() {
		return parents;
	}

	public Set<String> getRoots() {
		return roots;
	}

	public boolean isAssignable(String type, String subType) {
		String checkType = subType;
		while (checkType != null) {
			if (type.equals(checkType)) {
				return true;
			}
			checkType = parents.get(checkType);
		}
		return false;
	}
}
