package app.saikat.DIManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.Traverser;

import app.saikat.LogManagement.Logger;
import app.saikat.LogManagement.LoggerFactory;
import app.saikat.PojoCollections.CommonObjects.Tuple;
import app.saikat.DIManagement.Exceptions.ClassNotUnderDIException;
import app.saikat.DIManagement.Exceptions.NoProviderFoundForClassException;

class ObjectMap {

	private final Map<DIBean, Tuple<Provider<?>, Object>> objectMap;
	private final Set<Class<? extends Annotation>> QUALIFIERS;
	private final ImmutableGraph<DIBean> dependencyGraph;
	private Logger logger;

	// Vars to maintain state of operation so that we don't need to lock the whole object
	private final Set<DIBean> creatingProvider;
	private final Set<Provider<?>> buildingObjects;

	public ObjectMap(Set<Class<? extends Annotation>> qualifiers, ImmutableGraph<DIBean> graph) {
		objectMap = new HashMap<>();
		QUALIFIERS = qualifiers;
		dependencyGraph = graph;
		logger = LoggerFactory.getLogger(this.getClass());

		creatingProvider = Collections.synchronizedSet(new HashSet<>());
		buildingObjects = Collections.synchronizedSet(new HashSet<>());
	}

	@SuppressWarnings("unchecked")
	public <T> T get(DIBean bean) throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, ClassNotUnderDIException, InterruptedException {

		// if (!objectMap.containsKey(bean)) {

		// 	DIBean b = Utils.getProviderBean(dependencyGraph.nodes(), bean);

		// 	// Order in which instances will be created
		// 	List<DIBean> dependencyList = getBuildListFor(b);

		// 	for (DIBean d : dependencyList) {
		// 		// dependency bean should already be present or, if is class, no args constructor or no args @inject constructor
		// 		// must be present, or can be a no arg method

		// 		synchronized (objectMap) {
		// 			if (!objectMap.containsKey(d)) {
		// 				objectMap.put(d, Tuple.of(getProviderOf(d), null));
		// 			}
		// 		}
		// 	}
		// }

		// Tuple<Provider<?>, Object> tuple = objectMap.get(bean);
		// if (tuple.second == null) {
		// 	tuple.second = tuple.first.newInstance();
		// }

		// return (T) tuple.second;

		Provider<T> provider = getProvider(bean);

		synchronized (buildingObjects) {
			while (buildingObjects.contains(provider)) {
				buildingObjects.wait(1000);
			}

			synchronized (objectMap) {
				Tuple<Provider<?>, Object> val = objectMap.get(bean);
				if (val.second != null) {
					return (T) val.second;
				}
			}

			buildingObjects.add(provider);
		}

		T object = provider.newInstance();

		synchronized (objectMap) {
			Tuple<Provider<?>, Object> val = objectMap.get(bean);
			val.second = object;
		}

		synchronized (buildingObjects) {
			buildingObjects.remove(provider);
			buildingObjects.notifyAll();
		}

		return object;
	}

	public <T> Provider<T> getProvider(DIBean bean) throws InterruptedException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotUnderDIException {
		synchronized (creatingProvider) {
			while (creatingProvider.contains(bean)) {
				creatingProvider.wait(1000);
			}

			synchronized (objectMap) {
				if (objectMap.containsKey(bean)) {
					return (Provider<T>) objectMap.get(bean).first;
				}
			}

			creatingProvider.add(bean);
		}

		// Initialize Provider s of dependencies
		List<DIBean> dependencyList = getBuildListFor(bean);

		for (DIBean d : dependencyList) {
			getProvider(d);
		}

		// Create the required Provider
		Provider<T> provider = getProviderOf(bean);

		synchronized (objectMap) {
			objectMap.put(bean, Tuple.of(provider, null));
		}

		synchronized (creatingProvider) {
			creatingProvider.remove(bean);
			creatingProvider.notifyAll();
		}

		return provider;
	}

	public Map<DIBean, Object> getObjectMap() {
		return Collections.unmodifiableMap(objectMap);
	}

