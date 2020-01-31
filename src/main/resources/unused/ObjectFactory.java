// package app.saikat.DIManagement.Impl;

// import java.lang.ref.WeakReference;
// import java.lang.reflect.Constructor;
// import java.lang.reflect.InvocationTargetException;
// import java.lang.reflect.Method;
// import java.util.Collections;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import java.util.concurrent.ConcurrentHashMap;

// import javax.inject.Provider;

// import com.google.common.collect.Lists;
// import com.google.common.graph.ImmutableGraph;
// import com.google.common.graph.Traverser;

// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;

// import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
// import app.saikat.DIManagement.Impl.DIBeans.DIManagerBean;
// import app.saikat.DIManagement.Interfaces.DIBean;
// import app.saikat.DIManagement.Interfaces.Results;
// import app.saikat.PojoCollections.CommonObjects.Either;
// import app.saikat.PojoCollections.Utils.CommonFunc;

// // Last interesting part. Place to implement most interesting object management.
// class ObjectFactory {

// 	private final ImmutableGraph<DIBean<?>> dependencyGraph;
// 	private final Set<DIBean<?>> alreadyProvidedBeans;

// 	private final DIBeanManagerHelper helper;
// 	private final Results results;

// 	private final Map<DIBean<?>, Set<WeakReference<?>>> allCreatedObjects = new ConcurrentHashMap<>();
// 	private final Map<DIBean<?>, Set<WeakReference<?>>> immutableViewOfObjectMap = Collections
// 			.unmodifiableMap(allCreatedObjects);

// 	private final Logger logger = LogManager.getLogger(ObjectFactory.class);

// 	public ObjectFactory(ImmutableGraph<DIBean<?>> dependencyGraph, DIBeanManagerHelper helper, Results results, DIManagerBean bean) {
// 		this.dependencyGraph = dependencyGraph;
// 		this.alreadyProvidedBeans = new HashSet<>();
// 		this.alreadyProvidedBeans.add(bean);
		
// 		this.helper = helper;
// 		this.results = results;
// 	}

// 	public synchronized void generateProviders() {
// 		dependencyGraph.nodes().forEach(this::createAndSetProvider);
// 	}

// 	private <T> void createAndSetProvider(DIBean<T> bean) {

// 		if (alreadyProvidedBeans.contains(bean)) {
// 			return;
// 		}

// 		logger.debug("Trying to create provider for {}", bean);

// 		List<DIBean<?>> buildList = getBuildListFor(bean);
// 		logger.debug("Build list for {} is {}", bean, buildList.toArray());

// 		buildList.forEach(this::createAndSetProvider);

// 		DIBeanImpl<T> beanImpl = (DIBeanImpl<T>) bean;

// 		Provider<T> provider = new ProviderImpl<>(beanImpl, this);

// 		beanImpl.setProvider(provider);
// 		helper.beanProviderCreated(beanImpl, this.results);

// 		alreadyProvidedBeans.add(bean);
// 	}

// 	private List<DIBean<?>> getBuildListFor(DIBean<?> bean) {

// 		if (!dependencyGraph.nodes().contains(bean)) {
// 			logger.error("Bean {} not present", bean);
// 			return Collections.emptyList();
// 		}

// 		Traverser<DIBean<?>> traverser = Traverser.forGraph(dependencyGraph);
// 		Iterable<DIBean<?>> dfsIterator = traverser.depthFirstPostOrder(bean);

// 		List<DIBean<?>> buildList = Lists.newArrayList(dfsIterator);
// 		buildList.remove(buildList.size() - 1);

// 		return Collections.unmodifiableList(buildList);
// 	}

// 	/**
// 	 * Returns immutable view of object map. Underlying map may change anytime, and
// 	 * trying to synchronize on the returned map won't be useful
// 	 * @return an immutable view of object map (map of DIBean to set of objects created by provider of the bean)
// 	 */
// 	public Map<DIBean<?>, Set<WeakReference<?>>> getImmutableViewOfObjectMap() {
// 		return this.immutableViewOfObjectMap;
// 	}

// 	Map<DIBean<?>, Set<WeakReference<?>>> getObjectMap() {
// 		return this.allCreatedObjects;
// 	}

// 	DIBeanManagerHelper getHelper() {
// 		return helper;
// 	}

// 	Results getResults() {
// 		return results;
// 	}

// }

// class ProviderImpl<T> implements Provider<T> {

// 	// If bean is singleton, store a hard reference. allCreatedObjects only stores WeakReferences
// 	private T singletonInstance = null;

// 	// Pointer to parent bean
// 	private final DIBeanImpl<T> bean;

