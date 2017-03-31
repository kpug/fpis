# 제4장. 예외를 이용하지 않는 오류 처리

예외를 던지는 것도 하나의 부수 효과인데, 함수적 코드에서 예외를 사용하지 않고, 오류를 함수적으로 제기하고 처리하는 기본원리를 살펴본다.

- 실패 상황과 예외를 보통의 값으로 표현할 수 있어야한다.  
  => 참조 투명성(referential transparency, RT)을 유지
- 일반적인 오류처리-복구 패턴을 추상화한 고차 함수를 작성할 수 있다는 것  
  => 오류 처리 논리의 통합(consolidation of error-handling logic) 유지

## 4.1. 예외의 장단점

- 예외의 기본 장점: 오류 처리 논리의 통합과 중앙집중화

- 예외의 주된 단점
  - 예외는 참조 투명성을 위반하고 문맥 의존성을 도입한다.
     - 참조에 투명한 표현식 => 문맥에 의존하지 않으며 지역적으로 추론
     - 참조에 투명하지 않은 표현식 => 문맥에 의존적(context-dependent) 좀 더 전역의 추론이 필요
  - 예외는 형식에 안전하지 않다.

```scala
// y가 참조에 투명하지 않음을 증명

def failingFn(i: Int): Int = {
  val y: Int = throw new Exception(“fail”)
  try {
    val x = 42 + 5
    x + y
  }
  catch { case e: Exception => 43 }
}

def failingFn2(i: Int): Int = {
  val y: Int = 
  try {
    val x = 42 + 5
    x + ((throw new Exception(“fail”): Int)
  }
  catch { case e: Exception => 43 }
}
```


```
scala> failingFn(12)
java.lang.Exception: fail!

scala> failingFn2(12)
res1: Int = 43
```


> 위의 예에서 42 + 5 의 의미는 더 큰표현식의 의존하지 않으며 그냥 영원히 47이지만, throw new Exception 문맥에 의존한다.(try 블록인지 아닌지에 따라 결과가 바뀜)


> failingFn의 형식인 Int => Int 만 보고는 이 함수가 예외를 던질 수 있다는 사실을 알 수 없다. 프로그래머가 실수로 예외 점검 코드를 추가하지 않으면 그 예외는 실행시점에서야 검출 된다.


- 예외의 대안
  - 기법 : 예외를 던지는 대신, 예외적인 조건이 발생했을 뜻하는 값을 돌려준다. 
     - 오래된 착안
     - C에서 예외 처리를 위해 오류 부호(error code)를 돌려 준다.
  - 적용 : '미리 정의해 둘 수 있는 값들'을 대표하는 새로운 일반적 형식을 도입하고, 오류의 처리와 전파에 관한 공통적인 패턴들을 고차 함수들을 이용해서 캡슐화.
  - 장점 : 형식에 완전히 안전하며, 최소한의 구문적잡음으로도 스칼라의 형식 점검기의 도움을 받아서 실수를 미리 발견할 수 있다.


## 4.2. 예외의 가능한 대안

```scala
// 예외: 빈 목록에 대해서는 평균이 정의되지 않는다.
def mean(xs: Seq[Double]): Double =
  if (xs.isEmpty)
    throw new ArithmeticException(“mean of empty list!”)
  else xs.sum / xs.length
```

### 대안 1. Double 형식의 가짜 값을 돌려주는 것 

모든 경우에 그냥 xs.sum / xs.length 를 돌려준다면 빈 목록에 대해서는 0.0/0.0을 돌려주게 되는데, 
이는 Double.NaN이다. 아니면 다른 어떤 경계 값(sentinel value)을 돌려줄 수도 있고, 상황에 따라서는 
원하는 형식의 값 대신 null일 수도 있다.)

이런 접근 방식은 예외기능이 없는 언어에서 오류를 처리하는데 흔히 사용하지만, 우리는 아래 이유로 사용하지 않는다.

