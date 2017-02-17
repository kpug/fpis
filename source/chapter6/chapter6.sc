import scala.annotation.tailrec

sealed trait RNG {
  def nextInt: (Int, RNG)
}

case class SimpleRNG(seed: Long) extends RNG {
  def nextInt: (Int, RNG) = {
    val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL
    val nextRNG = SimpleRNG(newSeed)
    val n = ( newSeed >>> 16 ).toInt
    (n, nextRNG)
  }
}

val rng = SimpleRNG(42)

val (n1, rng2) = rng.nextInt

val (n2, rng3) = rng2.nextInt


/*
* exercise 6.1
* 0 <= n <= Int.Maxvalue
* 32 bit int range : -2147483648 ~ 2147483647
*/
def nonNegativeInt(rng: RNG): (Int, RNG) = {
  val (n, rng2) = rng.nextInt
  (if(n > 0) n else -(n + 1), rng2)
}

/*
* exercise 6.2
* 0 <= n < 1
* */
def double(rng: RNG) : (Double, RNG) = {
  val (n, rng2) = nonNegativeInt(rng)
  (n / (Int.MaxValue.toDouble + 1d), rng2)
}

/*
* exercise 6.3
* */
def intDouble(rng: RNG): ((Int, Double), RNG) = {
  val (i, rng2) = rng.nextInt
  val (d, rng3) = double(rng2)
  ((i, d), rng3)
}

def doubleInt(rng: RNG): ((Double, Int), RNG) = {
  val ((i, d), rng2) = intDouble(rng)
  ((d, i), rng2)
}

def double3(rng: RNG): ((Double, Double, Double), RNG) = {
  val (d1, rng2) = double(rng)
  val (d2, rng3) = double(rng2)
  val (d3, rng4) = double(rng3)
  ((d1, d2, d3), rng4)
}

/*
* exercise 6.4
*/
def ints(count: Int)(rng: RNG): (List[Int], RNG) = {
  if(count > 0) {
    val (n1, rng2) = rng.nextInt
    val (list, rng3) = ints(count-1)(rng2)
    (n1 :: list, rng3)
  }else{
    (List(), rng)
  }
}

def ints2(count: Int)(rng: RNG): (List[Int], RNG) = {
  @tailrec
  def loop(i: Int, r: RNG, l: List[Int]):(List[Int], RNG) = {
    if(i > 0){
      val (n1, r2) = r.nextInt
      loop(i - 1, r2, n1::l)
    }else{
      (l, r)
    }
  }

  loop(count, rng, List())
}

def ints3(count: Int)(rng: RNG): (List[Int], RNG) = {
  List.fill(count)(0)
    .foldRight( (List[Int](), rng) )( (_, t) => {
      val (l, r) = t
      val (nextInt, nextRNG) = r.nextInt
      ( nextInt :: l, nextRNG)
    })
}

type Rand[+A] = RNG => (A, RNG)

val int: Rand[Int] = _.nextInt


def unit[A](a: A): Rand[A] =
  rng => (a, rng)

val unit1: Rand[Int] = unit(10)
val unit2: (Int, RNG) = unit1(rng)


def map[A, B](s: Rand[A])(f: A => B): Rand[B] =
  rng => {
    val (a, rng2) = s(rng)
    (f(a), rng2)
  }

def nonNegativeEven: Rand[Int] =
  map(nonNegativeInt)(i => i - i % 2)


/*
* exercise 6.5
*/
def gracefulDouble: Rand[Double] =
  map(nonNegativeInt)( i => i / (Int.MaxValue.toDouble + 1d))

double(rng)
gracefulDouble(rng)


def map2[A, B, C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
  rng => {
    val (a, rngA) = ra(rng)
    val (b, rngB) = rb(rngA)
    (f(a, b), rngB)
  }

def both[A, B](ra: Rand[A], rb: Rand[B]): Rand[(A, B)] =
  map2(ra, rb)((_, _))

val randIntDouble: Rand[(Int, Double)] =
  both(int, double)

val randDoubleInt: Rand[(Double, Int)] =
  both(double, int)

/*
* exercise 6.7
* Rand[List[A]] =>  rng => (alist, rng2)
*/
def sequence[A](fs: List[Rand[A]]): Rand[List[A]] =
  (rng) => {
    fs.foldLeft((List[A](), rng))(
      (t, rand) => {
        val (tail, prevRng) = t
        val (h, nextRng) = rand(prevRng)
        (h :: tail, nextRng)
      })
  }

/*
* r: Rand[A]
* acc: Rand[List[A]]
*/
def sequence2[A](fs: List[Rand[A]]): Rand[List[A]] =
  fs.foldRight(unit(List[A]()))((r, acc) => map2(r, acc)( (h, t) => h :: t))


def ints4(count: Int): Rand[List[Int]] =
  sequence(List.fill(count)(int))


/*
* exercise 6.8
*/

def flatMap[A, B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
  rng => {
    val (a, nextRng) = f(rng)
    g(a)(nextRng)
  }

def nonNegativeLessThan(n: Int): Rand[Int] = {
  flatMap(nonNegativeInt)(
    i => {
      val mod = i % n
      if (i + (n-1) - mod >= 0) unit(mod) else nonNegativeLessThan(n)
    }
  )
}

/*
* exercise 6.9
*/
def mapViaFlatmap[A, B](s: Rand[A])(f: A => B): Rand[B] =
  flatMap(s)(a => unit(f(a)))

def map2ViaFlatmap[A, B, C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
  flatMap(ra)( a => mapViaFlatmap(rb)(b => f(a, b)))


