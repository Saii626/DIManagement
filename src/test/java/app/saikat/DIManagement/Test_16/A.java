package app.saikat.DIManagement.Test_16;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.GenParam;
import app.saikat.Annotations.DIManagement.Generate;
import app.saikat.Annotations.DIManagement.Provides;

@Singleton
public class A {

	@Generate
	private C createC(B b, @GenParam long l, @GenParam byte by, @Q1 double d) {
		return new C(b, l, by, d);
	}

	@Q1
	@Provides
	private int get_Q1_count() {
		return 100;
	}

	@Q2
	@Provides
	private int get_Q2_count() {
		return 200;
	}
}