package app.saikat.DIManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Qualifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String POST_CONSTRUCT = PostConstruct.class.getName();
    private static List<String> QUALIFIERS_NAME;
    private static List<Class<? extends Annotation>> QUALIFIERS;

    public static void initialize(String packageToScan) {

        logger.info("Initializing DIManager. Starting scans");
        try (ScanResult results = new ClassGraph().ignoreClassVisibility().enableClassInfo().enableMethodInfo()
                .ignoreMethodVisibility().enableAnnotationInfo().whitelistPackages(packageToScan).scan()) {

            // Gather all @Qualifiers annotations
            QUALIFIERS = scanForQualifiers(results);
            QUALIFIERS_NAME = QUALIFIERS.stream().map(qualifier -> qualifier.getName()).collect(Collectors.toList());
            logger.info("All declared qualifiers are: {}", Arrays.toString(QUALIFIERS_NAME.toArray()));

            dependencyGraph = new DependencyGraph(QUALIFIERS);

            List<ClassInfo> allClasses = results.getAllClasses();

            Set<DIBean> allBeans = Collections.synchronizedSet(new HashSet<>());
            Set<DIBean> initBeans = Collections.synchronizedSet(new HashSet<>());

            logger.debug("Scanning objects({}) in package", allClasses.size());

            scanAndAddProvidesAnnotation(allClasses, allBeans);

            scanAndAddInjectAnnotation(allClasses, allBeans, initBeans);

            Map<DIBean, Method> postConstructBeans =  scanAndAddPostConstructAnnotation(allClasses, allBeans, initBeans);

            if (logger.isDebugEnabled()) {
                logger.debug("Found beans: {}", Utils.getStringRepresentationOf(allBeans));
            }

            // Complete dependency graph generate
            dependencyGraph.addAll(allBeans);

            logger.info("Dependency graph generation complete");
            logger.info("All scanned beans: {}", Utils.getStringRepresentationOf(dependencyGraph.getAllBeans()));

            objectMap = new ObjectMap(QUALIFIERS, dependencyGraph.getDependencyGraph());
            buildObjects(initBeans);
            invokePostConstruct(postConstructBeans);
        } 
    }

    private static void buildObjects(Set<DIBean> beans) {
        for (DIBean bean : beans) {
            try {
                logger.debug("Building bean: {}", bean);
                objectMap.get(bean);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                logger.error("Error: {}", e);
            }
        }
    }

    private static void invokePostConstruct(Map<DIBean, Method> postConstructMap) {
        logger.info("Invoking @PostConstruct methods");
        for (Map.Entry<DIBean, Method> entry : postConstructMap.entrySet()) {
            try {
                logger.debug("Invoking {} in class {}", entry.getValue().getName(), entry.getKey().provides().getSimpleName());
                entry.getValue().setAccessible(true);
                Object parent = objectMap.get(entry.getKey());
                entry.getValue().invoke(parent);

            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                logger.error("Error: {}", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Class<? extends Annotation>> scanForQualifiers(ScanResult results) {
        logger.debug("Scanning for @Qualifier annotations");
        List<ClassInfo> allAnnotations = results.getAllAnnotations();
        return Collections.unmodifiableList(allAnnotations.stream().filter(clsInfo -> clsInfo.hasAnnotation(QUALIFIER))
                .map(cls -> (Class<? extends Annotation>) cls.loadClass()).collect(Collectors.toList()));

    }

    private static void scanAndAddProvidesAnnotation(List<ClassInfo> allClasses, Set<DIBean> allBeans) {
        logger.debug("Scanning for @Provides annotation");

        allClasses.parallelStream().forEach(cls -> {
            logger.trace("Scanning {}", cls.getSimpleName());

            // Check if class is annotated with @Provides and add to beansList
            AnnotationInfo info = cls.getAnnotationInfo(PROVIDES);
            if (info != null) {
                Class<?> loadedClass = cls.loadClass();
                DIBean bean = new DIBean(loadedClass,
                        Utils.getQualifierAnnotation(loadedClass.getAnnotations(), QUALIFIERS), true);
                logger.debug("Bean {} (@Provides annotated class) scanned and added", bean);

                synchronized (allBeans) {
                    allBeans.add(bean);
                }
            }

            cls.getMethodInfo().parallelStream().forEach(method -> {
                logger.trace("Scanning method: {}.{}", method.getClassInfo(), method.getName());

                // Check if method is annotated with @Provides
                AnnotationInfo providesAnnotation = method.getAnnotationInfo(PROVIDES);
                if (providesAnnotation != null) {
                    Method m = method.loadClassAndGetMethod();
                    DIBean bean = new DIBean(m, Utils.getQualifierAnnotation(m.getAnnotations(), QUALIFIERS), true);
                    logger.debug("Bean {} (@Provides annotated function) scanned and added", bean);

                    synchronized (allBeans) {
                        allBeans.add(bean);
                    }
                }
            });
        });
    }

    private static void scanAndAddInjectAnnotation(List<ClassInfo> allClasses, Set<DIBean> allBeans,
            Set<DIBean> initBeans) {

        logger.debug("Scanning for @Inject annotation");

        allClasses.parallelStream().forEach(cls -> {
            logger.trace("Scanning {}", cls.getSimpleName());

            cls.getConstructorInfo().parallelStream().forEach(constructor -> {
                logger.trace("Scanning constructor: {}", constructor.getName());

                // Check if constructor is annotated with @Inject
                AnnotationInfo injectAnnotation = constructor.getAnnotationInfo(INJECT);
                if (injectAnnotation != null) {
                    Class<?> c = cls.loadClass();
                    
                    DIBean bean = new DIBean(c, Utils.getQualifierAnnotation(c.getAnnotations(), QUALIFIERS));
                    logger.debug("Bean {} (@Inject annotated) scanned and added", bean);

                    synchronized (allBeans) {
                        allBeans.add(bean);
                    }

                    synchronized (initBeans) {
                        initBeans.add(bean);
                    }
                }
            });
        });
    }

    private static Map<DIBean, Method> scanAndAddPostConstructAnnotation(List<ClassInfo> allClasses, Set<DIBean> allBeans,
            Set<DIBean> initBeans) {
        logger.debug("Scanning for @PostConstruct annotation");

        Map<DIBean, Method> map = new HashMap<>();

        allClasses.parallelStream().forEach(cls -> {
            logger.debug("Scanning {}", cls.getSimpleName());

            cls.getMethodInfo().parallelStream().forEach(method -> {
                logger.debug("Scanning method: {}.{}", method.getClassInfo(), method.getName());

                // Check if method is annotated with @PostConstruct
                AnnotationInfo postConstructAnnotation = method.getAnnotationInfo(POST_CONSTRUCT);
                if (postConstructAnnotation != null) {
                    Class<?> c = cls.loadClass();
                    
                    DIBean bean = new DIBean(c, Utils.getQualifierAnnotation(c.getAnnotations(), QUALIFIERS));
                    logger.debug("Bean {} (@PostConstruct annotated) scanned and added", bean);

                    synchronized (allBeans) {
                        allBeans.add(bean);
                    }

                    synchronized (initBeans) {
                        initBeans.add(bean);
                    }

                    map.put(bean, method.loadClassAndGetMethod());
                }
            });
        });

        return Collections.unmodifiableMap(map);
    }

    public static <T> T get(Class<?> cls) {
        return get(cls, null);
    }

    public static <T> T get(Class<?> cls, Class<? extends Annotation> qualifier) {
        return get(new DIBean(cls, qualifier));
    }

    public static <T> T get(DIBean bean) {
        try {
            return objectMap.get(bean);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            logger.error("Error: {}", e);
            return null;
        }
    }

}