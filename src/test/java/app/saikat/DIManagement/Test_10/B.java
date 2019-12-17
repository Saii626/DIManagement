package app.saikat.DIManagement.Test_10;

import javax.inject.Singleton;

@Singleton
public class B {

	private C c;
	private D d;

	public B(C c, D d) {
		this.c = c;
		this.d = d;
	}

	public C getC() {
		return c;
	}

	public D getD() {
		return d;
	}
}