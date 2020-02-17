package app.saikat.DIManagement.Test_16;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.GenParam;
import app.saikat.Annotations.DIManagement.Generate;
import app.saikat.Annotations.DIManagement.Provides;

@Singleton
public class B {

	@Q1
	@Provides
	private double get_Q1_double() {
		return 10.101010101010;
	}

	@Q2
	@Provides
	private double get_Q2_double() {
		return 20.202020202020;
	}

	@Q1
	@Generate
	private boolean getNextVal(@GenParam int i, @Q1 int q2) {
		return true;
	}

	@Q2
	@Generate
	private boolean getNextVal2(@GenParam int i, @Q2 int q2) {
		return false;
	}
}