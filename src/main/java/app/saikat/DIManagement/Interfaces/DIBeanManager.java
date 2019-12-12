package app.saikat.DIManagement.Interfaces;

import java.lang.ref.WeakReference;
import java.util.Set;

public interface DIBeanManager {

	void beanCreated(DIBean<?> bean, Results results);

	void beanProviderCreated(DIBean<?> bean, Results results);

	void beanCreatedNewInstance(DIBean<?> bean, Object instance, Set<WeakReference<Object>> allInstances);

	void beanDependencyResolved(DIBean<?> bean);

}