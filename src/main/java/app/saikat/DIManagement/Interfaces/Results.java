package app.saikat.DIManagement.Interfaces;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.Annotations.DIManagement.ScanAnnotation;
import app.saikat.Annotations.DIManagement.ScanInterface;
import app.saikat.Annotations.DIManagement.ScanSubClass;
import app.saikat.PojoCollections.Utils.CommonFunc;

public class Results {

	private Set<Class<? extends Annotation>> qualifierAnnotations = Collections.synchronizedSet(new HashSet<>());

	private Map<Class<? extends Annotation>, Set<DIBean<?>>> annotationMap = new ConcurrentHashMap<>();
	private Map<Class<?>, Set<DIBean<?>>> interfacesMap = new ConcurrentHashMap<>();
	private Map<Class<?>, Set<DIBean<?>>> superClassesMap = new ConcurrentHashMap<>();

	private Map<Class<? extends Annotation>, ScanAnnotation> annotationsToScan = new ConcurrentHashMap<>();
	private Map<Class<?>, ScanInterface> interfacesToScan = new ConcurrentHashMap<>();
	private Map<Class<?>, ScanSubClass> superClassesToScan = new ConcurrentHashMap<>();

	private Set<DIBean<?>> annotationBeans = Collections.synchronizedSet(new HashSet<>());
	private Set<DIBean<?>> interfaceBeans = Collections.synchronizedSet(new HashSet<>());
	private Set<DIBean<?>> subclassBeans = Collections.synchronizedSet(new HashSet<>());
	private Set<DIBean<?>> generatedBeans = Collections.synchronizedSet(new HashSet<>());

	private boolean isImmutable = false;

	public Map<Class<? extends Annotation>, Set<DIBean<?>>> getAnnotationMap() {
		return annotationMap;
	}

	public Map<Class<?>, Set<DIBean<?>>> getInterfacesMap() {
		return interfacesMap;
	}

	public Map<Class<?>, Set<DIBean<?>>> getSubClassesMap() {
		return superClassesMap;
	}

	public Map<Class<? extends Annotation>, ScanAnnotation> getAnnotationsToScan() {
		return annotationsToScan;
	}

	public Set<Class<? extends Annotation>> getQualifierAnnotations() {
		return qualifierAnnotations;
	}

	public Map<Class<?>, ScanInterface> getInterfacesToScan() {
		return interfacesToScan;
	}

	public Map<Class<?>, ScanSubClass> getSuperClassesToScan() {
		return superClassesToScan;
	}

	public Set<DIBean<?>> getAnnotationBeans() {
		return annotationBeans;
	}

	public Set<DIBean<?>> getInterfaceBeans() {
		return interfaceBeans;
	}

	public Set<DIBean<?>> getSubclassBeans() {
		return subclassBeans;
	}

	public Set<DIBean<?>> getGeneratedBeans() {
		return generatedBeans;
	}

	public boolean isImmutable() {
		return isImmutable;
	}

	public void makeImmutable() {
		if (!isImmutable) {
			isImmutable = true;
			annotationMap = Collections.unmodifiableMap(annotationMap);
			interfacesMap = Collections.unmodifiableMap(interfacesMap);
			superClassesMap = Collections.unmodifiableMap(superClassesMap);

			annotationsToScan = Collections.unmodifiableMap(annotationsToScan);
			interfacesToScan = Collections.unmodifiableMap(interfacesToScan);
			superClassesToScan = Collections.unmodifiableMap(superClassesToScan);

			annotationBeans = Collections.unmodifiableSet(annotationBeans);
			interfaceBeans = Collections.unmodifiableSet(interfaceBeans);
			subclassBeans = Collections.unmodifiableSet(subclassBeans);

			qualifierAnnotations = Collections.unmodifiableSet(qualifierAnnotations);
		}
	}

	public void addAnnotationToScan(Class<? extends Annotation> annot, ScanAnnotation item) {
		annotationsToScan.put(annot, item);
	}

	public void addQualifierToScan(Class<? extends Annotation> item) {
		qualifierAnnotations.add(item);
		annotationMap.put(item, new HashSet<>());
	}

	public void addnterfaceToScan(Class<?> cls, ScanInterface item) {
		interfacesToScan.put(cls, item);
		interfacesMap.put(cls, new HashSet<>());
	}

	public void addSuperClassToScan(Class<?> cls, ScanSubClass item) {
		superClassesToScan.put(cls, item);
		superClassesMap.put(cls, new HashSet<>());
	}

	public void addAnnotationBean(DIBean<?> item) {
		annotationBeans.add(item);
		Class<? extends Annotation> qualifier = item.getQualifier();
		addToMap(annotationMap, qualifier != null ? qualifier : NoQualifier.class, item);
	}

	public void addInterfaceBean(DIBean<?> item, Class<?> inter) {
		interfaceBeans.add(item);
		addToMap(interfacesMap, inter, item);
	}

	public void addSubclassBean(DIBean<?> item, Class<?> superCls) {
		subclassBeans.add(item);
		addToMap(superClassesMap, superCls, item);
	}

	public void addGeneratedBean(DIBean<?> item) {
		generatedBeans.add(item);
	}

	private <K, V> void addToMap(Map<K, Set<V>> map, K key, V item) {
		if (!isImmutable) {
			CommonFunc.addToMapSet(map, key, item);
		} else {
			throw new UnsupportedOperationException("Immutable object");
		}
	}
}