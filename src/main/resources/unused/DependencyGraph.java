// package app.saikat.DIManagement.Impl;

// import java.lang.annotation.Annotation;
// import java.lang.reflect.Constructor;
// import java.lang.reflect.Method;
// import java.lang.reflect.Modifier;
// import java.lang.reflect.ParameterizedType;
// import java.lang.reflect.Type;
// import java.util.ArrayDeque;
// import java.util.ArrayList;
// import java.util.Collection;
// import java.util.Collections;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Queue;
// import java.util.Set;
// import java.util.stream.Collectors;

// import javax.inject.Provider;

// import com.google.common.collect.Lists;
// import com.google.common.collect.Sets;
// import com.google.common.graph.GraphBuilder;
// import com.google.common.graph.Graphs;
// import com.google.common.graph.ImmutableGraph;
// import com.google.common.graph.MutableGraph;

// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;

// import app.saikat.PojoCollections.CommonObjects.Either;
// import app.saikat.Annotations.DIManagement.NoQualifier;
// import app.saikat.DIManagement.Exceptions.CircularDependencyException;
// import app.saikat.DIManagement.Exceptions.NoProviderFoundForClassException;
// import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
// import app.saikat.DIManagement.Impl.DIBeans.DIManagerBean;
// import app.saikat.DIManagement.Impl.DIBeans.UnresolvedDIBeanImpl;
// import app.saikat.DIManagement.Interfaces.DIBean;
// import app.saikat.DIManagement.Interfaces.Results;

// class DependencyGraph {

// 	private final MutableGraph<DIBean<?>> graph;
// 	private final Set<DIBeanImpl<?>> alreadyScanned;
// 	private final Results results;
// 	private final DIBeanManagerHelper helper;

// 	private boolean isModified;
// 	private ImmutableGraph<DIBean<?>> immutableGraph;

// 	private Logger logger = LogManager.getLogger(this.getClass());

// 	public DependencyGraph(Results results, DIBeanManagerHelper helper, DIManagerBean managerBean) {
// 		graph = GraphBuilder.directed().allowsSelfLoops(false).build();

// 		alreadyScanned = new HashSet<>();
// 		alreadyScanned.add(managerBean);

// 		this.helper = helper;
// 		this.results = results;
// 		this.results.addAnnotationBean(managerBean);
// 	}

// 	@SuppressWarnings({ "unchecked", "rawtypes" })
// 	public synchronized void generateGraph() {

// 		logger.debug("Starting graph generation");

// 		// Managed annotations
// 		Set<Class<? extends Annotation>> managedAnnotations = results.getAnnotationsToScan().entrySet().parallelStream()
// 				.filter(t -> t.getValue().autoManage()).map(t -> t.getKey()).collect(Collectors.toSet());

// 		logger.debug("Auto-managed annotations: {}", managedAnnotations);

// 		Collection<DIBeanImpl<?>> beans = (Collection) results.getAnnotationBeans().parallelStream()
// 				.filter(b -> getAnnotation(b.getNonQualifierAnnotations(), managedAnnotations) != null)
// 				.collect(Collectors.toSet());

// 		logger.debug("Auto-managed annotatation beans are: {}", beans);
// 		addBeans(beans);

// 		// Managed interfaces
// 		Set<Class<?>> managedInterfaces = results.getInterfacesToScan().entrySet().parallelStream()
// 				.filter(t -> t.getValue().autoManage()).map(t -> t.getKey()).collect(Collectors.toSet());

// 		logger.debug("Auto-managed interfaces: {}", managedInterfaces);

// 		beans = (Collection) results.getInterfaceBeans().parallelStream()
// 				.filter(bean -> hasSuperClass(bean.getProviderType(), managedInterfaces)).collect(Collectors.toSet());

// 		logger.debug("Auto-managed interface beans are: {}", beans);

// 		addBeans(beans);

// 		// Managed superclasses
// 		Set<Class<?>> managedSuperClasses = results.getSuperClassesToScan().entrySet().parallelStream()
// 				.filter(t -> t.getValue().autoManage()).map(t -> t.getKey()).collect(Collectors.toSet());

