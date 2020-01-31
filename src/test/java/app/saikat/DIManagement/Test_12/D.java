package app.saikat.DIManagement.Test_12;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class D {

	private B b;

	@Inject
	public void injectB(B b) {
		this.b = b;
	}

	public B getB() {
		return b;
	}
}