package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Provider;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.graph.MutableGraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.Generator;
import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.Annotations.DIManagement.GenParam;
import app.saikat.DIManagement.Exceptions.WrongGeneratorParamsProvided;
import app.saikat.DIManagement.Impl.DIBeanManagerHelper;
import app.saikat.DIManagement.Impl.DependencyHelper;
import app.saikat.DIManagement.Impl.ProviderImpl;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIBeanType;
import app.saikat.DIManagement.Interfaces.Results;
import app.saikat.PojoCollections.Utils.CommonFunc;

public class GeneratorBeanManager extends DIBeanManager {

	// private Map<>
	@SuppressWarnings("rawtypes")
	private Map<DIBean<?>, ConstantProviderBean<Generator>> toBeGeneratedMap = new ConcurrentHashMap<>();

	public GeneratorBeanManager(Results results, MutableGraph<DIBean<?>> mutableGraph,
			Map<DIBean<?>, Set<WeakReference<?>>> objectMap, DIBeanManagerHelper helper) {
		super(results, mutableGraph, objectMap, helper);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void beanCreated(DIBean<?> bean, Class<?> type) {
		super.beanCreated(bean, type);

		ConstantProviderBean<Generator> genProvider = new ConstantProviderBean<>(null, Generator.class,
				bean.getQualifier(), Collections.singletonList(bean.getProviderType().getTypeName()),
				DIBeanType.GENERATED);

		toBeGeneratedMap.put(bean, genProvider);
		results.addGeneratedBean(genProvider);
	}

	@Override
	public List<DIBean<?>> resolveDependencies(DIBean<?> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved) {

		logger.debug("Scanning dependencies of {}", target);
		DependencyHelper.scanAndSetDependencies(target, results);
		List<DIBean<?>> unresolvedDependencies = target.getDependencies();

		Method generatorMethod = target.get().getRight().get();

		List<Annotation[]> paramsAnnotations = Lists.newArrayList(generatorMethod.getParameterAnnotations());
		List<DIBean<?>> generatorParams = new ArrayList<>();

		for (int i = 0; i < paramsAnnotations.size(); i++) {
			Set<Annotation> annotations = Sets.newHashSet(paramsAnnotations.get(i));
			boolean isGeneratorParam = annotations.parallelStream()
					.anyMatch(a -> a.annotationType().equals(GenParam.class));
			if (isGeneratorParam)
				generatorParams.add((unresolvedDependencies.set(i + 1, null)));
		}

		logger.debug("All dependencies to resolve {}", unresolvedDependencies);
		List<DIBean<?>> resolvedDependencies = DependencyHelper.resolveAndSetDependencies(target, alreadyResolved,
				toBeResolved);

		// ((DIBeanImpl<?>) target).setDependencies(resolvedDependencies);

		resolvedDependencies.stream().filter(dep -> dep != null).forEach(dep -> checkAndAddPair(target, dep));
		mutableGraph.addNode(target);

		createGeneratorBean(target, generatorParams);
		return resolvedDependencies;
	}

	@Override
	public boolean shouldCreateProvider() {
		return false;
	}

	@SuppressWarnings("rawtypes")
	private void createGeneratorBean(DIBean<?> bean, List<DIBean<?>> generatorParams) {

		logger.debug("Creating generator: {} with generator params: {}", bean, generatorParams);
		Generator<?> generator = new Generator() {

			private Logger logger = LogManager.getLogger(this.getClass());
			private DIBeanImpl<?> partialBean = new DIBeanImpl<>(bean);

			private boolean validateInput(Object[] args) {
				if (args.length != generatorParams.size())
					return false;

				for (int i = 0; i < args.length; i++) {
					if (!generatorParams.get(i).getProviderType().isAssignableFrom(args[i].getClass()))
						return false;
				}

				return true;
			}

			@Override
			@SuppressWarnings("unchecked")
			public Object generate(Object... args) {
				if (!validateInput(args)) {
					StringBuilder builder = new StringBuilder("Wrong arguments provided for Generator<");
					builder.append(partialBean.getProviderType().getSimpleName()).append(">.\n Required: ( ");

					generatorParams
							.forEach(param -> builder.append(param.getProviderType().getSimpleName()).append(", "));

					builder.append(" ). Found: ( ");

					for (Object object : args) {
						builder.append(object.getClass().getSimpleName()).append(", ");
					}

					builder.append(" )");

					throw new WrongGeneratorParamsProvided(builder.toString());
				}

				logger.debug("Arguments to generator correct");
				List<ConstantProviderBean<?>> dynamicParams = new ArrayList<>(args.length);

				logger.debug("Creating dynamic dependencies for {}", partialBean);
				for (Object obj : args) {
					dynamicParams.add(new ConstantProviderBean(obj, obj.getClass(), NoQualifier.class,
							Collections.emptyList(), DIBeanType.GENERATED));
				}

				logger.debug("Dynamic dependencies created: {}", dynamicParams);

				List<DIBean<?>> deps = partialBean.getDependencies();
				int j = 0;
				for (int i = 1; i < deps.size(); i++) {
					if (deps.get(i) == null) {
						deps.set(i, dynamicParams.get(j));
						j++;
					}
				}

				logger.debug("Final dependencies: {}", deps);

				ProviderImpl<?> provider = new ProviderImpl<>(partialBean);
				ConstantProviderBean<Provider> providerBean = new ConstantProviderBean<>(provider, Provider.class,
						partialBean.getQualifier(),
						Collections.singletonList(partialBean.getProviderType().getTypeName()), DIBeanType.GENERATED);

				provider.setHelper(helper);
				partialBean.setProviderBean((DIBeanImpl) providerBean);

				Object createdObj = partialBean.getProvider().get();
				logger.debug("New instance of {} created {}.", partialBean, createdObj);
				CommonFunc.safeAddToMapSet(objectMap, bean, new WeakReference<>(createdObj));

				return createdObj;
			}
		};

		ConstantProviderBean<Generator> genBean = toBeGeneratedMap.get(bean);
		genBean.setProvider(new Provider<Generator>() {
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