package app.saikat.DIManagement.Test_10;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import app.saikat.DIManagement.Interfaces.DIManager;

@Singleton
public class A {

	private DIManager manager;

	private A aInstance;
	private B bInstance;
	private C cInstance;
	private D dInstance;

	public A(DIManager manager) {
		this.manager = manager;

		bInstance = manager.getBeanOfType(B.class).getProvider().get();
		cInstance = manager.getBeanOfType(C.class).getProvider().get();
		dInstance = manager.getBeanOfType(D.class).getProvider().get();
	}

	@PostConstruct
	public void postConstruct() {
		aInstance = manager.getBeanOfType(A.class).getProvider().get();
	}
	
	public DIManager getManager() {
		return manager;
	}

	public A getaInstance() {
		return aInstance;
	}

	public B getbInstance() {
		return bInstance;
	}

	public C getcInstance() {
		return cInstance;
	}

	public D getdInstance() {
		return dInstance;
	}
}