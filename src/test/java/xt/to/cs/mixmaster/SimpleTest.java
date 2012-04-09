/* * * * * *_* * * * * * * * * * * * * * * * * * * * * * * *\
 *         |_)                           _                 *
 *  _  _  _ _ _   _ _  _  _ _____  ___ _| |_ _____  ____   *
 * | ||_|| | ( \ / ) ||_|| (____ |/___|_   _) ___ |/ ___)  *
 * | |   | | |) X (| |   | / ___ |___ | | |_| ____| |      *
 * |_|   |_|_(_/ \_)_|   |_\_____(___/   \__)_____)_|      *
 *                                                         *
 *                         See LICENSE for license details *
\* * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package xt.to.cs.mixmaster;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

interface SampleInterface {
  public int a();

  public char b();

  public String c();

  public long d();

  public NestedObjectInterface e();
}

interface NestedObjectInterface {
  public String test();
}

abstract class ImplementsA implements SampleInterface {

  protected int a = 'a';

  public ImplementsA() {
    System.out.println("new ImplementsA()");
  }

  public int a() {
    return a;
  }
}

abstract class ImplementsB implements SampleInterface {

  protected static char b = 'b';

  public ImplementsB() {
    System.out.println("new ImplementsA()");
  }

  public char b() {
    return b;
  }
}

abstract class ImplementsABC implements SampleInterface {

  protected String c = "c";

  public ImplementsABC() {
    System.out.println("new ImplementsAB()");
  }

  public int a() {
    throw new RuntimeException("ImplementsABC.a()");
  }

  public char b() {
    throw new RuntimeException("ImplementsABC.a()");
  }

  public String c() {
    return c;
  }
}

abstract class ImplementsDE implements SampleInterface {

  protected String e = "e";

  protected class InnerClass implements NestedObjectInterface {
    public String test() {
      return e;
    }
  }

  public ImplementsDE() {
    System.out.println("new ImplementsDE()");
  }

  public long d() {
    return 123;
  }

  public InnerClass e() {
    return new InnerClass();
  }

}

public class SimpleTest {

  protected ClassNode   implementsA   = new ClassNode();
  protected ClassNode   implementsB   = new ClassNode();
  protected ClassNode   implementsABC = new ClassNode();
  protected ClassNode   implementsDE  = new ClassNode();
  protected ClassLoader tccl          = Thread.currentThread().getContextClassLoader();

  protected String internalName(Class<?> c) {
    return c.getName().replace('.', '/');
  }

  protected String fileName(Class<?> c) {
    return internalName(c) + ".class";
  }

  @Before
  public void before() {

    try {
      new ClassReader(tccl.getResourceAsStream(fileName(ImplementsA.class))).accept(implementsA, 0);
      new ClassReader(tccl.getResourceAsStream(fileName(ImplementsB.class))).accept(implementsB, 0);
      new ClassReader(tccl.getResourceAsStream(fileName(ImplementsABC.class))).accept(implementsABC, 0);
      new ClassReader(tccl.getResourceAsStream(fileName(ImplementsDE.class))).accept(implementsDE, 0);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void test1() throws InstantiationException, IllegalAccessException {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    MixMaster mix = new MixMaster("xt/to/cs/mixmaster/1", cw);
    mix.mix(implementsA);
    mix.mix(implementsABC);
    mix.generate();

    @SuppressWarnings("unchecked")
    Class<SampleInterface> c = (Class<SampleInterface>) ClassInjector.inject(tccl, "xt.to.cs.mixmaster.1", cw.toByteArray());
    SampleInterface obj = c.newInstance();

    assertEquals("a() should return " + 'a', 'a', obj.a());
    assertEquals("c() should return \"c\"", "c", obj.c());
  }

  @Test
  public void test2() throws InstantiationException, IllegalAccessException {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    MixMaster mix = new MixMaster("xt/to/cs/mixmaster/2", cw);
    mix.mix(implementsA);
    mix.mix(implementsB);
    mix.mix(implementsABC);
    mix.generate();

    @SuppressWarnings("unchecked")
    Class<SampleInterface> c = (Class<SampleInterface>) ClassInjector.inject(tccl, "xt.to.cs.mixmaster.2", cw.toByteArray());
    SampleInterface obj = c.newInstance();

    assertEquals("a() should return " + 'a', 'a', obj.a());
    assertEquals("b() should return " + 'b', 'b', obj.b());
    assertEquals("c() should return \"c\"", "c", obj.c());
  }

  @Test(expected = NoSuchMethodError.class)
  public void test3() throws InstantiationException, IllegalAccessException {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    TraceClassVisitor tracer = new TraceClassVisitor(cw, new PrintWriter(System.out));
    MixMaster mix = new MixMaster("xt/to/cs/mixmaster/3", tracer);
    mix.mix(implementsDE);
    mix.mix(implementsA);
    mix.mix(implementsB);
    mix.mix(implementsABC);
    mix.generate();

    @SuppressWarnings("unchecked")
    Class<SampleInterface> c = (Class<SampleInterface>) ClassInjector.inject(tccl, "xt.to.cs.mixmaster.3", cw.toByteArray());
    SampleInterface obj = c.newInstance();

    assertEquals("a() should return " + 'a', 'a', obj.a());
    assertEquals("b() should return " + 'b', 'b', obj.b());
    assertEquals("c() should return \"c\"", "c", obj.c());
    assertEquals("d() should return " + 123, 123, obj.d());
    assertEquals("e().test() should return \"e\"", "e", obj.e().test()); // Current throws NoSuchMethodError!
  }

}
