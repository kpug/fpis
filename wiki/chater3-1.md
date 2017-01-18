Chapter 3. Functional data structures
==

작성자: 고재도 / 일시: 2017년 1월 18일

## 3.1. DEFINING FUNCTIONAL DATA STRUCTURES

functional data structures are by definition immutable.

```scala
sealed trait List[+A]
case object Nil extends List[Nothing]
case class Cons[+A](head: A, tail: List[A]) extends List[A]

object List {
  def sum(ints: List[Int]): Int = ints match {
    case Nil => 0
    case Cons(x: Int, xs: List[Int]) => x + sum(xs)
  }

  def product(ds: List[Double]): Double = ds match {
    case Nil => 1.0
    case Cons(0.0, _) => 0.0
    case Cons(x, xs) => x * product(xs)
  }

  def apply[A](as: A*): List[A] =
    if (as.isEmpty) Nil
    else Cons(as.head, apply(as.tail: _*))
}

```

trait is an abstract interface that may optionally contain implementations of some methods.

a List can be empty => Nil
A nonempty list => Cons

Cons consists of an initial element, head, followed by a List (possibly empty) of remaining elements (the tail):

the + in front of the type parameter A is a variance annotation that signals that A is a covariant or “positive” parameter of List.
List[Dog] is considered a subtype of List[Animal]

But notice now that Nil extends List[Nothing]. Nothing is a subtype of all types, which means that in conjunction with the variance annotation, Nil can be considered a List[Int], a List[Double], and so on, exactly as we want

## 3.2. PATTERN MATCHING

A companion object is an object with the same name as a class or trait and is defined in the same source file as the associated file or trait. A companion object differs from other objects as it has access rights to the class/trait that other objects do not. In particular it can access methods and fields that are private in the class/trait.
 
```scala
class A(d: String) {
  private var a = "" 
  override def toString = d + a; 
}

object A {
  def apply(b:String, e:String) = {
    val a = new A(b)
    a.a = e
    a
  }
}
case class B()
object B {
  def apply() = {
    val a = new A("")
    //can not access a.a
    new B()
  }
}
```

Pattern matching works a bit like a fancy switch statement that may descend into the structure of the expression it examines and extract subexpressions of that structure.

1. List(1,2,3) match { case _ => 42 } results in 42. Here we’re using a variable pattern, _, which matches any expression
2. List(1,2,3) match { case Cons(h,_) => h } results in 1
3. List(1,2,3) match { case Cons(_,t) => t } results in List(2,3)
4. List(1,2,3) match { case Nil => 42 } results in a MatchError at Runtime. A MatchError indicates that none of the cases in a match expression matched the target.
 
 The function apply in the object List is a variadic function, meaning it accepts zero or more arguments of type A
 The special _* type annotation allows us to pass a Seq to a variadic method.
 