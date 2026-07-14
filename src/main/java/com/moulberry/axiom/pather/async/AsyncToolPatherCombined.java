package com.moulberry.axiom.pather.async;

public class AsyncToolPatherCombined implements AsyncToolPather {
   private final AsyncToolPather one;
   private final AsyncToolPather two;

   public AsyncToolPatherCombined(AsyncToolPather one, AsyncToolPather two) {
      this.one = one;
      this.two = two;
   }

   @Override
   public void update() {
      this.one.update();
      this.two.update();
   }

   @Override
   public void acceptInitial(long position) {
      this.one.acceptInitial(position);
      this.two.acceptInitial(position);
   }

   @Override
   public void accept(long[] positions) {
      this.one.accept(positions);
      this.two.accept(positions);
   }
}
