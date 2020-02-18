package app.saikat.DIManagement.Interfaces;

import java.util.EnumSet;

public enum DIBeanType {
	ANNOTATION, INTERFACE, SUBCLASS, GENERATED;

	public static EnumSet<DIBeanType> CREATED = EnumSet.of(ANNOTATION, INTERFACE, SUBCLASS);
}
