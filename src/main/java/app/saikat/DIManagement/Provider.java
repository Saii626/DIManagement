package app.saikat.DIManagement;

public interface Provider<T> {
	
	T newInstance();

	Class<T> getType();
	
}