package ch4

sealed trait Option[+A] {
  // exercise 4.1
  def map[B](f: A => B): Option[B] = this match {
    case None => None
    case Some(a) => Some(f(a))
  }

  def getOrElse[B>:A](default: => B): B = this match {
    case None => default
    case Some(a) => a
  }

  def flatMap[B](f: A => Option[B]): Option[B] =
    map(f) getOrElse None

  def flatMap_Match[B](f: A => Option[B]): Option[B] = this match {
    case None => None
    case Some(a) => f(a)
  }

  def orElse[B >: A](ob: => Option[B]): Option[B] =
    this map (Some(_)) getOrElse ob

  def orElse_Match[B >: A](ob: => Option[B]): Option[B] = this match {
    case None => ob
    case _ => this
  }

  def filter(f: A => Boolean): Option[A] = this match {
    case Some(a) if f(a) => this
    case _ => None
  }

  def filter_Match(f: A => Boolean): Option[A] =
    flatMap( a => if (f(a)) Some(a) else None )

}
case class Some[+A](get: A) extends Option[A]
case object None extends Option[Nothing]

object Option extends App{

  def mean(xs: Seq[Double]): Option[Double] =
    if (xs.isEmpty) None
    else Some(xs.sum / xs.length)

  // exercise 4.2
  // (variance)분산이란? - https://en.wikipedia.org/wiki/Variance#Definition
  def variance(xs: Seq[Double]): Option[Double] =
    mean(xs).flatMap( m => mean( xs.map(x => math.pow(x - m, 2)) ))

  // exercise 4.3
  def map2[A,B,C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] =
    a.flatMap(aa => b.map(bb => f(aa, bb)))

  // exercise 4.4
  def sequence[A](a: List[Option[A]]): Option[List[A]] =
    a match {
      case Nil => Some(Nil)
      case h :: t => h.flatMap(hh => sequence(t) map (hh :: _))
    }

  // exercise 4.4.1
  def sequence_foldRight[A](a: List[Option[A]]): Option[List[A]] =
    a.foldRight[Option[List[A]]](Some(Nil))((x,y) => map2(x,y)(_ :: _))


  // exercise 4.5
  def traverse[A, B](a: List[A])(f: A => Option[B]): Option[List[B]] =
    a match {
      case Nil => Some(Nil)
      case h::t => map2(f(h), traverse(t)(f))(_ :: _)
    }

  // exercise 4.5.1
  def traverse_foldRight[A, B](a: List[A])(f: A => Option[B]): Option[List[B]] =
    a.foldRight[Option[List[B]]](Some(Nil))((h,t) => map2(f(h),t)(_ :: _))

  // exercise 4.5.2
  def sequenceViaTraverse[A](a: List[Option[A]]): Option[List[A]] =
    traverse(a)(x => x)

}