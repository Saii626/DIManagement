package app.saikat.DIManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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


    public synchronized void addBeans(Collection<DIBean> beans) {

        Queue<DIBean> scanQueue = new ArrayDeque<>(beans);

        while (!scanQueue.isEmpty()) {
            DIBean target = scanQueue.poll();

            if (alreadyScanned.contains(target)) {
                continue;
            }

            logger.debug("Adding {} to dependencyGraph", target);
            graph.addNode(target);
            List<DIBean> dependencies;
            Collection<DIBean> allDeclaredBeans = Lists.newArrayList(Iterables.unmodifiableIterable(Iterables.concat(alreadyScanned, scanQueue)));

            if (target.isMethod()) {
                Method method = target.getMethod();
                method.setAccessible(true);

                List<DIBean> classDep = Utils.getParameterBeans(method.getParameterTypes(), method.getParameterAnnotations(),
                        QUALIFIERS);

                dependencies = classDep.stream().map(d -> Utils.getProviderBean(allDeclaredBeans, d)).collect(Collectors.toList());

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

                List<DIBean> classDep = Utils.getParameterBeans(toUse.getParameterTypes(), toUse.getParameterAnnotations(),
                        QUALIFIERS);

                dependencies = classDep.stream().map(d -> Utils.getProviderBean(allDeclaredBeans, d)).collect(Collectors.toList());
            }

            logger.debug("Dependencies for {} are: {}", target, Utils.getStringRepresentationOf(dependencies));

            for (DIBean dep : dependencies) {
                checkAndAddPair(target, dep);

                if (!allDeclaredBeans.contains(dep)) {
                    scanQueue.add(dep);
                }
            }

            alreadyScanned.add(target);
        }
    }

    public synchronized ImmutableGraph<DIBean> getDependencyGraph() {
        return ImmutableGraph.copyOf(graph);
    }

    private void checkAndAddPair(DIBean target, DIBean dependent) {
        graph.putEdge(target, dependent);

        if (Graphs.hasCycle(graph)) {
            throw new CircularDependencyException(target.getType(), dependent.getType());
        }
    }
}