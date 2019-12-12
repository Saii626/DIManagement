package app.saikat.DIManagement.Impl;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.google.common.graph.ImmutableGraph;

import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIManager;

public class DIManagerImpl extends DIManager {

	private ObjectFactory objectFactory;

	@Override
	public void initialize(String... pathsToScan) {

		DIBeanManagerHelper helper = new DIBeanManagerHelper();

		ClasspathScanner scanner = new ClasspathScanner(helper);
		this.results = scanner.scan(pathsToScan);

		// Set<DIBean<?>> providedBeans = this.results.getAnnotationBeans().parallelStream()
		// 		.filter(b -> b.getNonQualifierAnnotations().contains(Provides.class)).collect(Collectors.toSet());
		// logger.debug("Beans annotated with Provides: {}", providedBeans);
		// providedBeans.parallelStream().map(b -> (DIBeanImpl<?>) b).forEach(b -> {
		// 	Provides provides = b.get().getRight().get().getAnnotation(Provides.class);
		// 	logger.debug("Setting {} singletonStatus to {}", b, provides.singleton());
		// 	b.setSingleton(provides.singleton());
		// });

		DependencyGraph dependencyGraph = new DependencyGraph(results, helper);
		dependencyGraph.generateGraph();
		ImmutableGraph<DIBean<?>> graph = dependencyGraph.getDependencyGraph();

		objectFactory = new ObjectFactory(graph, helper, results);
		objectFactory.generateProviders();

		makeResultImmutable();

		Set<DIBean<?>> beans = getBeansAnnotatedWith(PostConstruct.class);
		logger.debug("Postconstruct beans: {}", beans);

		beans.parallelStream().forEach(bean -> bean.getProvider().get());

	}

	@Override
	public Map<DIBean<?>, Set<WeakReference<?>>> getImmutableObjectMap() {
		return this.objectFactory.getImmutableViewOfObjectMap();
	}

}