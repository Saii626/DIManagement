package app.saikat.DIManagement.Impl.DIBeans;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;
import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Impl.DIManagerImpl;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIManager;

public class DIManagerBean extends DIBeanImpl<DIManager> {

	@SuppressWarnings("unchecked")
	public DIManagerBean(DIManager manager) {
		super((Constructor<DIManager>)DIManagerImpl.class.getDeclaredConstructors()[0], NoQualifier.class, Collections.singleton(Singleton.class), true);

		this.provider = new Provider<DIManager>() {

			@Override
			public DIManager get() {
				return manager;
			}
		};
	}

	@Override
	public void setProvider(Provider<DIManager> provider) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDependencies(List<DIBeanImpl<?>> dependencies) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSingleton(boolean singleton) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addManagers(Collection<Class<? extends DIBeanManager>> managers) {
		throw new UnsupportedOperationException();
	}
}