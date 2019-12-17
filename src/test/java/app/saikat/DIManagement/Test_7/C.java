package app.saikat.DIManagement.Test_7;

import app.saikat.Annotations.DIManagement.Provides;

public class C {

	private static int NO_OF_INSTANCES = 0;

	private A a;
	private B b;
	
	public C(A a, B b) {
		this.a=a;
		this.b=b;
		NO_OF_INSTANCES++;
	}

	public A getA() {
		return a;
	}

	public B getB() {
		return b;
	}

	@Provides
	public static D getD(E e, F f) {
		return new D();
	}

	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}
}