package app.saikat.DIManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import app.saikat.DIManagement.Configurations.AnnotationConfig;
import app.saikat.PojoCollections.CommonObjects.Tuple;
import io.github.classgraph.AnnotationInfo;

class Utils {

	/**
	 * Returns first matching annotation from the list of qualifiers
	 * @param annotations all annotations of a field
	 * @param QUALIFIERS annotations to search
	 * @return first matching annotation from QUALIFIERS if present, else null
	 */
	static Class<? extends Annotation> getQualifierAnnotation(Annotation[] annotations,
			List<Class<? extends Annotation>> QUALIFIERS) {

		for (Annotation annotation : annotations) {
			if (QUALIFIERS.contains(annotation.annotationType())) {
				return annotation.annotationType();
			}
		}
		return null;

	}

	/**
	 * Searches through all constructors to find appropriate constructor for dependency injection
	 * @param constructors all constructors to search
	 * @return appropriate constructor for dependency injection. If only 1 constructor is present,
	 *		 returns that, else searches for @Inject annotated constructor
	 */
	static Constructor<?> getAppropriateConstructor(Constructor<?>[] constructors) {
		Constructor<?> toUse = null;
		if (constructors.length > 1) {
			for (Constructor<?> constructor : constructors) {
				Annotation[] annotations = constructor.getAnnotations();
				for (Annotation a : annotations) {
					if (a.getClass()
							.equals(Inject.class)) {
						toUse = constructor;
						break;
					}
				}
			}
		} else if (constructors.length == 1) {
			toUse = constructors[0];
		}

		return toUse;
	}

	/**
	 * Creates a list of DIBean from list of parameters and their corrosponding annotaions
	 * @param parameters parameters to a function
	 * @param annotations annotations of those parameters
	 * @param QUALIFIERS searchspace of annoations
	 * @return list of DIBeans
	 */
	static List<DIBean> getParameterBeans(Class<?>[] parameters, Annotation[][] annotations,
			List<Class<? extends Annotation>> QUALIFIERS) {

		List<DIBean> beans = new ArrayList<>();

		for (int i = 0; i < parameters.length; i++) {
			beans.add(new DIBean(parameters[i], Utils.getQualifierAnnotation(annotations[i], QUALIFIERS), true));
		}

		return beans;
	}

	/**
	 * Returns the provider bean present in collection
	 * @param beans list of all already scanned beans
	 * @param bean the bean whose provider is required
	 * @return the provider bean foe the given bean
	 */
	static DIBean getProviderBean(Collection<DIBean> beans, DIBean bean) {
		for (DIBean b : beans) {
			if (b.equals(bean)) {
				return b;
			}
		}

		return bean;
	}

	/**
	 * Returns a string representation of collection of beans
	 * @param beans beans to print
	 * @return string representation of beans
	 */
	static String getStringRepresentationOf(Collection<DIBean> beans) {
		if (beans == null)
			return "null";
		if (beans.isEmpty())
			return "none";

		StringBuffer buffer = new StringBuffer("[ ");

		for (DIBean bean : beans) {
			buffer.append(bean.toString());
			buffer.append(", ");
		}

		buffer.delete(buffer.length() - 2, buffer.length());
		buffer.append(" ]");

		return buffer.toString();
	}

	/**
	 * Gets all annotations present in a class or method
	 * @param <T> type of annotation (ClassAnnotationConfig, MethodAnnotationConfig)
	 * @param getAnnotation the function pointer to get AnnotationInfo by giving annotation
	 * @param annotaionsToScan list of AnnotationConfig to search
	 * @return list of AnnotationConfig with its corrosponding AnnotationTnfo
	 */
	static <T extends AnnotationConfig> List<Tuple<T, AnnotationInfo>> getAnnotations(Function<String, AnnotationInfo> getAnnotation,
			Set<T> annotaionsToScan) {

		return annotaionsToScan.stream()
				.map(annotation -> Tuple.of(annotation, getAnnotation.apply(annotation.getAnnotation().getName())))
				.filter(a -> a.second != null)
				.collect(Collectors.toList());
	}
}