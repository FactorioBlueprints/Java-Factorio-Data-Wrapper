package com.demod.factorio;

import java.io.FileInputStream;
import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;

public final class Config {
	private static ObjectNode config = null;

	public static synchronized ObjectNode get() {
		if (config == null) {
			loadConfig();
		}
		return config;
	}

	private static void loadConfig() {
		try {
			config = Utils.readJsonFromStream(new FileInputStream("config.json"));
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("################################");
			System.err.println("Missing or bad config.json file!");
			System.err.println("################################");
			System.exit(0);
		}
	}

	private Config() {
	}
}
