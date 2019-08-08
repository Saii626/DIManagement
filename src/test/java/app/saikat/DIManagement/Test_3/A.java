package app.saikat.DIManagement.Test_3;

import app.saikat.DIManagement.Provides;

public class A {

    private B b;
    private D d;

    public A(B b) {
        this.b = b;
    }

    public B getB() {
        return b;
    }

    public D getD() {
        return d;
    }

    @Provides
    public D createD(C c) {
        if (d == null) {
            d = new D(b, c);
        }

        return d;
    }
}