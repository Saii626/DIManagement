package app.saikat.DIManagement.Impl.ExternalImpl;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Provider;

import com.google.common.reflect.Invokable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Impl.BuildContext;
import app.saikat.DIManagement.Impl.BeanManagers.InjectBeanManager;
import app.saikat.DIManagement.Impl.BeanManagers.PostConstructBeanManager;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;

public class ProviderImpl<T> implements Provider<T> {

	// If bean is singleton, store a hard reference. objectMap only stores WeakReferences
	private T singletonInstance = null;

	// Pointer to parent bean
	private final DIBeanImpl<T> bean;

	private final InjectBeanManager injectBeanManager;
	private final PostConstructBeanManager postConstructBeanManager;

	private Logger logger = LogManager.getLogger(Provider.class);

	public ProviderImpl(DIBeanImpl<T> bean, InjectBeanManager injectBeanManager, PostConstructBeanManager postConstructBeanManager) {
		this.bean = bean;
		this.injectBeanManager = injectBeanManager;
		this.postConstructBeanManager = postConstructBeanManager;
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
								this.bean.getBeanManager()
										.newInstanceCreated(this.bean, this.singletonInstance);
							}
						}
					}
				}
				instanceToReturn = this.singletonInstance;

			} else {
				try (BuildContext context = BuildContext.getBuildContext()) {
					instanceToReturn = this.createNewInstance();
					this.bean.getBeanManager()
							.newInstanceCreated(this.bean, instanceToReturn);
				}
			}
		} catch (Exception e) {
			logger.error("Error while creating {}", this.bean);
			logger.error("", e);
		}

		logger.debug("Instance returned for bean {}: {}", this.bean, instanceToReturn);
		return instanceToReturn;
	}

	private T createNewInstance()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Invokable<Object, T> underlyingExecutable = (Invokable<Object, T>) this.bean.getInvokable();
		List<DIBean<?>> dependencies = this.bean.getDependencies();

		List<Object> parameters = dependencies.stream()
				.map(b -> b == null ? null
						: b.getProvider()
								.get())
				.collect(Collectors.toList());

		T ret = underlyingExecutable.invoke(parameters.get(0), parameters.subList(1, parameters.size())
				.toArray());
		logger.debug("Created new object {}", ret);

		ConstantProviderBean<T> currentObjectProvider = new ConstantProviderBean<>(this.bean.getProviderType(),
				NoQualifier.class);
		currentObjectProvider.setProvider(() -> ret);

		// Add setter injections to buildContext
		Set<DIBean<?>> setterInjections = new HashSet<>(injectBeanManager.getSetterInjectionsFor(bean));

		setterInjections.parallelStream()
				.map(b -> b.copy())
				.forEach(b -> {
					b.getDependencies()
							.set(0, currentObjectProvider);
					BuildContext.addToSetterInjection(b);
				});

		// Add postConstruct to buildcontext
		DIBean<?> origPostBean = postConstructBeanManager.getPostConstructBean(bean);
		if (origPostBean != null) {
			DIBeanImpl<?> postConstructBean = ((DIBeanImpl<?>) origPostBean).copy();
			postConstructBean.getDependencies()
					.set(0, currentObjectProvider);
			BuildContext.addToPostConstruct(postConstructBean);
		}
		return ret;
	}

	@Override
	public String toString() {
		return "p" + bean.toString();
	}
}