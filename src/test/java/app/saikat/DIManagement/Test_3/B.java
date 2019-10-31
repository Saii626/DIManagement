package app.saikat.DIManagement.Test_3;

import app.saikat.DIManagement.Provides;

public class B {

	private E e;

	public E getE() {
		return e;
	}

	@Provides
	public E createE() {
		if (e == null) {
			e = new E();
		}
		return e;
	}

}