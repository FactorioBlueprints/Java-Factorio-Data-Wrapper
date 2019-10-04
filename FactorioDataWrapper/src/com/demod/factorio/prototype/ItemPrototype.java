package com.demod.factorio.prototype;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.luaj.vm2.LuaTable;

public class ItemPrototype extends DataPrototype {
	public ItemPrototype(ObjectNode objectNode, String name, String type) {
		super(objectNode, name, type);
	}
}