// 	// Pointer to ObjectFactory to update map of allCreatedObjects
// 	private final ObjectFactory objectFactory;

// 	private Provider<?>[] providers;
// 	// If param is provider or normal object 
// 	private boolean[] provider;

// 	private Logger logger = LogManager.getLogger(Provider.class);

// 	public ProviderImpl(DIBeanImpl<T> bean, ObjectFactory objectFactory) {
// 		this.bean = bean;
// 		this.objectFactory = objectFactory;

// 		extractProviders(bean.getDependencies(),
// 				Lists.newArrayList(bean.get().apply(c -> c, m -> m).getParameterTypes()));

// 		logger.debug("{} created with dependent providers {}", this, this.providers);
// 	}

// 	// Dependency list of methods have 1st element as the on object on which to invoke the method
// 	private void extractProviders(List<DIBeanImpl<?>> dependencies, List<Class<?>> parameterTypes) {

// 		providers = new Provider[dependencies.size()];
// 		provider = new boolean[dependencies.size()];

// 		int shift;
// 		if (bean.get().containsRight()) {
// 			shift = 1;
// 			DIBeanImpl<?> parent = dependencies.get(0);
// 			providers[0] = parent != null ? parent.getProvider() : null;
// 			provider[0] = false;
// 		} else {
// 			shift = 0;
// 		}

// 		for (int i = shift; i < providers.length; i++) {
// 			providers[i] = dependencies.get(i).getProvider();
// 			provider[i] = parameterTypes.get(i - shift).equals(Provider.class);
// 		}
// 	}

// 	@Override
// 	@SuppressWarnings({"unchecked", "rawtypes"})
// 	public T get() {
// 		T instanceToReturn = null;

// 		logger.debug("get of {} called", this);

// 		try {
// 			if (this.bean.isSingleton()) {
// 				logger.debug("{} is singleton", this.bean);

// 				if (this.singletonInstance == null) {
// 					logger.debug("No instance of {} created. Need to create a new one", this.bean);

// 					// No one else should be creating this bean
// 					synchronized (this.bean) {
// 						if (this.singletonInstance == null) {
// 							this.singletonInstance = createNewInstance();

// 							this.objectFactory.getHelper().beanCreatedNewInstance(bean, this.singletonInstance,
// 									(Set) this.objectFactory.getImmutableViewOfObjectMap().get(this.bean),
// 									this.objectFactory.getResults());
// 						}
// 					}
// 				}

// 				instanceToReturn = this.singletonInstance;
// 			} else {
// 				instanceToReturn = this.createNewInstance();

// 				this.objectFactory.getHelper().beanCreatedNewInstance(bean, instanceToReturn,
// 						(Set) this.objectFactory.getImmutableViewOfObjectMap().get(this.bean),
// 						this.objectFactory.getResults());
// 			}

// 			CommonFunc.safeAddToMapSet(objectFactory.getObjectMap(), this.bean, new WeakReference<T>(instanceToReturn));
// 		} catch (Exception e) {
// 			logger.error("Error: ", e);
// 		}

// 		logger.debug("Instance returned for bean {}: {}", this.bean, instanceToReturn);
// 		return instanceToReturn;
// 	}

// 	@SuppressWarnings("unchecked")
// 	private T createNewInstance()
// 			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
// 		Either<Constructor<T>, Method> underlyingExecutable = this.bean.get();
// 		List<DIBeanImpl<?>> dependencies = this.bean.getDependencies();

// 		logger.info("Creating new instance from bean {}", bean);
// 		int shift = this.bean.get().containsRight() ? 1 : 0;

// 		Object[] parameters = new Object[dependencies.size() - shift];

// 		for (int i = 0; i < parameters.length; i++) {
// 			parameters[i] = provider[i + shift] ? providers[i + shift] : providers[i + shift].get();
// 		}

// 		T ret;
// 		if (shift == 0) {
// 			Constructor<T> constructor = underlyingExecutable.getLeft().get();
// 			ret = constructor.newInstance(parameters);
// 		} else {
// 			Method method = underlyingExecutable.getRight().get();
// 			DIBeanImpl<?> parent = dependencies.get(0);
// 			Object o = parent != null ? parent.getProvider().get() : null;
// 			ret = (T) method.invoke(o, parameters);
// 		}

// 		logger.debug("Created object: {}", ret);
// 		return ret;
// 	}

// 	@Override
// 	public String toString() {
// 		return "<Provider of " + bean.toString() + ">";
// 	}

// 	Provider<?>[] getProviders() {
// 		return providers;
// 	}

// 	public boolean[] getIsProvider() {
// 		return provider;
// 	}

// }