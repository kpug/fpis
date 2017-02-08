/**
  * Created by longcoding on 2017. 1. 17..
  */
sealed trait List[+A]
case object Nil extends List[Nothing]
case class Cons[+A](head: A, tail: List[A]) extends List[A]

object List {
    def sum(ints: List[Int]): Int = ints match {
        case Nil => 0
        case Cons(x,xs) => x + sum(xs)
    }

    def product(ds: List[Double]): Double = ds match {
        case Nil => 1.0
        case Cons(0.0, _) => 0.0
        case Cons(x, xs) => x * product(xs)
    }

    def apply[A](as: A*): List[A] =
        if (as.isEmpty) Nil
        else Cons(as.head, apply(as.tail: _*))

    def removeFirstNode[A](data: List[A]) = {
        data match {
            case Nil => data
            case Cons(_, t) => t
        }
    }

    def setHead[A](element: A, data: List[A]) = {
        Cons(element, data)
    }

    def drop[A](l: List[A], n: Int): List[A] = {
        l match {
            case Nil => l
            case Cons(x, xs) if n.equals(0) => l
            case Cons(x, xs) => drop(xs, n-1)
        }
    }

    def isMatched[A](value: A): Boolean = {
       value.equals(7)
    }
    def dropWhile[A](l: List[A], f: A => Boolean): List[A] = {
        l match {
            case Cons(h, t) if !f(h) => dropWhile(t, f)
            case _ => l
        }
    }

    def append[A](a1: List[A], a2: List[A]): List[A] = {
        a1 match {
            case Cons(h, Nil) => Cons(h, a2)
            case Cons(h, t) => Cons(h, append(t, a2))
        }
    }

    def init[A](l: List[A]): List[A] = {
        l match {
            case Cons(h, Nil) => Nil
            case Cons(h, t) => Cons(h, init(t))
        }
    }

    def foldRight[A, B](as: List[A], z: B)(f: (A, B) => B): B = {
        as match {
            case Nil => z
            case Cons(x, xs) => f(x, foldRight(xs, z)(f))
        }
    }

    def sum2(ns: List[Int]) = {
        foldRight(ns, 0)((x,y) => x + y)
    }

    def product2(ns: List[Double]) = {
        foldRight(ns, 1.0)(_ * _)
    }

    def length[A](as: List[A]): Int = {
        foldRight(as, 0)((_,y) => y+1)
    }

    def foldLeft[A, B](as: List[A], z: B)(f: (B, A) => B): B = {
        as match {
            case Nil => z
            case Cons(x, xs) => foldLeft(xs, f(z, x))(f)
        }
    }

    def sum3(ns: List[Int]) = {
        foldLeft(ns, 0)((x,y) => x + y)
    }

    def product3(ns: List[Double]) = {
        foldLeft(ns, 1.0)(_ * _)
    }

    def length3[A](as: List[A]): Int = {
        foldLeft(as, 0)((x,_) => x+1)
    }

    def reverse[A](as: List[A]): List[A] = {
        foldLeft(as, Nil:List[A])((head, tail) => Cons(tail, head))
    }

//    def append[A](a1: List[A], a2: List[A]): List[A] = {
//        a1 match {
//            case Cons(h, Nil) => Cons(h, a2)
//            case Cons(h, t) => Cons(h, append(t, a2))
//        }
//    }

    def append2[A](a1: List[A], a2: List[A]): List[A] = {
        foldRight(a1, a2)((left, right) => Cons(left, right))
        //foldRight(a1, a2)(Cons(_,_))
    }

    def concat[A](l: List[List[A]]): List[A] = {
        foldRight(l, Nil:List[A])((x,y) => append2(x, y))
    }
}