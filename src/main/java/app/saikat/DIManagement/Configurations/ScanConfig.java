package app.saikat.DIManagement.Configurations;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Lists;

/**
 * Not thread safe. Dont use from multiple threads
 */
public class ScanConfig {

    private final Set<ClassAnnotationConfig> classAnnotationConfigs;
    private final Set<MethodAnnotationConfig> methodAnnotationConfigs;
    private final Set<String> packagesToScan;

    public ScanConfig(Set<ClassAnnotationConfig> classAnnotations, Set<MethodAnnotationConfig> methodAnnotations,
            Set<String> scanPackages) {
        this.classAnnotationConfigs = classAnnotations;
        this.methodAnnotationConfigs = methodAnnotations;
        this.packagesToScan = scanPackages;
    }

    public void addConfig(ClassAnnotationConfig config) {
        addToSet(classAnnotationConfigs, Collections.singleton(config));
    }

    public void addConfig(MethodAnnotationConfig config) {
        addToSet(methodAnnotationConfigs, Collections.singleton(config));
    }

    private <T> void addToSet(Set<T> set, Collection<T> items) {
        synchronized (set) {
                set.addAll(items);
        }
    }

    public void addPackagesToScan(String... pkgs) {
        addToSet(packagesToScan, Lists.newArrayList(pkgs));
    }

    public Set<ClassAnnotationConfig> getClassAnnotationConfig() {
        synchronized (this.classAnnotationConfigs) {
            return Collections.unmodifiableSet(this.classAnnotationConfigs);
        }
    }

    public Set<MethodAnnotationConfig> getMethodAnnotationConfig() {
        synchronized (this.methodAnnotationConfigs) {
            return Collections.unmodifiableSet(this.methodAnnotationConfigs);
        }
    }

    public Set<String> getPackagesToScan() {
        synchronized (this.packagesToScan) {
            return Collections.unmodifiableSet(this.packagesToScan);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Set<ClassAnnotationConfig> classAnnotationConfigs = Collections.synchronizedSet(new HashSet<>());
        private Set<MethodAnnotationConfig> methodAnnotationConfigs = Collections.synchronizedSet(new HashSet<>());;
        private Set<String> packagesToScan = Collections.synchronizedSet(new HashSet<>());;

        public Builder addAnnotationConfig(ClassAnnotationConfig annotationConf) {
            synchronized (classAnnotationConfigs) {
                classAnnotationConfigs.add(annotationConf);
            }
            return this;
        }

        public Builder addAnnotationConfig(MethodAnnotationConfig annotationConf) {
            synchronized (methodAnnotationConfigs) {
                methodAnnotationConfigs.add(annotationConf);
            }
            return this;
        }

        public Builder addPackagesToScan(String... str) {
            synchronized (packagesToScan) {
                packagesToScan.addAll(Lists.newArrayList(str));
            }
            return this;
        }

        public ScanConfig build() {
            return new ScanConfig(classAnnotationConfigs, methodAnnotationConfigs, packagesToScan);
        }
    }

}