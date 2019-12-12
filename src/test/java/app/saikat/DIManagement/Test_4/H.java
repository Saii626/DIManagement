package app.saikat.DIManagement.Test_4;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class H {

	private D d0, d1, d2;

	@Inject
	public H(D d0, @Q1 D d1, @Q2 D d2) {
		this.d0 = d0;
		this.d1 = d1;
		this.d2 = d2;
	}

	public D getD0() {
		return d0;
	}

	public D getD1() {
		return d1;
	}

	public D getD2() {
		return d2;
	}
}