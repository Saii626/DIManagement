package app.saikat.DIManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.Traverser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.saikat.DIManagement.Exceptions.ClassNotUnderDIException;
import app.saikat.DIManagement.Exceptions.InsufficientDependency;

class ObjectMap {

    private final Map<DIBean, Object> objectMap;
    private final List<Class<? extends Annotation>> QUALIFIERS;
    private final ImmutableGraph<DIBean> dependencyGraph;
    private Logger logger;

    public ObjectMap(List<Class<? extends Annotation>> qualifiers, ImmutableGraph<DIBean> graph) {
        objectMap = new HashMap<>();
        QUALIFIERS = qualifiers;
        dependencyGraph = graph;
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> T get(DIBean bean)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (!objectMap.containsKey(bean)) {
            Set<DIBean> allBeans = dependencyGraph.nodes();

            // Order in which instances will be created
            List<DIBean> dependencyList = getBuildListFor(bean).stream()
                    .map(b -> Utils.getProviderBean(allBeans, b)).collect(Collectors.toList());

            logger.debug("dependencyList for {} is: {}", bean, Utils.getStringRepresentationOf(dependencyList));

            for (DIBean d : dependencyList) {
                // dependency bean should already be present or, if is class, no args constructor or no args @inject constructor
                // must be present, or can be a no arg method
                if (!objectMap.containsKey(d)) {
                    Object dep = buildDependency(d);

                    logger.debug("Built {}. Associated object: {}", d, dep);
                    objectMap.put(d, dep);
                }
            }
        }

        return (T) objectMap.get(bean);
    }

    private Object buildDependency(DIBean bean)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (bean.isClass()) {
            logger.debug("Building class: {}", bean.getType().getSimpleName());

            Constructor<?> toUse = Utils.getAppropriateConstructor(bean.getType().getDeclaredConstructors());
            toUse.setAccessible(true);

            List<DIBean> dependencies = Utils.getParameterBeans(toUse.getParameterTypes(),
                    toUse.getParameterAnnotations(), QUALIFIERS);
            List<Object> params = new ArrayList<>();

            for (DIBean parameter : dependencies) {
                Object obj = objectMap.get(parameter);
                if (obj == null) {
                    params.add(null);
                    throw new InsufficientDependency(bean.getType(), parameter.getType());
                }
                params.add(obj);
            }

            Object o = toUse.newInstance(params.toArray());
            return o;
        } else {

            logger.debug("Building method: {}", bean.getMethod().getName());
            Method method = bean.getMethod();

            Class<?> declaringClass = method.getDeclaringClass();
            DIBean enclosingClassBean = new DIBean(declaringClass,
                    Utils.getQualifierAnnotation(declaringClass.getAnnotations(), QUALIFIERS));

            Object parent = null;
            if (!Modifier.isStatic(method.getModifiers())) {
                // If not present in map then the bean is not a leaf.
                if (!objectMap.containsKey(enclosingClassBean)) {
                    throw new InsufficientDependency(method.getReturnType(), enclosingClassBean.getType());
                }
                parent = objectMap.get(enclosingClassBean);
            }

            List<DIBean> dependencies = Utils.getParameterBeans(method.getParameterTypes(),
                    method.getParameterAnnotations(), QUALIFIERS);
            List<Object> params = new ArrayList<>();

            for (DIBean parameter : dependencies) {
                Object obj = objectMap.get(parameter);
                if (obj == null) {
                    params.add(null);
                    throw new InsufficientDependency(bean.getType(), parameter.getType());
                }
                params.add(obj);
            }

            return method.invoke(parent, params.toArray());
        }
    }

    private List<DIBean> getBuildListFor(DIBean bean) {

        if (!dependencyGraph.nodes().contains(bean)) {
            logger.error("Bean {} not present. All beans are: {}", bean,
                    Arrays.toString(dependencyGraph.nodes().toArray()));
            throw new ClassNotUnderDIException(bean.getType());
        }

        Traverser<DIBean> traverser = Traverser.forGraph(dependencyGraph);
        Iterable<DIBean> dfsIterator = traverser.depthFirstPostOrder(bean);

        List<DIBean> list = Lists.newArrayList(dfsIterator);
        return Collections.unmodifiableList(list);
    }

}