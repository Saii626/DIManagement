package app.saikat.DIManagement.Impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import javax.inject.Provider;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Impl.BeanManagers.NoOpBeanManager;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIBeanType;
import app.saikat.DIManagement.Interfaces.DIManager;

public class DIManagerImpl extends DIManager {

	// Data structures
	private MutableGraph<DIBean<?>> mutableGraph;

	private DIBeanManagerHelper helper;

	public DIManagerImpl() {
		mutableGraph = GraphBuilder.directed().allowsSelfLoops(false).build();
	}

	@Override
	public void initialize(String... pathsToScan) {

		logger.info("Initializing DIManager");
		helper = new DIBeanManagerHelper(results, mutableGraph, objectMap);

		ConstantProviderBean<DIManager> managerBean = new ConstantProviderBean<>(this, DIManager.class,
				NoQualifier.class, Collections.emptyList(), DIBeanType.GENERATED);
		results.addGeneratedBean(managerBean);

		// Scanning
		logger.info("Starting scans");
		ClasspathScanner scanner = new ClasspathScanner(results, helper);
		scanner.scan(pathsToScan);

		managerBean.setBeanManager(helper.getManagerOf(NoOpBeanManager.class));
		helper.getAllBeanManagers().parallelStream().forEach(DIBeanManager::scanComplete);

		// Create providers first. Since the dependencies are resolved when the provider
		// is actually invoked, there is no issue in creating providers first
		createProviders();
		helper.getAllBeanManagers().parallelStream().forEach(DIBeanManager::providerCreated);

		logger.debug("All generated beans: {}", results.getGeneratedBeans());

		// Resolving dependencies
		resolveDependencies();

		helper.getAllBeanManagers().parallelStream().forEach(DIBeanManager::dependencyResolved);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void createProviders() {
		Queue<DIBean<?>> toCreate = new LinkedList<>();
		toCreate.addAll(results.getAnnotationBeans());
		toCreate.addAll(results.getInterfaceBeans());
		toCreate.addAll(results.getSubclassBeans());

		while (!toCreate.isEmpty()) {
			DIBean<?> current = toCreate.poll();

			if (!current.getBeanManager().shouldCreateProvider())
				continue;

			logger.debug("Creating provider of {}", current);
			ProviderImpl<?> provider = new ProviderImpl<>(current);
			provider.setHelper(helper);
			
			ConstantProviderBean<Provider> providerBean = new ConstantProviderBean<>(provider, Provider.class,
					current.getQualifier(), Collections.singletonList(current.getProviderType().getTypeName()),
					DIBeanType.GENERATED);

			providerBean.setBeanManager(helper.getManagerOf(DIBeanManager.class));
			logger.debug("Provider created for {}", current);

			mutableGraph.putEdge(providerBean, current);
			results.addGeneratedBean(providerBean);

			logger.debug("Setting {} as provider bean of {} ", providerBean, current);
			((DIBeanImpl) current).setProviderBean(providerBean);
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
			if (!current.getBeanManager().shouldResolveDependency())
				continue;

			current.getBeanManager().resolveDependencies(current, resolved, toResolve);
			resolved.add(current);
		}
	}

	// private List<DIBean<?>> getBuildListFor(DIBean<?> bean) {

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