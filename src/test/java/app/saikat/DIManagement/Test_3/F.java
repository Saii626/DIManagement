package app.saikat.DIManagement.Test_3;

import javax.inject.Inject;

public class F {

    private D d;
    private E e;

    @Inject
    public F(D d, E e) {
        this.d = d;
        this.e = e;
    }

    public D getD() {
        return d;
    }

    public E getE() {
        return e;
    }
}