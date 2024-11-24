package software.coley.recaf.launcher;

import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Launch wrapper which reads classpath entries from {@link System#in}, detects the main-class, and invokes it.
 */
public final class ApplicationLauncher {
	private ApplicationLauncher() {
	}

	/**
	 * @param args
	 * 		Input arguments to pass to Recaf.
	 *
	 * @throws Throwable
	 * 		When reading {@link System#in} fails, or reading from the current classpath manifest fails.
	 */
	public static void main(String[] args) throws Throwable {
		// Read classpath urls from input
		AppClassLoader classLoader;
		{
			System.out.println("Receiving classpath entries from parent process...");
			List<URL> urls = new ArrayList<>(8);
			DataInputStream in = new DataInputStream(System.in);
			String path;
			while (!(path = in.readUTF()).isEmpty())
				urls.add(Paths.get(path).toUri().toURL());
			ClassLoader appClassLoader = ApplicationLauncher.class.getClassLoader();
			ClassLoader platformClassLoader = appClassLoader.getParent();
			classLoader = new AppClassLoader(urls.toArray(new URL[0]), platformClassLoader);
		}

		// Get the main class
		System.out.println("Resolving Recaf entry-point...");
		URL manifestUrl = classLoader.findResource(JarFile.MANIFEST_NAME);
		if (manifestUrl == null) {
			System.err.printf("Cannot locate '%s' entry%n", JarFile.MANIFEST_NAME);
			System.exit(1);
		}
		Manifest manifest;
		try (InputStream in = manifestUrl.openStream()) {
			manifest = new Manifest(in);
		}
		String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
		if (mainClass == null) {
			System.err.printf("Cannot find '%s' in '%s'%n", Attributes.Name.MAIN_CLASS, JarFile.MANIFEST_NAME);
			System.exit(2);
		}

		// Launch Recaf
		System.out.println("Launching Recaf...");
		Thread.currentThread().setContextClassLoader(classLoader);
		MethodHandles.lookup()
				.findStatic(classLoader.loadClass(mainClass), "main", MethodType.methodType(void.class, String[].class))
				.asFixedArity()
				.invokeExact((String[]) args);
	}
}
