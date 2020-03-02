package app.saikat.DIManagement.Impl.Repository;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import app.saikat.Annotations.DIManagement.Scan;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;

public class Repository {

	/** 
	 * Qualifier annotation i.e. annotations annotated with {@link javax.inject.Qualifier}
	 */
	private SetContainer<Class<? extends Annotation>> qualifierContainer;

	/**
	 * BeanManagers i.e. instances of {@link DIBeanManager}
	 */
	private MapContainer<Class<? extends DIBeanManager>, DIBeanManager> beanManagerContainer;

	/**
	 * All created beans
	 */
	private SetContainer<DIBean<?>> beansContainer;

	/**
	 * All scanned Annotation, Interface or SuperClass and its Scan annotation object. Contains configurations of beans to create
	 */
	private MapContainer<Class<?>, Scan> scandataContainer;

	public Repository() {
		this.qualifierContainer = new SetContainer<>(new HashSet<>());

		this.beanManagerContainer = new MapContainer<>(new HashMap<>());

		this.beansContainer = new SetContainer<>(new HashSet<>());

		this.scandataContainer = new MapContainer<>(new HashMap<>());
	}

	public ImmutableSet<Class<? extends Annotation>> getQualifierAnnotations() {
		return qualifierContainer.getImmutable();
	}

	public Set<DIBean<?>> getBeans() {
		return beansContainer.getImmutable();
	}

	public Map<Class<?>, Scan> getScanData() {
		return scandataContainer.getImmutable();
	}

	public Map<Class<? extends DIBeanManager>, DIBeanManager> getBeanManagers() {
		return beanManagerContainer.getImmutable();
	}

	@SuppressWarnings("unchecked")
	public <T extends DIBeanManager> T getBeanManagerOfType(Class<T> cls) {
		return (T) this.beanManagerContainer.data.get(cls);
	}

	public void addBeanManager(DIBeanManager beanManager) {
		beanManagerContainer.put(beanManager.getClass(), beanManager);
	}

	public void addBeanManagers(Collection<DIBeanManager> beanManagers) {
		beanManagers.parallelStream()
				.forEach(this::addBeanManager);
	}

	public boolean addQualifierAnnotation(Class<? extends Annotation> qualifier) {
		return this.qualifierContainer.add(qualifier);
	}

	public boolean addQualifierAnnotations(Collection<Class<? extends Annotation>> qualifiers) {
		return this.qualifierContainer.addAll(qualifiers);
	}

	public boolean addBean(DIBean<?> bean) {
		return this.beansContainer.add(bean);
	}

	public boolean addBeans(Collection<DIBean<?>> beans) {
		return this.beansContainer.addAll(beans);
	}

	public void addScanData(Class<?> cls, Scan scan) {
		this.scandataContainer.put(cls, scan);
	}

	public void addScanMap(Map<Class<?>, Scan> scanData) {
		this.scandataContainer.putAll(scanData);
	}

	public void merge(Repository repo) {
		this.qualifierContainer.addAll(repo.getQualifierAnnotations());
		this.beansContainer.addAll(repo.getBeans());
		this.beanManagerContainer.putAll(repo.getBeanManagers());
		this.scandataContainer.putAll(repo.getScanData());
	}
}