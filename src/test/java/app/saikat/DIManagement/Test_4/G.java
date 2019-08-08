package app.saikat.DIManagement.Test_4;

import app.saikat.DIManagement.Provides;

public class G {

    private F f;

    public G(@Q2 F f) {
        this.f = f;
    }

    public F getF() {
        return f;
    }

    @Provides
    public D createD() {
        return new D("Type: none");
    }
}