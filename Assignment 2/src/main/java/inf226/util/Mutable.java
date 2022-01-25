package inf226.util;
import java.util.function.Consumer;

/**
 * Store a mutable variable as a consumer.
 * This class is a wrapper for avoiding some limitations
 * of Java's λ-abstractions.
 **/
public class Mutable<A> implements Consumer<A>{
   private A value;

   public Mutable(A value) {
     this.value = value;
   }

   public static<U> Mutable<U> init(U value) {
     return new Mutable<U>(value);
   }
 
   @Override
   public void accept(A value) {
      this.value = value;
   }

   public A get() {
      return value;
   }
}

