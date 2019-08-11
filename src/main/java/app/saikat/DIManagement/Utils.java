package app.saikat.DIManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

class Utils {

    static Class<? extends Annotation> getQualifierAnnotation(Annotation[] annotations,
            List<Class<? extends Annotation>> QUALIFIERS) {

        for (Annotation annotation : annotations) {
            if (QUALIFIERS.contains(annotation.annotationType())) {
                return annotation.annotationType();
            }
        }
        return null;

    }

    static Constructor<?> getAppropriateConstructor(Constructor<?>[] constructors) {
        Constructor<?> toUse = null;
        if (constructors.length > 1) {
            for (Constructor<?> constructor : constructors) {
                Annotation[] annotations = constructor.getAnnotations();
                for (Annotation a : annotations) {
                    if (a.getClass().equals(Inject.class)) {
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

    static List<DIBean> getParameterBeans(Class<?>[] parameters, Annotation[][] annotations,
            List<Class<? extends Annotation>> QUALIFIERS) {

        List<DIBean> beans = new ArrayList<>();

        for (int i = 0; i < parameters.length; i++) {
            beans.add(new DIBean(parameters[i], Utils.getQualifierAnnotation(annotations[i], QUALIFIERS)));
        }

        return beans;
    }

    static DIBean getProviderBean(Collection<DIBean> beans, DIBean provider) {
        for (DIBean b : beans) {
            if (b.equals(provider)) {
                return b;
            }
        }

        return provider;
    }


    static String getStringRepresentationOf(Collection<DIBean> beans) {
        if (beans == null) return "null";
        if (beans.isEmpty()) return "none";

        StringBuffer buffer = new StringBuffer("[ ");

        for (DIBean bean : beans) {
            buffer.append(bean.toString());
            buffer.append(", ");
        }

        buffer.delete(buffer.length()-2, buffer.length());
        buffer.append(" ]");

        return buffer.toString();
    }
}