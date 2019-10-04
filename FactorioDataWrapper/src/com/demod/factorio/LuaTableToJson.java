package com.demod.factorio;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaNil;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public final class LuaTableToJson {
	private LuaTableToJson() {
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	public static ObjectNode convert(@Nonnull LuaTable luaTable) {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode objectNode = objectMapper.createObjectNode();
		// TODO: Handle top-level array
		LuaTableToJson.convertObjectParent(luaTable, objectNode);
		return objectNode;
	}

	private static void convertObjectParent(@Nonnull LuaTable luaTable, @Nonnull ObjectNode parentObjectNode) {
		LuaValue[] keys = luaTable.keys();
		for (LuaValue luaTableKey : keys) {
			LuaValue luaTableValue = luaTable.get(luaTableKey);
			String keyString = LuaTableToJson.convertToString(luaTableKey);
			LuaTableToJson.convertObjectChild(luaTableValue, parentObjectNode, keyString);
		}
	}

	private static void convertArrayParent(@Nonnull LuaTable luaArray, @Nonnull ArrayNode parentArrayNode) {
		int length = luaArray.length();
		for (int i = 0; i < length; i++) {
			LuaValue luaValue = luaArray.get(i + 1);
			LuaTableToJson.convertArrayChild(parentArrayNode, luaValue);
		}
	}

	private static void convertObjectChild(
			@Nonnull LuaValue luaValue,
			@Nonnull ObjectNode parentObjectNode,
			@Nonnull String keyString) {
		if (luaValue instanceof LuaString) {
			String valueString = luaValue.tojstring();
			parentObjectNode.put(keyString, valueString);
			return;
		}

		if (luaValue instanceof LuaDouble) {
			parentObjectNode.put(keyString, luaValue.todouble());
			return;
		}

		if (luaValue instanceof LuaInteger) {
			parentObjectNode.put(keyString, luaValue.toint());
			return;
		}

		if (luaValue instanceof LuaBoolean) {
			parentObjectNode.put(keyString, luaValue.toboolean());
			return;
		}

		if (luaValue instanceof LuaTable) {
			LuaTable luaTable = (LuaTable) luaValue;
			LuaTableToJson.convertLuaTable(parentObjectNode, keyString, luaTable);
			return;
		}

		if (luaValue instanceof LuaClosure) {
			// Deliberately empty
			return;
		}

		throw new AssertionError(luaValue.getClass().getSimpleName());
	}

	private static void convertArrayChild(@Nonnull ArrayNode parentArrayNode, @Nonnull LuaValue luaValue) {
		if (luaValue instanceof LuaNil) {
			parentArrayNode.addNull();
			return;
		}

		if (luaValue instanceof LuaString) {
			String valueString = luaValue.tojstring();
			parentArrayNode.add(valueString);
			return;
		}

		if (luaValue instanceof LuaDouble) {
			parentArrayNode.add(luaValue.todouble());
			return;
		}

		if (luaValue instanceof LuaInteger) {
			parentArrayNode.add(luaValue.toint());
			return;
		}

		if (luaValue instanceof LuaBoolean) {
			parentArrayNode.add(luaValue.toboolean());
			return;
		}

		if (luaValue instanceof LuaTable) {
			LuaTable childLuaTable = (LuaTable) luaValue;
			LuaTableToJson.convertLuaTable(parentArrayNode, childLuaTable);
			return;
		}

		if (luaValue instanceof LuaClosure) {
			// Deliberately empty
			return;
		}

		throw new AssertionError(luaValue.getClass().getSimpleName());
	}

	private static void convertLuaTable(
			@Nonnull ObjectNode parentObjectNode,
			@Nonnull String keyString,
			@Nonnull LuaTable childLuaTable) {
		if (childLuaTable.length() > 0) {
			LuaTableToJson.convertArray(parentObjectNode, keyString, childLuaTable);
		} else {
			LuaTableToJson.convertObject(parentObjectNode, keyString, childLuaTable);
		}
	}

	private static void convertLuaTable(@Nonnull ArrayNode parentArrayNode, LuaTable childLuaTable) {
		if (childLuaTable.length() > 0) {
			LuaTableToJson.convertArray(parentArrayNode, childLuaTable);
		} else {
			LuaTableToJson.convertObject(parentArrayNode, childLuaTable);
		}
	}

	private static void convertArray(
			@Nonnull ObjectNode parentObjectNode,
			@Nonnull String keyString,
			@Nonnull LuaTable luaArray) {
		ArrayNode arrayNode = parentObjectNode.putArray(keyString);
		LuaTableToJson.convertArrayParent(luaArray, arrayNode);
	}

	private static void convertArray(@Nonnull ArrayNode parentArrayNode, @Nonnull LuaTable luaArray) {
		ArrayNode arrayNode = parentArrayNode.addArray();
		LuaTableToJson.convertArrayParent(luaArray, arrayNode);
	}

	private static void convertObject(
			@Nonnull ObjectNode parentObjectNode,
			@Nonnull String keyString,
			@Nonnull LuaTable luaTable) {
		ObjectNode childObjectNode = parentObjectNode.putObject(keyString);
		LuaTableToJson.convertObject(luaTable, childObjectNode);
	}

	private static void convertObject(@Nonnull ArrayNode parentArrayNode, @Nonnull LuaTable luaTable) {
		ObjectNode childObjectNode = parentArrayNode.addObject();
		LuaTableToJson.convertObject(luaTable, childObjectNode);
	}

	private static void convertObject(@Nonnull LuaTable luaTable, @Nonnull ObjectNode childObjectNode) {
		for (LuaValue luaTableKey : luaTable.keys()) {
			LuaValue luaTableValue = luaTable.get(luaTableKey);
			String childKeyString = LuaTableToJson.convertToString(luaTableKey);
			LuaTableToJson.convertObjectChild(luaTableValue, childObjectNode, childKeyString);
		}
	}

	private static String convertToString(LuaValue luaValue) {
		if (luaValue instanceof LuaString) {
			return luaValue.tojstring();
		}
		throw new AssertionError(luaValue.getClass().getSimpleName());
	}
}
