package com.demod.factorio.prototype;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class DataPrototype {
	private final ObjectNode objectNode;
	private final String name;
	private final String type;

	public DataPrototype(ObjectNode objectNode, String name, String type) {
		this.objectNode = objectNode;
		this.name = name;
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataPrototype other = (DataPrototype) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public ObjectNode getObjectNode() {
		return objectNode;
	}

	@Override
	public String toString() {
		return name;
	}
}
