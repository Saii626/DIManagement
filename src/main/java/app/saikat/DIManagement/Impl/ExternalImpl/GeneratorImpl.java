package app.saikat.DIManagement.Impl.ExternalImpl;

import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.TypeToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.Generator;
import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Exceptions.WrongGeneratorParamsProvided;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Interfaces.DIBean;

public class GeneratorImpl<T> implements Generator<T> {
	
	private final DIBeanImpl<T> partialBean;
	private final List<DIBean<?>> generatorParams;
	private final DIBeanManagerHelper helper;
	
	private Logger logger = LogManager.getLogger(this.getClass());

	public GeneratorImpl(DIBeanImpl<T> bean, List<DIBean<?>> generatorParams, DIBeanManagerHelper helper) {
		this.partialBean = bean;
		this.generatorParams = generatorParams;
		this.helper = helper;
	}

	private boolean validateInput(Object[] args) {
		if (args.length != generatorParams.size())
			return false;

		for (int i = 0; i < args.length; i++) {
			if (!generatorParams.get(i).getProviderType().wrap().isSupertypeOf(TypeToken.of(args[i].getClass()).wrap()))
				return false;
		}

		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T generate(Object... args) {
		//Copy the bean so that
		DIBeanImpl<T> partialBean = this.partialBean.copy();

		if (!validateInput(args)) {
			StringBuilder builder = new StringBuilder("Wrong arguments provided for Generator<");
			builder.append(partialBean.getProviderType().toString()).append(">.\n Required: ( ");

			generatorParams.forEach(param -> builder.append(param.getProviderType().toString()).append(", "));
			builder.delete(builder.length()-2, builder.length());

			builder.append(" ). Found: ( ");

			for (Object object : args) {
				builder.append(object.getClass().getSimpleName()).append(", ");
			}
			builder.delete(builder.length()-2, builder.length());

			builder.append(" )");

			throw new WrongGeneratorParamsProvided(builder.toString());
		}

		logger.debug("Arguments to generator correct");
		List<ConstantProviderBean<?>> dynamicParams = new ArrayList<>(args.length);

		logger.debug("Creating dynamic dependencies for {}", partialBean);

		for (Object obj : args) {
			TypeToken.of(obj.getClass());
			ConstantProviderBean<Object> providerBean = new ConstantProviderBean<>((TypeToken<Object>) TypeToken.of(obj.getClass()), NoQualifier.class);
			providerBean.setProvider(() -> obj);
			dynamicParams.add(providerBean);
		}

		logger.debug("Dynamic dependencies created: {}", dynamicParams);

		List<DIBean<?>> deps = partialBean.getDependencies();
		int j = 0;
		for (int i = 1; i < deps.size(); i++) {
			if (deps.get(i) == null) {
				deps.set(i, dynamicParams.get(j));
				j++;
			}
		}

		logger.debug("Final dependencies: {}", deps);

		ProviderImpl<T> provider = new ProviderImpl<>(partialBean, helper);
		return provider.get();
	}
}