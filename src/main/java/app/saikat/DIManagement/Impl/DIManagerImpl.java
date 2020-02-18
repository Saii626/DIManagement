package app.saikat.DIManagement.Impl;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIManager;

public class DIManagerImpl extends DIManager {

	// Data structures
	// private MutableGraph<DIBean<?>> mutableGraph;

	private DIBeanManagerHelper helper;

	// public DIManagerImpl() {
	// mutableGraph = GraphBuilder.directed().allowsSelfLoops(false).build();
	// }

	@Override
	public void initialize(String... pathsToScan) {

		logger.info("Initializing DIManager");
		helper = new DIBeanManagerHelper(results, objectMap);

		// Scanning
		logger.info("Starting scans");
		ClasspathScanner scanner = new ClasspathScanner(results, helper);
		scanner.scan(this, pathsToScan);

		helper.getAllBeanManagers()
				.parallelStream()
				.forEach(DIBeanManager::scanComplete);

		// Create providers first. Since the dependencies are resolved when the provider
		// is actually invoked, there is no issue in creating providers first
		createProviderBeans();
		helper.getAllBeanManagers()
				.parallelStream()
				.forEach(DIBeanManager::providerCreated);

		logger.info("All generated beans: {}", results.getGeneratedBeans());

		// Resolving dependencies
		resolveDependencies();

		helper.getAllBeanManagers()
				.parallelStream()
				.forEach(DIBeanManager::dependencyResolved);

	}

	private void createProviderBeans() {
		Queue<DIBean<?>> toCreate = new LinkedList<>();
		toCreate.addAll(results.getAnnotationBeans());
		toCreate.addAll(results.getInterfaceBeans());
		toCreate.addAll(results.getSubclassBeans());

		while (!toCreate.isEmpty()) {
			DIBean<?> current = toCreate.poll();

			if (!current.getBeanManager()
					.shouldCreateProvider()) {
				logger.debug("Skipping creation on Provider for {} as shouldCreateProvider is false", current);
				continue;
			}

			current.getBeanManager()
					.createProviderBean(current);
		}
	}

	private void resolveDependencies() {
		Queue<DIBean<?>> toResolve = new LinkedList<>();
		toResolve.addAll(results.getAnnotationBeans());
		toResolve.addAll(results.getInterfaceBeans());
		toResolve.addAll(results.getSubclassBeans());

		Collection<DIBean<?>> resolved = new HashSet<>();
		resolved.addAll(results.getGeneratedBeans());

		while (!toResolve.isEmpty()) {
			DIBean<?> current = toResolve.poll();
			if (!current.getBeanManager()
					.shouldResolveDependency())
				continue;

			current.getBeanManager()
					.resolveDependencies(current, resolved, toResolve);
			resolved.add(current);
		}
	}

	// private List<DIBean<?>> getBuildListFor(DIBean<?> bean) {

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <T> Set<DIBean<T>> getBeanOfTypeUncached(Class<T> cls, Class<? extends Annotation> annot) {
		TypeToken<T> type = TypeToken.of(cls);

		BiFunction<DIBean<?>, Class<? extends Annotation>, Boolean> beanHasAnnotation = (bean, annotation) -> {
			Class<? extends Annotation> q = bean.getQualifier();
			Class<? extends Annotation> o = bean.getNonQualifierAnnotation();
			return (q != null && q.equals(annot)) || (o != null && o.equals(annot));
		};

		Stream<DIBean<?>> annotBeans = results.getAnnotationBeans()
				.parallelStream()
				.filter(bean -> bean.getProviderType()
						.isSubtypeOf(type) && beanHasAnnotation.apply(bean, annot));
		Stream<DIBean<?>> interfaceBeans = results.getInterfaceBeans()
				.parallelStream()
				.filter(bean -> bean.getProviderType()
						.isSubtypeOf(type) && beanHasAnnotation.apply(bean, annot));
		Stream<DIBean<?>> superclassBeans = results.getSubclassBeans()
				.parallelStream()
				.filter(bean -> bean.getProviderType()
						.isSubtypeOf(type) && beanHasAnnotation.apply(bean, annot));
		Stream<DIBean<?>> generatedBeans = results.getGeneratedBeans()
				.parallelStream()
				.filter(bean -> bean.getProviderType()
						.isSubtypeOf(type) && beanHasAnnotation.apply(bean, annot));

		Set<DIBean<?>> beans = Stream.of(annotBeans, interfaceBeans, superclassBeans, generatedBeans)
				.flatMap(s -> s)
				.collect(Collectors.toSet());
		logger.debug("Added {} in cache", beans);

		return (Set) beans;
	}

	// 	if (!mutableGraph.nodes().contains(bean)) {
	// 		logger.warn("Bean {} not present", bean);
	// 		return Collections.emptyList();
	// 	}

	// 	Traverser<DIBean<?>> traverser = Traverser.forGraph(mutableGraph);
	// 	Iterable<DIBean<?>> dfsIterator = traverser.depthFirstPostOrder(bean);

	// 	List<DIBean<?>> buildList = Lists.newArrayList(dfsIterator);
	// 	buildList.remove(buildList.size() - 1);

	// 	return Collections.unmodifiableList(buildList);
	// }

}