package app.saikat.DIManagement.Test_16;

import javax.inject.Singleton;

@Singleton
public class D {

	private int q1, q2;
	private double d1, d2;

	public D(@Q1 int q1, @Q2 int q2, @Q1 double d1, @Q2 double d2) {
		this.q1 = q1;
		this.q2 = q2;
		this.d1 = d1;
		this.d2 = d2;
	}

	public int getQ1() {
		return this.q1;
	}

	public int getQ2() {
		return this.q2;
	}

	public double getD1() {
		return this.d1;
	}

	public double getD2() {
		return this.d2;
	}

}