	private <T> Provider<T> getProviderOf(DIBean bean)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (bean.isMethod()) {
			logger.debug("Building method: {}", bean.getMethod()
					.getName());
			Method method = bean.getMethod();

			Provider<?> parent;
			if (!Modifier.isStatic(method.getModifiers())) {
				Class<?> declaringClass = method.getDeclaringClass();
				DIBean enclosingClassBean = new DIBean(declaringClass,
						Utils.getQualifierAnnotation(declaringClass.getAnnotations(), QUALIFIERS));
				// If not present in map then the bean is not a leaf.

				synchronized (objectMap) {
					if (!objectMap.containsKey(enclosingClassBean)) {
						throw new NoProviderFoundForClassException(declaringClass);
					}
				}
				parent = objectMap.get(enclosingClassBean).first;
			} else {
				parent = new Provider() {
					@Override
					public Object newInstance() {
						return null;
					}

					@Override
					public Class<?> getType() {
						return null;
					}
				};
			}

			List<DIBean> dependencies = Utils.getParameterBeansWithoutProvider(method.getParameterTypes(),
					method.getParameterAnnotations(), method.getGenericParameterTypes(), QUALIFIERS);

			return new Provider<T>() {

				private final Logger logger = LoggerFactory.getLogger(this.getClass());

				// private final List<Provider<?>> paramProviders = dependencies.stream()
				// 		.map(bean -> {
				// 			try {
				// 				return getProvider(bean);
				// 			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				// 					| InvocationTargetException | InterruptedException | ClassNotUnderDIException e) {
				// 					logger.error("Error: ", e);
				// 				return null;
				// 			}
				// 		})
				// 		.collect(Collectors.toList());
				private final Provider<?> parentProvider = parent;
				private final Method m = method;

				private final List<Tuple<DIBean, Class<?>>> deps = Utils.getParameterBeansWithProvider(
						m.getParameterTypes(), m.getParameterAnnotations(), m.getGenericParameterTypes(), QUALIFIERS);;

				@Override
				public T newInstance() {
					try {
						List<Object> params = getArgumentsForDependencies(deps);
						return (T) method.invoke(parent, params.toArray());
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						logger.error("Error: ", e);
						return null;
					}
				}

				@Override
				public Class<T> getType() {
					return (Class<T>) bean.getType();
				}
			};

		} else {
			logger.debug("Building class: {}", bean.getType()
					.getSimpleName());

			Constructor<?> toUse = Utils.getAppropriateConstructor(bean.getType()
					.getDeclaredConstructors());
			toUse.setAccessible(true);

			List<Tuple<DIBean, Class<?>>> dependencies = Utils.getParameterBeansWithProvider(toUse.getParameterTypes(),
					toUse.getParameterAnnotations(), toUse.getGenericParameterTypes(), QUALIFIERS);

			// logger.debug("parameters of {} are {}", bean, Utils.getStringRepresentationOf(dependencies));

			return new Provider<T>() {
				private final List<Object> params = getArgumentsForDependencies(dependencies);

				private final Logger logger = LoggerFactory.getLogger(this.getClass());

				@Override
				public T newInstance() {
					try {
						return (T) toUse.newInstance(params.toArray());
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						logger.error("Error: ", e);
						return null;
					}
				}

				@Override
				public Class<T> getType() {
					return (Class<T>) bean.getType();
				}
			};
		}
	}

	private List<DIBean> getBuildListFor(DIBean bean) throws ClassNotUnderDIException {

		if (!dependencyGraph.nodes()
				.contains(bean)) {
			logger.error("Bean {} not present", bean);
			throw new ClassNotUnderDIException(bean.getType());
		}

		Traverser<DIBean> traverser = Traverser.forGraph(dependencyGraph);
		Iterable<DIBean> dfsIterator = traverser.depthFirstPostOrder(bean);

		List<DIBean> list = Lists.newArrayList(dfsIterator);
		logger.debug("dependencyList {}: {}", bean, Utils.getStringRepresentationOf(list));

		return Collections.unmodifiableList(list);
	}

	private List<Object> getArgumentsForDependencies(List<Tuple<DIBean, Class<?>>> params)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			InterruptedException, ClassNotUnderDIException {

		List<Object> parameters = new ArrayList<>();

		for (Tuple<DIBean, Class<?>> parameter : params) {

			if (parameter.first.getType()
			.equals(Provider.class)) {
				
				parameters.add(getProvider(new DIBean(parameter.second, parameter.first.getQualifier())));
				continue;
			}
			
			DIBean dep;
			dep = parameter.first;

			logger.debug("Searching for dependency: {}", dep);
			Tuple<Provider<?>, Object> obj = objectMap.get(dep);
			if (obj == null || obj.first == null) {
				throw new NoProviderFoundForClassException(dep.getType());
			}

			if (parameter.first.getType()
					.equals(Provider.class)) {
				parameters.add(obj.first);
			} else {
				if (obj.second == null) {
					Object o = obj.first.newInstance();
					logger.debug("New instance created {}", o);
					obj.second = o;

				}

				parameters.add(obj.second);
			}
		}
		return parameters;

	}
}