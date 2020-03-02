package app.saikat.DIManagement.Impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.google.common.collect.Sets;
import com.google.common.collect.Streams;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.Annotations.DIManagement.Scan;
import app.saikat.DIManagement.Exceptions.NoManagerFoundException;
import app.saikat.DIManagement.Exceptions.NoValidConstructorFoundException;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Impl.Repository.Repository;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIBeanType;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

public class ClasspathScanner {

	private Logger logger = LogManager.getLogger(this.getClass());
	private final String SCAN = Scan.class.getName();
	private final String QUALIFIER_ANNOTATION = Qualifier.class.getName();
	private final String SINGLETON_ANNOTATION = Singleton.class.getName();

	@SuppressWarnings("unchecked")
	public void scan(Repository currentRepo, Repository globalRepo, String... packagesToScan) {
		try (ScanResult scanResult = new ClassGraph().enableClassInfo()
				.enableMethodInfo()
				.ignoreMethodVisibility()
				.ignoreClassVisibility()
				.enableAnnotationInfo()
				.whitelistPackages(packagesToScan)
				.scan()) {

			logger.debug("Total classes: {}", scanResult.getAllClasses()
					.size());

			// Create DIBeanManagers first
			Set<Class<? extends DIBeanManager>> beanManagers = scanResult.getSubclasses(DIBeanManager.class.getName())
					.parallelStream()
					.map(c -> (Class<? extends DIBeanManager>) c.loadClass())
					.filter(c -> !globalRepo.getBeanManagers()
							.keySet()
							.contains(c))
					.collect(Collectors.toSet());

			logger.debug("Bean Managers: {}", beanManagers);

			// Add BeanManagers to currentRepo and set their Repo pointer
			currentRepo.addBeanManagers(DIBeanManagerHelper.createBeanManagers(beanManagers));
			currentRepo.getBeanManagers()
					.values()
					.parallelStream()
					.forEach(bm -> bm.setRepo(currentRepo));

			// Add annotations, interfaces, subclasses to scan as given by beanManager created by this scan cycle
			currentRepo.getBeanManagers()
					.entrySet()
					.parallelStream()
					.forEach(entry -> currentRepo.addScanMap(entry.getValue()
							.addToScan()));


			// Scan for other annotations
			scanForScanAnnotations(scanResult, currentRepo);

			Set<Class<?>> toBeScanned = Streams.concat(currentRepo.getScanData()
					.keySet()
					.parallelStream(), globalRepo.getScanData()
							.keySet()
							.parallelStream())
					.collect(Collectors.toSet());

			logger.info("All @Qualifier annotations: {}", currentRepo.getQualifierAnnotations());
			logger.info("All Non @Qualifier annotations to scan: {}", currentRepo.getScanData()
					.keySet());

			Set<String> qualifierAnnotations = Streams.concat(currentRepo.getQualifierAnnotations()
					.parallelStream()
					.map(a -> a.getName()), globalRepo.getQualifierAnnotations()
							.parallelStream()
							.map(a -> a.getName()))
					.collect(Collectors.toSet());

			Set<String> otherAnnotations = toBeScanned.parallelStream()
					.map(s -> s.getName())
					.collect(Collectors.toSet());

			scanAnnotations(scanResult, qualifierAnnotations, otherAnnotations, currentRepo, globalRepo);

			// Scan for interfaces
			scanInterfaces(scanResult, qualifierAnnotations, otherAnnotations, toBeScanned, currentRepo, globalRepo);
			scanSubclasses(scanResult, qualifierAnnotations, otherAnnotations, toBeScanned, currentRepo, globalRepo);

			logger.debug("All created beans: {}", currentRepo.getBeans());
		}
	}

