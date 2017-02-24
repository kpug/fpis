package ch7

import java.util.concurrent._

import scala.language.implicitConversions


object Par {
  type Par[A] = ExecutorService => Future[A]

  def run[A](s: ExecutorService)(a: Par[A]): Future[A] = a(s)

  def unit[A](a: A): Par[A] = (es: ExecutorService) => UnitFuture(a)

  private case class UnitFuture[A](get: A) extends Future[A] {
    def isDone = true
    def get(timeout: Long, units: TimeUnit) = get
    def isCancelled = false
    def cancel(evenIfRunning: Boolean): Boolean = false
  }

  // 연습문제 7.3
  def map2[A,B,C](a: Par[A], b: Par[B])(f: (A,B) => C): Par[C] =
    (es: ExecutorService) => {
      val af = a(es)
      val bf = b(es)
      UnitFuture(f(af.get, bf.get))
    }


  // 연습문제 7.4
  def asyncF[A,B](f: A => B): A => Par[B] =
    a => lazyUnit(f(a))

  def lazyUnit[A](a: => A): Par[A] = fork(unit(a))

  def fork[A](a: => Par[A]): Par[A] =
    es => es.submit(new Callable[A] {
      def call = a(es).get
    })


  // 연습문제 7.5
  def sequence[A](as: List[Par[A]]): Par[List[A]] =
    map(sequenceBalanced(as.toIndexedSeq))(_.toList)

  def map[A,B](pa: Par[A])(f: A => B): Par[B] =
    map2(pa, unit(()))((a,_) => f(a))


  def sequenceBalanced[A](as: IndexedSeq[Par[A]]): Par[IndexedSeq[A]] = fork {
    if (as.isEmpty) unit(Vector())
    else if (as.length == 1) map(as.head)(a => Vector(a))
    else {
      val (l,r) = as.splitAt(as.length/2)
      map2(sequenceBalanced(l), sequenceBalanced(r))(_ ++ _)
    }
  }


  // 연습문제 7.6
  def parFilter[A](l: List[A])(f: A => Boolean): Par[List[A]] = {
    val pars: List[Par[List[A]]] =
      l map (asyncF((a: A) => if (f(a)) List(a) else List()))
    map(sequence(pars))(_.flatten)
  }

  // 연습문제 7-13
  def chooser[A,B](p: Par[A])(choices: A => Par[B]): Par[B] =
    es => {
      val k = run(es)(p).get
      run(es)(choices(k))
    }

  // 연습문제 7-14
  def joinViaFlatMap[A](a: Par[Par[A]]): Par[A] =
    flatMap(a)(x => x)

  def flatMap[A,B](p: Par[A])(choices: A => Par[B]): Par[B] =
    es => {
      val k = run(es)(p).get
      run(es)(choices(k))
    }


  def main(args: Array[String]) {


    def asyncIntToString = Par.asyncF((x: Int) => x.toString())
    val executorService = Executors.newFixedThreadPool(3)

    println(executorService)
    println(Par.run(executorService)(asyncIntToString(10)).get())
    println(Par.run(executorService)(asyncIntToString(20)).get())
    println(Par.run(executorService)(asyncIntToString(30)).get())
    println(Par.run(executorService)(asyncIntToString(40)).get())
    println(Par.run(executorService)(asyncIntToString(50)).get())
    println(executorService)



//    val filterOperation = parFilter(List(1, 2, 3, 4, 5))(_ < 4)
//    val executorService = Executors.newCachedThreadPool()
//    val run = Par.run(executorService)(filterOperation).get()
//    println(run)

//
//    val choices = (a: Int) => {
//      if (a % 2 == 0) Par.unit("scala")
//      else Par.unit("java")
//    }
//
//    val executorService = Executors.newFixedThreadPool(3)
//    println(executorService)
//    println(chooser(Par.unit(1))(choices).apply(executorService).get())
//    println(chooser(Par.unit(2))(choices).apply(executorService).get())
//    println(chooser(Par.unit(3))(choices).apply(executorService).get())
//    println(chooser(Par.unit(4))(choices).apply(executorService).get())
//    println(chooser(Par.unit(5))(choices).apply(executorService).get())
//
//    val par = Par.unit(Par.unit("scala"))
//    val par2 = Par.unit(Par.unit("java"))
//    val executorService = Executors.newFixedThreadPool(3)
//    println(joinViaFlatMap(par)(executorService).get())
//    println(joinViaFlatMap(par2)(executorService).get())
//    println(joinViaFlatMap(par2)(executorService).get())
  }
}
