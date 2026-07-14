package com.moulberry.axiom.mask;

import java.util.HashMap;
import java.util.Map;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.luajc.JavaGen;

public class LuaJavaLoader extends ClassLoader {
   private final Map<String, byte[]> unloaded = new HashMap<>();

   public LuaJavaLoader(ClassLoader parent) {
      super(parent);
   }

   public LuaFunction load(Prototype p, String classname, String filename, LuaValue env) {
      JavaGen jg = new JavaGen(p, classname, filename, false);
      return this.load(jg, env);
   }

   public LuaFunction load(JavaGen jg, LuaValue env) {
      this.include(jg);
      return this.load(jg.classname, env);
   }

   public LuaFunction load(String classname, LuaValue env) {
      try {
         Class c = this.loadClass(classname);
         LuaFunction v = (LuaFunction)c.newInstance();
         v.initupvalue1(env);
         return v;
      } catch (Exception var5) {
         var5.printStackTrace();
         throw new IllegalStateException("bad class gen: " + var5);
      }
   }

   public void include(JavaGen jg) {
      this.unloaded.put(jg.classname, jg.bytecode);
      int i = 0;

      for (int n = jg.inners != null ? jg.inners.length : 0; i < n; i++) {
         this.include(jg.inners[i]);
      }
   }

   @Override
   public Class findClass(String classname) throws ClassNotFoundException {
      byte[] bytes = this.unloaded.get(classname);
      return bytes != null ? this.defineClass(classname, bytes, 0, bytes.length) : super.findClass(classname);
   }
}