	@SuppressWarnings("unchecked")
	private void scanForScanAnnotations(ScanResult scanResults, Repository repo) {
		logger.info("Scanning for @Scan annotations");
		// Scan for annotations
		Set<ClassInfo> allAnnotations = Sets.newHashSet(scanResults.getAllAnnotations());

		allAnnotations.parallelStream()
				.forEach(annot -> {
					logger.trace("Scanning annotation: {}", annot.getSimpleName());

					if (annot.hasAnnotation(SCAN)) {
						Class<? extends Annotation> annotation = (Class<? extends Annotation>) annot.loadClass();
						Scan scanAnnotation = annotation.getAnnotation(Scan.class);
						repo.addScanData(annotation, scanAnnotation);

					} else if (annot.hasAnnotation(QUALIFIER_ANNOTATION)) {
						Class<? extends Annotation> annotation = (Class<? extends Annotation>) annot.loadClass();
						repo.addQualifierAnnotation(annotation);
					}
				});
		logger.debug("Annotation scan complete.");

		scanResults.getAllInterfaces()
				.parallelStream()
				.filter(cls -> cls.hasAnnotation(SCAN))
				.map(cls -> cls.loadClass())
				.forEach(cls -> repo.addScanData(cls, cls.getAnnotation(Scan.class)));
		logger.debug("interface scan complete");

		scanResults.getAllClasses()
				.parallelStream()
				.filter(cls -> cls.hasAnnotation(SCAN) && !cls.isInterface() && !cls.isAnnotation())
				.map(cls -> cls.loadClass())
				.filter(cls -> cls.getAnnotation(Scan.class) != null && !cls.isInterface() && !cls.isAnnotation())
				.forEach(cls -> {
					logger.debug("Adding scan data for: {}", cls);
					repo.addScanData(cls, cls.getAnnotation(Scan.class));
				});
		logger.debug("Superclass scan complete");
	}

	@SuppressWarnings("unchecked")
	private void scanAnnotations(ScanResult scanResults, Set<String> qualifierAnnotations, Set<String> otherAnnotations,
			Repository currentRepo, Repository globalRepo) {

		Set<ClassInfo> allClasses = Sets.newHashSet(scanResults.getAllClasses());

		allClasses.parallelStream()
				.forEach(cls -> {
					ClassInfo otherAnnot = getAnnotation(cls.getAnnotations(), otherAnnotations);
					// logger.debug("Scanning {}\n for annotations: {}\nother annotations:{}\nother Annot:{}", cls.getSimpleName(), cls.getAnnotations(), otherAnnotations, otherAnnot);
					Set<DIBeanImpl<?>> consBeans = new HashSet<>();

					// Scanning mehods
					cls.getMethodAndConstructorInfo()
							.parallelStream()
							.forEach(meth -> {
								logger.trace("Scanning method {} for annotations", meth.getName());
								DIBeanImpl<?> methBean = createMethodBean(meth, qualifierAnnotations, otherAnnotations, DIBeanType.ANNOTATION, currentRepo, globalRepo);

								if (methBean != null) {
									logger.debug("Adding methodBean {} as with beanManager: {}", methBean, methBean
											.getBeanManager()
											.getClass()
											.getSimpleName());

									methBean.getBeanManager()
											.beanCreated(methBean);
									if (meth.isConstructor()) {
										consBeans.add(methBean);
									}
								}
							});
					logger.trace("Methods scan complete");

					if (otherAnnot != null) {
						Class<? extends Annotation> o = (Class<? extends Annotation>) otherAnnot.loadClass();
						if (o.equals(Singleton.class)) {
							if (consBeans.size() == 0) {
								logger.debug("No constructor bean already exists. Creating one");
								DIBeanImpl<?> bean = createClassBean(cls, qualifierAnnotations, otherAnnotations, Singleton.class, true, DIBeanType.ANNOTATION, currentRepo, globalRepo);

								logger.debug("Adding {} as annotation bean with beanManager: {}", bean, bean
										.getBeanManager()
										.getClass()
										.getSimpleName());

								bean.getBeanManager()
										.beanCreated(bean);
							} else {
								logger.debug("{} constructors found. Setting them singleton", consBeans);
								consBeans.parallelStream()
										.forEach(b -> b.setSingleton(true));
							}
						} else {
							logger.debug("Creating bean for class {} with non qualifier: {}", cls.getSimpleName(), otherAnnot);
							DIBeanImpl<?> bean = createClassBean(cls, qualifierAnnotations, otherAnnotations, null, false, DIBeanType.ANNOTATION, currentRepo, globalRepo);

							if (bean != null) {
								logger.debug("Adding {} as annotation bean with beanManager: {}", bean, bean
										.getBeanManager()
										.getClass()
										.getSimpleName());

								bean.getBeanManager()
										.beanCreated(bean);
							}
						}
					}

				});

		logger.debug("Scan for annotation beans complete");

	}

