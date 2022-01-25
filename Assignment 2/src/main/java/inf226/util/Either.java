package inf226.util;


import java.util.function.Consumer;
import java.util.function.Function;


public class Either<A,B> {
  private final boolean isLeft;
  private final A left;
  private final B right;

  private Either(A leftValue, B rightValue, boolean isLeft) {
     this.left = leftValue;
     this.right = rightValue;
     this.isLeft = isLeft;
  }

  public static<U,V> Either<U,V> left(U value) {
    return new Either<U,V>(value, null, true);
  }

  public static<U,V> Either<U,V> right(V value) {
    return new Either<U,V>(null, value, false);
  }

  public void branch(Consumer<A> leftBranch, Consumer<B> rightBranch) {
     if (isLeft)
        leftBranch.accept(left);
     else
        rightBranch.accept(right);
  }

  public<C> C cases(Function<A,C> leftCase, Function<B,C> rightCase) {
     if (isLeft)
        return leftCase.apply(left);
     else
        return rightCase.apply(right);
  }
}
