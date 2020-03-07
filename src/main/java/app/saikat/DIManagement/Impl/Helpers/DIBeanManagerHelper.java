package app.saikat.DIManagement.Impl.Helpers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.Scan;
import app.saikat.DIManagement.Exceptions.NoManagerFoundException;
import app.saikat.DIManagement.Impl.Repository.Repository;
import app.saikat.DIManagement.Interfaces.DIBeanManager;

/**
 * Helper class of DIBeanManager s. Creates and stores all instances of beanmanagers,
 * sets beanmanagers of beans, and holds other data structures required to create
 */
public class DIBeanManagerHelper {

	private static Logger logger = LogManager.getLogger(DIBeanManagerHelper.class);

	/**
	 * Instantiate all {@link DIBeanManager} classes provided
	 * @param managers DIBeanManager classes which needs to be instantiated
	 * @return instantiated collection of DIBeanManagers'
	 */
	public static Collection<DIBeanManager> createBeanManagers(Collection<Class<? extends DIBeanManager>> managers) {
		return managers.parallelStream()
				.filter(m -> !Modifier.isInterface(m.getModifiers()) && !Modifier.isAbstract(m.getModifiers()))
				.map(b -> {
					try {
						DIBeanManager manager = b.getConstructor()
								.newInstance();

						logger.debug("Created new instance of {}: {}", b.getSimpleName(), manager);
						return manager;
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException | NoSuchMethodException | SecurityException e) {
						logger.error("Error: ", e);
					}
					return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	/**
	 * Gets the manager of a class. The cls is either a non qualifier, interface or superclass
	 * @param cls the class whose manager is being queried
	 * @param currentScanRepo the current scan repo. This is temporary repo. If no problem occurs,
	 * will be merged back to globalRepo after scanning. Can be null if not required
	 * @param globalRepo the global repo containing all already scanned objects
	 * @return manager of the cls. currentScanRepo is checked first before checking globalRepo
	 * @throws NoManagerFoundException exception is thrown when no DIBeanManager is found for the requested cls
	 */
	@SuppressWarnings("unchecked")
	public static DIBeanManager getManagerOf(Class<?> cls, Repository currentScanRepo, Repository globalRepo)
			throws NoManagerFoundException {
		if (cls == null)
			return null;

		Function<Repository, DIBeanManager> managerGetter = repo -> {
			Scan scan = repo.getScanData()
					.get(cls);

			if (scan == null)
				return null;
			Class<?>[] managerClasses = scan.beanManager();

			if (managerClasses.length == 0 || managerClasses.length > 1)
				return null;

			Class<?> managerClass = managerClasses[0];

			if (managerClass == null || !DIBeanManager.class.isAssignableFrom(managerClass))
				return null;

			return repo.getBeanManagers()
					.get((Class<? extends DIBeanManager>) managerClass);

		};

		DIBeanManager mgr = currentScanRepo != null ? managerGetter.apply(currentScanRepo) : null;
		if (mgr == null)
			mgr = managerGetter.apply(globalRepo);

		if (mgr == null) {
			logger.error("No manager found for {}. ScanData in currentRepo: {}, globalRepo: {}", cls, currentScanRepo != null
					? currentScanRepo.getScanData()
					: null, globalRepo.getScanData());
			throw new NoManagerFoundException(cls);
		}

		return mgr;

	}
}