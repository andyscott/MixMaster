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

import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * A MethodAdapter that strips out supercalls in constructor methods.
 * @author Andy Scott
 */
public class StripSupercallAdapter extends MethodAdapter {
  public StripSupercallAdapter(final MethodVisitor nextMv, int access, String name, String desc) {
    super(null);
    this.mv = new AdviceAdapter(new EmptyVisitor(), access, name, desc) {
      protected void onMethodEnter() {
        StripSupercallAdapter.this.mv = nextMv;
      }
    };
  }
}