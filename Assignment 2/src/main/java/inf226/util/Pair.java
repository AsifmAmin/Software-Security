package inf226.util;

public final class Pair<A,B> {
   public final A first;
   public final B second;
   
   public Pair(A first, B second) {
     this.first = first;
     this.second = second;
   }

   public static<U,V> Pair<U,V> pair(U first, V second) {
     return new Pair<U,V>(first, second);
   }

   @Override
   public final boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final Pair<Object,Object> pair_other = (Pair<Object,Object>) other;
    return this.first.equals(pair_other.first)
        && this.second.equals(pair_other.second);

  
   }
}