1. 오류가 소리 없이 전파될 수 있다. 호출자가 이런 오류 조건의 점검을 실수로 빼먹어도 컴파일러가 경고해 주지 않으며, 그러면 이후의 코드가 제대로 작동하지 않을 수 있다. 오류가 코드의 훨씬 나중 부분에서 검출되는 경우가 많다.
2. 호출하는 쪽의 호출자가 ‘진짜’ 결과를 받았는지 점검하는 명시적 if문들로 구성된 판에 박힌 코드가 상당히 늘어난다. 
3. 다형적 코드에 적용할 수 없다. 출력 형식에 따라서는 그 형식의 경계 값을 결정하는 것이 불가능할 수도 있다.
4. 호출자에게 특별한 방침이나 호출 규약을 요구한다. mean함수를 제대로 사용하려면 호출자가 그냥 mean을 호출해서 그 결과를 사용하는 것이상의 작업을 수행해야 한다. 이런 방침은 모든 인수를 균일한 방식으로 처리해야 하는 고차 함수에 전달이 어려워진다.

```scala
def max[A](xs: Seq[A])(greater: (A,A) => Boolean): A

// A형식의 값 중 입력이 빈 순차열임을 나타내는 데 사용할 하나의 값을 정하는 것이 불가능. 
// null은 오직 기본 형식이 아닌 형식에만 유효한데, A는 Double 이나 Int같은 기본형일 수도 있다.
```    

### 대안 2. 함수가 입력을 처리할 수 없는 상황에 처했을 때 무엇을 해야 하는지 말해주는 인수를 호출자가 지정하는 것 

```scala
def mean_1(xs: IndexedSeq[Double], onEmpty: Double): Double = 
  if (xs.isEmpty) onEmpty
  else xs.sum / xs.length
```
 
- 이렇게 하면 mean은 부분함수가 아닌 완전 함수(total function) 가 된다.
- 결과가 정의되지 않는 경우의 처리 방식을 함수의 직접적인 호출자가 알고 있어야하고, 그런 경우에도 항상 하나의 Double 값을 결과로 돌려주어야 한다는 단점.
- 이는 유연하지 않고 우리에게 필요한 것은 정의되지 않은 경우가 가장 적당한 수준에서 처리되도록 그 처리 방식의 결정을 미룰 수 있게 하는 방법이다.

## 4.3 Option 자료 형식

함수가 항상 답을 내지는 못한다는 점을 반환 형식을 통해서 명시적으로 표현

```scala
sealed trait Option[+A]
case class Some[+A](get: A) extends Option[A]
case object None extends Option[Nothing]
```

`Option`은 값을 정의할 수 있는 경우 `Some`이 되고, 정의할 수 없는 경우에는 `None`이 된다.

```scala
def mean(xs: Seq[Deouble]): Option[Double] = 
  if (xs.isEmpty) None
  else Some(xs.sum / xs.length)
```

함수가 선언된 반환 형식(Option[Double])을 반환한다는 사실은 여전하므로 `mean` 함수는 이제 하나의 **완전 함수**이다. 이 함수는 입력 형식의 모든 값에 대해 정확히 하나의 출력 형식 값을 돌려준다.

### 4.3.1 Option의 사용 패턴

부분 함수는 프로그래밍에서 흔히 볼 수 있으며, FP에서는 그런 부분성을 흔히 Option 같은 자료 형식(또는 Either 자료 형식)으로 처리한다.

