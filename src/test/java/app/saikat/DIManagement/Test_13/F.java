package app.saikat.DIManagement.Test_13;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.GenParam;
import app.saikat.Annotations.DIManagement.Generate;

@Singleton
public class F {

	private G g;

	public F(G g) {
		this.g = g;
	}

	@Generate
	public H generateH(A a, @GenParam D d, @GenParam C c) {
		E e = new E(g, "Hello world");

		return new H(a, c, d ,e);
	}

}