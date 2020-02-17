package app.saikat.DIManagement.Test_16;

public class C {

	private B b;
	private long l;
	private byte by; 
	private double d;

	public C(B b, long l, byte by, double d) {
		this.b = b;
		this.l = l;
		this.by = by;
		this.d = d;
	}

	public B getB() {
		return this.b;
	}

	public long getL() {
		return this.l;
	}

	public byte getBy() {
		return this.by;
	}
	
	public double getD() {
		return this.d;
	}
}