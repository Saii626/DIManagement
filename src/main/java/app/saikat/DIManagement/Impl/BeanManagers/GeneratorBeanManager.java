package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Provider;

import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import app.saikat.Annotations.DIManagement.Generator;
import app.saikat.Annotations.DIManagement.GenParam;
import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Impl.Helpers.DependencyHelper;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Impl.ExternalImpl.GeneratorImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.Results;

public class GeneratorBeanManager extends BeanManagerImpl {

	// private Map<>
	// @SuppressWarnings("rawtypes")
	private Map<DIBean<?>, ConstantProviderBean<Generator<?>>> toBeGeneratedMap = new ConcurrentHashMap<>();

	public GeneratorBeanManager(Results results, Map<DIBean<?>, Set<WeakReference<?>>> objectMap,
			DIBeanManagerHelper helper) {
		super(results, objectMap, helper);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	public <T> void beanCreated(DIBean<T> bean) {
		super.beanCreated(bean);

		if (!(bean instanceof DIBeanImpl<?>)) {
			throw new RuntimeException(
					String.format("Wrorng bean type for Inject bean. Expected type DIBeanImpl.class found %s",
							bean.getClass().getSimpleName()));
		}

		TypeToken<Generator<T>> generatorTypeToken = new TypeToken<Generator<T>>() {
		}.where(new TypeParameter<T>() {
		}, bean.getProviderType().wrap());

		ConstantProviderBean<Generator<T>> genProvider = new ConstantProviderBean<>(generatorTypeToken,
				bean.getQualifier());
		toBeGeneratedMap.put(bean, (ConstantProviderBean) genProvider);
		results.addGeneratedBean(genProvider);
	}

	@Override
	public <T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved) {

		if (!(target instanceof DIBeanImpl<?>)) {
			throw new RuntimeException(String.format(
					"Dont know how to resolve dependency of %s, as it is not instance of DIBeanImpl.class",
					target.toString()));
		}

		DIBeanImpl<T> t = (DIBeanImpl<T>) target;
		logger.debug("Scanning dependencies of {}", t);
		DependencyHelper.scanAndSetDependencies(t, results);
		List<DIBean<?>> unresolvedDependencies = target.getDependencies();

		List<DIBean<?>> generatorParams = new ArrayList<>();
		List<Parameter> parameters = t.getInvokable().getParameters();

		for (int i = 0; i < parameters.size(); i++) {
			if (parameters.get(i).getAnnotation(GenParam.class) != null)
				generatorParams.add((unresolvedDependencies.set(i + 1, null)));
		}

		logger.debug("All dependencies to resolve {}", unresolvedDependencies);
		List<DIBean<?>> resolvedDependencies = DependencyHelper.resolveAndSetDependencies(t, alreadyResolved,
				toBeResolved);

		// resolvedDependencies.stream().filter(dep -> dep != null).forEach(dep -> checkAndAddPair(t, dep));
		// mutableGraph.addNode(t);

		createGeneratorBean(t, generatorParams);
		return resolvedDependencies;
	}

	@Override
	public boolean shouldCreateProvider() {
		return false;
	}

	@SuppressWarnings("rawtypes")
	private void createGeneratorBean(DIBeanImpl<?> bean, List<DIBean<?>> generatorParams) {

		logger.debug("Creating generator: {} with generator params: {}", bean, generatorParams);
		Generator<?> generator = new GeneratorImpl<>(bean, generatorParams, helper);

		ConstantProviderBean<Generator<?>> genBean = toBeGeneratedMap.get(bean);
		genBean.setProvider(new Provider<Generator<?>>() {
			@Override
			public Generator get() {
				return generator;
			}
		});

		toBeGeneratedMap.remove(bean);
	}

	@Override
	public void dependencyResolved() {
		if (!toBeGeneratedMap.isEmpty()) {
			logger.error("Generator of {} not found", toBeGeneratedMap);
			throw new RuntimeException("Not all generators found");
		}
	}

	// public DIBean<?> getGeneratorBeanOf(DIBean<?> cls) {
	// 	if (generatorMap.containsKey(cls)) {
	// 		return generatorMap.get(cls);
	// 	} else if (toBeGeneratedMap.containsKey(cls)) {
	// 		return toBeGeneratedMap.get(cls);
	// 	} else {
	// 		ConstantProviderBean<?> constantProviderBean = new ConstantProviderBean<>(null, cls.getProviderType(),
	// 				cls.getQualifier());
	// 		toBeGeneratedMap.put(cls, constantProviderBean);
	// 		return constantProviderBean;
	// 	}
	// }

}