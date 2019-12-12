package app.saikat.DIManagement.Test_1;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class B {

	private C c;
	private D d;
	private E e;

	@Inject
	public B(C c, D d, E e) {
		this.c = c;
		this.d = d;
		this.e = e;
	}

	public C getC() {
		return this.c;
	}

	public D getD() {
		return this.d;
	}

	public E getE() {
		return this.e;
	}

}