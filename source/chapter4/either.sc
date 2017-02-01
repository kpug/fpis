
/* =========================================================================
 * Exercise 4.6
 * ------------------------------------------------------------------------- */
sealed trait Either[+E, +A] {
  def map[B](f: A => B): Either[E, B] =
    this match {
      case Left(e) => Left(e)
      case Right(a) => Right(f(a))
    }

  def flatMap[EE >: E, B](f: A => Either[EE, B]): Either[EE, B] =
    this match {
      case Left(e) => Left(e)
      case Right(a) => f(a)
    }

  def orElse[EE >: E, B >: A](b: => Either[EE, B]): Either[EE, B] =
    this match {
      case Left(_) => b
      case Right(a) => Right(a)
    }

  def map2[EE >: E, B, C](b: Either[EE, B])
                         (f: (A, B) => C): Either[EE, C] =
    this.flatMap(a1 => b.map(b1 => f(a1, b1)))

  def map2_for_yield[EE >: E, B, C](b: Either[EE, B])
                                   (f: (A, B) => C): Either[EE, C] =
    for {
      a1 <- this
      b1 <- b
    } yield f(a1, b1)
}
case class Left[+E](value: E) extends Either[E, Nothing]
case class Right[+A](value: A) extends Either[Nothing, A]


/* =========================================================================
 * Exercise 4.7
 * ------------------------------------------------------------------------- */
def sequence[E, A](es: List[Either[E, A]]): Either[E, List[A]] =
  es match {
    case Nil => Right(Nil)
    case h :: t => h.flatMap(h1 => sequence(t).map(t1 => h1 :: t1))
  }

sequence(List(Right(1), Right(2), Right(3)))

def traverse[E, A, B](es: List[A])
                     (f: A => Either[E, B]): Either[E, List[B]] =
  es match {
    case Nil => Right(Nil)
    case h :: t => f(h).map2(traverse(t)(f))(_ :: _)
  }

traverse(List(1, 2, -1, 3, 4))(n => if (n > 0) Right(n) else Left("failed"))

def sequence2[E, A](es: List[Either[E, A]]): Either[E, List[A]] =
  traverse(es)(x => x)

sequence2(List(Right(1), Right(2), Right(3)))


/* =========================================================================
 * Exercise 4.8
 * ------------------------------------------------------------------------- */
case class Person(name: Name, age: Age)
sealed class Name(val value: String)
sealed class Age(val value: Int)

def mkName(name: String): Either[String, Name] =
  if (name == "" || name == null) Left("Name is empty.")
  else Right(new Name(name))

def mkAge(age: Int): Either[String, Age] =
  if (age < 0) Left("Age is out of range.")
  else Right(new Age(age))

def mkPerson(name: String, age: Int): Either[String, Person] =
  mkName(name).map2(mkAge(age))(Person)

mkPerson("name", 1)
mkPerson("", 1)
mkPerson("name", -1)
mkPerson("", -1)

def mkPerson2(name: String, age: Int): Either[Seq[String], Person] =
  (mkName(name), mkAge(age)) match {
    case (Left(e1), Left(e2)) => Left(Seq(e1, e2))
    case (Left(e), Right(_)) => Left(Seq(e))
    case (Right(_), Left(e)) => Left(Seq(e))
    case (Right(n), Right(a)) => Right(Person(n, a))
  }

mkPerson2("name", 1)
mkPerson2("", 1)
mkPerson2("name", -1)
mkPerson2("", -1)
