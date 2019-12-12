package app.saikat.DIManagement.Test_6;

import javax.inject.Singleton;

@Singleton
public class A {
	
	private B b;
	private String str;

	public A(B b) {
		this.b = b;
		this.str = "State 1";
	}

	public B getB() {
		return b;
	}

	public String getStr() {
		return str;
	}

	@MethodAnnot_1
	public void func_1() {
		str = "State 2";
	}
}