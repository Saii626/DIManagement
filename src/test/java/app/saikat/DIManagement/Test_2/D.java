package app.saikat.DIManagement.Test_2;

import javax.inject.Inject;

public class D {

    private B b;

    @Inject
    public D(B b) {
        this.b = b;
    }

    public B getB() {
        return b;
    }
}