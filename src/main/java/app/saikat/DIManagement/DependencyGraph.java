package app.saikat.DIManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.saikat.DIManagement.Exceptions.CircularDependencyException;
import app.saikat.DIManagement.Exceptions.NoValidConstructorFoundException;

class DependencyGraph {

    private final MutableGraph<DIBean> graph;
    private final List<Class<? extends Annotation>> QUALIFIERS;
    private final Set<DIBean> alreadyScanned;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DependencyGraph(List<Class<? extends Annotation>> qualifiers) {
        graph = GraphBuilder.directed().allowsSelfLoops(false).build();
        alreadyScanned = new HashSet<>();
        QUALIFIERS = qualifiers;
    }

    public synchronized void addAll(Collection<DIBean> beans) {
        Set<DIBean> providedBeans = beans.stream().filter(b -> b.isProvider()).collect(Collectors.toSet());
        Set<DIBean> otherBeans = beans.stream().filter(b -> !providedBeans.contains(b)).collect(Collectors.toSet());

        addBeans(providedBeans);
        addBeans(otherBeans);
    }

    private synchronized void addBeans(Collection<DIBean> beans) {

        Queue<DIBean> scanQueue = new ArrayDeque<>(beans);

        while (!scanQueue.isEmpty()) {
            DIBean target = scanQueue.poll();

            if (alreadyScanned.contains(target)) {
                continue;
            }

            logger.debug("Adding {} to dependencyGraph", target);
            graph.addNode(target);
            List<DIBean> dependencies;

            if (target.isMethod()) {
                Method method = target.getMethod();
                method.setAccessible(true);

                dependencies = Utils.getParameterBeans(method.getParameterTypes(), method.getParameterAnnotations(),
                        QUALIFIERS);

                if (!Modifier.isStatic(method.getModifiers())) {
                    Class<?> parent = method.getDeclaringClass();
                    DIBean dep = new DIBean(parent, Utils.getQualifierAnnotation(parent.getAnnotations(), QUALIFIERS));
                    logger.debug("Not static method. Adding declaring {} as dependency", dep);

                    checkAndAddPair(target, dep);
                    scanQueue.add(dep);
                }
            } else {
                Constructor<?> toUse = Utils.getAppropriateConstructor(target.getType().getDeclaredConstructors());
                if (toUse == null) {
                    throw new NoValidConstructorFoundException(target.getType());
                }
                toUse.setAccessible(true);

                dependencies = Utils.getParameterBeans(toUse.getParameterTypes(), toUse.getParameterAnnotations(),
                        QUALIFIERS);
            }

            logger.debug("Dependencies for {} are: {}", target, Utils.getStringRepresentationOf(dependencies));

            for (DIBean dep : dependencies) {
                checkAndAddPair(target, dep);
                scanQueue.add(dep);
            }

            alreadyScanned.add(target);
        }
    }

    // private synchronized void addClass(Collection<DIBean> beans) {

    //     List<DIBean> toBeScanned = beans.stream().filter(bean -> !alreadyScanned.contains(bean))
    //             .collect(Collectors.toList());

    //     while (!toBeScanned.isEmpty()) {

    //         DIBean target = toBeScanned.pop();
    //         logger.debug("Adding class {} to dependencyGraph", target);

    //         graph.addNode(target);

    //         Constructor<?> toUse = Utils.getAppropriateConstructor(target.getType().getDeclaredConstructors());
    //         if (toUse == null) {
    //             throw new NoValidConstructorFoundException(target.getType());
    //         }

    //         toUse.setAccessible(true);
    //         List<DIBean> dependencies = Utils.getParameterBeans(toUse.getParameterTypes(),
    //                 toUse.getParameterAnnotations(), QUALIFIERS);
    //         logger.debug("Dependencies for {} are: {}", target, Utils.getStringRepresentationOf(dependencies));

    //         for (DIBean dep : dependencies) {

    //             checkAndAddPair(target, dep);

    //             if (!toBeScanned.contains(dep) && !alreadyScanned.contains(dep) && !providedBeans.contains(dep)) {
    //                 toBeScanned.add(dep);
    //             }
    //         }
    //         alreadyScanned.add(target);
    //     }
    // }

    // private synchronized void addMethod(DIBean target) {
    //     if (alreadyScanned.contains(target)) {
    //         return;
    //     }

    //     logger.debug("Adding method {} to dependency graph", target);

    //     graph.addNode(target);
    //     Method method = target.getMethod();

    //     List<DIBean> dependencies = Utils.getParameterBeans(method.getParameterTypes(),
    //             method.getParameterAnnotations(), QUALIFIERS);
    //     logger.debug("Dependencies for {} are: {}", target, Utils.getStringRepresentationOf(dependencies));

    //     for (DIBean dep : dependencies) {
    //         checkAndAddPair(target, dep);
    //         addClass(dep);
    //     }

    //     alreadyScanned.add(target);

    //     if (!Modifier.isStatic(method.getModifiers())) {
    //         Class<?> parent = method.getDeclaringClass();
    //         DIBean dep = new DIBean(parent, Utils.getQualifierAnnotation(parent.getAnnotations(), QUALIFIERS));
    //         logger.debug("Not static method. Adding declaring {} as dependency", dep);

    //         checkAndAddPair(target, dep);
    //         addClass(dep);
    //     }
    // }

    public synchronized ImmutableGraph<DIBean> getDependencyGraph() {
        return ImmutableGraph.copyOf(graph);
    }

    public synchronized Set<DIBean> getAllBeans() {
        return Collections.unmodifiableSet(alreadyScanned);
    }

    private void checkAndAddPair(DIBean target, DIBean dependent) {
        graph.putEdge(target, dependent);

        if (Graphs.hasCycle(graph)) {
            throw new CircularDependencyException(target.getType(), dependent.getType());
        }
    }
}