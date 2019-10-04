package com.demod.factorio;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ModInfo {
	public static class Dependency {
		private final boolean optional;
		private final String name;
		private final String conditional;
		private final String version;

		private Dependency(boolean optional, String name, String conditional, String version) {
			this.optional = optional;
			this.name = name;
			this.conditional = conditional;
			this.version = version;
		}

		public String getConditional() {
			return conditional;
		}

		public String getName() {
			return name;
		}

		public String getVersion() {
			return version;
		}

		public boolean isOptional() {
			return optional;
		}
	}

	private final String name;
	private final String version;
	private final String title;
	private final String author;
	private final String contact;
	private final String homepage;
	private final String description;
	private final List<Dependency> dependencies = new ArrayList<>();

	public ModInfo(JsonNode json) {
		name = json.path("name").textValue();
		version = json.path("version").asText("???");
		title = json.path("title").asText("");
		author = json.path("author").asText("");
		contact = json.path("contact").asText("");
		homepage = json.path("homepage").asText("");
		description = json.path("description").asText("");
		ArrayNode dependenciesJson = (ArrayNode) json.path("dependencies");
		for (int i = 0; i < dependenciesJson.size(); i++) {
			String depString = dependenciesJson.path(i).textValue();
			String[] depSplit = depString.split("\\s");
			if (depSplit.length == 2) {
				dependencies.add(new Dependency(true, depSplit[1], null, null));
			} else if (depSplit.length == 3) {
				dependencies.add(new Dependency(false, depSplit[0], depSplit[1], depSplit[2]));
			} else if (depSplit.length == 4) {
				dependencies.add(new Dependency(true, depSplit[1], depSplit[2], depSplit[3]));
			}
		}
	}

	public String getAuthor() {
		return author;
	}

	public String getContact() {
		return contact;
	}

	public List<Dependency> getDependencies() {
		return dependencies;
	}

	public String getDescription() {
		return description;
	}

	public String getHomepage() {
		return homepage;
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public String getVersion() {
		return version;
	}

}