// 		logger.debug("Auto-managed superclasses: {}", managedSuperClasses);
// 		logger.debug("subclass map: {}", results.getSubClassesMap());

// 		beans = (Collection) results.getSubclassBeans().parallelStream()
// 				.filter(bean -> hasSuperClass(bean.getProviderType(), managedSuperClasses)).collect(Collectors.toSet());

// 		logger.debug("Auto-managed subclass beans are: {}", beans);
// 		addBeans(beans);

// 	}

// 	public ImmutableGraph<DIBean<?>> getDependencyGraph() {
// 		if (isModified || immutableGraph == null) {
// 			synchronized (this) {
// 				if (isModified || immutableGraph == null) {
// 					immutableGraph = ImmutableGraph.copyOf(graph);
// 					isModified = false;
// 				}
// 			}
// 		}
// 		return immutableGraph;
// 	}

// 	// These are called from synchronized addBeans(). Hence already locked on 'this'.
// 	// Doublecheck before parallelizing any calls
// 	private void addBeans(Collection<DIBeanImpl<?>> beans) {

// 		Queue<DIBeanImpl<?>> scanQueue = new ArrayDeque<>(beans);

// 		while (!scanQueue.isEmpty()) {
// 			DIBeanImpl<?> target = scanQueue.poll();

// 			if (alreadyScanned.contains(target)) {
// 				continue;
// 			}

// 			logger.debug("Adding {} to dependencyGraph", target);
// 			graph.addNode(target);

// 			List<DIBeanImpl<?>> dependencies = scanForDependencies(target);
// 			dependencies = resolveDependencies(dependencies, scanQueue);
// 			helper.beanDependencyResolved(target, this.results);
// 			logger.info("Dependencies of {} are resolved to {}", target, dependencies);

// 			//Store this dependency list so that we don't have to recompute this later during Provider creation
// 			target.setDependencies(dependencies);

// 			dependencies.forEach(bean -> checkAndAddPair(target, bean));

// 			alreadyScanned.add(target);
// 		}
// 	}

// 	private void checkAndAddPair(DIBean<?> target, DIBean<?> dependent) {
// 		// Static methods have 1st dependent null

// 		if (dependent == null)
// 			return;
// 		try {
// 			graph.putEdge(target, dependent);

// 			if (Graphs.hasCycle(graph)) {
// 				throw new CircularDependencyException(target, dependent);
// 			}
// 			isModified = true;
// 		} catch (IllegalArgumentException e) {
// 			throw new CircularDependencyException(target, dependent);
// 		}
// 	}

// 	private <T> List<DIBeanImpl<?>> scanForDependencies(DIBean<T> target) {
// 		List<DIBeanImpl<?>> dependencies = Collections.synchronizedList(new ArrayList<>());

// 		List<Class<?>> params;
// 		List<Annotation[]> paramsAnnotations;
// 		List<Type> genericTypes;

// 		Either<Constructor<T>, Method> t = ((DIBeanImpl<T>) target).get();

// 		if (t.containsLeft()) {
// 			Constructor<T> cons = t.getLeft().get();
// 			logger.trace("Scanning parameters of {} constructor", cons.getName());

// 			params = Lists.newArrayList(cons.getParameterTypes());
// 			paramsAnnotations = Lists.newArrayList(cons.getParameterAnnotations());
// 			genericTypes = Lists.newArrayList(cons.getGenericParameterTypes());
// 		} else {
// 			Method meth = t.getRight().get();
// 			logger.trace("Scanning parameters of {} method", meth.getName());

// 			params = Lists.newArrayList(meth.getParameterTypes());
// 			paramsAnnotations = Lists.newArrayList(meth.getParameterAnnotations());
// 			genericTypes = Lists.newArrayList(meth.getGenericParameterTypes());

// 			if (!Modifier.isStatic(meth.getModifiers())) {
// 				Set<Annotation> annotationsOnParent = Sets.newHashSet(meth.getDeclaringClass().getAnnotations());
// 				DIBeanImpl<?> parentClass = getUnresolvedBean(meth.getDeclaringClass(), annotationsOnParent,
// 						results.getQualifierAnnotations());

