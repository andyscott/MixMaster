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

import java.lang.reflect.Method;

/**
 * Utility that lets you inject a class into a class loader. Uses reflection.
 * 
 * @author Andy Scott
 */
public class ClassInjector {

  /** Method for ClassLoader.defineClass */
  private static final Method defineClassMethod;

  public static Class<?> inject(ClassLoader cl, String className, byte[] b) {
    Class<?> clazz = null;
    try {
      Object[] args = new Object[] { className, b, new Integer(0), new Integer(b.length) };
      clazz = (Class<?>) defineClassMethod.invoke(cl, args);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return clazz;
  }

  static {
    Method localMethod;
    try {
      localMethod = java.lang.ClassLoader.class.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class, int.class });
      localMethod.setAccessible(true);
    } catch (NoSuchMethodException localNoSuchMethodException) {
      localMethod = null;
    }
    defineClassMethod = localMethod;
  }
}