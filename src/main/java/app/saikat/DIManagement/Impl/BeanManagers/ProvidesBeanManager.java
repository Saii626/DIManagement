package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import app.saikat.Annotations.DIManagement.Provides;
import app.saikat.Annotations.DIManagement.ScanAnnotation;
import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.Results;

public class ProvidesBeanManager extends BeanManagerImpl {

	public ProvidesBeanManager(Results results, Map<DIBean<?>, Set<WeakReference<?>>> objectMap,
			DIBeanManagerHelper helper) {
		super(results, objectMap, helper);
	}

	@Override
	public Map<Class<? extends Annotation>, ScanAnnotation> addAnnotationsToScan() {
		return Collections.singletonMap(Provides.class, createScanAnnotationWithBeanManager(this.getClass()));
	}

	@Override
	public <T> void beanCreated(DIBean<T> bean) {
		super.beanCreated(bean);
		DIBeanImpl<?> b = (DIBeanImpl<?>) bean;

		b.setSingleton(b.getInvokable()
				.getAnnotation(Provides.class)
				.singleton());
	}

}