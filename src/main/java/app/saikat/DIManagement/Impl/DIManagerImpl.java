package app.saikat.DIManagement.Impl;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import com.google.common.reflect.TypeToken;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Impl.BeanManagers.InjectBeanManager;
import app.saikat.DIManagement.Impl.BeanManagers.PostConstructBeanManager;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.Repository.Repository;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIManager;

public class DIManagerImpl extends DIManager {

	private boolean isInitialScan = true;

	@SuppressWarnings("serial")
	public DIManagerImpl() {
		logger.info("Initializing DIManager");

		// Add a bean of DIManager, DIBeanManagerHelper and Results. Anyone can ask for an instance of them
		TypeToken<DIManagerImpl> managerProviderToken = new TypeToken<DIManagerImpl>() {};
		ConstantProviderBean<DIManagerImpl> managerBean = new ConstantProviderBean<>(managerProviderToken,
				NoQualifier.class);
		managerBean.setProvider(() -> this);

		TypeToken<Repository> repoToken = new TypeToken<Repository>() {};
		ConstantProviderBean<Repository> repoBean = new ConstantProviderBean<>(repoToken, NoQualifier.class);
		repoBean.setProvider(() -> repository);

		this.repository.addBean(managerBean);
		this.repository.addBean(repoBean);

		logger.info("Initial beans added");
	}

	@Override
	public void scan(String... pathsToScan) {
		try {
			Repository currentScanRepo = new Repository();

			// Scanning
			logger.info("Scanning {}", Arrays.toString(pathsToScan));

			ClasspathScanner scanner = new ClasspathScanner();
			scanner.scan(currentScanRepo, repository, pathsToScan);

			currentScanRepo.getBeanManagers()
					.values()
					.parallelStream()
					.forEach(DIBeanManager::scanComplete);

			// Create providers first. Since the dependencies are resolved when the provider
			// is actually invoked, there is no issue in creating providers first
			Repository r = isInitialScan ? currentScanRepo : repository;
			InjectBeanManager injectBeanManager = r.getBeanManagerOfType(InjectBeanManager.class);
			PostConstructBeanManager postConstructBeanManager = r.getBeanManagerOfType(PostConstructBeanManager.class);
			createProviderBeans(currentScanRepo.getBeans(), injectBeanManager, postConstructBeanManager);

			currentScanRepo.getBeanManagers()
					.values()
					.parallelStream()
					.forEach(DIBeanManager::providerCreated);

			logger.debug("Beans and their providers: {}", currentScanRepo.getBeans());

			// Resolving dependencies
			Collection<Class<? extends Annotation>> allQualifiers = Streams
					.concat(currentScanRepo.getQualifierAnnotations()
							.parallelStream(), repository.getQualifierAnnotations()
									.parallelStream())
					.collect(Collectors.toSet());
			resolveDependencies(currentScanRepo, repository, allQualifiers);

			currentScanRepo.getBeanManagers()
					.values()
					.parallelStream()
					.forEach(DIBeanManager::dependencyResolved);

			logger.info("All created beans: {}", currentScanRepo.getBeans());

			// All scanning and beanCreation is successful. Update pointers
			// of DIBeanManagers' and merge to globalRepo
			currentScanRepo.getBeanManagers()
					.values()
					.parallelStream()
					.forEach(mgr -> {
						mgr.setRepo(repository);
						mgr.setObjectMap(objectMap);
					});
			repository.merge(currentScanRepo);

			logger.info("Paths {} scanned successfully", Arrays.toString(pathsToScan));
			isInitialScan = false;

		} catch (Exception e) {
			logger.error("Error while scanning ", e);
			logger.error("unloading {}", Arrays.toString(pathsToScan));
			throw e;
		}
	}

	private void createProviderBeans(Collection<DIBean<?>> beans, InjectBeanManager injectBeanManager,
			PostConstructBeanManager postConstructBeanManager) {
		logger.info("Creating providers");
		Queue<DIBean<?>> toCreate = new LinkedList<>();
		toCreate.addAll(beans);

		logger.debug("{} provider beans need to be created", toCreate);

		while (!toCreate.isEmpty()) {
			DIBean<?> current = toCreate.poll();

			if (!current.getBeanManager()
					.shouldCreateProvider()) {
				logger.debug("Skipping creation on Provider for {} as shouldCreateProvider is false", current);
				continue;
			}

			logger.debug("Creating provider for: {}", current);
			current.getBeanManager()
					.createProviderBean(current, injectBeanManager, postConstructBeanManager);
		}
	}

	private void resolveDependencies(Repository currentScanRepo, Repository globalRepo,
			Collection<Class<? extends Annotation>> allQualifiers) {

		logger.info("Resolving dependencies");
		Queue<DIBean<?>> toResolve = new LinkedList<>();
		toResolve.addAll(currentScanRepo.getBeans());

		Collection<DIBean<?>> resolved = new HashSet<>();
		resolved.addAll(globalRepo.getBeans());

		while (!toResolve.isEmpty()) {
			DIBean<?> current = toResolve.poll();
			if (current.getBeanManager() == null || !current.getBeanManager()
					.shouldResolveDependency()) {
				resolved.add(current);
				continue;
			}

			logger.debug("resolving dependency of: {}", current);

			current.getBeanManager()
					.resolveDependencies(current, resolved, toResolve, allQualifiers);
			resolved.add(current);
		}
	}
}