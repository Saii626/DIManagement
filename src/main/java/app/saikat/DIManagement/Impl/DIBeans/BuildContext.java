package app.saikat.DIManagement.Impl.DIBeans;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.DIManagement.Interfaces.DIBean;

public class BuildContext implements AutoCloseable {

	private static class BuildContextData {
		int currDepth;
		Queue<DIBean<?>> setterInject;
		Queue<DIBean<?>> postConstruct;

		BuildContextData() {
			currDepth = -1;
			setterInject = new LinkedList<>();
			postConstruct = new LinkedList<>();
		}
	}

	private static ThreadLocal<BuildContextData> contextData = new ThreadLocal<>();
	private static Logger logger = LogManager.getLogger(BuildContext.class);

	public static BuildContext getBuildContext() {
		BuildContextData data = contextData.get();

		if (data == null) {
			data = new BuildContextData();
			contextData.set(data);
		}

		data.currDepth += 1;

		return new BuildContext(data.currDepth);
	}

	public static void addToSetterInjection(DIBean<?> p) {
		BuildContextData data = contextData.get();

		if (data != null) {
			logger.debug("Adding setter inject bean {} to build context", p);
			
			data.setterInject.add(p);
		} else {
			throw new NullPointerException("No build context created");
		}
	}

	public static void addToPostConstruct(DIBean<?> p) {
		BuildContextData data = contextData.get();

		if (data != null) {
			logger.debug("Adding postConstruct bean {} to build context", p);
			data.postConstruct.add(p);
		} else {
			throw new NullPointerException("No build context created");
		}
	}

	private int depth;

	private BuildContext(int depth) {
		this.depth = depth;
	}

	@Override
	public void close() {
		// Invoke only at root BuildContext
		if (depth != 0) {
			BuildContextData data = contextData.get();
			data.currDepth -= 1;
			return;
		}

		BuildContextData data = contextData.get();
		Consumer<Queue<DIBean<?>>> invokeQueue = queue -> {
			while (!queue.isEmpty()) {
				DIBean<?> bean = queue.poll();
				logger.debug("Invoking {}", bean);
				bean.getProvider().get();
			}
		};

		logger.debug("Executing setter injection");
		invokeQueue.accept(data.setterInject);
		logger.debug("Setter injections executed");

		logger.debug("Executing postconstruct");
		invokeQueue.accept(data.postConstruct);
		logger.debug("Postconstructs executed");

		contextData.remove();
	}
}