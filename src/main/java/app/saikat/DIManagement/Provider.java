package app.saikat.DIManagement;

public interface Provider<T> {
	
	T getNewInstance();
	
}