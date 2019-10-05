package app.saikat.DIManagement.Test_5;

@ClassAnnot_2
public class C {
    
    private D d;
    private E e;
    private static String state = "Not instanciated";

    public C(D d, E e) {
        this.d = d;
        this.e = e;
        state = "Instanciated";
    }

    public D getD() {
        return d;
    }

    public E getE() {
        return e;
    }

    public static String getState() {
        return state;
    }

    @ClassAnnot_2
    public void modifyState() {
        state = "Wrong";
    }
}