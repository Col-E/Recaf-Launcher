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

public final class ApplicationLauncher {
	private ApplicationLauncher() {
	}

	public static void main(String[] args) throws Throwable {
		AppClassLoader classLoader;
		{
			List<URL> urls = new ArrayList<>(8);
			DataInputStream in = new DataInputStream(System.in);
			String path;
			while (!(path = in.readUTF()).isEmpty()) {
				urls.add(Paths.get(path).toUri().toURL());
			}
			classLoader = new AppClassLoader(urls.toArray(new URL[0]), ApplicationLauncher.class.getClassLoader().getParent());
		}
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
		System.out.println("Launching Recaf...");
		Thread.currentThread().setContextClassLoader(classLoader);
		MethodHandles.lookup()
				.findStatic(classLoader.loadClass(mainClass), "main", MethodType.methodType(void.class, String[].class))
				.asFixedArity()
				.invokeExact((String[]) args);
	}
}
