package app.saikat.DIManagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNoException;

import org.junit.Test;

import app.saikat.DIManagement.Exceptions.CircularDependencyException;

public class DITester {

    @Test
    public void generateObjects_1() {
        DIManager.initialize("app.saikat.DIManagement.Test_1");

        app.saikat.DIManagement.Test_1.A a = DIManager.get(app.saikat.DIManagement.Test_1.A.class);
        app.saikat.DIManagement.Test_1.B b = DIManager.get(app.saikat.DIManagement.Test_1.B.class);
        app.saikat.DIManagement.Test_1.C c = DIManager.get(app.saikat.DIManagement.Test_1.C.class);
        app.saikat.DIManagement.Test_1.D d = DIManager.get(app.saikat.DIManagement.Test_1.D.class);
        app.saikat.DIManagement.Test_1.E e = DIManager.get(app.saikat.DIManagement.Test_1.E.class);

        assertEquals("A.B vs B", a.getB(), b);
        assertEquals("A.C vs C", a.getC(), c);
        assertEquals("B.C vs C", b.getC(), c);
        assertEquals("B.D vs D", b.getD(), d);
        assertEquals("B.E vs E", b.getE(), e);
        assertEquals("C.D vs D", c.getD(), d);
        assertEquals("D.E vs E", d.getE(), e);

    }

    @Test
    public void generateObjects_2() {
        try {
            DIManager.initialize("app.saikat.DIManagement.Test_2");
        } catch (CircularDependencyException e) {
            assumeNoException(e);
        }
        app.saikat.DIManagement.Test_2.A a = DIManager.get(app.saikat.DIManagement.Test_2.A.class);

        assertEquals("Circular dependency", a, null);
    }

    @Test
    public void generateObjects_3() {
        DIManager.initialize("app.saikat.DIManagement.Test_3");

        app.saikat.DIManagement.Test_3.A a = DIManager.get(app.saikat.DIManagement.Test_3.A.class);
        app.saikat.DIManagement.Test_3.B b = DIManager.get(app.saikat.DIManagement.Test_3.B.class);
        app.saikat.DIManagement.Test_3.C c = DIManager.get(app.saikat.DIManagement.Test_3.C.class);
        app.saikat.DIManagement.Test_3.D d = DIManager.get(app.saikat.DIManagement.Test_3.D.class);
        app.saikat.DIManagement.Test_3.E e = DIManager.get(app.saikat.DIManagement.Test_3.E.class);
        app.saikat.DIManagement.Test_3.F f = DIManager.get(app.saikat.DIManagement.Test_3.F.class);

        assertEquals("A.B vs B", a.getB(), b);
        assertEquals("A.D vs D", a.getD(), d);
        assertEquals("B.E vs E", b.getE(), e);
        assertEquals("D.B vs B", d.getB(), b);
        assertEquals("D.C vs C", d.getC(), c);
        assertEquals("F.D vs D", f.getD(), d);
        assertEquals("F.E vs E", f.getE(), e);
    }

    @Test
    public void generateObjects_4() {
        DIManager.initialize("app.saikat.DIManagement.Test_4");

        app.saikat.DIManagement.Test_4.A a = DIManager.get(app.saikat.DIManagement.Test_4.A.class);
        app.saikat.DIManagement.Test_4.B b = DIManager.get(app.saikat.DIManagement.Test_4.B.class, app.saikat.DIManagement.Test_4.Q1.class);
        app.saikat.DIManagement.Test_4.C c = DIManager.get(app.saikat.DIManagement.Test_4.C.class);
        app.saikat.DIManagement.Test_4.D d0 = DIManager.get(app.saikat.DIManagement.Test_4.D.class);
        app.saikat.DIManagement.Test_4.D d1 = DIManager.get(app.saikat.DIManagement.Test_4.D.class, app.saikat.DIManagement.Test_4.Q1.class);
        app.saikat.DIManagement.Test_4.D d2 = DIManager.get(app.saikat.DIManagement.Test_4.D.class, app.saikat.DIManagement.Test_4.Q2.class);
        app.saikat.DIManagement.Test_4.E e = DIManager.get(app.saikat.DIManagement.Test_4.E.class);
        app.saikat.DIManagement.Test_4.F f = DIManager.get(app.saikat.DIManagement.Test_4.F.class, app.saikat.DIManagement.Test_4.Q2.class);
        app.saikat.DIManagement.Test_4.G g = DIManager.get(app.saikat.DIManagement.Test_4.G.class);
        app.saikat.DIManagement.Test_4.H h = DIManager.get(app.saikat.DIManagement.Test_4.H.class);

        assertEquals("A.B vs B", a.getB(), b);
        assertEquals("A.C vs C", a.getC(), c);
        assertEquals("B.C vs C", b.getC(), c);
        assertEquals("E.D1 vs D1", e.getD(), d1);
        assertEquals("G.F vs C", g.getF(), f);
        assertEquals("H.D0 vs D0", h.getD0(), d0);
        assertEquals("H.D1 vs D1", h.getD1(), d1);
        assertEquals("H.D2 vs D2", h.getD2(), d2);

        assertEquals("D0 val", d0.getType(), "Type: none");
        assertEquals("D1 val", d1.getType(), "Type: Q1");
        assertEquals("D2 val", d2.getType(), "Type: Q2");
        assertEquals("E PostConstruct", e.getStr(), "Hello world");
    }
}