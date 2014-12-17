package fj.data;

import fj.Equal;
import fj.F;
import fj.F2;
import fj.test.Arbitrary;
import fj.test.CheckResult;
import fj.test.Property;
import org.junit.Assert;
import org.junit.Test;

import static fj.F1Functions.map;
import static fj.F2Functions.curry;
import static fj.test.Arbitrary.*;
import static fj.test.Coarbitrary.coarbInteger;
import static fj.test.Property.prop;
import static fj.test.Property.property;
import static org.junit.Assert.assertTrue;

/**
 * Created by MarkPerry on 17/12/2014.
 */
public class WriterTest {

    @Test
    public void base() {
        Assert.assertTrue(tellTruth("a", "b", 0));
    }

    boolean tellTruth(String s1, String s2, int i) {
        Writer<String, Integer> w = defaultWriter.f(i);
        Writer<String, Integer> w1 = w.tell(s1).tell(s2);
        Writer<String, Integer> w2 = w.tell(w.monoid().sum(s1, s2));
        boolean b = eq.eq(w1, w2);
//        System.out.println(String.format("p1: %s, p2: %s, b: %s", w1, w2, b));
        return b;
    }

    final Equal<Writer<String, Integer>> eq = Equal.writerEqual(Equal.stringEqual, Equal.intEqual);
    final F<Integer, Writer<String, Integer>> defaultWriter = Writer.<Integer>stringLogger();

    void assertProperty(Property p) {
        CheckResult cr = p.check();
        CheckResult.summary.println(cr);
        assertTrue(cr.isExhausted() || cr.isPassed() || cr.isProven());
    }

    @Test
    public void testTellProp() {
        Property p = property(arbString, arbString, arbInteger, (s1, s2, i) -> prop(tellTruth(s1, s2, i)));
        assertProperty(p);
    }

    @Test
    public void testMap() {
        Property p = property(arbInteger, arbF(coarbInteger, arbInteger), (i, f) -> {
            boolean b = eq.eq(defaultWriter.f(i).map(f), defaultWriter.f(f.f(i)));
            return prop(b);
        });
        assertProperty(p);
    }

    @Test
    public void testFlatMap() {
        Property p = property(arbInteger,arbF(coarbInteger, arbWriterStringInt()), (i, f) -> {
            boolean b = eq.eq(Writer.<Integer>stringLogger().f(i).flatMap(f), f.f(i));
            return prop(b);
        });
        assertProperty(p);

    }

    public Arbitrary<Writer<String, Integer>> arbWriterStringInt() {
        return arbWriterString(arbInteger);
    }

    public <A> Arbitrary<Writer<String, A>> arbWriterString(Arbitrary<A> arb) {
        return Arbitrary.arbitrary(arb.gen.map(a -> Writer.<A>stringLogger().f(a)));
    }

    // Left identity: return a >>= f == f a
    @Test
    public void testLeftIdentity() {
        Property p = Property.property(
                arbInteger,
                arbF(coarbInteger, arbWriterStringInt()),
                (i, f) -> {
                    return prop(eq.eq(defaultWriter.f(i).flatMap(f), f.f(i)));
                });
        assertProperty(p);
    }

    // Right identity: m >>= return == m
    @Test
    public void testRightIdentity() {
        Property p = Property.property(
                arbWriterStringInt(),
                (w) -> prop(eq.eq(w.flatMap(a -> defaultWriter.f(a)), w))
        );
        assertProperty(p);
    }

    // Associativity: (m >>= f) >>= g == m >>= (\x -> f x >>= g)
    @Test
    public void testAssociativity() {
        Property p = Property.property(
                arbWriterStringInt(),
                arbF(coarbInteger, arbWriterStringInt()),
                arbF(coarbInteger, arbWriterStringInt()),
                (w, f, g) -> {
                    boolean t = eq.eq(w.flatMap(f).flatMap(g), w.flatMap(x -> f.f(x).flatMap(g)));
                    return prop(t);
                });
        assertProperty(p);
    }




}