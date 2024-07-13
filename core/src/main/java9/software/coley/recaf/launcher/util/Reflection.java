package software.coley.recaf.launcher.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;

/**
 * Reflection hacks: https://github.com/xxDark/deencapsulation
 */
public class Reflection {
	private static final MethodHandle CLASS_MODULE;
	private static final MethodHandle CLASS_LOADER_MODULE;
	private static final MethodHandle METHOD_MODIFIERS;

	private Reflection() {}

	public static void setup() {
		deencapsulate(Reflection.class);
	}

	public static void deencapsulate(Class<?> classBase) {
		try {
			Method export = Module.class.getDeclaredMethod("implAddOpens", String.class);
			setMethodModifiers(export, Modifier.PUBLIC);
			HashSet<Module> modules = new HashSet<>();
			Module base = getClassModule(classBase);
			if (base.getLayer() != null)
				modules.addAll(base.getLayer().modules());
			modules.addAll(ModuleLayer.boot().modules());
			for (ClassLoader cl = classBase.getClassLoader(); cl != null; cl = cl.getParent()) {
				modules.add(getLoaderModule(cl));
			}
			for (Module module : modules) {
				for (String name : module.getPackages()) {
					try {
						export.invoke(module, name);
					} catch (Exception ex) {
						throw new AssertionError(ex);
					}
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Could not export packages", ex);
		}
	}

	private static Module getClassModule(Class<?> klass) {
		try {
			return (Module) CLASS_MODULE.invokeExact(klass);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
	}

	private static Module getLoaderModule(ClassLoader loader) {
		try {
			return (Module) CLASS_LOADER_MODULE.invokeExact(loader);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
	}

	private static void setMethodModifiers(Method method, int modifiers) {
		try {
			METHOD_MODIFIERS.invokeExact(method, modifiers);
		} catch (Throwable t) {
			throw new AssertionError(t);
		}
	}

	static {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			Unsafe unsafe = (Unsafe) field.get(null);
			field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
			MethodHandles.publicLookup();
			MethodHandles.Lookup lookup = (MethodHandles.Lookup)
					unsafe.getObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field));
			MethodType type = MethodType.methodType(Module.class);
			CLASS_MODULE = lookup.findVirtual(Class.class, "getModule", type);
			CLASS_LOADER_MODULE = lookup.findVirtual(ClassLoader.class, "getUnnamedModule", type);
			METHOD_MODIFIERS = lookup.findSetter(Method.class, "modifiers", Integer.TYPE);
		} catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}
