package app.saikat.DIManagement.Test_12;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class B {

	private D d;
	private E e;

	@Inject
	public B(D d, E e) {
		this.d = d;
		this.e = e;
	}

	public D getD() {
		return this.d;
	}

	public E getE() {
		return this.e;
	}

}