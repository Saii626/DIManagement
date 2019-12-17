package app.saikat.DIManagement.Test_11;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class D {

	private Set<E> eInstances = new HashSet<>();

	public D(Provider<E> eProvider) {
		for (int i = 0; i < 5; i++) {
			eInstances.add(eProvider.get());
		}
	}

	public Set<E> geteInstances() {
		return eInstances;
	}

}