package app.saikat.DIManagement.Impl.ExternalImpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.TypeToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.Generator;
import app.saikat.DIManagement.Exceptions.WrongGeneratorParamsProvided;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;

public class GeneratorImpl<T> implements Generator<T> {

	private final DIBeanImpl<T> partialBean;
	private final List<DIBean<?>> generatorParams;

	private Logger logger = LogManager.getLogger(this.getClass());

	public GeneratorImpl(DIBeanImpl<T> bean, List<DIBean<?>> generatorParams) {
		this.partialBean = bean;
		this.generatorParams = generatorParams;
	}

	private boolean validateInput(Object[] args) {
		if (args.length != generatorParams.size())
			return false;

		for (int i = 0; i < args.length; i++) {
			if (!generatorParams.get(i)
					.getProviderType()
					.wrap()
					.isSupertypeOf(TypeToken.of(args[i].getClass())
							.wrap()))
				return false;
		}

		return true;
	}

	@Override
	public T generate(Object... args) {
		if (!validateInput(args)) {
			StringBuilder builder = new StringBuilder("Wrong arguments provided for Generator<");
			builder.append(partialBean.getProviderType()
					.toString())
					.append(">.\n Required: ( ");

			generatorParams.forEach(param -> builder.append(param.getProviderType()
					.toString())
					.append(", "));
			builder.delete(builder.length() - 2, builder.length());

			builder.append(" ). Found: ( ");

			for (Object object : args) {
				builder.append(object.getClass()
						.getSimpleName())
						.append(", ");
			}
			builder.delete(builder.length() - 2, builder.length());

			builder.append(" )");

			throw new WrongGeneratorParamsProvided(builder.toString());
		}

		logger.debug("Arguments to generator correct");


		List<DIBean<?>> dependencies = partialBean.getDependencies();
		List<Object> parameters = new ArrayList<>(dependencies.size());
		parameters.add(dependencies.get(0) == null ? null : dependencies.get(0).getProvider().get());

		int argc = 0;
		for (int i = 1; i < dependencies.size(); i++) {
			if (dependencies.get(i) == null) {
				parameters.add(args[argc]);
				argc++;
			} else {
				parameters.add(dependencies.get(i).getProvider().get());
			}
		}

		logger.debug("Creating an instance with dependencies: {}", parameters);
		T ret = null;
		try {
			ret = partialBean.getInvokable()
					.invoke(parameters.get(0), parameters.subList(1, parameters.size())
							.toArray());
		} catch (InvocationTargetException | IllegalAccessException e) {
			logger.error("Error: ", e);
		}
		logger.info("Created new object {}", ret);
		return ret;
	}
}