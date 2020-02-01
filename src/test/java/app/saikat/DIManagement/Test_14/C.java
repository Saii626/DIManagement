package app.saikat.DIManagement.Test_14;

import app.saikat.Annotations.DIManagement.GenParam;
import app.saikat.Annotations.DIManagement.Generate;

public class C {

	private A a;
	private B b;

	@Generate
	public C(A a, @GenParam B b) {
		this.a = a;
		this.b = b;
	}

	public A getA() {
		return this.a;
	}

	public B getB() {
		return this.b;
	}

}