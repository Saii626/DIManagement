package app.saikat.DIManagement.Impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.google.common.collect.Sets;

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
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIBeanType;
import app.saikat.DIManagement.Interfaces.Results;
import app.saikat.PojoCollections.CommonObjects.Tuple;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.MethodParameterInfo;
import io.github.classgraph.ScanResult;
import io.github.classgraph.TypeSignature;

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

	@SuppressWarnings("unchecked")
	public void scan(String... packagesToScan) {
		try (ScanResult results = new ClassGraph().enableClassInfo().enableMethodInfo().ignoreMethodVisibility()
				.ignoreClassVisibility().enableAnnotationInfo().whitelistPackages(packagesToScan).scan()) {

			logger.debug("Total classes: {}", results.getAllClasses().size());
			Tuple<Set<String>, Set<String>> metaAnnotations = scanMetaAnnotations(results);

			Set<Class<? extends DIBeanManager>> beanManagers = results.getSubclasses(DIBeanManager.class.getName())
					.parallelStream().map(c -> (Class<? extends DIBeanManager>) c.loadClass())
					.collect(Collectors.toSet());
			beanManagers.add(DIBeanManager.class);

			logger.debug("Bean Managers: {}", beanManagers);
			helper.createBeanManagers(beanManagers);

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

	private void scanAnnotations(ScanResult results, Set<String> qualifierAnnotations, Set<String> otherAnnotations) {

		logger.info("Begining scan for annotation beans");
		Set<ClassInfo> allClasses = Sets.newHashSet(results.getAllClasses());

		allClasses.parallelStream().forEach(cls -> {
			logger.trace("Scanning {} for annotations", cls.getSimpleName());
			DIBeanImpl<?> bean = createClassBean(cls, qualifierAnnotations, otherAnnotations, false, null,
					DIBeanType.ANNOTATION);

			if (bean != null) {
				logger.debug("Adding {} as annotation bean", bean);
				helper.beanCreated(bean, bean.getNonQualifierAnnotation());
			}

			// Scanning constructors
			cls.getConstructorInfo().parallelStream().forEach(cons -> {
				logger.trace("Scanning constructor {} for @Inject annotation", cons.getName());
				if (cons.hasAnnotation(Inject.class.getName())) {
					DIBeanImpl<?> clsBean = createClassBean(cls, qualifierAnnotations, otherAnnotations, true,
							Inject.class, DIBeanType.ANNOTATION);

					logger.debug("Found constructor {} with @Inject annotation. Adding bean {} as annotation bean",
							cons.getName(), clsBean);
					helper.beanCreated(clsBean, clsBean.getNonQualifierAnnotation());
				} else {
					DIBeanImpl<?> clsBean = createMethodBean(cons, qualifierAnnotations, otherAnnotations, false,
							DIBeanType.ANNOTATION);

					if (clsBean != null) {
						logger.debug("Found interested constructor {}. Adding bean {} as annotation bean",
								cons.getName(), clsBean);
						helper.beanCreated(clsBean, clsBean.getNonQualifierAnnotation());
					}
				}
			});
			logger.trace("Constructors scan complete");

			// Scanning mehods
			cls.getMethodInfo().parallelStream().forEach(meth -> {
				logger.trace("Scanning method {} for annotations", meth.getName());
				DIBeanImpl<?> methBean = createMethodBean(meth, qualifierAnnotations, otherAnnotations, false,
						DIBeanType.ANNOTATION);

				if (methBean != null) {
					logger.debug("Found method {} with interested annotation(s). Created bean {}", meth.getName(),
							methBean);
					helper.beanCreated(methBean, methBean.getNonQualifierAnnotation());
				}
			});
			logger.trace("Methods scan complete");

		});

		logger.debug("Scan for annotation beans complete");

		logger.info("All annotation beans created: {}", scanResult.getAnnotationBeans());
	}

	private void scanInterfaces(ScanResult results, Set<String> qualifierAnnotations, Set<String> otherAnnotations) {

		logger.info("Begining scan for interfaces");
		results.getAllInterfaces().parallelStream().filter(cls -> cls.hasAnnotation(SCAN_INTERFACE))
				.map(cls -> cls.loadClass()).forEach(cls -> {
					logger.trace("ScanInterface {} found", cls.getSimpleName());
					scanResult.addnterfaceToScan(cls, cls.getAnnotation(ScanInterface.class));
				});
		logger.debug("Scan for interfaces complete");

		logger.info("All interfaces: {}", scanResult.getInterfacesToScan());

		logger.info("Begining scan for interface beans");
		scanResult.getInterfacesToScan().keySet().parallelStream().forEach(item -> {
			logger.trace("Scanning for classes implementing {} interface", item.getSimpleName());

			results.getClassesImplementing(item.getName()).parallelStream().map(clsInfo -> {
				DIBeanImpl<?> bean = createClassBean(clsInfo, qualifierAnnotations, otherAnnotations, true, null,
						DIBeanType.INTERFACE);
				logger.debug("Found class {} implementing {}", clsInfo.getSimpleName(), item.getSimpleName());
				return bean;
			}).forEach(b -> helper.beanCreated(b, item));
		});

		logger.debug("Scan for interface beans complete");

		logger.info("All interface beans created: {}", scanResult.getInterfaceBeans());
	}

	private void scanSubclasses(ScanResult results, Set<String> qualifierAnnotations, Set<String> otherAnnotations) {

		logger.info("Begining scan for super classes");
		results.getAllClasses().parallelStream().filter(cls -> cls.hasAnnotation(SCAN_SUBCLASS))
				.map(cls -> cls.loadClass()).forEach(cls -> {
					logger.trace("ScanSubClass {} found", cls.getSimpleName());
					scanResult.addSuperClassToScan(cls, cls.getAnnotation(ScanSubClass.class));
				});
		logger.debug("Scan for super classes complete");

		logger.info("Begining scan for subclass beans");
		scanResult.getSuperClassesToScan().keySet().parallelStream().forEach(item -> {
			logger.trace("Scanning for classes extending {}", item.getSimpleName());

			results.getSubclasses(item.getName()).parallelStream().map(clsInfo -> {
				DIBeanImpl<?> bean = createClassBean(clsInfo, qualifierAnnotations, otherAnnotations, true, null,
						DIBeanType.SUBCLASS);
				logger.debug("Found class {} extending {}", clsInfo.getSimpleName(), item.getSimpleName());
				return bean;
			}).forEach(b -> helper.beanCreated(b, item));
		});

		logger.debug("Scan for subclass beans complete");

		logger.info("All subclass beans scanned: {}", scanResult.getSubclassBeans());
	}

	@SuppressWarnings("unchecked")
	private DIBeanImpl<?> createClassBean(ClassInfo cls, Set<String> qualifierAnnotations, Set<String> otherAnnotations,
			boolean forceCreate, Class<? extends Annotation> defaultNonQualifier, DIBeanType type) {

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
					: defaultNonQualifier;

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
			return new DIBeanImpl<>(toUse, q, o, cls.hasAnnotation(SINGLETON_ANNOTATION), type);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private DIBeanImpl<?> createMethodBean(MethodInfo meth, Set<String> qualifierAnnotations,
			Set<String> otherAnnotations, boolean forceCreate, DIBeanType type) {

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
					MethodParameterInfo[] allParameterInfo = meth.getParameterInfo();
					final List<Class<?>> parameterClasses = new ArrayList<>(allParameterInfo.length);
					for (final MethodParameterInfo mpi : allParameterInfo) {
						final TypeSignature parameterType = mpi.getTypeSignatureOrTypeDescriptor();
						parameterClasses.add(Class.forName(parameterType.toString()));
					}
					final Class<?>[] parameterClassesArr = parameterClasses.toArray(new Class<?>[0]);

					Class<?> cls = meth.getClassInfo().loadClass();
					Constructor<?> cons = cls.getConstructor(parameterClassesArr);

					return new DIBeanImpl<>(cons, q, o, meth.hasAnnotation(SINGLETON_ANNOTATION), type);

				} catch (Exception e) {
				}
			}
			
			Method m = meth.loadClassAndGetMethod();
			m.setAccessible(true);

			List<String> genericParams = new ArrayList<>();
			if (m.getGenericReturnType() instanceof ParameterizedType) {
				Type[] geneticTypes = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments();

				for (Type t : geneticTypes) {
					genericParams.add(t.getTypeName());
				}
			}

			DIBeanImpl<?> bean = new DIBeanImpl<>(m, q, o, meth.hasAnnotation(SINGLETON_ANNOTATION), type);
			bean.getGenericParameters().addAll(genericParams);
			return bean;
		}
		return null;
	}

	private ClassInfo getAnnotation(Collection<ClassInfo> annotations, Set<String> annotationsToSearch) {

		return annotations.parallelStream().filter(a -> annotationsToSearch.contains(a.getName())).findAny()
				.orElse(null);

	}

}