// 				logger.debug("Method {} is non static. Adding {} as unresolved dependency", meth, parentClass);
// 				dependencies.add(parentClass);
// 			} else {
// 				// Do not remove. Method bean has enclosing class as 1st dependency in dependencyList. It is null
// 				// for static methods
// 				dependencies.add(null);
// 			}
// 		}

// 		for (int i = 0; i < params.size(); i++) {
// 			Class<?> p = params.get(i);

// 			// Special case Provider. In this case, find out the parameterized type of Provider. Hence Provider<T>
// 			// will throw error and correct way is to use with Class only i.e. Provider<Server>
// 			if (p.equals(Provider.class)) {
// 				try {
// 					p = Class.forName(
// 							((ParameterizedType) genericTypes.get(i)).getActualTypeArguments()[0].getTypeName());

// 					logger.trace("Provider resolved to {}", p.getSimpleName());
// 				} catch (ClassNotFoundException e) {
// 					logger.error("Error: ", e);
// 				}
// 			}

// 			DIBeanImpl<?> dep = getUnresolvedBean(p, Sets.newHashSet(paramsAnnotations.get(i)),
// 					results.getQualifierAnnotations());
// 			logger.debug("Unresolved dependency {} added", dep);
// 			dependencies.add(dep);
// 		}

// 		return dependencies;
// 	}

// 	private List<DIBeanImpl<?>> resolveDependencies(List<DIBeanImpl<?>> dependencies,
// 			Collection<DIBeanImpl<?>> currentScanBatch) {
// 		return dependencies.stream().map(toResolve -> {
// 			logger.debug("Trying to resolve dependency {}", toResolve);

// 			// Static methods will have 1st parameter as null
// 			if (toResolve == null)
// 				return null;

// 			DIBeanImpl<?> resolvedDep = alreadyScanned.parallelStream()
// 					.filter(b -> toResolve.getProviderType().isAssignableFrom(b.getProviderType())
// 							&& b.getQualifier().equals(toResolve.getQualifier()))
// 					.findFirst().orElse(null);

// 			if (resolvedDep == null) {
// 				logger.debug("Not already scanned");
// 				resolvedDep = currentScanBatch.parallelStream()
// 						.filter(b -> b.getProviderType().equals(toResolve.getProviderType())
// 								&& b.getQualifier().equals(toResolve.getQualifier()))
// 						.findFirst().orElse(null);
// 			}

// 			if (resolvedDep == null) {
// 				logger.debug("Not in currentbatch too. Hence no provider was found");
// 				throw new NoProviderFoundForClassException(toResolve.getProviderType());
// 			} else {
// 				logger.debug("Dependency {} resolved to {}", toResolve, resolvedDep);
// 				return resolvedDep;
// 			}
// 		}).collect(Collectors.toList());
// 	}

// 	private DIBeanImpl<?> getUnresolvedBean(Class<?> cls, Set<Annotation> annotations,
// 			Set<Class<? extends Annotation>> qualifierAnnotations) {

// 		Set<Class<? extends Annotation>> annotationClasses = annotations.parallelStream().map(a -> a.annotationType())
// 				.collect(Collectors.toSet());

// 		Class<? extends Annotation> q = getAnnotation(annotationClasses, qualifierAnnotations);

// 		// All that matters is the qualifier and what type of object this bean will create
// 		return new UnresolvedDIBeanImpl<>(cls, q != null ? q : NoQualifier.class);
// 	}

// 	private Class<? extends Annotation> getAnnotation(Collection<Class<? extends Annotation>> annotations,
// 			Collection<Class<? extends Annotation>> annotationsToSearch) {

// 		return annotations.parallelStream().filter(a -> annotationsToSearch.contains(a)).findAny().orElse(null);

// 	}

// 	private boolean hasSuperClass(Class<?> cls, Set<Class<?>> set) {
// 		return set.parallelStream().filter(b -> b.isAssignableFrom(cls)).findAny().isPresent();
// 	}
// }