package app.saikat.DIManagement.Impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.Generate;
import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.Annotations.DIManagement.Provides;
import app.saikat.Annotations.DIManagement.ScanAnnotation;
import app.saikat.Annotations.DIManagement.ScanInterface;
import app.saikat.Annotations.DIManagement.ScanSubClass;
import app.saikat.DIManagement.BeanManagers.ProvidesBeanManager;
import app.saikat.DIManagement.BeanManagers.SingletonBeanManager;
import app.saikat.DIManagement.Exceptions.NoValidConstructorFoundException;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Impl.DIBeans.DIManagerBean;
import app.saikat.DIManagement.Interfaces.Results;
import app.saikat.PojoCollections.CommonObjects.Tuple;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

public class ClasspathScanner {

	private Logger logger = LogManager.getLogger(this.getClass());
	private final String SCAN_ANNOTATION = ScanAnnotation.class.getName();
	private final String SCAN_INTERFACE = ScanInterface.class.getName();
	private final String SCAN_SUBCLASS = ScanSubClass.class.getName();
	private final String QUALIFIER_ANNOTATION = Qualifier.class.getName();
	private final String SINGLETON_ANNOTATION = Singleton.class.getName();

	private final DIBeanManagerHelper helper;
	private final DIManagerBean managerBean;

	public ClasspathScanner(DIBeanManagerHelper helper, DIManagerBean managerBean) {
		this.helper = helper;
		this.managerBean = managerBean;
	}

	public Results scan(String... packagesToScan) {
		Results scanResult = new Results();

		logger.info("Initializing DIManager. Starting scans");
		try (ScanResult results = new ClassGraph().enableClassInfo().enableMethodInfo().ignoreMethodVisibility()
				.ignoreClassVisibility().enableAnnotationInfo().whitelistPackages(packagesToScan).scan()) {

			logger.debug("Total classes: {}", results.getAllClasses().size());
			Tuple<Set<String>, Set<String>> metaAnnotations = scanMetaAnnotations(scanResult, results);

			scanAnnotations(scanResult, results, metaAnnotations.first, metaAnnotations.second);
			scanInterfaces(scanResult, results, metaAnnotations.first, metaAnnotations.second);
			scanSubclasses(scanResult, results, metaAnnotations.first, metaAnnotations.second);
		}

		return scanResult;
	}

	@SuppressWarnings("unchecked")
	private Tuple<Set<String>, Set<String>> scanMetaAnnotations(Results scanResult, ScanResult results) {
		// Scan for annotations
		Set<ClassInfo> allAnnotations = Sets.newHashSet(results.getAllAnnotations());

		ScanAnnotation singletonScanAnnotation = new ScanAnnotation() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return ScanAnnotation.class;
			}

			@Override
			public boolean autoManage() {
				return true;
			}