	private void scanInterfaces(ScanResult scanResults, Set<String> qualifierAnnotations, Set<String> otherAnnotations,
			Set<Class<?>> interfaces, Repository currentRepo, Repository globalRepo) {

		interfaces.parallelStream()
				.filter(i -> i.isInterface() && !i.isAnnotation())
				.forEach(item -> {
					logger.trace("Scanning for classes implementing {} interface", item.getSimpleName());

					scanResults.getClassesImplementing(item.getName())
							.parallelStream()
							.map(clsInfo -> {
								DIBeanImpl<?> bean = createClassBean(clsInfo, qualifierAnnotations, otherAnnotations, item, true, DIBeanType.INTERFACE, currentRepo, globalRepo);
								logger.debug("Found class {} implementing {}", clsInfo.getSimpleName(), item
										.getSimpleName());
								logger.debug("Adding {} as interface bean with beanManager: {}", bean, bean
										.getBeanManager()
										.getClass()
										.getSimpleName());
								return bean;
							})
							.forEach(b -> b.getBeanManager()
									.beanCreated(b));
				});

		logger.debug("Scan for interface beans complete");
	}

	private void scanSubclasses(ScanResult scanResults, Set<String> qualifierAnnotations, Set<String> otherAnnotations,
			Set<Class<?>> superclasses, Repository currentRepo, Repository globalRepo) {

		superclasses.parallelStream()
				.forEach(item -> {
					logger.trace("Scanning for classes extending {}", item.getSimpleName());

					scanResults.getSubclasses(item.getName())
							.parallelStream()
							.map(clsInfo -> {
								DIBeanImpl<?> bean = createClassBean(clsInfo, qualifierAnnotations, otherAnnotations, item, true, DIBeanType.SUBCLASS, currentRepo, globalRepo);
								logger.debug("Found class {} extending {}", clsInfo.getSimpleName(), item
										.getSimpleName());
								logger.debug("Adding {} as subclass bean with beanManager: {}", bean, bean
										.getBeanManager()
										.getClass()
										.getSimpleName());
								return bean;
							})
							.forEach(b -> b.getBeanManager()
									.beanCreated(b));
				});

		logger.debug("Scan for subclass beans complete");
	}

