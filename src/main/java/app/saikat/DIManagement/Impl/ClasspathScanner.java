package app.saikat.DIManagement.Impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.Generate;
import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.Annotations.DIManagement.Provides;
import app.saikat.Annotations.DIManagement.ScanAnnotation;
import app.saikat.Annotations.DIManagement.ScanInterface;
import app.saikat.Annotations.DIManagement.ScanSubClass;
import app.saikat.DIManagement.Exceptions.NoValidConstructorFoundException;
import app.saikat.DIManagement.Impl.BeanManagers.GeneratorBeanManager;
import app.saikat.DIManagement.Impl.BeanManagers.InjectBeanManager;
import app.saikat.DIManagement.Impl.BeanManagers.PostConstructBeanManager;
import app.saikat.DIManagement.Impl.BeanManagers.ProvidesBeanManager;
import app.saikat.DIManagement.Impl.BeanManagers.SingletonBeanManager;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIBeanType;
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
	private final Results scanResult;

	public ClasspathScanner(Results results, DIBeanManagerHelper helper) {
		this.helper = helper;
		this.scanResult = results;
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public void scan(DIManagerImpl manager, String... packagesToScan) {
		try (ScanResult results = new ClassGraph().enableClassInfo().enableMethodInfo().ignoreMethodVisibility()
				.ignoreClassVisibility().enableAnnotationInfo().whitelistPackages(packagesToScan).scan()) {

			logger.debug("Total classes: {}", results.getAllClasses().size());
			Tuple<Set<String>, Set<String>> metaAnnotations = scanMetaAnnotations(results);

			Set<Class<? extends DIBeanManager>> beanManagers = results
					.getClassesImplementing(DIBeanManager.class.getName()).parallelStream()
					.map(c -> (Class<? extends DIBeanManager>) c.loadClass()).collect(Collectors.toSet());

			logger.debug("Bean Managers: {}", beanManagers);
			helper.createBeanManagers(beanManagers);

			TypeToken<DIManagerImpl> managerProviderToken = new TypeToken<DIManagerImpl>() {};
			ConstantProviderBean<DIManagerImpl> managerBean = new ConstantProviderBean<>(managerProviderToken, NoQualifier.class);
			managerBean.setProvider(() -> manager);

			scanResult.addGeneratedBean(managerBean);

			scanAnnotations(results, metaAnnotations.first, metaAnnotations.second);
			scanInterfaces(results, metaAnnotations.first, metaAnnotations.second);
			scanSubclasses(results, metaAnnotations.first, metaAnnotations.second);

		}
	}

	@SuppressWarnings("unchecked")
	private Tuple<Set<String>, Set<String>> scanMetaAnnotations(ScanResult results) {
		// Scan for annotations
		Set<ClassInfo> allAnnotations = Sets.newHashSet(results.getAllAnnotations());

		Function<Class<? extends DIBeanManager>, ScanAnnotation> scanAnnotationGenerator = cls -> new ScanAnnotation() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return ScanAnnotation.class;
			}

			@Override
			public Class<?>[] beanManager() {
				return new Class<?>[] { cls };
			}
		};

		scanResult.addAnnotationToScan(Singleton.class, scanAnnotationGenerator.apply(SingletonBeanManager.class));
		scanResult.addAnnotationToScan(PostConstruct.class,
				scanAnnotationGenerator.apply(PostConstructBeanManager.class));
		scanResult.addAnnotationToScan(Inject.class, scanAnnotationGenerator.apply(InjectBeanManager.class));
		scanResult.addAnnotationToScan(Provides.class, scanAnnotationGenerator.apply(ProvidesBeanManager.class));
		scanResult.addAnnotationToScan(Generate.class, scanAnnotationGenerator.apply(GeneratorBeanManager.class));

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

	@SuppressWarnings("unchecked")
	private void scanAnnotations(ScanResult results, Set<String> qualifierAnnotations, Set<String> otherAnnotations) {

		logger.debug("Begining scan for annotation beans");
		Set<ClassInfo> allClasses = Sets.newHashSet(results.getAllClasses());

		allClasses.parallelStream().forEach(cls -> {
			logger.trace("Scanning {} for annotations", cls.getSimpleName());
			Set<DIBeanImpl<?>> consBeans = new HashSet<>();

			// Scanning mehods
			cls.getMethodAndConstructorInfo().parallelStream().forEach(meth -> {
				logger.trace("Scanning method {} for annotations", meth.getName());
				DIBeanImpl<?> methBean = createMethodBean(meth, qualifierAnnotations, otherAnnotations, null, false,
						DIBeanType.ANNOTATION);

				if (methBean != null) {
					logger.debug("Adding method {} as annotation bean with beanManager: {}", meth.getName(), methBean,
							methBean.getBeanManager().getClass().getSimpleName());

					methBean.getBeanManager().beanCreated(methBean);
					if (meth.isConstructor()) {
						consBeans.add(methBean);
					}
				}
			});
			logger.trace("Methods scan complete");

			ClassInfo otherAnnot = getAnnotation(cls.getAnnotations(), otherAnnotations);
			if (otherAnnot != null) {
				Class<? extends Annotation> o = (Class<? extends Annotation>) otherAnnot.loadClass();
				if (o.equals(Singleton.class)) {
					logger.debug("Class {} is marked singleton", cls);

					if (consBeans.size() == 0) {
						logger.debug("No constructor bean already exists. Creating one");
						DIBeanImpl<?> bean = createClassBean(cls, qualifierAnnotations, otherAnnotations, Singleton.class, true,
								DIBeanType.ANNOTATION);

							logger.debug("Adding {} as annotation bean with beanManager: {}", bean,
									bean.getBeanManager().getClass().getSimpleName());
						bean.getBeanManager().beanCreated(bean);
					} else {
						logger.debug("{} constructors found. Setting them singleton", consBeans);
						consBeans.parallelStream().forEach(b -> b.setSingleton(true));
					}
				} else {
					DIBeanImpl<?> bean = createClassBean(cls, qualifierAnnotations, otherAnnotations, null, false,
							DIBeanType.ANNOTATION);

					if (bean != null) {
						logger.debug("Adding {} as annotation bean with beanManager: {}", bean,
								bean.getBeanManager().getClass().getSimpleName());
						bean.getBeanManager().beanCreated(bean);
					}
				}
			}

		});

		logger.debug("Scan for annotation beans complete");

		logger.info("All annotation beans created: {}", scanResult.getAnnotationBeans());
	}

	private void scanInterfaces(ScanResult results, Set<String> qualifierAnnotations, Set<String> otherAnnotations) {

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
				DIBeanImpl<?> bean = createClassBean(clsInfo, qualifierAnnotations, otherAnnotations, item, true,
						DIBeanType.INTERFACE);
				logger.debug("Found class {} implementing {}", clsInfo.getSimpleName(), item.getSimpleName());
				logger.debug("Adding {} as interface bean with beanManager: {}", bean,
						bean.getBeanManager().getClass().getSimpleName());
				return bean;
			}).forEach(b -> b.getBeanManager().beanCreated(b));
		});

		logger.debug("Scan for interface beans complete");

		logger.info("All interface beans created: {}", scanResult.getInterfaceBeans());
	}

	private void scanSubclasses(ScanResult results, Set<String> qualifierAnnotations, Set<String> otherAnnotations) {

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
				DIBeanImpl<?> bean = createClassBean(clsInfo, qualifierAnnotations, otherAnnotations, item, true,
						DIBeanType.SUBCLASS);
				logger.debug("Found class {} extending {}", clsInfo.getSimpleName(), item.getSimpleName());
				logger.debug("Adding {} as subclass bean with beanManager: {}", bean,
						bean.getBeanManager().getClass().getSimpleName());
				return bean;
			}).forEach(b -> b.getBeanManager().beanCreated(b));
		});

		logger.debug("Scan for subclass beans complete");

		logger.info("All subclass beans scanned: {}", scanResult.getSubclassBeans());
	}

	@SuppressWarnings("unchecked")
	private DIBeanImpl<?> createClassBean(ClassInfo cls, Set<String> qualifierAnnotations, Set<String> otherAnnotations,
			Class<?> superClass, boolean forceCreate, DIBeanType type) {

		logger.trace("Creating bean for class {}", cls);
		ClassInfo qualifierAnnot = getAnnotation(cls.getAnnotations(), qualifierAnnotations);

		ClassInfo otherAnnots = getAnnotation(cls.getAnnotations(), otherAnnotations);

		if (qualifierAnnot != null || otherAnnots != null || forceCreate) {
			if (logger.isTraceEnabled()) {
				logger.trace("Qualifier annotation: {}, non qualifier annotaion: {}, forceCreate: {}",
						qualifierAnnot == null ? "null" : qualifierAnnot.getSimpleName(),
						otherAnnots == null ? "null" : otherAnnots.getSimpleName(), forceCreate);
			}
			Class<? extends Annotation> q = qualifierAnnot != null
					? (Class<? extends Annotation>) qualifierAnnot.loadClass()
					: NoQualifier.class;

			Class<? extends Annotation> o = otherAnnots != null ? (Class<? extends Annotation>) otherAnnots.loadClass()
					: null;

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

			DIBeanManager beanManager = helper.getManagerOf(o);
			return new DIBeanImpl<>(toUse, q, o, superClass, cls.hasAnnotation(SINGLETON_ANNOTATION), beanManager,
					type);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private DIBeanImpl<?> createMethodBean(MethodInfo meth, Set<String> qualifierAnnotations,
			Set<String> otherAnnotations, Class<?> superclass, boolean forceCreate, DIBeanType type) {

		logger.trace("Creating bean for method {}", meth);
		Set<ClassInfo> annotations = meth.getAnnotationInfo().parallelStream().map(a -> a.getClassInfo())
				.collect(Collectors.toSet());

		ClassInfo qualifierAnnot = getAnnotation(annotations, qualifierAnnotations);

		ClassInfo otherAnnots = getAnnotation(annotations, otherAnnotations);

		if (qualifierAnnot != null || otherAnnots != null || forceCreate) {
			if (logger.isTraceEnabled()) {
				logger.trace("Qualifier annotation: {}, non qualifier annotaions: {}, forceCreate: {}",
						qualifierAnnot == null ? "null" : qualifierAnnot.getSimpleName(),
						otherAnnots == null ? "null" : otherAnnots.getSimpleName(), forceCreate);
			}
			Class<? extends Annotation> q = qualifierAnnot != null
					? (Class<? extends Annotation>) qualifierAnnot.loadClass()
					: NoQualifier.class;

			Class<? extends Annotation> o = otherAnnots != null ? (Class<? extends Annotation>) otherAnnots.loadClass()
					: null;

			if (meth.isConstructor()) {
				try {
					Class<?> cls = meth.getClassInfo().loadClass();
					Constructor<?>[] cons = cls.getConstructors();

					boolean isUnique = true;
					Constructor<?> c = null;

					for (Constructor<?> constructor : cons) {
						BiFunction<Class<? extends Annotation>, Set<Class<? extends Annotation>>, Boolean> hasAnnot = (
								annot, set) -> {
							if (annot == null) {
								return getAnnotation(Sets.newHashSet(constructor.getAnnotations()), set) == null;
							} else {
								return constructor.getAnnotation(annot) != null;
							}
						};

						if (hasAnnot.apply(o, scanResult.getAnnotationsToScan().keySet())
								&& (hasAnnot.apply(q, scanResult.getQualifierAnnotations())
										|| q.equals(NoQualifier.class))) {
							if (c == null) {
								c = constructor;
							} else {
								isUnique = false;
								break;
							}
						}
					}

					if (!isUnique || c == null) {
						throw new RuntimeException((isUnique ? "No" : "Multiple") + " constructor found with qualifier "
								+ qualifierAnnot + " and non-qualifier " + otherAnnots.getSimpleName());
					}

					c.setAccessible(true);

					DIBeanManager beanManager = helper.getManagerOf(o);
					return new DIBeanImpl<>(c, q, o, superclass, meth.hasAnnotation(SINGLETON_ANNOTATION), beanManager,
							type);

				} catch (Exception e) {
					logger.error("Error:", e);
					return null;
				}
			}

			Method m = meth.loadClassAndGetMethod();
			m.setAccessible(true);

			DIBeanManager beanManager = helper.getManagerOf(o);
			DIBeanImpl<?> bean = new DIBeanImpl<>(m, q, o, superclass, meth.hasAnnotation(SINGLETON_ANNOTATION),
					beanManager, type);
			return bean;
		}
		return null;
	}

	private ClassInfo getAnnotation(Collection<ClassInfo> annotations, Set<String> annotationsToSearch) {

		return annotations.parallelStream().filter(a -> annotationsToSearch.contains(a.getName())).findAny()
				.orElse(null);

	}

	private Class<? extends Annotation> getAnnotation(Collection<Annotation> annotations,
			Collection<Class<? extends Annotation>> annotationsToSearch) {

		return annotations.parallelStream().filter(a -> annotationsToSearch.contains(a.annotationType()))
				.map(a -> a.annotationType()).findAny().orElse(null);

	}

}