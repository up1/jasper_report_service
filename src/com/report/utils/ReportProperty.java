package com.report.utils;

import java.util.Properties;

public class ReportProperty {
	private static ReportProperty instance;
	private Properties p;

	public static ReportProperty getInstance() {
		if (null == instance)
			instance = new ReportProperty();
		return instance;
	}

	public ReportProperty() {
		try {
			p = new Properties();
			p.load(ReportProperty.class.getResourceAsStream("/env.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String get(String key) {
		return p.getProperty(key);
	}

	public int getInt(String key) {
		return Integer.parseInt(get(key));
	}

}
