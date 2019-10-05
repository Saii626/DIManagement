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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Qualifier;

import com.google.common.graph.ImmutableGraph;

import app.saikat.LogManagement.Logger;
import app.saikat.LogManagement.LoggerFactory;
import app.saikat.PojoCollections.CommonObjects.Tuple;
import app.saikat.DIManagement.Configurations.AnnotationConfig;
import app.saikat.DIManagement.Configurations.ClassAnnotationConfig;
import app.saikat.DIManagement.Configurations.MethodAnnotationConfig;
import app.saikat.DIManagement.Configurations.ScanConfig;
import app.saikat.DIManagement.Exceptions.ClassNotUnderDIException;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class DIManager {

    private static ObjectMap objectMap;
    private static DependencyGraph dependencyGraph;
    private static Logger logger = LoggerFactory.getLogger(DIManager.class);

    private static final String PROVIDES = Provides.class.getName();
    private static final String QUALIFIER = Qualifier.class.getName();
    private static final String INJECT = Inject.class.getName();
    private static List<String> QUALIFIERS_NAME;
    private static List<Class<? extends Annotation>> QUALIFIERS;
    private static ScanConfig config;
    private static Map<Class<? extends Annotation>, List<Class<?>>> annotatedClasses;
    private static Map<Class<? extends Annotation>, List<Method>> annotatedMethods;

    public static void initialize(ScanConfig scanConfig) {

        logger.info("Initializing DIManager. Starting scans");
        config = scanConfig;
        try (ScanResult results = new ClassGraph().enableClassInfo()
                .enableMethodInfo()
                .ignoreMethodVisibility()
                .ignoreClassVisibility()
                .enableAnnotationInfo()
                .whitelistPackages(config.getPackagesToScan()
                        .toArray(new String[] {}))
                .scan()) {

            // Gather all @Qualifiers annotations
            QUALIFIERS = scanForQualifiers(results);
            QUALIFIERS_NAME = QUALIFIERS.stream()
                    .map(qualifier -> qualifier.getName())
                    .collect(Collectors.toList());
            logger.info("All declared qualifiers are: {}", Arrays.toString(QUALIFIERS_NAME.toArray()));

            dependencyGraph = new DependencyGraph(QUALIFIERS);

            List<ClassInfo> allClasses = results.getAllClasses();

            Set<DIBean> initBeans = Collections.synchronizedSet(new HashSet<>());
            Set<DIBean> autoInvokeBeans = Collections.synchronizedSet(new HashSet<>());

            logger.info("Scanning {} classes in package(s) {}", allClasses.size(), config.getPackagesToScan());

            scanAndAddProvidesAnnotation(allClasses);

            scanAndAddInjectAnnotation(allClasses, initBeans);

            config.addConfig(MethodAnnotationConfig.getBuilder()
                    .forAnnotation(PostConstruct.class)
                    .autoBuild(true)
                    .checkDependency(false)
                    .autoInvoke(true)
                    .build());

            annotatedClasses = new ConcurrentHashMap<>();
            annotatedMethods = new ConcurrentHashMap<>();

            scanAndAddAnnotations(allClasses, initBeans, autoInvokeBeans);

            ImmutableGraph<DIBean> depGraph = dependencyGraph.getDependencyGraph();
            Set<DIBean> allBeans = depGraph.nodes();

            logger.info("Dependency graph generation complete");
            if (logger.isDebugEnabled()) {
                logger.debug("All scanned beans: {}", Utils.getStringRepresentationOf(allBeans));
            }

            objectMap = new ObjectMap(QUALIFIERS, depGraph);
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
                    | InvocationTargetException | ClassNotUnderDIException e) {
                logger.error("Error: {}", e);
            }
        }
    }

    private static void invokeMethods(Set<DIBean> autoInvokeBeans) {

        autoInvokeBeans.forEach(bean -> {
            try {
                objectMap.buildDependency(bean);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                logger.error("Error: {}", e);
            }

        });
    }

    @SuppressWarnings("unchecked")
    private static List<Class<? extends Annotation>> scanForQualifiers(ScanResult results) {
        logger.debug("Scanning for @Qualifier annotations");
        List<ClassInfo> allAnnotations = results.getAllAnnotations();
        return Collections.unmodifiableList(allAnnotations.stream()
                .filter(clsInfo -> clsInfo.hasAnnotation(QUALIFIER))
                .map(cls -> (Class<? extends Annotation>) cls.loadClass())
                .collect(Collectors.toList()));

    }

    private static void scanAndAddProvidesAnnotation(List<ClassInfo> allClasses) {
        logger.debug("Scanning for @Provides annotation");

        Set<DIBean> provideBeans = Collections.synchronizedSet(new HashSet<>());

        allClasses.parallelStream()
                .forEach(cls -> {
                    logger.trace("Scanning {}", cls.getSimpleName());

                    // Check if class is annotated with @Provides and add to beansList
                    AnnotationInfo info = cls.getAnnotationInfo(PROVIDES);
                    if (info != null) {
                        Class<?> loadedClass = cls.loadClass();
                        DIBean bean = new DIBean(loadedClass,
                                Utils.getQualifierAnnotation(loadedClass.getAnnotations(), QUALIFIERS), true);

                        safeAdd(provideBeans, bean);
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
                                            Utils.getQualifierAnnotation(m.getAnnotations(), QUALIFIERS), true);

                                    safeAdd(provideBeans, bean);
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
                                            Utils.getQualifierAnnotation(c.getAnnotations(), QUALIFIERS), true);

                                    safeAdd(initBeans, bean);
                                    safeAdd(injectBeans, bean);
                                    logger.debug("Bean {} (@Inject annotated) scanned and added", bean);
                                }
                            });
                });

        logger.debug("Scan for @Inject bean complete. Found {}", Utils.getStringRepresentationOf(injectBeans));
        dependencyGraph.addBeans(injectBeans);
    }

    private static void scanAndAddAnnotations(List<ClassInfo> allClasses, Set<DIBean> initBeans,
            Set<DIBean> autoInvokeBeans) {

        if (logger.isDebugEnabled()) {
            List<String> classAnnotations = config.getClassAnnotationConfig()
                    .stream()
                    .map(config -> config.getAnnotation()
                            .getName())
                    .collect(Collectors.toList());
            List<String> methodAnnotations = config.getMethodAnnotationConfig()
                    .stream()
                    .map(config -> config.getAnnotation()
                            .getName())
                    .collect(Collectors.toList());
            logger.debug("Scanning for {} classAnnotations and {} methodAnnotations",
                    Arrays.toString(classAnnotations.toArray()), Arrays.toString(methodAnnotations.toArray()));
        }

        Set<DIBean> beans = Collections.synchronizedSet(new HashSet<>());

        BiConsumer<AnnotationConfig, DIBean> checkedAddInitBeans = (entry, bean) -> {
            if (entry.isAutoBuild()) {
                safeAdd(initBeans, bean);
            }
        };

        BiConsumer<MethodAnnotationConfig, DIBean> checkedAddAutoinvokeBeans = (entry, bean) -> {
            if (entry.autoInvoke()) {
                safeAdd(autoInvokeBeans, bean);
            }
        };

        allClasses.parallelStream()
                .forEach(cls -> {
                    logger.trace("Scanning {}", cls.getSimpleName());

                    List<Tuple<ClassAnnotationConfig, AnnotationInfo>> classAnnotationInfos = Utils
                            .getAnnotations(cls::getAnnotationInfo, config.getClassAnnotationConfig());

                    if (classAnnotationInfos != null && classAnnotationInfos.size() > 0) {
                        Class<?> loadedClass = cls.loadClass();

                        classAnnotationInfos.forEach(entry -> {
                            DIBean bean = new DIBean(loadedClass, entry.first.getAnnotation(),
                                    entry.first.checkDependency());
                            checkedAddInitBeans.accept(entry.first, bean);
                            safeAdd(beans, bean);
                            addToMap(annotatedClasses, entry.first.getAnnotation(), loadedClass);
                        });
                    }

                    cls.getMethodInfo()
                            .parallelStream()
                            .forEach(method -> {
                                logger.trace("Scanning method: {}.{}", method.getClassInfo(), method.getName());

                                List<Tuple<MethodAnnotationConfig, AnnotationInfo>> methodAnnotationInfos = Utils
                                        .getAnnotations(method::getAnnotationInfo, config.getMethodAnnotationConfig());

                                if (methodAnnotationInfos != null && methodAnnotationInfos.size() > 0) {
                                    Class<?> c = cls.loadClass();
                                    Method m = method.loadClassAndGetMethod();
                                    m.setAccessible(true);

                                    methodAnnotationInfos.forEach(entry -> {
                                        // If autobuild is enabled, only then check dependency of parent
                                        DIBean bean = new DIBean(m, entry.first.getAnnotation(),
                                                entry.first.checkDependency());
                                        DIBean parent = new DIBean(c,
                                                Utils.getQualifierAnnotation(c.getAnnotations(), QUALIFIERS),
                                                entry.first.isAutoBuild());
                                        checkedAddInitBeans.accept(entry.first, parent);
                                        checkedAddAutoinvokeBeans.accept(entry.first, bean);
                                        safeAdd(beans, parent);
                                        safeAdd(beans, bean);
                                        addToMap(annotatedMethods, entry.first.getAnnotation(), m);

                                        // Need to add dependencies to initBeans. Else dependencies won't be instanciated
                                        if (entry.first.autoInvoke() && entry.first.checkDependency()) {
                                            Utils.getParameterBeans(m.getParameterTypes(), m.getParameterAnnotations(),
                                                    QUALIFIERS)
                                                    .forEach(b -> safeAdd(initBeans, b));
                                        }
                                    });
                                }
                            });
                });

        logger.debug("Scan for annotations complete. Found {}", Utils.getStringRepresentationOf(beans));
        dependencyGraph.addBeans(beans);
    }

    private static <T> void safeAdd(Collection<T> collection, T item) {
        synchronized (collection) {
            collection.add(item);
        }
    }

    private static <T> void addToMap(Map<Class<? extends Annotation>, List<T>> map,
            Class<? extends Annotation> annotation, T item) {

        map.compute(annotation, (k, v) -> {
            List<T> list;
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

    public static <T> T get(DIBean bean) throws ClassNotUnderDIException {
        try {
            return objectMap.get(bean);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            logger.error("Error", e);
            return null;
        }
    }

    public static List<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation) {
        return annotatedClasses.get(annotation);
    }

    public static List<Method> getAnnotatedMethods(Class<? extends Annotation> annotation) {
        return annotatedMethods.get(annotation);
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