package app.saikat.DIManagement.Test_3;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.Provides;

@Singleton
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