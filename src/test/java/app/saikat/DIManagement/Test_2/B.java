package app.saikat.DIManagement.Test_2;

import javax.inject.Inject;

public class B {

    private C c;
    private D d;
    private E e;

    @Inject
    public B(C c, D d, E e) {
        this.c = c;
        this.d = d;
        this.e = e;
    }

    public C getC() {
        return this.c;
    }

    public D getD() {
        return this.d;
    }

    public E getE() {
        return this.e;
    }

}