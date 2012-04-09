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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A tool that allows you to generate a new class by mixing together code from multiple classes.
 * @author Andy Scott
 */
public class MixMaster {
  
  protected static final EmptyVisitor EMPTY_VISITOR = new EmptyVisitor();
  
  protected int version = Opcodes.V1_6;
  protected int access = Opcodes.ACC_PUBLIC;
  protected String name;
  protected String signature = null;
  protected String superName = "java/lang/Object";
  protected Set<String> interfaces = new HashSet<>();
  protected ClassVisitor cv;
  protected Set<ClassNode> nodes;
  protected MixAdapter mixAdapter;
  protected Set<String> methods = new HashSet<>();
  protected ClassNode extendsNode;
  protected StripSupercallAdapter noSuperInitVisitor;
  protected MethodNode initNode;
  protected LinkedList<String> initMethods;
  
  protected class MixAdapter extends ClassAdapter {
    
    protected String className;
    
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.className = name;
      System.out.println("C " + className);
    }
    
    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
      System.out.println("INNER " + innerName);
      super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      System.out.println("GRR " + name);
      if ("<init>".equals(name)) {
        
        if (initNode == null) {
          initNode = new MethodNode(access, name, desc, signature, exceptions);
          return new RemappingMethodAdapter(
              access, desc,
              initNode,
              new SimpleRemapper(className, MixMaster.this.name));
          }
        
        
        String newName = "#init#" + className.hashCode();
        initMethods.push(newName);
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PRIVATE, newName, desc, signature, exceptions);
        return new RemappingMethodAdapter(
            access, desc,
            new StripSupercallAdapter(mv, access, name, desc),
            new SimpleRemapper(className, MixMaster.this.name));

      }
      
      
      if (methods.add(name + desc)) {
        return new RemappingMethodAdapter(
            access, desc,
            super.visitMethod(access, name, desc, signature, exceptions),
            new SimpleRemapper(className, MixMaster.this.name));
      }
      return EMPTY_VISITOR;
    }

    @Override
    public void visitEnd() {
    }

    public MixAdapter(ClassVisitor cv) {
      super(cv);
    }
    
  }
  
  public MixMaster(String name, ClassVisitor cv) {
    this.name = name;
    this.cv = cv;
    this.mixAdapter = new MixAdapter(cv);
    this.nodes = new LinkedHashSet<>();
    this.initMethods = new LinkedList<>();
  }
  
  @SuppressWarnings("unchecked")
  public void mix(ClassNode node) {
    nodes.add(node);
    interfaces.addAll(node.interfaces);
  }
  
  public void extend(ClassNode node) {
    this.superName = node.name;
    this.extendsNode = node;
  }
  
  @SuppressWarnings("unchecked")
  public void generate() {
    cv.visit(version, access, name, signature, superName, interfaces.toArray(new String[]{}));
    
    for (ClassNode node : nodes)
      node.accept(mixAdapter);
    
    String[] exceptions = new String[initNode.exceptions.size()];
    initNode.exceptions.toArray(exceptions);
    MethodVisitor mv = cv.visitMethod(access,
            initNode.name,
            initNode.desc,
            initNode.signature,
            exceptions);
    
    initNode.accept(
        new AdviceAdapter(mv, initNode.access, initNode.name, initNode.desc) {
          @Override
          protected void onMethodEnter() {
            for (String initMethod : initMethods) {
              visitIntInsn(ALOAD, 0);
              visitMethodInsn(Opcodes.INVOKESPECIAL, MixMaster.this.name, initMethod, initNode.desc);
            }
          }
    });
    
    cv.visitEnd();
  }

}
