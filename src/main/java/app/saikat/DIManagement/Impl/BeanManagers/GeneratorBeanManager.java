package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import app.saikat.Annotations.DIManagement.Generator;
import app.saikat.Annotations.DIManagement.Scan;
import app.saikat.Annotations.DIManagement.GenParam;
import app.saikat.Annotations.DIManagement.Generate;
import app.saikat.DIManagement.Impl.Helpers.DependencyHelper;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Impl.ExternalImpl.GeneratorImpl;
import app.saikat.DIManagement.Interfaces.DIBean;

public class GeneratorBeanManager extends BeanManagerImpl {

	private Map<ConstantProviderBean<Generator<?>>, DIBeanImpl<?>> toBeGeneratedMap = new ConcurrentHashMap<>();

	@Override
	public Map<Class<?>, Scan> addToScan() {
		return Collections.singletonMap(Generate.class, createScanObject());
	}

	@Override
	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	public <T> void beanCreated(DIBean<T> bean) {
		// Dont add the bean to result. Instead create ConstantProviderBean of Generator<?> and add it instead
		// Set it not qualifier to @Generate to get its callbacks
		if (!(bean instanceof DIBeanImpl<?>)) {
			throw new RuntimeException(
					String.format("Wrorng bean type for Generator bean. Expected type DIBeanImpl.class found %s", bean
							.getClass()
							.getSimpleName()));
		}

		TypeToken<Generator<T>> generatorTypeToken = new TypeToken<Generator<T>>() {}
				.where(new TypeParameter<T>() {}, bean.getProviderType()
						.wrap());

		ConstantProviderBean<Generator<T>> genProvider = new ConstantProviderBean<>(generatorTypeToken,
				bean.getQualifier());
		genProvider.setManager(this);
		toBeGeneratedMap.put((ConstantProviderBean) genProvider, (DIBeanImpl<?>) bean);
		repo.addBean(genProvider);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved, Collection<Class<? extends Annotation>> qualifierAnnotations) {

		if (!(target instanceof ConstantProviderBean<?>)) {
			throw new RuntimeException(String
					.format("Dont know how to resolve dependency of %s, as it is not instance of ConstantProvider.class", target
							.toString()));
		}

		ConstantProviderBean<Generator<?>> genBean = (ConstantProviderBean<Generator<?>>) target;
		DIBeanImpl<?> t = toBeGeneratedMap.get((genBean));
		logger.debug("Scanning dependencies of {}", t);
		DependencyHelper.scanAndSetDependencies(t, qualifierAnnotations);
		List<DIBean<?>> unresolvedDependencies = t.getDependencies();

		List<DIBean<?>> generatorParams = new ArrayList<>();
		List<Parameter> parameters = t.getInvokable()
				.getParameters();

		for (int i = 0; i < parameters.size(); i++) {
			if (parameters.get(i)
					.getAnnotation(GenParam.class) != null) {
						DIBean<?> gP = unresolvedDependencies.set(i +1, null);
				generatorParams.add(gP);
			}
		}

		logger.debug("All dependencies to resolve {}", unresolvedDependencies);
		List<DIBean<?>> resolvedDependencies = DependencyHelper
				.resolveAndSetDependencies(t, alreadyResolved, toBeResolved);

		logger.debug("Creating Generator: {} with generator params: {}", genBean, resolvedDependencies);
		Generator<?> generator = new GeneratorImpl<>(t, generatorParams);

		genBean.setProvider(() -> generator);

		toBeGeneratedMap.remove(genBean);
		return resolvedDependencies;
	}

	@Override
	public boolean shouldCreateProvider() {
		return false;
	}
	// @SuppressWarnings("rawtypes")
	// private void createGeneratorBean(DIBeanImpl<?> bean, List<DIBean<?>> generatorParams) {

	// 	logger.debug("Creating generator: {} with generator params: {}", bean, generatorParams);
	// 	Generator<?> generator = new GeneratorImpl<>(bean, generatorParams);

	// 	ConstantProviderBean<Generator<?>> genBean = toBeGeneratedMap.get(bean);
	// 	genBean.setProvider(new Provider<Generator<?>>() {
	// 		@Override
	// 		public Generator get() {
	// 			return generator;
	// 		}
	// 	});

	// 	toBeGeneratedMap.remove(bean);
	// }

	@Override
	public void dependencyResolved() {
		if (!toBeGeneratedMap.isEmpty()) {
			logger.error("Generator of {} not found", toBeGeneratedMap.keySet());
			throw new RuntimeException("Not all generators found");
		}
	}
}