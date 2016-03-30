package utils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * This class fetches the Configuration from settings.config. Copy and rename the provided
 * settings.config.example and edit it to your needs.
 * @author Christian Chartron
 *
 */
public class Settings {
	
	private static Map<String, String> settings;
	
	static {
		settings = new HashMap<String, String>();
		
		try {
			URL config = Settings.class.getClass().getResource("/settings.config");
			InputStream configStream = config.openStream();
			Properties props = new Properties();
			props.load(configStream);

			for (String kv : props.stringPropertyNames()) {
				System.out.println(kv);
				settings.put(kv, props.getProperty(kv));
			}

			configStream.close();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Request a configuration by key.
	 * @param key The setting you want to get.
	 * @return
	 */
	public static String getSetting(String key) {
		return settings.get(key);
	}
}
