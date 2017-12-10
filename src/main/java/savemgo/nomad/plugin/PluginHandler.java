package savemgo.nomad.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PluginHandler {

	private static final Logger logger = LogManager.getLogger();

	private static PluginHandler INSTANCE = null;

	private Plugin plugin = new Plugin();

	private PluginHandler() {

	}

	public static PluginHandler get() {
		if (INSTANCE == null) {
			INSTANCE = new PluginHandler();
		}
		return INSTANCE;
	}

	public void loadPlugin(String className) throws Exception {
		try {
			Class<?> clazz = getClass().getClassLoader().loadClass(className);
			if (clazz != null && Plugin.class.isAssignableFrom(clazz)) {
				plugin = (Plugin) clazz.newInstance();
				logger.debug("Loaded Plugin: {}", className);
			}
		} catch (Exception e) {
			logger.error("Exception while loading plugin.", e);
		}
	}

	public Plugin getPlugin() {
		return plugin;
	}

}
