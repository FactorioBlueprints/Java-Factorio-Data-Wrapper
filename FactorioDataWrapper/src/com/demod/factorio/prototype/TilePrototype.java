package com.demod.factorio.prototype;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.luaj.vm2.LuaTable;

public class TilePrototype extends DataPrototype {
	public TilePrototype(ObjectNode json, String name, String type) {
		super(json, name, type);
	}
}
