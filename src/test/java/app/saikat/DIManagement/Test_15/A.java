package app.saikat.DIManagement.Test_15;

public class A<P, Q, R> {

	private P p;
	private Q q;
	private R r;

	public A(P p, Q q, R r){
		this.p = p;
		this.q = q;
		this.r = r;
	}

	public P getP() {
		return this.p;
	}

	public Q getQ() {
		return this.q;
	}

	public R getR() {
		return this.r;
	}

}