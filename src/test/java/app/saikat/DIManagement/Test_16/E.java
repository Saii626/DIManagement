package app.saikat.DIManagement.Test_16;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.Generator;

@Singleton
public class E {

	private Generator<Boolean> g1, g2;
	private Generator<C> g3;

	public E(@Q1 Generator<Boolean> g1, @Q2 Generator<Boolean> g2, Generator<C> g3) {
		this.g1 = g1;
		this.g2 = g2;
		this.g3 = g3;
	}

	public Generator<Boolean> getG1() {
		return this.g1;
	}

	public Generator<Boolean> getG2() {
		return this.g2;
	}

	public Generator<C> getG3() {
		return this.g3;
	}

}