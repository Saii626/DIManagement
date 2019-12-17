package app.saikat.DIManagement.Impl;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.Results;

class DIBeanManagerHelper {

	private Map<Class<? extends DIBeanManager>, DIBeanManager> beanManagerMap = new HashMap<>();
	private Logger logger = LogManager.getLogger(DIBeanManagerHelper.class);

	@SuppressWarnings("unchecked")
	public void beanCreated(DIBean<?> bean, Results results) {
		((DIBeanImpl<?>) bean).addManagers(bean.getNonQualifierAnnotations().parallelStream()
				.map(a -> results.getAnnotationsToScan().get(a).beanManagers()).flatMap(arr -> Arrays.stream(arr))
				.map(cls -> DIBeanManager.class.isAssignableFrom(cls) ? (Class<? extends DIBeanManager>) cls : null)
				.filter(cls -> cls != null).collect(Collectors.toSet()));

		bean.getBeanManagers().parallelStream().map(this::getManagerOf).forEach(m -> m.beanCreated(bean, results));
	}

	public void beanProviderCreated(DIBean<?> bean, Results results) {
		bean.getBeanManagers().parallelStream().map(this::getManagerOf)
				.forEach(m -> m.beanProviderCreated(bean, results));
	}

	public void beanDependencyResolved(DIBean<?> bean, Results results) {
		bean.getBeanManagers().parallelStream().map(this::getManagerOf).forEach(m -> m.beanDependencyResolved(bean));
	}

	public void beanCreatedNewInstance(DIBean<?> bean, Object newInstance, Set<WeakReference<Object>> allObjects,
			Results results) {
		bean.getBeanManagers().parallelStream().map(this::getManagerOf)
				.forEach(m -> m.beanCreatedNewInstance(bean, newInstance, allObjects));
	}

	private DIBeanManager getManagerOf(Class<? extends DIBeanManager> cls) {
		if (!beanManagerMap.containsKey(cls)) {
			synchronized (beanManagerMap) {
				if (!beanManagerMap.containsKey(cls)) {
					DIBeanManager o;
					try {
						o = cls.newInstance();
						logger.debug("Created new instance of {}: {}", cls.getSimpleName(), o);

						beanManagerMap.put(cls, o);
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}

			}
		}

		synchronized (beanManagerMap) {
			return beanManagerMap.get(cls);
		}
	}
}