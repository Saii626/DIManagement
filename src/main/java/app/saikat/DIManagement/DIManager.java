package app.saikat.DIManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Qualifier;

import com.google.common.collect.Sets;
import com.google.common.graph.ImmutableGraph;

import app.saikat.LogManagement.Logger;
import app.saikat.LogManagement.LoggerFactory;
import app.saikat.PojoCollections.CommonObjects.Either;
import app.saikat.PojoCollections.CommonObjects.Tuple;
import app.saikat.DIManagement.Annotations.ScanAnnotation;
import app.saikat.DIManagement.Annotations.Provides;
import app.saikat.DIManagement.Exceptions.ClassNotUnderDIException;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class DIManager {

	private static ObjectMap objectMap;
	private static DependencyGraph dependencyGraph;
	private static Logger logger = LoggerFactory.getLogger(DIManager.class);

	private static List<String> QUALIFIER_ANNOT;
	private static List<String> SCANNED_ANNOT;
	private static Set<Class<? extends Annotation>> QUALIFIER_ANNOATTIONS;
	private static Set<Class<? extends Annotation>> SCANNED_ANNOATTIONS;
	private static Map<Class<? extends Annotation>, List<Either<Class<?>, Method>>> annotationsMap;

	public static void initialize(String... packagesToScan) {

		logger.info("Initializing DIManager. Starting scans");
		try (ScanResult results = new ClassGraph().enableClassInfo()
				.enableMethodInfo()
				.ignoreMethodVisibility()
				.ignoreClassVisibility()
				.enableAnnotationInfo()
				.whitelistPackages(packagesToScan)
				.scan()) {

			// Gather all @Qualifiers annotations
			QUALIFIER_ANNOATTIONS = scanAnnotationForAnnotation(Qualifier.class, results);
			SCANNED_ANNOATTIONS = scanAnnotationForAnnotation(ScanAnnotation.class, results);

			QUALIFIER_ANNOT = QUALIFIER_ANNOATTIONS.stream()
					.map(annot -> annot.getName())
					.collect(Collectors.toList());
			logger.info("All declared @Qualifier annotations are: {}", Arrays.toString(QUALIFIER_ANNOT.toArray()));

			SCANNED_ANNOT = SCANNED_ANNOATTIONS.stream()
					.map(annot -> annot.getName())
					.collect(Collectors.toList());
			logger.info("All declared @AnnotationConfig annotations are: {}", Arrays.toString(SCANNED_ANNOT.toArray()));

			dependencyGraph = new DependencyGraph(QUALIFIER_ANNOATTIONS);

			List<ClassInfo> allClasses = results.getAllClasses();

			Set<DIBean> initBeans = Collections.synchronizedSet(new HashSet<>());
			Set<DIBean> autoInvokeBeans = Collections.synchronizedSet(new HashSet<>());

			logger.info("Scanning {} classes in package(s) {}", allClasses.size(), packagesToScan);

			scanAndAddProvidesAnnotation(allClasses);

			scanAndAddInjectAnnotation(allClasses, initBeans);

			scanAndAddAnnotations(allClasses, initBeans, autoInvokeBeans, SCANNED_ANNOATTIONS);

			ImmutableGraph<DIBean> depGraph = dependencyGraph.getDependencyGraph();
			Set<DIBean> allBeans = depGraph.nodes();

			logger.info("Dependency graph generation complete");
			if (logger.isDebugEnabled()) {
				logger.debug("All scanned beans: {}", Utils.getStringRepresentationOf(allBeans));
			}

			objectMap = new ObjectMap(QUALIFIER_ANNOATTIONS, depGraph);
			logger.info("Building autoBuild beans");
			buildObjects(initBeans);
			logger.info("Auto building of beans complete");

			logger.info("Invoking autoInvoke beans");
			invokeMethods(autoInvokeBeans);
			logger.info("Auto invoking of beans complete");
		}
	}

	private static void buildObjects(Set<DIBean> beans) {

		for (DIBean bean : beans) {
			try {
				objectMap.get(bean);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | ClassNotUnderDIException | InterruptedException e) {
				logger.error("Error: {}", e);
			}
		}
	}

	private static void invokeMethods(Set<DIBean> autoInvokeBeans) {

		autoInvokeBeans.forEach(bean -> {
			try {
				objectMap.get(bean);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | ClassNotUnderDIException | InterruptedException e) {
				logger.error("Error: {}", e);
			}

		});
	}

	@SuppressWarnings("unchecked")
	private static Set<Class<? extends Annotation>> scanAnnotationForAnnotation(Class<? extends Annotation> annotation,
			ScanResult results) {
		logger.debug("Scanning for @{} annotation", annotation.getSimpleName());

		Set<ClassInfo> allAnnotations = Sets.newHashSet(results.getAllAnnotations());
		final String annot = annotation.getName();
		return Collections.unmodifiableSet(allAnnotations.stream()
				.filter(clsInfo -> clsInfo.hasAnnotation(annot))
				.map(cls -> (Class<? extends Annotation>) cls.loadClass())
				.collect(Collectors.toSet()));

	}

	private static void scanAndAddProvidesAnnotation(List<ClassInfo> allClasses) {
		logger.debug("Scanning for @Provides annotation");

		Set<DIBean> provideBeans = Collections.synchronizedSet(new HashSet<>());

		final String PROVIDES = Provides.class.getName();
		allClasses.parallelStream()
				.forEach(cls -> {
					logger.trace("Scanning {}", cls.getSimpleName());

					// Check if class is annotated with @Provides and add to beansList
					AnnotationInfo info = cls.getAnnotationInfo(PROVIDES);
					if (info != null) {
						Class<?> loadedClass = cls.loadClass();
						DIBean bean = new DIBean(loadedClass,
								Utils.getQualifierAnnotation(loadedClass.getAnnotations(), QUALIFIER_ANNOATTIONS),
								true);

						safeAdd(provideBeans, Collections.singleton(bean));
						logger.debug("Bean {} (@Provides annotated class) scanned and added", bean);
					}

					cls.getMethodInfo()
							.parallelStream()
							.forEach(method -> {
								logger.trace("Scanning method: {}.{}", method.getClassInfo(), method.getName());

								// Check if method is annotated with @Provides
								AnnotationInfo providesAnnotation = method.getAnnotationInfo(PROVIDES);
								if (providesAnnotation != null) {
									Method m = method.loadClassAndGetMethod();
									DIBean bean = new DIBean(m,
											Utils.getQualifierAnnotation(m.getAnnotations(), QUALIFIER_ANNOATTIONS),
											true);

									safeAdd(provideBeans, Collections.singleton(bean));
									logger.debug("Bean {} (@Provides annotated function) scanned and added", bean);
								}
							});
				});

		logger.debug("Scan for @Provides bean complete. Found {}", Utils.getStringRepresentationOf(provideBeans));
		dependencyGraph.addBeans(provideBeans);
	}

	private static void scanAndAddInjectAnnotation(List<ClassInfo> allClasses, Set<DIBean> initBeans) {

		logger.debug("Scanning for @Inject annotation");

		Set<DIBean> injectBeans = Collections.synchronizedSet(new HashSet<>());

		final String INJECT = Inject.class.getName();

		allClasses.parallelStream()
				.forEach(cls -> {
					logger.trace("Scanning {}", cls.getSimpleName());

					cls.getConstructorInfo()
							.parallelStream()
							.forEach(constructor -> {
								logger.trace("Scanning constructor: {}", constructor.getName());

								// Check if constructor is annotated with @Inject
								AnnotationInfo injectAnnotation = constructor.getAnnotationInfo(INJECT);
								if (injectAnnotation != null) {
									Class<?> c = cls.loadClass();

									DIBean bean = new DIBean(c,
											Utils.getQualifierAnnotation(c.getAnnotations(), QUALIFIER_ANNOATTIONS),
											true);

									safeAdd(initBeans, Collections.singleton(bean));
									safeAdd(injectBeans, Collections.singleton(bean));
									logger.debug("Bean {} (@Inject annotated) scanned and added", bean);
								}
							});
				});

		logger.debug("Scan for @Inject bean complete. Found {}", Utils.getStringRepresentationOf(injectBeans));
		dependencyGraph.addBeans(injectBeans);
	}

	private static void scanAndAddAnnotations(List<ClassInfo> allClasses, Set<DIBean> initBeans,
			Set<DIBean> autoInvokeBeans, Set<Class<? extends Annotation>> configAnnotations) {

		// if (logger.isDebugEnabled()) {
		// 	List<String> classAnnotations = config.getClassAnnotationConfig()
		// 			.stream()
		// 			.map(config -> config.getAnnotation()
		// 					.getName())
		// 			.collect(Collectors.toList());
		// 	List<String> methodAnnotations = config.getMethodAnnotationConfig()
		// 			.stream()
		// 			.map(config -> config.getAnnotation()
		// 					.getName())
		// 			.collect(Collectors.toList());
		// 	logger.debug("Scanning for {} classAnnotations and {} methodAnnotations",
		// 			Arrays.toString(classAnnotations.toArray()), Arrays.toString(methodAnnotations.toArray()));
		// }

		Map<String, Tuple<Class<? extends Annotation>, ScanAnnotation>> annotationConfiguration = configAnnotations
				.parallelStream()
				.map(cls -> {
					ScanAnnotation config = cls.getAnnotation(ScanAnnotation.class);
					if (config == null)
						return null;
					return Tuple.of(cls, config);
				})
				.filter(tuple -> tuple != null)
				.collect(Collectors.toMap(t -> t.first.getName(), t -> Tuple.of(t.first, t.second)));

		Set<DIBean> beans = Collections.synchronizedSet(new HashSet<>());

		BiConsumer<ScanAnnotation, DIBean> checkedAddAutoBuildBeans = (entry, bean) -> {
			if (entry.autoBuild()) {
				safeAdd(initBeans, Collections.singleton(bean));
			}
		};

		BiConsumer<ScanAnnotation, DIBean> checkedAddAutoInvokeBeans = (entry, bean) -> {
			if (entry.autoInvoke()) {
				safeAdd(autoInvokeBeans, Collections.singleton(bean));
			}
		};

		allClasses.parallelStream()
				.forEach(cls -> {
					logger.trace("Scanning {}", cls.getSimpleName());

					List<AnnotationInfo> classAnnotationInfos = Utils.getAnnotations(cls::getAnnotationInfo,
							configAnnotations);

					// cls.getAnnotationInfo();
					if (classAnnotationInfos != null && classAnnotationInfos.size() > 0) {
						Class<?> loadedClass = cls.loadClass();

						classAnnotationInfos.forEach(annotInfo -> {
							Tuple<Class<? extends Annotation>, ScanAnnotation> config = annotationConfiguration
									.get(annotInfo.getClassInfo()
											.getName());

							DIBean bean = new DIBean(loadedClass, config.first, config.second.checkDependency());
							checkedAddAutoBuildBeans.accept(config.second, bean);
							safeAdd(beans, Collections.singleton(bean));
							addToMap(annotationsMap, config.first, Either.left(loadedClass));
						});
					}

					cls.getMethodInfo()
							.parallelStream()
							.forEach(method -> {
								logger.trace("Scanning method: {}.{}", method.getClassInfo(), method.getName());

								// ClassRefTypeSignature clsRef = (ClassRefTypeSignature) method.getParameterInfo()[0]
								// 		.getTypeSignature();
								// logger.debug("method {}: {}", method.getName(), clsRef.getTypeArguments().get(0));
								List<AnnotationInfo> methodAnnotationInfos = Utils
										.getAnnotations(method::getAnnotationInfo, configAnnotations);

								if (methodAnnotationInfos != null && methodAnnotationInfos.size() > 0) {
									Class<?> c = cls.loadClass();
									Method m = method.loadClassAndGetMethod();
									m.setAccessible(true);

									methodAnnotationInfos.forEach(annotInfo -> {
										Tuple<Class<? extends Annotation>, ScanAnnotation> config = annotationConfiguration
												.get(annotInfo.getClassInfo()
														.getName());

										// If autobuild is enabled, only then check dependency of parent
										DIBean bean = new DIBean(m, config.first, config.second.checkDependency());
										DIBean parent = new DIBean(c,
												Utils.getQualifierAnnotation(c.getAnnotations(), QUALIFIER_ANNOATTIONS),
												config.second.autoBuild());
										checkedAddAutoBuildBeans.accept(config.second, parent);
										checkedAddAutoInvokeBeans.accept(config.second, bean);
										safeAdd(beans, Collections.singleton(parent));
										safeAdd(beans, Collections.singleton(bean));
										addToMap(annotationsMap, config.first, Either.right(m));

										// Need to add dependencies to initBeans. Else dependencies won't be instanciated
										if (config.second.autoInvoke() && config.second.checkDependency()) {
											Utils.getParameterBeansWithoutProvider(m.getParameterTypes(),
													m.getParameterAnnotations(), m.getGenericParameterTypes(),
													QUALIFIER_ANNOATTIONS)
													.forEach(b -> safeAdd(initBeans, Collections.singleton(b)));
										}
									});
								}
							});
				});

		logger.debug("Scan for annotations complete. Found {}", Utils.getStringRepresentationOf(beans));
		dependencyGraph.addBeans(beans);
	}

	private static <T> void safeAdd(Collection<T> collection, Collection<T> items) {
		synchronized (collection) {
			collection.addAll(items);
		}
	}

	private static <T> void addToMap(Map<Class<? extends Annotation>, List<Either<Class<?>, Method>>> map,
			Class<? extends Annotation> annotation, Either<Class<?>, Method> item) {

		map.compute(annotation, (k, v) -> {
			List<Either<Class<?>, Method>> list;
			if (v == null) {
				list = Collections.synchronizedList(new ArrayList<>());
			} else {
				list = v;
			}

			synchronized (list) {
				list.add(item);
			}
			return list;
		});
	}

	public static <T> T get(Class<?> cls) throws ClassNotUnderDIException {
		return get(cls, null);
	}

	public static <T> T get(Class<?> cls, Class<? extends Annotation> qualifier) throws ClassNotUnderDIException {
		return get(new DIBean(cls, qualifier));
	}

	public static <T> Provider<T> getProvider(Class<?> cls) throws InterruptedException, ClassNotUnderDIException {
		return getProvider(cls, null);
	}

	public static <T> Provider<T> getProvider(Class<?> cls, Class<? extends Annotation> qualifier)
			throws InterruptedException, ClassNotUnderDIException {
		try {
			return objectMap.getProvider(new DIBean(cls, qualifier));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			logger.error("Error: ", e);
			return null;
		}
	}

	public static Class<? extends Annotation> getQualifierAnnotation(Class<?> cls) {
		return Utils.getQualifierAnnotation(cls.getAnnotations(), QUALIFIER_ANNOATTIONS);
	}

	private static <T> T get(DIBean bean) throws ClassNotUnderDIException {
		try {
			return objectMap.get(bean);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | InterruptedException e) {
			logger.error("Error", e);
			return null;
		}
	}

	public static List<Class<?>> getAllClassesUnderDI() {
		return Collections.unmodifiableList(dependencyGraph.getDependencyGraph()
				.nodes()
				.stream()
				.map(bean -> bean.getType())
				.collect(Collectors.toList()));
	}

	public static Map<Class<?>, Object> getObjectMap() {
		Map<DIBean, Object> map = objectMap.getObjectMap();
		Map<Class<?>, Object> retMap = new HashMap<>();

		map.forEach((b, o) -> retMap.put(b.getType(), o));

		return Collections.unmodifiableMap(retMap);
	}

}