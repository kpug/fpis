## 9. 파서 조합기 라이브러리

* 파서(Parser) 생성을 위한 라이브러리 설계
* 파싱 자체 보다는 함수적 설계 과정에 대한 영감을 제공하는데 초점
* 대수적 설계: 앞장에서 이미 해왔던 방식  
   (인터페이스를 먼저 설계하고, 관련 법칙들을 파악하고, 그에 따라 자료 형식의 표현을 선핵하는 등)의 자연스러운 진화

### 9.1 대수의 설계: 첫 시도

* 하나의 character를 인식하는 파서, 조합기 char를 창안

```scala
def char(c: Char): Parser[Char]
```

* 파서를 실행할 수 있는 함수를 창안하자.  
   성공하면 파싱된 값을 실패시 실패에 관한 정보를 돌려주도록 ...

```scala
def run[A](p: Parser[A])(input: String): Either[ParseError, A]
```

* Parser, ParseError의 표현을 신경쓰지 않고 라이브러리를 설계 하기 위해 두 형식을 사용하는 인터페이스를 명시하자.

```scala
trait Parsers[ParseError, Parser[+, _]] {
  def run[A](p: Parser[A])(input: String): Either[ParseError, A]
  def char[c: Char]: Parser[Char]
}
```

* 함수 char는 다음의 자명한 법칙을 만족해야 한다.

```scala
run(char(c))(c.toString) == Right(c)
```

* 이제 "abracadabra" 같은 문자열을 인식하는 수단을 추가하자.

```scala
def string(s: String): Parser[String]
```

* 이 함수 역시 다음의 자명한 법칙을 따른다.

```scala
run(string(s))(s) == Right(s)
```

* 문자열 "abra" 또는 "cadabra"를 인식하기 위해서...

```scala
def orString(s1: String, s2: String): Parser[String]
```

* 좀 더 일반화하여 결과 형식에 무관하게 두 파서 중 하나를 선택하게 하는 조합기

```scala
def or[A](s1: Parser[A], s2: Parser[A]): Parser[A]
```

* or를 이용한 orString

```scala
def orString(s1: String, s2: String): Parser[String] =
  or(string(s1), string(s2))

run(or(string("abra"), string("cadabra")))("abra") == Right("abra")
run(or(string("abra"), string("cadabra")))("cadabra") == Right("cadabra")
```

* or 조합기에 s1 | s2 또는 s1 or s2 같은 멋진 중위 구문(infix syntax)를 부여하는 것도 가능  
  (암묵적 변환을 사용하자.)

```scala
trait Parsers[ParseError, Parser[+_]] { self =>
  ...
  def or[A](s1: Parser[A], s2: Parser[A]): Parser[A]
  implicit def string(s: String): Parser[String]
  implicit def operators[A](p: Parser[A]) = ParserOps[A](p)
  implicit def asStringParser[A](a: A)(implicit f: A => Parser[String]): ParserOps[String] = ParserOps(f(a))

  case class ParserOps[A](p: Parser[A) {
    def |[B>:A](p2: Parser[B]): Parser[B] = self.or(p, p2)
    def or[B>:A](p2: Parser[B]): Parser[B] = self.or(p, p2)
  }
}

// "abra" | "cadabra"

// implicit def asStringParser[A](a: A)(implicit f: A => Parser[String]): ParserOps[String] = ParserOps(f(a))
// "abra".|("cadabra")
// asStringParser("abra")(string)
// ParserOps(string("abra"))
// ParserOps(string("abra")).|(string("cadabra"))
// Parsers.or(string("abra"), string("cadabra"))

// implicit def operators[A](p: Parser[A]) = ParserOps[A](p)
// operators(string("abra")) | operators(string("cadabra"))

// implicit def operators[A](p: Parser[A]) = ParserOps[A](p)
// operators(string("abra")) | operators(string("cadabra"))
```

> **explicitly-typed-self-references**  
http://docs.scala-lang.org/tutorials/tour/explicitly-typed-self-references.html

```scala
trait Parsers[ParseError, Parser[+_]] {
  private[this] val self = this
  ...
}
```

* "abra" | "cadabra" 를 세번 되풀이 되는 경우, 새 조합기를 추가하자.

```scala
def listOfN[A](n: Int, p: Parser[A]):Parser[List[A]]

run(listOfN(3, "ab" | "cad"))("ababcad") == Right(List("ab", "ab", "cad"))
run(listOfN(3, "ab" | "cad"))("cadabab") == Right(List("cad", "ab", "ab"))
run(listOfN(3, "ab" | "cad"))("ababab") == Right(List("ab", "ab", "ab"))
```

### 9.2 가능한 대수 하나

* 0개 이상의 문자 'a'를 인식해서 개수를 돌려 주는 파서.

```scala
def many[A](p: Parser[A]): Parser[List[A]]
```

