package app.saikat.DIManagement.Impl.Helpers;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import com.google.common.base.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.ScanAnnotation;
import app.saikat.Annotations.DIManagement.ScanInterface;
import app.saikat.Annotations.DIManagement.ScanSubClass;
import app.saikat.DIManagement.Impl.BeanManagers.NoOpBeanManager;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.Results;

/**
 * Helper class of DIBeanManager s. Creates and stores all instances of beanmanagers,
 * sets beanmanagers of beans, and holds other data structures required to create
 */
public class DIBeanManagerHelper {

	private final Results results;
	private final Map<DIBean<?>, Set<WeakReference<?>>> map;

	private Map<Class<? extends DIBeanManager>, DIBeanManager> beanManagers = new ConcurrentHashMap<>();

	private Logger logger = LogManager.getLogger(DIBeanManagerHelper.class);

	public DIBeanManagerHelper(Results results, Map<DIBean<?>, Set<WeakReference<?>>> map) {
		this.results = results;
		this.map = map;
	}

	public void createBeanManagers(Set<Class<? extends DIBeanManager>> managers) {

		managers.parallelStream()
				.filter(m -> !Modifier.isInterface(m.getModifiers()) && !Modifier.isAbstract(m.getModifiers()))
				.map(b -> {
					try {
						DIBeanManager manager = b.getConstructor(Results.class, Map.class, DIBeanManagerHelper.class)
								.newInstance(results, map, this);

						logger.debug("Created new instance of {}: {}", b.getSimpleName(), manager);
						return manager;
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException | NoSuchMethodException | SecurityException e) {
						logger.error("Error: ", e);
					}
					return null;
				}).filter(Objects::nonNull).forEach(m -> {
					beanManagers.put(m.getClass(), m);
				});
	}

	@SuppressWarnings("unchecked")
	public DIBeanManager getManagerOf(Class<?> cls) {
		BiFunction<Object, Function<Object, Class<?>[]>, Class<? extends DIBeanManager>> getBeanManager = (o,
				managerGetter) -> {
			if (o == null)
				return null;
			Class<?>[] managerClasses = managerGetter.apply(o);
			if (managerClasses.length == 0 || managerClasses.length > 1)
				return null;

			Class<?> managerClass = managerClasses[0];

			return (managerClass == null || !DIBeanManager.class.isAssignableFrom(managerClass)) ? null
					: (Class<? extends DIBeanManager>) managerClass;
		};

		Class<? extends DIBeanManager> mgr = getBeanManager.apply(results.getAnnotationsToScan().get(cls),
				m -> ((ScanAnnotation) m).beanManager());
		if (mgr != null)
			return beanManagers.get(mgr);

		mgr = getBeanManager.apply(results.getInterfacesToScan().get(cls), m -> ((ScanInterface) m).beanManager());
		if (mgr != null)
			return beanManagers.get(mgr);

		mgr = getBeanManager.apply(results.getSuperClassesToScan().get(cls), m -> ((ScanSubClass) m).beanManager());
		return beanManagers.get(mgr == null ? NoOpBeanManager.class : mgr);
	}

	@SuppressWarnings("unchecked")
	public <T extends DIBeanManager> T getManagerOfType(Class<? extends DIBeanManager> cls) {
		return (T) beanManagers.get(cls);
	}

	public Collection<DIBeanManager> getAllBeanManagers() {
		return this.beanManagers.values();
	}
}