package app.saikat.DIManagement.Impl;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import com.google.common.base.Function;
import com.google.common.graph.MutableGraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.ScanAnnotation;
import app.saikat.Annotations.DIManagement.ScanInterface;
import app.saikat.Annotations.DIManagement.ScanSubClass;
import app.saikat.DIManagement.Impl.BeanManagers.NoOpBeanManager;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.Results;

/**
 * Helper class of DIBeanManager s. Creates and stores all instances of beanmanagers,
 * sets beanmanagers of beans, and holds other data structures required to create
 */
public class DIBeanManagerHelper {

	private final Results results;
	private final MutableGraph<DIBean<?>> graph;
	private final Map<DIBean<?>, Set<WeakReference<?>>> map;

	private Map<Class<? extends DIBeanManager>, DIBeanManager> beanManagers = new ConcurrentHashMap<>();

	private Logger logger = LogManager.getLogger(DIBeanManagerHelper.class);

	public DIBeanManagerHelper(Results results, MutableGraph<DIBean<?>> graph,
			Map<DIBean<?>, Set<WeakReference<?>>> map) {
		this.results = results;
		this.graph = graph;
		this.map = map;
	}

	// cls in null for annotation beans. For interface nad subclass beans, cls hold superclass
	public void beanCreated(DIBean<?> bean, Class<?> cls) {

		DIBeanManager manager = getManagerOf(cls);

		logger.debug("Setting bean manager of {} to {}", bean, manager);
		((DIBeanImpl<?>) bean).setBeanManager(manager);
		bean.getBeanManager().beanCreated(bean, cls);
	}

	public void createBeanManagers(Set<Class<? extends DIBeanManager>> managers) {

		managers.parallelStream().map(b -> {
			try {
				DIBeanManager manager = b.getConstructor(Results.class, MutableGraph.class, Map.class, DIBeanManagerHelper.class)
						.newInstance(results, graph, map, this);

				logger.debug("Created new instance of {}: {}", b.getSimpleName(), manager);
				return manager;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				logger.error(e);
			}
			return null;
		}).forEach(m -> {
			beanManagers.put(m.getClass(), m);
		});
	}

	@SuppressWarnings("unchecked")
	public DIBeanManager getManagerOf(Class<?> cls) {
		BiFunction<Object, Function<Object, Class<?>[]>, Class<? extends DIBeanManager>> getBeanManager = (o, managerGetter) -> {
			if (o == null) return null;
			Class<?>[] managerClasses = managerGetter.apply(o);
			if (managerClasses.length == 0 || managerClasses.length > 1) return null;

			Class<?> managerClass = managerClasses[0];

			return (managerClass == null || !DIBeanManager.class.isAssignableFrom(managerClass))
				? null
				: (Class<? extends DIBeanManager>) managerClass;
		};

		Class<? extends DIBeanManager> mgr = getBeanManager.apply(results.getAnnotationsToScan().get(cls), m -> ((ScanAnnotation)m).beanManager());
		if (mgr != null) return beanManagers.get(mgr);

		mgr = getBeanManager.apply(results.getInterfacesToScan().get(cls), m -> ((ScanInterface)m).beanManager());
		if (mgr != null) return beanManagers.get(mgr);

		mgr = getBeanManager.apply(results.getSuperClassesToScan().get(cls), m -> ((ScanSubClass)m).beanManager());
		return beanManagers.get(mgr == null ? NoOpBeanManager.class : mgr);
	}

	public Collection<DIBeanManager> getAllBeanManagers() {
		return this.beanManagers.values();
	}
}