package com.demod.factorio;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import com.google.common.collect.Streams;

public final class Utils {

	@SuppressWarnings("unchecked")
	public static <T> T convertLuaToJson(LuaValue value) {
		if (value.istable()) {
			ObjectMapper objectMapper = new ObjectMapper();
			if (isLuaArray(value)) {
				ArrayNode json = objectMapper.createArrayNode();
				Utils.forEach(value, (v) -> {
					json.add(Utils.<JsonNode>convertLuaToJson(v));
				});
				return (T) json;
			} else {
				ObjectNode json = objectMapper.createObjectNode();
				Utils.forEach(value, (k, v) -> {
					JsonNode oldData = json.replace(k.tojstring(), Utils.<JsonNode>convertLuaToJson(v));
					assert oldData == null;
				});
				return (T) json;
			}
		} else {
			if (value.isnil()) {
				return null;
			} else if (value.isboolean()) {
				return (T) (Boolean) value.toboolean();
			} else if (value.isnumber()) {
				Double number = value.todouble();
				if (number == Double.POSITIVE_INFINITY) {
					return (T) "infinity";
				} else if (number == Double.NEGATIVE_INFINITY) {
					return (T) "-infinity";
				} else if (number == Double.NaN) {
					return (T) "NaN";
				} else {
					return (T) number;
				}
			}
			return (T) value.tojstring();
		}
	}

	public static void debugPrintJson(ArrayNode json) {
		debugPrintJson("", json);
	}

	public static void debugPrintJson(ObjectNode json) {
		debugPrintJson("", json);
	}

	private static void debugPrintJson(String prefix, ArrayNode json) {
		forEach(json, (i, v) -> {
			if (v instanceof ArrayNode) {
				debugPrintJson(prefix + "[" + i + "]", (ArrayNode) v);
			} else if (v instanceof ObjectNode) {
				debugPrintJson(prefix + "[" + i + "].", (ObjectNode) v);
			} else {
				System.out.println(prefix + i + " = " + v);
			}
		});
	}

	private static void debugPrintJson(String prefix, ObjectNode json) {
		forEach(json, (k, v) -> {
			if (v instanceof ArrayNode) {
				debugPrintJson(prefix + k, (ArrayNode) v);
			} else if (v instanceof ObjectNode) {
				debugPrintJson(prefix + k + ".", (ObjectNode) v);
			} else {
				System.out.println(prefix + k + " = " + v);
			}
		});
	}

	public static void debugPrintLua(LuaValue value) {
		debugPrintLua("", value, System.out);
	}

	public static void debugPrintLua(LuaValue value, PrintStream ps) {
		debugPrintLua("", value, ps);
	}

