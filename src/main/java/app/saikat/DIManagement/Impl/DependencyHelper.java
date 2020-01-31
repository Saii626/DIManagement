package app.saikat.DIManagement.Impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Exceptions.NoProviderFoundForClassException;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Impl.DIBeans.UnresolvedDIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.Results;
import app.saikat.PojoCollections.CommonObjects.Either;

public class DependencyHelper {

	private static Logger logger = LogManager.getLogger(DependencyHelper.class);

	public static <T> List<DIBean<?>> scanAndSetDependencies(DIBean<T> target, Results results) {
		List<DIBean<?>> dependencies = new ArrayList<>();

		List<Type> params;
		List<Annotation[]> paramsAnnotations;

		Either<Constructor<T>, Method> t = ((DIBeanImpl<T>) target).get();

		if (t.containsLeft()) {
			Constructor<T> cons = t.getLeft().get();
			logger.trace("Scanning parameters of {} constructor", cons.getName());

			params = Lists.newArrayList(cons.getGenericParameterTypes());
			paramsAnnotations = Lists.newArrayList(cons.getParameterAnnotations());
		} else {
			Method meth = t.getRight().get();
			logger.trace("Scanning parameters of {} method", meth.getName());

			params = Lists.newArrayList(meth.getGenericParameterTypes());
			paramsAnnotations = Lists.newArrayList(meth.getParameterAnnotations());

			if (!Modifier.isStatic(meth.getModifiers())) {
				Set<Annotation> annotationsOnParent = Sets.newHashSet(meth.getDeclaringClass().getAnnotations());
				DIBeanImpl<?> parentClass = getUnresolvedBean(meth.getDeclaringClass(), annotationsOnParent,
						results.getQualifierAnnotations(), Collections.emptyList());

				logger.debug("Method {} is non static. Adding {} as unresolved dependency", meth, parentClass);
				dependencies.add(parentClass);
			} else {
				// Do not remove. Method bean has enclosing class as 1st dependency in dependencyList. It is null
				// for static methods
				dependencies.add(null);
			}
		}

		for (int i = 0; i < params.size(); i++) {
			if (params.get(i) == null)
				continue;

			try {
				Class<?> p;
				List<String> genParams = new ArrayList<>();

				if (params.get(i) instanceof ParameterizedType) {
					ParameterizedType aType = (ParameterizedType) params.get(i);
					Type[] parameterArgTypes = aType.getActualTypeArguments();

					for (Type gen : parameterArgTypes) {
						genParams.add(gen.getTypeName());
					}

					p = Class.forName(aType.getRawType().getTypeName());
				} else {
					p = Class.forName(params.get(i).getTypeName());
				}

				DIBeanImpl<?> dep = getUnresolvedBean(p, Sets.newHashSet(paramsAnnotations.get(i)),
						results.getQualifierAnnotations(), genParams);

				logger.debug("Unresolved dependency {} added", dep);
				dependencies.add(dep);
			} catch (ClassNotFoundException e) {
				logger.error(e);
				throw new RuntimeException("Class not found");
			}
		}

		((DIBeanImpl<?>) target).getDependencies().addAll(dependencies);
		return dependencies;
	}

	public static List<DIBean<?>> resolveAndSetDependencies(DIBean<?> target, Collection<DIBean<?>> alreadyScanned,
			Collection<DIBean<?>> toBeScanned) {

		List<DIBean<?>> dependencies = target.getDependencies().stream().map(toResolve -> {
			logger.debug("Trying to resolve dependency {}", toResolve);

			// Static methods will have 1st parameter as null
			if (toResolve == null)
				return null;

			DIBean<?> resolvedDep = alreadyScanned.parallelStream()
					.filter(b -> toResolve.getProviderType().isAssignableFrom(b.getProviderType())
							&& b.getQualifier().equals(toResolve.getQualifier())
							&& b.getGenericParameters().equals(toResolve.getGenericParameters()))
					.findFirst().orElse(null);

			if (resolvedDep == null) {
				logger.debug("Not already scanned");
				resolvedDep = toBeScanned.parallelStream()
						.filter(b -> toResolve.getProviderType().isAssignableFrom(b.getProviderType())
								&& b.getQualifier().equals(toResolve.getQualifier())
								&& b.getGenericParameters().equals(toResolve.getGenericParameters()))
						.findFirst().orElse(null);
			}

			if (resolvedDep == null) {
				logger.debug("Not in currentbatch too. Hence no provider was found");
				throw new NoProviderFoundForClassException(toResolve.getProviderType());
			} else {
				logger.debug("Dependency {} resolved to {}", toResolve, resolvedDep);
				return resolvedDep;
			}
		}).collect(Collectors.toList());

		DIBeanImpl<?> t = (DIBeanImpl<?>) target;
		t.getDependencies().clear();
		t.getDependencies().addAll(dependencies);
		return dependencies;

	}

	private static DIBeanImpl<?> getUnresolvedBean(Class<?> cls, Set<Annotation> annotations,
			Set<Class<? extends Annotation>> qualifierAnnotations, List<String> genParams) {

		Set<Class<? extends Annotation>> annotationClasses = annotations.parallelStream().map(a -> a.annotationType())
				.collect(Collectors.toSet());

		Class<? extends Annotation> q = getAnnotation(annotationClasses, qualifierAnnotations);

		// All that matters is the qualifier and what type of object this bean will create
		return new UnresolvedDIBeanImpl<>(cls, q != null ? q : NoQualifier.class, genParams);
	}

	private static Class<? extends Annotation> getAnnotation(Collection<Class<? extends Annotation>> annotations,
			Collection<Class<? extends Annotation>> annotationsToSearch) {

		return annotations.parallelStream().filter(a -> annotationsToSearch.contains(a)).findAny().orElse(null);

	}
}