* Parser[Int]가 돌려줄 것으로 기대했지만, 너무 특화된 조합기가 될 수 있으므로, 또 다른 조합기를 도입해서 해결하자.

```scala
def map[A, B](a: Parser[A])(f: A =>  B): Parser[B]
```
```scala
map(many(char('a')))(_.size)
```

* 좀 더 깔끔한 코드를 위해 map과 many를 ParserOps에 추가하자.

```scala
val numA: Parser[Int] = char('a').many.map(_.size)

run(numA)("aaa") = Right(3)
run(numA)("bbb") = Right(0)
```

* map 구조를 보존해야한다.

```scala
map(p)(a => a) == p
```

```scala
trait Parsers[ParserError, Parser[+_]] {
  ...
  object Laws {
    def equal[A](p1: Parser[A], p2: Parser[A])(in: Gen[String]): Prop =
      forAll(in)(s => run(p1)(s) == run(p2)(s))

    def mapLaw[A](p: Parser[A])(in: Gen[String]): Prop =
      equal(p, p.map(a => a))(in)
  }
}
```

* map 과 string을 이용해 char를 구현

```scala
def char(c: Char): Parser[Char] =
  string(c.toString) map (_.charAt(0))
```

* map 과 string을 이용한 또 다른 조합기 succeed  
  항상 파싱에 성공해 입력값 a를 돌려준다.

```scala
def succeed[A](a: A): Parser[A] =
  string("") map (_ => a)

run(succeed(a))(s) == Right(a)
```

* 문자 'a'의 개수를 세는 파싱 과정에서 실제로 길이만 추출하고 중간에 구축된 List[Char] 값들은 폐기하므로 비효율 적이다.  
  파싱 성공 시 입력 문자열 중 파서가 조사한 부분만 돌려주게 하는 것

```scala
def slice[A](p: Parser[A]):Parser[String]
```

```scala
//String size - 상수 시간
char('a').many.slice.map(_.size)

//List[Char] size - 목록의 길이에 비례하는 시간
char('a').many.map(_.size)
```

* slice는 임수 목록이 생성되지 않아야 한다.  
이를 만족하기 위해 slice가 파서의 내부 구현에 접근해야 한다. (_하나의 기본 수단임을 강하게 암시_)  
  하나 이상의 'a' 문자들을 인식하기 위해서 '비지 않은 되풀이(non-empty repetition)'를 위한 새로운 조합기 many1

```scala
def many1[A](p: Parser[A]):Parser[List[A]]
```

* many1을 many를 이용해서 정의, many1(p)는 그냥 p 다음에 many(p)가 오는 것.  
  한 파서를 실행하고 성공하면, 또 다른 파서를 실행하는 조합기를 추가하자.

```scala
def product[A, B](p: Parser[A], p2: Parser[B]): Parser[(A, B)]
```
> ** 와 product를 ParserOps 의 메소드로 추가   

---
**연습문제 9.1**  
product을 이용해 map2 구현   
map2와 many를 이용해서 many1  
map2를 기본수단으로 두고 product를 map2를 이용해서 구현할 수도 있다.  

---

* 0개 이상의 a 다음에 하나 이상의 'b'들이 오는 문자열 파서

```scala
char('a').many.slice.map(_.size) ** char('b').many1.slice.map(_.size)
```

---
**연습문제 9.2**  
product의 행동 방식을 명시하는 법칙들을 고안하라.

---

* 새로운 조합기가 생기면 기존의 조합기들이 기본 수단인지 고찰할 필요가 있다.  
map2가 생겼으니, many가 기본수단인가? many가 하는 일을 생각해보자.

---
**연습문제 9.3**  
many를 or와 map2, succeed로 정의하라.

---

```scala
def many[A](p: Parser[A]): Parser[List[A]] =
  map2(p, many(p))(_ :: _) or succeed(List())
```


---
**연습문제 9.4**  
listOfN을 map2와 succeed로 구현하라.

---

> 위 구현의 문제점  
many가 map2의 둘째 인수로 제공된다.  
그런데 이 인수는 엄격한 인수라서 항상 평가된다.  
many의 재귀 호출과정이 p의 결과에 관계 없이 무조건 평가된다.  
이러한 문제점은 product와 map2의 둘째 인수를 엄격하지 않게 만들어야 한다.

```scala
def product[A, B](p: Parser[A], p2: => Parser[B]): Parser[(A, B)]

def map2(A, B, C)(p: Parser[A], p2: => Parser[B])(f: (A, B) => C): Parser[C] =
  product(p, p2) map (f.tupled)
```

---
**연습문제 9.5**  
비엄격성 문제를 위해 개별적인 조합기를 도입

---

```scala
def or[A](p1: Parser[A], p2: Parser[A]): Parser[A]

def or[A](p1: Parser[A], p2: => Parser[A]): Parser[A]
```