	private static void debugPrintLua(String prefix, LuaValue value, PrintStream ps) {
		if (value.istable()) {
			forEachSorted(value, (k, v) -> {
				if (v.istable()) {
					debugPrintLua(prefix + k + ".", v, ps);
				} else {
					ps.println(prefix + k + " = " + v);
				}
			});
		} else {
			ps.println(prefix.isEmpty() ? value : (prefix + " = " + value));
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> void forEach(ArrayNode arrayNode, BiConsumer<Integer, T> consumer) {
		for (int i = 0; i < arrayNode.size(); i++) {
			consumer.accept(i, (T) arrayNode.get(i));
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> void forEach(ArrayNode arrayNode, Consumer<T> consumer) {
		for (int i = 0; i < arrayNode.size(); i++) {
			consumer.accept((T) arrayNode.get(i));
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> void forEach(ObjectNode json, Throwing.BiConsumer<String, T> consumer) {
		Iterable<String> iterable = json::fieldNames;
		StreamSupport.stream(iterable.spliterator(), false).sorted()
				.forEach(Errors.rethrow().wrap((String k) -> consumer.accept(k, (T) json.get(k))));
	}

	public static void forEach(LuaValue table, BiConsumer<LuaValue, LuaValue> consumer) {
		LuaValue k = LuaValue.NIL;
		while (true) {
			Varargs n = table.next(k);
			if ((k = n.arg1()).isnil())
				break;
			LuaValue v = n.arg(2);
			consumer.accept(k, v);
		}
	}

	public static void forEach(LuaValue table, Consumer<LuaValue> consumer) {
		LuaValue k = LuaValue.NIL;
		while (true) {
			Varargs n = table.next(k);
			if ((k = n.arg1()).isnil())
				break;
			LuaValue v = n.arg(2);
			consumer.accept(v);
		}
	}

	private static void forEachSorted(LuaValue table, BiConsumer<LuaValue, LuaValue> consumer) {
		Streams.stream(new Iterator<Entry<LuaValue, LuaValue>>() {
			LuaValue k = LuaValue.NIL;
			Varargs next = null;

			@Override
			public boolean hasNext() {
				if (next == null) {
					next = table.next(k);
					k = next.arg1();
				}
				return !k.isnil();
			}

			@Override
			public Entry<LuaValue, LuaValue> next() {
				if (next == null) {
					next = table.next(k);
					k = next.arg1();
				}
				Entry<LuaValue, LuaValue> ret = new SimpleImmutableEntry<>(k, next.arg(2));
				next = null;
				return ret;
			}
		}).sorted((p1, p2) -> p1.getKey().toString().compareTo(p2.getKey().toString()))
				.forEach(p -> consumer.accept(p.getKey(), p.getValue()));
	}

	private static boolean isLuaArray(LuaValue value) {
		if (value.istable()) {
			LuaValue k = LuaValue.NIL;
			int i = 0;
			while (true) {
				i++;
				Varargs n = value.next(k);
				if ((k = n.arg1()).isnil())
					break;
				if (!k.isnumber() || k.toint() != i) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public static Color parseColor(LuaValue value) {
		float red = value.get("r").tofloat();
		float green = value.get("g").tofloat();
		float blue = value.get("b").tofloat();
		float alpha = value.get("a").tofloat();
		if (red > 1 || green > 1 || blue > 1 || alpha > 1) {
			red /= 255;
			green /= 255;
			blue /= 255;
			alpha /= 255;
		}
		return new Color(red, green, blue, alpha);
	}

	public static Point parsePoint(LuaValue value) {
		if (value.isnil()) {
			return new Point();
		}
		return new Point(value.get(1).checkint(), value.get(2).checkint());
	}

	public static Point2D.Double parsePoint2D(ObjectNode json) {
		JsonNode x = json.path("x");
		JsonNode y = json.path("y");
		assert x.isFloatingPointNumber();
		assert x.isFloatingPointNumber();
		return new Point2D.Double(x.doubleValue(), y.doubleValue());
	}

	public static Point2D.Double parsePoint2D(LuaValue value) {
		if (value.isnil()) {
			return new Point2D.Double();
		}
		return new Point2D.Double(value.get(1).checkdouble(), value.get(2).checkdouble());
	}

	public static Rectangle parseRectangle(ArrayNode json) {
		assert json.path(0).isIntegralNumber();
		assert json.path(1).isIntegralNumber();
		assert json.path(2).isIntegralNumber();
		assert json.path(3).isIntegralNumber();
		return new Rectangle(
				json.path(0).intValue(),
				json.path(1).intValue(),
				json.path(2).intValue(),
				json.path(3).intValue());
	}

	public static Rectangle2D.Double parseRectangle(LuaValue value) {
		LuaTable table = value.checktable();
		LuaValue p1 = table.get(1);
		LuaValue p2 = table.get(2);
		double x1 = p1.get(1).checkdouble();
		double y1 = p1.get(2).checkdouble();
		double x2 = p2.get(1).checkdouble();
		double y2 = p2.get(2).checkdouble();
		return new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
	}

	public static Rectangle2D.Double parseRectangle2D(ArrayNode json) {
		assert json.path(0).isFloatingPointNumber();
		assert json.path(1).isFloatingPointNumber();
		assert json.path(2).isFloatingPointNumber();
		assert json.path(3).isFloatingPointNumber();
		return new Rectangle2D.Double(
				json.path(0).doubleValue(),
				json.path(1).doubleValue(),
				json.path(2).doubleValue(),
				json.path(3).doubleValue());
	}

	@SuppressWarnings("resource")
	public static ObjectNode readJsonFromStream(InputStream in) throws IOException {
		Scanner scanner = new Scanner(in, "UTF-8").useDelimiter("\\A");
		String string = scanner.next();
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.readValue(string, ObjectNode.class);
	}

	public static BufferedImage tintImage(BufferedImage image, Color tint) {
		int w = image.getWidth();
		int h = image.getHeight();
		BufferedImage ret = new BufferedImage(w, h, image.getType());
		int[] pixels = new int[w * h];
		image.getRGB(0, 0, w, h, pixels, 0, w);
		for (int i = 0; i < pixels.length; i++) {
			int argb = pixels[i];

			int a = (((argb >> 24) & 0xFF) * tint.getAlpha()) / 255;
			int r = (((argb >> 16) & 0xFF) * tint.getRed()) / 255;
			int g = (((argb >> 8) & 0xFF) * tint.getGreen()) / 255;
			int b = (((argb) & 0xFF) * tint.getBlue()) / 255;

			pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
		}
		ret.setRGB(0, 0, w, h, pixels, 0, w);
		return ret;
	}

	public static ArrayNode toJson(Rectangle rectangle) {
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode json = objectMapper.createArrayNode();
		json.add(rectangle.x);
		json.add(rectangle.y);
		json.add(rectangle.width);
		json.add(rectangle.height);
		return json;
	}

	public static ArrayNode toJson(Rectangle2D.Double rectangle) {
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode json = objectMapper.createArrayNode();
		json.add(rectangle.x);
		json.add(rectangle.y);
		json.add(rectangle.width);
		json.add(rectangle.height);
		return json;
	}

	private Utils() {
	}
}
