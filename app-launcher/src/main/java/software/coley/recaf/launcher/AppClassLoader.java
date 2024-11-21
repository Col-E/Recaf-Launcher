package software.coley.recaf.launcher;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

final class AppClassLoader extends URLClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}

	AppClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> c = findLoadedClass(name);
		if (c != null)
			return c;
		synchronized (getClassLoadingLock(name)) {
			c = findLoadedClass(name);
			if (c != null)
				return c;
			try {
				return findClass(name);
			} catch (ClassNotFoundException e) {
				return getParent().loadClass(name);
			}
		}
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Enumeration<URL> resources = findResources(name);
		Enumeration<URL> parentResources = getParent().getResources(name);
		return new Enumeration<URL>() {
			@Override
			public boolean hasMoreElements() {
				return resources.hasMoreElements() || parentResources.hasMoreElements();
			}

			@Override
			public URL nextElement() {
				if (resources.hasMoreElements())
					return resources.nextElement();
				return parentResources.nextElement();
			}
		};
	}

	@Override
	public URL getResource(String name) {
		URL url = findResource(name);
		if (url == null) {
			url = getParent().getResource(name);
		}
		return url;
	}
}
