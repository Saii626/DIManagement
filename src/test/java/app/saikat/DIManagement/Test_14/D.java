package app.saikat.DIManagement.Test_14;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.Generator;

@Singleton
public class D {

	private Generator<C> cGen;

	public D(Generator<C> c) {
		this.cGen = c;
	}

	public C generateC() {
		B b = new B("Hello world");
		return cGen.generate(b);
	}

}