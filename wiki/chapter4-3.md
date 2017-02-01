4.4 Either 자료 형식
------------------

- 실패의 원인을 추적할 수 있다.
- 둘 중 하나일 수 있는 값들을 대표한다.
- 두 형식의 분리합집합(disjoint union)이라 할 수 있다.
- Left, Right 값을 가지며 Right는 성공, Left는 실패에 사용한다. (scala convention; right is right)

```scala
sealed trait Either[+E, +A]
case class Left[+E](value: E) extends Either[E, Nothing]
case class Right[+A](value: A) extends Either[Nothing, A]
```

##### Exception 대신 예외 정보를 돌려주는 예제

```scala
def mean(xs: IndexedSeq[Double]): Either[String, Double] = 
  if (xs.isEmpty) 
    Left("mean of empty list!") // ArithmeticException을 직접 던지는 대신 String을 돌려준다.
  else
    Right(xs.sum / xs.length)
  
def Try[A](a: => A): Either[Exception, A] =
  try Right(a)
  catch { case e: Exception => Left(e) }
  
def safeDiv(x: Int, y: Int): Either[Exception, Int] = 
  Try(x / y)
  
def parseInsuranceRateQuote(age: String, numberOfSpeedingTickets: String): Either[Exception, Double] =
  for {
    a <- Try { age.toInt }
    tickets <- Try { numberOfSpeedingTickets.toInt }
  } yield insuranceRateQuote(a, tickets)
  
def insuranceRateQuote(age: Int, numberOfSpeedingTickets: Int): Double = ??? 
```

4.5 요약
-------

예제에서는 대수적 자료 형식 Option, Either에 초점을 맞췄지만 좀 더 일반적인 착안은 예외를 보통의 값으로 표현하고 고차 함수를 이용해서 오류 처리 및 전파의 공통 패턴들을 캡슐화한다는 것이다. 
이를 더욱 일반화하면 임의의 효과를 값으로 표현한다는 착안이 된다.
