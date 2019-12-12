package app.saikat.DIManagement.Test_4;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.Provides;

@Singleton
public class G {

	private F f;

	public G(@Q2 F f) {
		this.f = f;
	}

	public F getF() {
		return f;
	}

	@Provides
	public D createD() {
		return new D("Type: none");
	}
}