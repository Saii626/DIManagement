package app.saikat.DIManagement.Test_4;

import javax.annotation.PostConstruct;

import app.saikat.DIManagement.Provides;

public class E {

    private D d;
    private String str;

    public E(@Q1 D d) {
        this.d = d;
    }

    public D getD() {
        return d;
    }

    @Provides
    @Q2
    public F createF() {
        return new F();
    }

    @PostConstruct
    private void setStr() {
        this.str = "Hello world";
    }

    public String getStr() {
        return str;
    }
} 