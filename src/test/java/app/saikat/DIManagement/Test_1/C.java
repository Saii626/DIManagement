package app.saikat.DIManagement.Test_1;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
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