- [Map](http://www.scala-lang.org/api/current/index.html#scala.collection.Map)에서 주어진 키를 찾는 함수는 Option을 돌려준다.
- [목록과 기타 반복 가능 자료 형식](http://www.scala-lang.org/api/current/index.html#scala.collection.immutable.List)에 정의된 headOption과 lastOption은 순차열이 비지 않은 경우 첫 요소 또는 마지막 요소를 담은 Option을 돌려준다.

Option이 편리한 이유는, 오류 처리의 공통 패턴을 고차 함수들을 이용해서 추출함으로써 예외 처리 코드에 흔히 수반되는 boilerplate 코드를 작성하지 않아도 된다는 점이다.

#### Option에 대한 기본적인 함수들

Option은 최대 하나의 원소를 담을 수 있다는 점을 제외하면 List와 비슷하다.

```scala
trait Option[+A] {
  def map[B](f: A => B): Option[B]
  def flatMap[B](f: A => Option[B]): Option[B]
  def getOrElse[B >: A](default: => B): B
  def orElse[B >: A](op: => Option[B]): Option[B]
  def filter(f: A => Boolean): Option[A]
}
```

###### 몇 가지 새로운 구문

> `default: => B`: **비엄격성(non-strictness)**을 표현하는 구문으로, 인수가 실제로 쓰일 때까지 평가되지 않는다.  
> `B >: A`: B가 반드시 A와 같거나 A의 **상위형식(supertype)**이어야 함을 뜻한다.

###### ■ 연습문제 4.1

> 목록 4.2에 나온 Option에 대한 함수들을 모두 구현하라. 각 함수를 구현할 때 그 함수가 어떤 일을 하고 어떤 상황에서 쓰일 것인지 생각해 볼 것. 이 함수들 각각의 용도를 잠시 후에 보게 될 것이다. 다음은 이 연습문제를 위한 몇 가지 힌트이다.
> 
> - 패턴 부합을 사용해도 좋으나, map과 getOrElse를 제외한 모든 함수는 패턴 부합 없이도 구현할 수 있어야 한다.
> - map과 flatMap의 형식 서명은 구현을 결정하기에 충분해야 한다.
> - getOrElse는 Option의 Some 안의 결과를 돌려준다. 단, Option이 None이면 주어진 기본값을 돌려준다.
> - orElse는 첫 Option이 정의되어 있으면 그것을 돌려주고 그렇지 않으면 둘째 Option을 돌려준다.

#### 기본적인 Option 함수들의 용례

`map` 함수는 Option 안의 결과를 변환하는 데 사용할 수 있다.

```scala
case class Employee(name: String, department: String)

def lookupByName(name: String): Option[Employee] = ...

val joeDepartment: Option[String] = 
  lookupByName("Joe").map(_.department)
```

만약 `lookupByName("Joe")`가 `None`을 돌려주었다면 **계산의 나머지 부분이 취소**되어서 `map`은 `_.department`를 호출하지 않는다.

변환을 위해 지정한 함수 자체가 실패할 수 있다는 점만 빼면 `flatMap`도 이와 비슷하다.

###### ■ 연습문제 4.2

> variance 함수를 flatMap을 이용해서 구현하라. 순차열의 평균이 m이라 할 때, 분산(variance)은 순차열의 각 요소 x에 대한 math.pow(x - m, 2)들의 평균이다. 분산의 좀 더 자세한 정의는 [위키백과](https://en.wikipedia.org/wiki/Variance#Definition)를 참고하기 바란다.
> 
> `def variance(xs: Seq[Double]): Option[Double]`

`filter`는 성공적인 값이 주어진 술어와 부합하지 않을 때 성공을 실패로 변환하는데 사용할 수 있다.

```scala
val dept: String = 
  lookupByName("Joe").map(_.dept)
                     .filter(_ != "Accounting")
                     .getOrElse("Default Dept")
```

`orElse`는 `getOrElse`와 비슷하되 첫 Option이 정의되지 않으면 다른 Option을 돌려준다.

흔한 관용구로, `o.getOrElse(throw new Exception("FAIL"))`은 Option의 None의 경우를 예외로 처리되게 만든다. _합리적인 프로그램이라면 결코 예외를 잡을 수 없을 상황에서만 예외를 사용한다._

이상에서 보듯이 오류를 보통의 값으로서 돌려주면 코드를 짜기가 편해지며, 고차 함수를 사용함으로써 예외의 주된 장점인 오류 처리 논리의 통합과 격리도 유지할 수 있다.

### 4.3.2 예외 지향적 API의 Option 합성과 승급, 감싸기

보통의 함수를 Option에 대해 작용하는 함수로 **승급(lift)** 시킬 수 있다.

```scala
def lift[A, B](f: A => B): Option[A] => Option[B] = _ map f

val abs0: Option[Double] => Option[Double] = lift(math.abs)
```

`lift`가 있으면 어떤 함수라도 Option 값의 **문맥 안에서** 작용하도록 변환할 수 있다.

##### 또 다른 예: 보험료율(insurance rate) 함수

```scala
/**
 * 두 가지 핵심 요인으로 연간 자동차 보험료를 계산하는 일급비밀 공식
 */
def insuranceRateQuote(age: Int, numberOfSpeedingTickets: Int): Double
```

`insuranceRateQuote` 함수를 사용하기 위해서 `age`와 `numberOfSpeedingTickets`을 넘겨주어야 하는데, 이 값이 문자열로 되어있을 경우 정수 값으로 파싱해야 한다.

```scala
def parseInsuranceRateQuote(
  age: String, 
  numberOfSpeedingTickets: String): Option[Double] = {
    val optAge: Option[Int] = Try(age.toInt)
    val optTickets: Option[Int] = Try(numberOfSpeedingTickets.toInt)

    insuranceRateQuote(optAge, optTickets) // 형식이 맞지 않는다.
  }

def Try[A](a: => A): Option[A] = 
  try Some(a)
  catch { case e: Exception => None }
```

`optAge`와 `optTickets`은 `Option[Int]` 형식인데, `insuranceRateQuote` 함수는 `Int` 형식을 요구한다. `insuranceRateQuote` 함수를 수정하는 대신, 그것을 Option 문맥 안에서 작동하도록 승급시키는 것이 바람직하다.

###### ■ 연습문제 4.3

> 두 Option 값을 이항 함수(binary function)을 이용해서 결합하는 일반적 함수 map2를 작성하라. 두 Option 값 중 하나라도 None이면 map2의 결과 역시 None이어야 한다. 서명은 다음과 같다.
> 
> `def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C]`

##### map2로 parseInsuranceRateQuote를 구현한 예

```scala
def parseInsuranceRateQuote(
  age: String,
  numberOfSpeedingTickets: String): Option[Double] = {
    val optAge: Option[Int] = Try { age.toInt }
    val optTickets: Option[Int] = Try { numberOfSpeedingTickets.toInt }
    
    map2(optAge, optTickets)(insuranceRateQuote)
  }
```

###### ■ 연습문제 4.4

> Option들의 목록을 받고, 그 목록에 있는 모든 Some 값으로 구성된 목록을 담은 Option을 돌려주는 함수 sequence를 작성하라. 원래의 목록에 None이 하나라도 있으면 함수의 결과도 None이어야 한다. 그렇지 않으면 원래의 목록에 있는 모든 값의 목록을 담은 Some을 돌려주어야 한다. 서명은 다음과 같다.
> 
> `def sequence[A](a: List[Option[A]]): Option[List[A]]`

실패할 수 있는 함수를 목록에 사상했을 때 만일 목록의 원소 중 하나라도 None을 돌려주면 전체 결과가 None이 되어야 하는 경우가 있다.

```scala
def parseInts(a: List[String]): Option[List[Int]] = 
  sequence(a map (i => Try(i.toint)))
```

위의 예에서 처럼 map의 결과를 sequence로 순차 결합하는 방식은 목록을 두 번 훑어야 하기 때문에 비효율적이다. 이러한 map의 결과의 순차 결합은 흔한 작업이기 때문에 다음과 같은 서명의 일반적 함수 `traverse`가 필요하다.

```scala
def traverse[A, B](a: List[A]))(f: A => Option[B]): Option[List[B]]
```

###### ■ 연습문제 4.5

> 이 함수를 구현하라. map과 sequence를 사용하면 간단하겠지만, 목록을 단 한 번만 훑는 좀 더 효율적인 구현을 시도해 볼 것. 더 나아가서, sequence를 이 traverse로 구현해 보라.

###### for-함축(for-comprehension)

> 스칼라에서 이런 승급 함수들이 흔히 쓰이기 때문에, **for-comprehension**이라고 하는 특별한 구문을 제공한다. for-comprehension은 자동으로 flatMap, map 호출들로 전개된다.
> 
> **원래의 버전**
> 
> ```scala
> def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] =
>   a flatMap (aa => 
>     b map (bb =>
>       f(aa, bb)))
> ```
> 
> **for-comprehension을 이용한 버전**
> 
> ```scala
> def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] =
>   for {
>     aa <- a
>     bb <- b
>   } yield f(aa, bb)
> ```
> 
> for-comprehension 구문은 중괄호쌍 안에 `aa <- a` 같은 binding이 있고 그 다음에 yield 표현식이 오는 형태이다. 컴파일러는 이러한 binding들을 flatMap 호출로 전개하되, 마지막 binding과 yield는 map 호출로 변환한다.

## 4.4 Either 자료 형식

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

## 4.5 요약

예제에서는 대수적 자료 형식 Option, Either에 초점을 맞췄지만 좀 더 일반적인 착안은 예외를 보통의 값으로 표현하고 고차 함수를 이용해서 오류 처리 및 전파의 공통 패턴들을 캡슐화한다는 것이다. 
이를 더욱 일반화하면 임의의 효과를 값으로 표현한다는 착안이 된다.

----------------------------------------------------------------------

## 4장 보충

### sealed trait Option[+A]

- +(covariant), -(contravariant)
- 변성 표기(Variance annotations)
- +(covariant) 는 Dog가 Animal 의 서브타입이면 Option[Dog]가 Option[Animal]의 서브타입
- -(contravariant)는 반대

### def orElse[B>:A](ob: ⇒ Option[B]): Option[B]

- >: 하위 바운드(Lower bounds)
- <: 상위 바운드(Upper bounds)
- 하위 바운드의 경우 B는 A의 슈퍼타입이어야 한다
- 상위 바운드는 반대
- 메소드의 반환값도 Option[A] 대신 Option[B]
- 예로 Fruit과 두 서브클래스인 Apple과 Orange 가 있을때 Orange를 Option[Apple]에 추가할 수 있고 그 결과는 Option[Fruit]

### covariant 와 Lower bounds의 관계

#### covariant의 부작용

```scala
class Cell[T](init: T) {
  private[this] var current = init
  def get = current
  def set(x:T) = current = x
}
```

- 위 무공변 코드를 공변으로 가정(Cell[+T]) 한다면

```scala
val a = new Cell[String]("abc")
val c2: Cell[Any] = c1
c2.set(1)
val s: String = c1.get
```

- 공변적이기 때문에 가능해 보이지만 타입 건전성에 위배
- Cell 코드를 공변적으로 바꾸어 보면

```
<console>:14: error: covariant type T occurs in contravariant position in type T of value x
         def set(x:T) = current = x
                 ^
```

- 위와 같은 에러가 발생
- 이는 위에서 보인 부작용을 방지하기 위해 컴파일러가 변성 표기 검사를 하기 때문
- 재할당 가능한 필드는 +로 표시한 타입 파라미터를 메소드 파라미터에 사용할 수 없다는 규칙

#### 대안

- 타입 건전성을 해치지 않고 컴파일 오류를 피하려면 하위 바운드를 이용
- 예) [Queue](https://github.com/scala/scala/blob/2.12.x/src/library/scala/collection/immutable/Queue.scala#L40) 자료구조에서 [enqueue](https://github.com/scala/scala/blob/2.12.x/src/library/scala/collection/immutable/Queue.scala#L114) 할때
- 마찬가지로 Fruit과 두 서브클래스인 Apple과 Orange

```scala
class Fruit
class Apple extends Fruit
class Orange extends Fruit
```

- Apple 이 들어있는 Queue에 Orange 를 enqueue 하면 Orange 의 슈퍼클래스인 Fruit이 된다

```
scala> val a = Queue(new Apple)
a: scala.collection.immutable.Queue[Apple] = Queue(Apple@460df441)
scala> a
res0: scala.collection.immutable.Queue[Apple] = Queue(Apple@460df441)
scala> a.enqueue(new Orange)
res1: scala.collection.immutable.Queue[Fruit] = Queue(Apple@460df441, Orange@28c38eeb)
```

- 재할당하는 필드에 대해서 슈퍼타입으로 (하위)바운드 시켜주면 공변적인 경우에도 부작용없이 처리가 가능(컴파일 오류도 없음)
- 따라서 공변성과 하위 바운드를 함께 사용하면 각기 다른 타입의 원소에 대해 유연해진다

**Reference** : Programming in Scala 책 19.3장 참고

덧 :
위 내용은 OOP적인 특성이므로 본 스터디의 목적인 함수형 프로그래밍과는 거리가 있음,
고로 갈길이 멀기 때문에 굳이 헛심 빼지 말자.
