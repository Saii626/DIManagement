package app.saikat.DIManagement.Test_6;

import javax.inject.Singleton;

@Singleton
public class C {
	
	private D d;
	private E e;

	public D getD() {
		return d;
	}
	
	public E getE() {
		return e;
	}

	@MethodAnnot_2
	public void setD(D d) {
		this.d = d;
	}

	@MethodAnnot_3
	public void setE(E e) {
		this.e = e;
	}
}