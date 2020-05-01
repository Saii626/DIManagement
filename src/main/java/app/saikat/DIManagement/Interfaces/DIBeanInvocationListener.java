package app.saikat.DIManagement.Interfaces;

public interface DIBeanInvocationListener<T> {

	void onObjectCreated(DIBean<T> bean, T object);
	
}