package app.saikat.DIManagement.Impl.Helpers;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Impl.DIBeans.UnresolvedDIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.Results;

public class DependencyHelper {

	private static Logger logger = LogManager.getLogger(DependencyHelper.class);

	public static <T> List<DIBean<?>> scanAndSetDependencies(DIBeanImpl<T> target, Results results) {

		Invokable<Object, T> invokable = target.getInvokable();
		List<Parameter> parameters = invokable.getParameters();

		List<DIBean<?>> dependencies = new ArrayList<>(parameters.size() + 1);

		if (!target.isMethod() || invokable.isStatic()) {
			dependencies.add(null);
		} else {
			UnresolvedDIBeanImpl<?> parentBean = getUnresolvedBean(TypeToken.of(invokable.getDeclaringClass()),
					Sets.newHashSet(invokable.getDeclaringClass().getAnnotations()), results.getQualifierAnnotations());

			dependencies.add(parentBean);
		}

		parameters.forEach(param -> {
			UnresolvedDIBeanImpl<?> unresolvedBean = getUnresolvedBean(param.getType(),
					Sets.newHashSet(param.getAnnotations()), results.getQualifierAnnotations());

			dependencies.add(unresolvedBean);
		});

		target.getDependencies().clear();
		target.getDependencies().addAll(dependencies);
		return dependencies;
	}

	public static List<DIBean<?>> resolveAndSetDependencies(DIBeanImpl<?> target, Collection<DIBean<?>> alreadyScanned,
			Collection<DIBean<?>> toBeScanned) {

		List<DIBean<?>> dependencies = target.getDependencies().stream().map(toResolve -> {
			// Static methods will have 1st parameter as null
			if (toResolve == null)
				return null;
				
			logger.debug("Trying to resolve dependency {}", toResolve);

			Set<DIBean<?>> resolvedDep = alreadyScanned.parallelStream()
					.filter(b -> toResolve.getProviderType().isSupertypeOf(b.getProviderType())
							&& b.getQualifier().equals(toResolve.getQualifier()))
					.collect(Collectors.toSet());

			if (resolvedDep.size() > 1) {
				throw new RuntimeException(
						String.format("No unique bean resolved for %s. All possible resolutions are: %s",
								toResolve.toString(), resolvedDep.toString()));
			} else if (resolvedDep.size() == 1) {
				DIBean<?> uniqueDep = resolvedDep.iterator().next();
				logger.debug("Dependency {} resolved to {}", toResolve, uniqueDep);
				return uniqueDep;
			} else {
				logger.debug("Not already scanned");
				resolvedDep = toBeScanned.parallelStream()
						.filter(b -> toResolve.getProviderType().isSupertypeOf(b.getProviderType())
								&& b.getQualifier().equals(toResolve.getQualifier()))
						.collect(Collectors.toSet());

				if (resolvedDep.size() > 1) {
					throw new RuntimeException(
							String.format("No unique bean resolved for %s. All possible resolutions are: %s",
									toResolve.toString(), resolvedDep.toString()));
				} else if (resolvedDep.size() == 1) {
					DIBean<?> uniqueDep = resolvedDep.iterator().next();
					logger.debug("Dependency {} resolved to {}", toResolve, uniqueDep);
					return uniqueDep;
				} else {
					logger.debug("Not in currentbatch too. Hence no provider was found");
					throw new RuntimeException(String.format("Unable to resolve dependency %s", toResolve.toString()));
				}
			}

		}).collect(Collectors.toList());

		target.getDependencies().clear();
		target.getDependencies().addAll(dependencies);
		return dependencies;

	}

	private static UnresolvedDIBeanImpl<?> getUnresolvedBean(TypeToken<?> type, Set<Annotation> annotations,
			Set<Class<? extends Annotation>> qualifierAnnotations) {

		Set<Class<? extends Annotation>> annotationClasses = annotations.parallelStream().map(a -> a.annotationType())
				.collect(Collectors.toSet());

		Class<? extends Annotation> q = getAnnotation(annotationClasses, qualifierAnnotations);

		// All that matters is the qualifier and what type of object this bean will create
		return new UnresolvedDIBeanImpl<>(type, q != null ? q : NoQualifier.class);
	}

	private static Class<? extends Annotation> getAnnotation(Collection<Class<? extends Annotation>> annotations,
			Collection<Class<? extends Annotation>> annotationsToSearch) {

		return annotations.parallelStream().filter(a -> annotationsToSearch.contains(a)).findAny().orElse(null);

	}
}