			@Override
			public Class<?>[] beanManagers() {
				return new Class<?>[] { SingletonBeanManager.class };
			}
		};

		scanResult.addAnnotationToScan(Singleton.class, singletonScanAnnotation);
		scanResult.addAnnotationToScan(PostConstruct.class, singletonScanAnnotation);

		// Inject annotation is not singleton
		ScanAnnotation injectScanAnnotation = new ScanAnnotation() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return ScanAnnotation.class;
			}

			@Override
			public boolean autoManage() {
				return true;
			}

			@Override
			public Class<?>[] beanManagers() {
				return new Class<?>[0];
			}
		};
		scanResult.addAnnotationToScan(Inject.class, injectScanAnnotation);

		ScanAnnotation providesScanAnnotation = new ScanAnnotation() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return ScanAnnotation.class;
			}

			@Override
			public Class<?>[] beanManagers() {
				return new Class<?>[] { ProvidesBeanManager.class };
			}

			@Override
			public boolean autoManage() {
				return true;
			}
		};
		scanResult.addAnnotationToScan(Provides.class, providesScanAnnotation);

		// ScanAnnotation generatorScanAnnotation = new ScanAnnotation(){
		
		// 	@Override
		// 	public Class<? extends Annotation> annotationType() {
		// 		return ScanAnnotation.class;
		// 	}
		
		// 	@Override
		// 	public Class<?>[] beanManagers() {
		// 		return new Class<?>[] { GeneratorBeanManager.class };
		// 	}
		
		// 	@Override
		// 	public boolean autoManage() {
		// 		return true;
		// 	}
		// // };
		// scanResult.addAnnotationToScan(Generate.class, generatorScanAnnotation);

		logger.debug("Begining annotation scan");
		allAnnotations.parallelStream().forEach(annot -> {
			logger.trace("Scanning annotation: {}", annot.getSimpleName());

			if (annot.hasAnnotation(SCAN_ANNOTATION)) {
				logger.trace("Non qualifier annotation found {}", annot.getSimpleName());
				Class<? extends Annotation> annotation = (Class<? extends Annotation>) annot.loadClass();
				ScanAnnotation scanAnnotation = annotation.getAnnotation(ScanAnnotation.class);
				scanResult.addAnnotationToScan(annotation, scanAnnotation);

			} else if (annot.hasAnnotation(QUALIFIER_ANNOTATION)) {
				logger.trace("Qualifier annotation found {}", annot.getSimpleName());
				Class<? extends Annotation> annotation = (Class<? extends Annotation>) annot.loadClass();
				scanResult.addQualifierToScan(annotation);
			}
		});

		logger.debug("Annotation scan complete");

		Set<String> qualifierAnnotations = scanResult.getQualifierAnnotations().parallelStream().map(a -> a.getName())
				.collect(Collectors.toSet());
		Set<String> otherAnnotations = scanResult.getAnnotationsToScan().keySet().parallelStream().map(e -> e.getName())
				.collect(Collectors.toSet());

		logger.info("All @Qualifier annotations: {}", qualifierAnnotations);
		logger.info("All Non @Qualifier annotations to scan: {}", otherAnnotations);

		return Tuple.of(qualifierAnnotations, otherAnnotations);
	}

	private void scanAnnotations(Results scanResult, ScanResult results, Set<String> qualifierAnnotations,
			Set<String> otherAnnotations) {

		logger.debug("Begining scan for annotation beans");
		Set<ClassInfo> allClasses = Sets.newHashSet(results.getAllClasses());

		allClasses.parallelStream().forEach(cls -> {
			logger.trace("Scanning {} for annotations", cls.getSimpleName());
			DIBeanImpl<?> bean = createClassBean(cls, qualifierAnnotations, otherAnnotations, false);

			if (bean != null) {
				logger.debug("Adding {} as annotation bean", bean);
				scanResult.addAnnotationBean(bean);
				helper.beanCreated(bean, scanResult);
			}

			// Scanning constructors
			cls.getConstructorInfo().parallelStream().forEach(cons -> {
				logger.trace("Scanning constructor {} for @Inject annotation", cons.getName());
				if (cons.hasAnnotation(Inject.class.getName())) {
					DIBeanImpl<?> clsBean = createClassBean(cls, qualifierAnnotations, otherAnnotations, true);

					// Special case. If constructor is annotated with inject, add Inject annotation in its non qualifier list
					clsBean.getNonQualifierAnnotations().add(Inject.class);

					logger.debug("Found constructor {} with @Inject annotation. Adding bean {} as annotation bean",
							cons.getName(), clsBean);
					scanResult.addAnnotationBean(clsBean);
					helper.beanCreated(clsBean, scanResult);
				}
			});
			logger.trace("Constructors scan complete");

			// Scanning mehods
			cls.getMethodInfo().parallelStream().forEach(meth -> {
				logger.trace("Scanning method {} for annotations", meth.getName());
				DIBeanImpl<?> methBean = createMethodBean(meth, qualifierAnnotations, otherAnnotations, false);

				if (methBean != null) {
					logger.debug("Found method {} with interested annotation(s). Created bean {}", meth.getName(),
							methBean);
					scanResult.addAnnotationBean(methBean);
					helper.beanCreated(methBean, scanResult);
				}
			});
			logger.trace("Methods scan complete");

		});

		scanResult.addAnnotationBean(managerBean);

		logger.debug("Scan for annotation beans complete");

		logger.info("All annotation beans created: {}", scanResult.getAnnotationBeans());
	}

	private void scanInterfaces(Results scanResult, ScanResult results, Set<String> qualifierAnnotations,
			Set<String> otherAnnotations) {

		logger.debug("Begining scan for interfaces");
		results.getAllInterfaces().parallelStream().filter(cls -> cls.hasAnnotation(SCAN_INTERFACE))
				.map(cls -> cls.loadClass()).forEach(cls -> {
					logger.trace("ScanInterface {} found", cls.getSimpleName());
					scanResult.addnterfaceToScan(cls, cls.getAnnotation(ScanInterface.class));
				});
		logger.debug("Scan for interfaces complete");

		logger.info("All interfaces: {}", scanResult.getInterfacesToScan());

		logger.debug("Begining scan for interface beans");
		scanResult.getInterfacesToScan().keySet().parallelStream().forEach(item -> {
			logger.trace("Scanning for classes implementing {} interface", item.getSimpleName());

			results.getClassesImplementing(item.getName()).parallelStream().map(clsInfo -> {
				DIBeanImpl<?> bean = createClassBean(clsInfo, qualifierAnnotations, otherAnnotations, true);
				logger.debug("Found class {} implementing {}", clsInfo.getSimpleName(), item.getSimpleName());
				return bean;
			}).forEach(bean -> {
				scanResult.addInterfaceBean(bean);
				helper.beanCreated(bean, scanResult);
			});
		});

		logger.debug("Scan for interface beans complete");

		logger.info("All interface beans created: {}", scanResult.getInterfaceBeans());
	}

	private void scanSubclasses(Results scanResult, ScanResult results, Set<String> qualifierAnnotations,
			Set<String> otherAnnotations) {

		logger.debug("Begining scan for super classes");
		results.getAllClasses().parallelStream().filter(cls -> cls.hasAnnotation(SCAN_SUBCLASS))
				.map(cls -> cls.loadClass()).forEach(cls -> {
					logger.trace("ScanSubClass {} found", cls.getSimpleName());
					scanResult.addSuperClassToScan(cls, cls.getAnnotation(ScanSubClass.class));
				});
		logger.debug("Scan for super classes complete");

		logger.debug("Begining scan for subclass beans");
		scanResult.getSuperClassesToScan().keySet().parallelStream().forEach(item -> {
			logger.trace("Scanning for classes extending {}", item.getSimpleName());

			results.getSubclasses(item.getName()).parallelStream().map(clsInfo -> {
				DIBeanImpl<?> bean = createClassBean(clsInfo, qualifierAnnotations, otherAnnotations, true);
				logger.debug("Found class {} extending {}", clsInfo.getSimpleName(), item.getSimpleName());
				return bean;
			}).forEach(bean -> {
				scanResult.addSubclassBean(bean);
				helper.beanCreated(bean, scanResult);
			});
		});

		logger.debug("Scan for subclass beans complete");

		logger.info("All subclass beans scanned: {}", scanResult.getSubclassBeans());
	}

	// private boolean forceCreateClassBean(ClassInfo clsInfo) {
	// 	return clsInfo.hasAnnotation(SINGLETON_ANNOTATION) || clsInfo.hasAnnotation(INJECT_ANNOTATION);
	// }

	// private boolean forceCreateMeathodBean(MethodInfo methInfo) {
	// 	return methInfo.hasAnnotation(SINGLETON_ANNOTATION) || methInfo.hasAnnotation(INJECT_ANNOTATION)
	// 			|| methInfo.hasAnnotation(POSTCONSTRUCT_ANNOTATION);
	// }

	@SuppressWarnings("unchecked")
	private DIBeanImpl<?> createClassBean(ClassInfo cls, Set<String> qualifierAnnotations, Set<String> otherAnnotations,
			boolean forceCreate) {

		logger.trace("Creating bean for class {}", cls);
		ClassInfo qualifierAnnot = getAnnotation(cls.getAnnotations(), qualifierAnnotations);

		Set<ClassInfo> otherAnnots = getAnnotationsSet(cls.getAnnotations(), otherAnnotations);

		if (qualifierAnnot != null || !otherAnnots.isEmpty() || forceCreate) {
			if (logger.isTraceEnabled()) {
				logger.trace("Qualifier annotation: {}, non qualifier annotaions: {}, forceCreate: {}",
						qualifierAnnot == null ? "null" : qualifierAnnot.getSimpleName(),
						Lists.newArrayList(otherAnnots.iterator()).toArray(), forceCreate);
			}
			Class<? extends Annotation> q = qualifierAnnot != null
					? (Class<? extends Annotation>) qualifierAnnot.loadClass()
					: NoQualifier.class;

			Set<Class<? extends Annotation>> o = otherAnnots.isEmpty() ? new HashSet<>()
					: otherAnnots.parallelStream().map(oa -> (Class<? extends Annotation>) oa.loadClass())
							.collect(Collectors.toSet());

			Class<?> loadedClass = cls.loadClass();
			Constructor<?>[] constructors = loadedClass.getDeclaredConstructors();
			Constructor<?> toUse;

			if (constructors.length == 1) {
				toUse = constructors[0];
			} else {
				toUse = Sets.newHashSet(loadedClass.getDeclaredConstructors()).parallelStream()
						.filter(c -> c.isAnnotationPresent(Inject.class)).findAny().orElse(null);

				if (toUse == null) {
					throw new NoValidConstructorFoundException(loadedClass);
				}
			}

			logger.trace("Using constructor: {}", toUse.getName());
			toUse.setAccessible(true);
			return new DIBeanImpl<>(toUse, q, o, cls.hasAnnotation(SINGLETON_ANNOTATION));
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private DIBeanImpl<?> createMethodBean(MethodInfo meth, Set<String> qualifierAnnotations,
			Set<String> otherAnnotations, boolean forceCreate) {

		logger.trace("Creating bean for method {}", meth);
		Set<ClassInfo> annotations = meth.getAnnotationInfo().parallelStream().map(a -> a.getClassInfo())
				.collect(Collectors.toSet());

		ClassInfo qualifierAnnot = getAnnotation(annotations, qualifierAnnotations);

		Set<ClassInfo> otherAnnots = getAnnotationsSet(annotations, otherAnnotations);

		if (qualifierAnnot != null || !otherAnnots.isEmpty() || forceCreate) {
			if (logger.isTraceEnabled()) {
				logger.trace("Qualifier annotation: {}, non qualifier annotaions: {}, forceCreate: {}",
						qualifierAnnot == null ? "null" : qualifierAnnot.getSimpleName(),
						Lists.newArrayList(otherAnnots.iterator()).toArray(), forceCreate);
			}
			Class<? extends Annotation> q = qualifierAnnot != null
					? (Class<? extends Annotation>) qualifierAnnot.loadClass()
					: NoQualifier.class;

			Set<Class<? extends Annotation>> o = otherAnnots.isEmpty() ? new HashSet<>()
					: otherAnnots.parallelStream().map(oa -> (Class<? extends Annotation>) oa.loadClass())
							.collect(Collectors.toSet());

			Method m = meth.loadClassAndGetMethod();
			m.setAccessible(true);
			return new DIBeanImpl<>(m, q, o, meth.hasAnnotation(SINGLETON_ANNOTATION));
		}
		return null;
	}

	private Set<ClassInfo> getAnnotationsSet(Collection<ClassInfo> annotations, Set<String> annotationsToSearch) {

		return annotations.parallelStream().filter(a -> annotationsToSearch.contains(a.getName()))
				.collect(Collectors.toSet());

	}

	private ClassInfo getAnnotation(Collection<ClassInfo> annotations, Set<String> annotationsToSearch) {

		return annotations.parallelStream().filter(a -> annotationsToSearch.contains(a.getName())).findAny()
				.orElse(null);

	}

}