package app.saikat.DIManagement.Test_15;

import javax.inject.Singleton;

@Singleton
public class G {

	private A<B, C, E> t1;
	private A<E, B, C> t2;
	private A<D, B, E> t3;
	private A<C, E, D> t4;
	private A<B, D, B> t5;

	public G(A<B, C, E> t1, A<E, B, C> t2, A<D, B, E> t3, A<C, E, D> t4, A<B, D, B> t5) {
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
		this.t4 = t4;
		this.t5 = t5;
	}

	public A<B,C,E> getT1() {
		return this.t1;
	}

	public A<E,B,C> getT2() {
		return this.t2;
	}

	public A<D,B,E> getT3() {
		return this.t3;
	}

	public A<C,E,D> getT4() {
		return this.t4;
	}

	public A<B,D,B> getT5() {
		return this.t5;
	}
}