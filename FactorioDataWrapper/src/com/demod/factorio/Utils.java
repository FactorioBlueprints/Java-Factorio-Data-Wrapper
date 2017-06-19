package com.demod.factorio;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import com.google.common.collect.Streams;

import javafx.util.Pair;

public final class Utils {

	public static void debugPrintJson(JSONArray json) {
		debugPrintJson("", json);
	}

	public static void debugPrintJson(JSONObject json) {
		debugPrintJson("", json);
	}

	private static void debugPrintJson(String prefix, JSONArray json) {
		forEach(json, (i, v) -> {
			if (v instanceof JSONArray) {
				debugPrintJson(prefix + "[" + i + "]", (JSONArray) v);
			} else if (v instanceof JSONObject) {
				debugPrintJson(prefix + "[" + i + "].", (JSONObject) v);
			} else {
				System.out.println(prefix + i + " = " + v);
			}
		});
	}

	private static void debugPrintJson(String prefix, JSONObject json) {
		forEach(json, (k, v) -> {
			if (v instanceof JSONArray) {
				debugPrintJson(prefix + k, (JSONArray) v);
			} else if (v instanceof JSONObject) {
				debugPrintJson(prefix + k + ".", (JSONObject) v);
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
	public static <T> void forEach(JSONArray jsonArray, BiConsumer<Integer, T> consumer) {
		for (int i = 0; i < jsonArray.length(); i++) {
			consumer.accept(i, (T) jsonArray.get(i));
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> void forEach(JSONArray jsonArray, Consumer<T> consumer) {
		for (int i = 0; i < jsonArray.length(); i++) {
			consumer.accept((T) jsonArray.get(i));
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> void forEach(JSONObject json, Throwing.BiConsumer<String, T> consumer) {
		json.keySet().stream().sorted()
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
		Streams.stream(new Iterator<Pair<LuaValue, LuaValue>>() {
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
			public Pair<LuaValue, LuaValue> next() {
				if (next == null) {
					next = table.next(k);
					k = next.arg1();
				}
				Pair<LuaValue, LuaValue> ret = new Pair<>(k, next.arg(2));
				next = null;
				return ret;
			}
		}).sorted((p1, p2) -> p1.getKey().toString().compareTo(p2.getKey().toString()))
				.forEach(p -> consumer.accept(p.getKey(), p.getValue()));
	}

	public static Color parseColor(LuaValue value) {
		float red = value.get("r").tofloat();
		float green = value.get("g").tofloat();
		float blue = value.get("b").tofloat();
		float alpha = value.get("a").tofloat();
		return new Color(red, green, blue, alpha);
	}

	public static Point parsePoint(LuaValue value) {
		if (value.isnil()) {
			return new Point();
		}
		return new Point(value.get(1).checkint(), value.get(2).checkint());
	}

	public static Double parsePoint2D(LuaValue value) {
		if (value.isnil()) {
			return new Point2D.Double();
		}
		return new Point2D.Double(value.get(1).checkdouble(), value.get(2).checkdouble());
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

	@SuppressWarnings("resource")
	public static JSONObject readJsonFromStream(InputStream in) throws JSONException, IOException {
		return new JSONObject(new Scanner(in, "UTF-8").useDelimiter("\\A").next());
	}

	public static void terribleHackToHaveOrderedJSONObject(JSONObject json) {
		try {
			Field map = json.getClass().getDeclaredField("map");
			map.setAccessible(true);// because the field is private final...
			map.set(json, new LinkedHashMap<>());
			map.setAccessible(false);// return flag
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			System.exit(0); // Oh well...
		}
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

	private Utils() {
	}
}
