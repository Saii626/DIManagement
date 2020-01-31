// package app.saikat.DIManagement.Impl.DIBeans;

// import java.lang.reflect.Constructor;
// import javax.inject.Provider;
// import javax.inject.Singleton;

// import app.saikat.Annotations.DIManagement.NoQualifier;
// import app.saikat.DIManagement.Impl.DIBeanManagerHelper;
// import app.saikat.DIManagement.Impl.DIManagerImpl;
// import app.saikat.DIManagement.Impl.BeanManagers.NoOpBeanManager;
// import app.saikat.DIManagement.Interfaces.DIBeanManager;
// import app.saikat.DIManagement.Interfaces.DIBeanType;
// import app.saikat.DIManagement.Interfaces.DIManager;

// public class DIManagerBean extends DIBeanImpl<DIManager> {

// 	@SuppressWarnings("unchecked")
// 	public DIManagerBean(DIManager manager) {
// 		super((Constructor<DIManager>) DIManagerImpl.class.getDeclaredConstructors()[0], NoQualifier.class,
// 				Singleton.class, true, DIBeanType.OTHER);

// 		this.provider = new Provider<DIManager>() {

// 			@Override
// 			public DIManager get() {
// 				return manager;
// 			}
// 		};
// 	}

// 	public void updateBeanmanager(DIBeanManagerHelper helper) {
// 		this.beanManager = helper.getManagerOf(NoOpBeanManager.class);
// 	}

// 	@Override
// 	public void setProvider(Provider<DIManager> provider) {
// 		throw new UnsupportedOperationException();
// 	}

// 	@Override
// 	public void setSingleton(boolean singleton) {
// 		throw new UnsupportedOperationException();
// 	}

// 	@Override
// 	public void setBeanManager(DIBeanManager beanManager) {
// 		throw new UnsupportedOperationException();
// 	}

// 	@Override
// 	protected String getTypeString() {
// 		return "DIManager";
// 	}
// }