	@SuppressWarnings("unchecked")
	private DIBeanImpl<?> createClassBean(ClassInfo cls, Set<String> qualifierAnnotations, Set<String> otherAnnotations,
			Class<?> superClass, boolean forceCreate, DIBeanType type, Repository curentRepo, Repository globalRepo) {

		logger.trace("Creating bean for class {}", cls);
		ClassInfo qualifierAnnot = getAnnotation(cls.getAnnotations(), qualifierAnnotations);

		ClassInfo otherAnnots = getAnnotation(cls.getAnnotations(), otherAnnotations);

		if (qualifierAnnot != null || otherAnnots != null || forceCreate) {
			if (logger.isTraceEnabled()) {
				logger.trace("Qualifier annotation: {}, non qualifier annotaion: {}, forceCreate: {}", qualifierAnnot == null
						? "null"
						: qualifierAnnot.getSimpleName(), otherAnnots == null ? "null"
								: otherAnnots.getSimpleName(), forceCreate);
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
				toUse = Sets.newHashSet(loadedClass.getDeclaredConstructors())
						.parallelStream()
						.filter(c -> c.isAnnotationPresent(Inject.class))
						.findAny()
						.orElse(null);

				if (toUse == null) {
					throw new NoValidConstructorFoundException(loadedClass);
				}
			}

			logger.trace("Using constructor: {}", toUse.getName());
			toUse.setAccessible(true);

			try {
				DIBeanManager beanManager = DIBeanManagerHelper.getManagerOf(o, curentRepo, globalRepo);
				return new DIBeanImpl<>(toUse, q, o, superClass, cls.hasAnnotation(SINGLETON_ANNOTATION), beanManager,
						type);
			} catch (NoManagerFoundException e) {
				logger.error("Error: ", e);
				return null;
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private DIBeanImpl<?> createMethodBean(MethodInfo meth, Set<String> qualifierAnnotations,
			Set<String> otherAnnotations, DIBeanType type, Repository curentRepo, Repository globalRepo) {

		logger.trace("Creating bean for method {}", meth);
		Set<ClassInfo> annotations = meth.getAnnotationInfo()
				.parallelStream()
				.map(a -> a.getClassInfo())
				.collect(Collectors.toSet());

		ClassInfo qualifierAnnot = getAnnotation(annotations, qualifierAnnotations);

		ClassInfo otherAnnots = getAnnotation(annotations, otherAnnotations);

		if (qualifierAnnot != null || otherAnnots != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Qualifier annotation: {}, non qualifier annotaions: {}", qualifierAnnot == null ? "null"
						: qualifierAnnot.getSimpleName(), otherAnnots == null ? "null" : otherAnnots.getSimpleName());
			}
			Class<? extends Annotation> q = qualifierAnnot != null
					? (Class<? extends Annotation>) qualifierAnnot.loadClass()
					: NoQualifier.class;

			Class<? extends Annotation> o = otherAnnots != null ? (Class<? extends Annotation>) otherAnnots.loadClass()
					: null;

			if (meth.isConstructor()) {
				try {
					Class<?> cls = meth.getClassInfo()
							.loadClass();
					Constructor<?>[] cons = cls.getConstructors();

					boolean isUnique = true;
					Constructor<?> c = null;

					for (Constructor<?> constructor : cons) {
						BiFunction<Class<? extends Annotation>, Set<Class<?>>, Boolean> hasAnnot = (annot, set) -> {
							if (annot == null) {
								return getAnnotation(Sets.newHashSet(constructor.getAnnotations()), (Set) set) == null;
							} else {
								return constructor.getAnnotation(annot) != null;
							}
						};

						if ((hasAnnot.apply(o, curentRepo.getScanData()
								.keySet()) || hasAnnot
										.apply(o, globalRepo.getScanData()
												.keySet()))
								&& ((hasAnnot.apply(q, (Set) curentRepo.getQualifierAnnotations())
										|| hasAnnot.apply(o, (Set) globalRepo.getQualifierAnnotations()))
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

					DIBeanManager beanManager = DIBeanManagerHelper.getManagerOf(o, curentRepo, globalRepo);
					return new DIBeanImpl<>(c, q, o, null, meth.hasAnnotation(SINGLETON_ANNOTATION), beanManager, type);

				} catch (Exception e) {
					logger.error("Error:", e);
					return null;
				}
			}

			Method m = meth.loadClassAndGetMethod();
			m.setAccessible(true);

			try {
				DIBeanManager beanManager = DIBeanManagerHelper.getManagerOf(o, curentRepo, globalRepo);
				return new DIBeanImpl<>(m, q, o, null, meth.hasAnnotation(SINGLETON_ANNOTATION), beanManager, type);

			} catch (NoManagerFoundException e) {
				logger.error("Error: ", e);
				return null;
			}
		}
		return null;
	}

	private ClassInfo getAnnotation(Collection<ClassInfo> annotations, Set<String> annotationsToSearch) {

		return annotations.parallelStream()
				.filter(a -> annotationsToSearch.contains(a.getName()))
				.findAny()
				.orElse(null);

	}

	private Class<? extends Annotation> getAnnotation(Collection<Annotation> annotations,
			Collection<Class<? extends Annotation>> annotationsToSearch) {

		return annotations.parallelStream()
				.filter(a -> annotationsToSearch.contains(a.annotationType()))
				.map(a -> a.annotationType())
				.findAny()
				.orElse(null);

	}

}