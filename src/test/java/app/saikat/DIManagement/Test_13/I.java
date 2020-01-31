package app.saikat.DIManagement.Test_13;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.Generator;

@Singleton
public class I {

	private Generator<H> hGenerator;
	private F f;

	public I(Generator<H> hGenerator, F f) {
		this.hGenerator = hGenerator;
		this.f = f;
	}

	public Generator<H> getHGenerator() {
		return this.hGenerator;
	}

	public F getF() {
		return this.f;
	}

	public H createH() {
		D d = new D("hello world");
		C c = new C();

		return hGenerator.generate(d, c);
	}

}