package app.saikat.DIManagement.Test_3;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.Provides;

@Singleton
public class B {

	@Provides (singleton = false)
	public E createE() {
		return new E();
	}
}