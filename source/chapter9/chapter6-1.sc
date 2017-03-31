

trait Parsers[ParseError, Parser[+_]] { self =>

  def char(c: Char): Parser[Char] =
    string(c.toString) map (_.charAt(0))

  def run[A](p: Parser[A])(input: String): Either[ParseError, A]
  def or[A](s1: Parser[A], s2: Parser[A]): Parser[A]

  /*
  * Exercise 9.4
  * */
  def listOfN[A](n: Int, p: Parser[A]):Parser[List[A]] =
    if( n > 0) map2(p, listOfN(n-1, p))(_ :: _)
    else succeed(List())

  /*
  * Exercise 9.3
  * */
  def many[A](p: Parser[A]): Parser[List[A]] =
    map2(p, many(p))(_ :: _) or succeed(List())

  /*
  * Exercise 9.5
  * */
  def toLazy[A](p: => Parser[A]): Parser[A] = p

  def manyToLazy[A](p: Parser[A]): Parser[List[A]] =
    map2(p, toLazy(many(p)))(_ :: _) or succeed(List())


  def map[A, B](a: Parser[A])(f: A =>  B): Parser[B]

  def succeed[A](a: A): Parser[A] =
    string("") map (_ => a)

  def slice[A](p: Parser[A]):Parser[String]

  def many1[A](p: Parser[A]):Parser[List[A]]
  def product[A, B](p: Parser[A], p2: Parser[B]): Parser[(A, B)]


  /*
  Exercise 9.1
   */
  def map2[A, B, C](p: Parser[A], p2: Parser[B])(f: (A, B) => C): Parser[C] =
    map(product(p, p2))( t => f(t._1, t._2) )

  def many1[A](p: Parser[A]): Parser[List[A]] =
    map2(p, many(p))( _ :: _)


  def productViaMap2[A, B](p: Parser[A], p2: Parser[B]): Parser[(A, B)] =
    map2(p, p2)((_, _))

  implicit def string(s: String): Parser[String]
  implicit def operators[A](p: Parser[A]): ParserOps[A] = ParserOps[A](p)
  implicit def asStringParser[A](a: A)(implicit f: A => Parser[String]): ParserOps[String] = ParserOps(f(a))

  case class ParserOps[A](p: Parser[A]) {
    def |[B>:A](p2: Parser[B]): Parser[B] = self.or(p, p2)
    def or[B>:A](p2: Parser[B]): Parser[B] = self.or(p, p2)

    def run(input: String): Either[ParseError, A] = self.run(p)(input)
    def listOfN(n: Int):Parser[List[A]] = self.listOfN(n, p)

    def map[B](f: A => B): Parser[B] = self.map(p)(f)
    def many(): Parser[List[A]] = self.many(p)

    def slice():Parser[String] = self.slice(p)

    def ** [B](p2: Parser[B]): Parser[(A, B)] = self.product(p, p2)
    def product[B](p2: Parser[B]): Parser[(A, B)] = self.product(p, p2)

  }

  object Laws {
    def equal[A](p1: Parser[A], p2: Parser[A])(in: Gen[String]): Prop =
      forAll(in)(s => run(p1)(s) == run(p2)(s))

    def mapLaw[A](p: Parser[A])(in: Gen[String]): Prop =
      equal(p, p.map(a => a))(in)

    /*
    * Exercies 9.2
    * `product` is associative. These two expressions are "roughly" equal:
    *  (a ** b) ** c == a ** (b ** c)
    * */

    def unbiasL[A,B,C](p: ((A,B), C)): (A,B,C) = (p._1._1, p._1._2, p._2)
    def unbiasR[A,B,C](p: (A, (B,C))): (A,B,C) = (p._1, p._2._1, p._2._2)

    def productLaw[A, B, C](p1: Parser[A], p2: Parser[B], p3: Parser[C])(in: Gen[String]): Prop =
      ((p1 ** p2) ** p3).map(unbiasL) == (p1 ** (p2 ** p3)).map(unbiasR)

  }

}