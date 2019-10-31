package app.saikat.DIManagement.Test_2;

import javax.inject.Inject;

public class C {
	private D d;

	@Inject
	public C(D d) {
		this.d = d;
	}

	public D getD() {
		return this.d;
	}

}