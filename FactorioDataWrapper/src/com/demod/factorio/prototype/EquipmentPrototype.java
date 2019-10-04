package com.demod.factorio.prototype;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.luaj.vm2.LuaTable;

public class EquipmentPrototype extends DataPrototype {
	public EquipmentPrototype(ObjectNode json, String name, String type) {
		super(json, name, type);
	}
}
