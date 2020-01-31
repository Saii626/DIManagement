package app.saikat.DIManagement.Impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.DIManagement.Impl.BeanManagers.InjectBeanManager;
import app.saikat.DIManagement.Impl.BeanManagers.PostConstructBeanManager;
import app.saikat.DIManagement.Impl.DIBeans.BuildContext;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanType;
import app.saikat.PojoCollections.CommonObjects.Either;

public class ProviderImpl<T> implements Provider<T> {

	// If bean is singleton, store a hard reference. objectMap only stores WeakReferences
	private T singletonInstance = null;

	// Pointer to parent bean
	private final DIBean<T> bean;

	private DIBeanManagerHelper helper;

	private Logger logger = LogManager.getLogger(Provider.class);

	public ProviderImpl(DIBean<T> bean) {
		this.bean = bean;
	}

	public void setHelper(DIBeanManagerHelper helper) {
		this.helper = helper;
	}

	@Override
	public T get() {
		T instanceToReturn = null;

		logger.debug("get called on {}'s provider", this.bean);

		try {
			if (this.bean.isSingleton()) {
				logger.debug("{} is singleton", this.bean);

				if (this.singletonInstance == null) {
					logger.debug("No instance of {} created. Need to create a new one", this.bean);

					// No one else should be creating this bean
					synchronized (this.bean) {
						if (this.singletonInstance == null) {
							try (BuildContext context = BuildContext.getBuildContext()) {
								this.singletonInstance = this.createNewInstance();
								this.bean.getBeanManager().newInstanceCreated(this.bean, this.singletonInstance);
							}
						}
					}
				}
				instanceToReturn = this.singletonInstance;

			} else {
				try (BuildContext context = BuildContext.getBuildContext()) {
					instanceToReturn = this.createNewInstance();
					this.bean.getBeanManager().newInstanceCreated(this.bean, instanceToReturn);
				}
			}
		} catch (Exception e) {
			logger.error("Error while creating {}", this.bean);
			logger.error("", e);
		}

		logger.debug("Instance returned for bean {}: {}", this.bean, instanceToReturn);
		return instanceToReturn;
	}

	// private T build()
	// 		throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	// 		T t = this.createNewInstance();
	// 		return t;
	// 	}

	// }

	@SuppressWarnings("unchecked")
	private T createNewInstance()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Either<Constructor<T>, Method> underlyingExecutable = this.bean.get();
		List<DIBean<?>> dependencies = this.bean.getDependencies();

		logger.info("Creating new instance from bean {}, with dependencies: {}", bean, dependencies);
		int shift = this.bean.get().containsRight() ? 1 : 0;

		Object[] parameters = new Object[dependencies.size() - shift];

		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = dependencies.get(i + shift).getProvider().get();
		}

		T ret;
		if (shift == 0) {
			Constructor<T> constructor = underlyingExecutable.getLeft().get();
			ret = constructor.newInstance(parameters);
		} else {
			Method method = underlyingExecutable.getRight().get();
			DIBean<?> parent = dependencies.get(0);
			Object o = parent != null ? parent.getProvider().get() : null;
			ret = (T) method.invoke(o, parameters);
		}

		logger.debug("Created object: {}", ret);

		ConstantProviderBean<T> currentObjectProvider = new ConstantProviderBean<>(ret, this.bean.getProviderType(),
				this.bean.getQualifier(), Collections.emptyList(), DIBeanType.GENERATED);

		// Add setter injections to buildContext
		InjectBeanManager manager = (InjectBeanManager) helper.getManagerOf(Inject.class);

		// Do shallow copy so that we can modify the dependencies in a thread safe way
		Set<DIBean<?>> setterInjections = new HashSet<>(manager.getSetterInjectionsFor(bean));

		setterInjections.parallelStream().map(b -> {
			// Do a shallow copy so that creating multiple instances are thread safe
			DIBean<?> copyBean = new DIBeanImpl<>(b);
			List<DIBean<?>> dep = copyBean.getDependencies();
			dep.set(0, currentObjectProvider);
			// ((DIBeanImpl<?>) copyBean).getDependencies().addAll(dep);

			return copyBean;
		}).forEach(BuildContext::addToSetterInjection);

		// Add postConstruct to buildcontext
		PostConstructBeanManager pManager = (PostConstructBeanManager) helper.getManagerOf(PostConstruct.class);

		DIBean<?> origPostBean = pManager.getPostConstructBean(bean);
		if (origPostBean != null) {
			DIBean<?> postConstructBean = new DIBeanImpl<>(origPostBean);
			List<DIBean<?>> dep = bean.getDependencies();
			dep.set(0, currentObjectProvider);

			BuildContext.addToPostConstruct(postConstructBean);
		}
		return ret;
	}

	@Override
	public String toString() {
		return "p" + bean.toString();
	}

	// Dependency list of methods have 1st element as the on object on which to invoke the method
	// private Tuple<Provider<?>[], boolean[]> extractProviders(List<DIBean<?>> dependencies,
	// 		List<Class<?>> parameterTypes) {

	// 	Provider<?>[] providers = new Provider[dependencies.size()];
	// 	boolean[] provider = new boolean[dependencies.size()];

	// 	int shift;
	// 	if (bean.get().containsRight()) {
	// 		shift = 1;
	// 		DIBean<?> parent = dependencies.get(0);
	// 		providers[0] = parent != null ? parent.getProvider() : null;
	// 		provider[0] = false;
	// 	} else {
	// 		shift = 0;
	// 	}

	// 	for (int i = shift; i < providers.length; i++) {
	// 		providers[i] = dependencies.get(i).getProvider();
	// 		provider[i] = parameterTypes.get(i - shift).equals(Provider.class);
	// 	}

	// 	return Tuple.of(providers, provider);
	// }
}