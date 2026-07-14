package com.moulberry.axiom;

import java.lang.ref.Cleaner;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;

public class GlobalCleaner {
   public static final Cleaner INSTANCE = Cleaner.create();

   public static GlobalCleaner.LeakChecker createLeakChecker(Object object, String name) {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      String callingClass = trace[3].getClassName();
      StringBuilder builder = new StringBuilder();
      builder.append(name).append(" [").append(callingClass).append("]\n");

      for (int i = 3; i < trace.length; i++) {
         builder.append("\tat ").append(trace[i]).append("\n");
      }

      GlobalCleaner.LeakChecker leakChecker = new GlobalCleaner.LeakCheckerImpl(builder.toString());
      INSTANCE.register(object, leakChecker);
      return leakChecker;
   }

   public interface LeakChecker extends Runnable {
      void disarm();
   }

   private static class LeakCheckerImpl implements Runnable, GlobalCleaner.LeakChecker {
      private boolean active = true;
      private final String trace;

      public LeakCheckerImpl(String trace) {
         this.trace = trace;
      }

      @Override
      public void run() {
         if (this.active) {
            Minecraft.getInstance().delayCrash(new CrashReport(this.trace, new Error("LeakChecker was triggered!")));
         }
      }

      @Override
      public void disarm() {
         this.active = false;
      }
   }
}
