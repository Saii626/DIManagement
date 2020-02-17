package app.saikat.DIManagement.Test_15;

import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class H {

	private Provider<A<B, C, E>> p1;
	private Provider<A<E, B, C>> p2;
	private Provider<A<D, B, E>> p3;
	private Provider<A<C, E, D>> p4;
	private Provider<A<B, D, B>> p5;

	public H(Provider<A<B, C, E>> p1, Provider<A<E, B, C>> p2, Provider<A<D, B, E>> p3, Provider<A<C, E, D>> p4,
			Provider<A<B, D, B>> p5) {
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		this.p4 = p4;
		this.p5 = p5;
	}

	public Provider<A<B,C,E>> getP1() {
		return this.p1;
	}

	public Provider<A<E,B,C>> getP2() {
		return this.p2;
	}

	public Provider<A<D,B,E>> getP3() {
		return this.p3;
	}

	public Provider<A<C,E,D>> getP4() {
		return this.p4;
	}

	public Provider<A<B,D,B>> getP5() {
		return this.p5;
	}	

}