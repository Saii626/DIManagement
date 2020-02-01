package app.saikat.DIManagement.Test_15;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.Provides;

@Singleton
public class F {

	private B b;
	private C c;
	private D d;
	private E e;

	public F(B b, C c, D d, E e) {
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
	}

	@Provides
	private A<B, C, E> getBCE() {
		return new A<>(b, c, e);
	}

	@Provides
	private A<E, B, C> getEBC() {
		return new A<>(e, b, c);
	}

	@Provides
	private A<D, B, E> getDBE() {
		return new A<>(d, b, e);
	}

	@Provides
	private A<C, E, D> getCED() {
		return new A<>(c, e, d);
	}

	@Provides
	private A<B, D, B> getBDB() {
		return new A<>(b, d, b);
	}
}