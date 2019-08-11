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

import com.google.common.collect.Lists;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.Traverser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.saikat.DIManagement.Exceptions.ClassNotUnderDIException;
import app.saikat.DIManagement.Exceptions.InsufficientDependency;
import app.saikat.DIManagement.Exceptions.NoProviderFoundForClassException;

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

            DIBean b = Utils.getProviderBean(dependencyGraph.nodes(), bean);

            // Order in which instances will be created
            List<DIBean> dependencyList = getBuildListFor(b);

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
        if (bean.isMethod()) {
            logger.debug("Building method: {}", bean.getMethod().getName());
            Method method = bean.getMethod();

            Object parent = null;
            if (!Modifier.isStatic(method.getModifiers())) {
                Class<?> declaringClass = method.getDeclaringClass();
                DIBean enclosingClassBean = new DIBean(declaringClass,
                        Utils.getQualifierAnnotation(declaringClass.getAnnotations(), QUALIFIERS));
                // If not present in map then the bean is not a leaf.
                if (!objectMap.containsKey(enclosingClassBean)) {
                    throw new InsufficientDependency(method.getReturnType(), enclosingClassBean.getType());
                }
                parent = objectMap.get(enclosingClassBean);
            }

            List<DIBean> dependencies = Utils.getParameterBeans(method.getParameterTypes(),
                    method.getParameterAnnotations(), QUALIFIERS);

            return method.invoke(parent, getArgumentsForDependencies(dependencies).toArray());

        } else {
            logger.debug("Building class: {}", bean.getType().getSimpleName());

            Constructor<?> toUse = Utils.getAppropriateConstructor(bean.getType().getDeclaredConstructors());
            toUse.setAccessible(true);

            List<DIBean> dependencies = Utils.getParameterBeans(toUse.getParameterTypes(),
                    toUse.getParameterAnnotations(), QUALIFIERS);

            return toUse.newInstance(getArgumentsForDependencies(dependencies).toArray());
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
        logger.debug("All beans {}", Utils.getStringRepresentationOf(dependencyGraph.nodes()));
        logger.debug("dependencyList {}: {}", bean, Utils.getStringRepresentationOf(list));

        // Weird dont know why all dependencies are of class type. Hack to overcome this
        // List<DIBean> dependencies = new ArrayList<>(list.size());
        // for (DIBean dep : list) {
        //     logger.debug("bean: {} and providedBean: {}", dep, Utils.getProviderBean(dependencyGraph.nodes(), dep));
        //     dependencies.add(Utils.getProviderBean(dependencyGraph.nodes(), dep));
        // }
        // list = list.stream().map(l -> Utils.getProviderBean(dependencyGraph.nodes(), l)).collect(Collectors.toList());
        // logger.debug("dependencyList {}: {}", bean, Utils.getStringRepresentationOf(list));
        return Collections.unmodifiableList(list);
    }

    private List<Object> getArgumentsForDependencies(List<DIBean> beans) {

        List<Object> params = new ArrayList<>();

        for (DIBean parameter : beans) {
            Object obj = objectMap.get(parameter);
            if (obj == null) {
                if (parameter.getType().isPrimitive()) {
                    params.add(0);
                } else {
                    params.add(null);
                }
                throw new NoProviderFoundForClassException(parameter.getClass());
            }
            params.add(obj);
        }
        return params;

    }
}