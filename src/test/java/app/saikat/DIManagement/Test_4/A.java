package app.saikat.DIManagement.Test_4;

import app.saikat.DIManagement.Provides;

public class A {

    private B b;
    private C c;

    public A(@Q1 B b, C c) {
        this.b = b;
        this.c = c;
    }

    public B getB() {
        return b;
    }

    public C getC() {
        return c;
    }

    @Provides
    @Q1
    public D createD() {
        return new D("Type: Q1");
    }
}