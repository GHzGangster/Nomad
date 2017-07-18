package savemgo.nomad.plugin;

import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.cfg.Configuration;

public class PluginHandler {

	private static final Logger logger = LogManager.getLogger();

	private Plugin plugin = null;

	public PluginHandler() {

	}

	public boolean load(File file) throws Exception {
		JarFile jar = null;
		try {
			jar = new JarFile(file);
			Enumeration<JarEntry> e = jar.entries();

			addURLToSystemClassLoader(new URL("jar:file:" + file.getAbsolutePath() + "!/"));
			ClassLoader classLoader = ClassLoader.getSystemClassLoader();
			
			while (e.hasMoreElements()) {
				JarEntry je = e.nextElement();
				if (je.isDirectory() || !je.getName().endsWith(".class")) {
					continue;
				}
				String className = je.getName().replaceAll(".class", "");
				className = className.replace('/', '.');
				Class<?> clazz = classLoader.loadClass(className);
				if (Plugin.class.isAssignableFrom(clazz)) {
					plugin = (Plugin) clazz.newInstance();
					logger.debug("Loaded Plugin: {} {}", file.getAbsolutePath(), className);
					return true;
				}
			}
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (Exception ignored) {
					//
				}
			}
		}
		return false;
	}

	public static void addURLToSystemClassLoader(URL url) throws IntrospectionException {
		URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class<URLClassLoader> classLoaderClass = URLClassLoader.class;

		try {
			Method method = classLoaderClass.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(systemClassLoader, new Object[] { url });
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IntrospectionException("Error when adding url to system ClassLoader ");
		}
	}

	public void initialize() {
		if (plugin != null) {
			plugin.initialize();
		}
	}

	public void addAnnotatedClass(Configuration configuration) {
		if (plugin != null) {
			plugin.addAnnotatedClass(configuration);
		}
	}

	public void onStart() {
		if (plugin != null) {
			plugin.onStart();
		}
	}

}
