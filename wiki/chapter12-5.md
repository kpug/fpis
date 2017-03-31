## 12.5 적용성 함수자 법칙

### 12.5.1 왼쪽, 오른쪽 항등법칙

##### 함수자 법칙

```scala
map(v)(id) == v
map(map(v)(g))(f) == map(x)(f compose g)
```

##### 항등법칙

```scala
// map의 정의
def map[A, B](fa: F[A])(f: A => B): F[B] = 
  map2(fa, unit(()))((a, _) => f(a))

// unit()이 좌변에 놓여도 동일
def map[A, B](fa: F[A])(f: A => B): F[B] = 
  map2(unit(()), fa)((_, a) => f(a))
```

`map`의 두 구현은 **모두** 함수자 법칙들을 지킨다고 말할 수 있으며, 어떤 `fa: F[A]`에 대해 `map2`는 `unit`과 함께 `fa`의 구조를 보존한다.

```scala
map2(unit(()), fa)((_, a) => a) == fa // 왼쪽 항등법칙
map2(fa, unit(()))((a, _) => a) == fa // 오른쪽 항등법칙
```

### 12.5.2 결합법칙

```scala
def map3[A, B, C, D](fa: F[A], 
                     fb: F[B], 
                     fc: F[C])(f: (A, B, C) => D): F[D]
```

`map3`를 `map2`로 구현한다면,

- `fa`와 `fb`를 결합한 후 그 결과를 `fc`와 결합
- `fb`와 `fc`를 결합한 후 그 결과를 `fa`와 결합

적용성 함수자에 대한 결합법칙에 따르면 어떤 방식을 따르든 같은 결과가 나와야 한다.

##### 모노이드, 모나드의 결합법칙

```scala
op(a, op(b, c)) == op(op(a, b), c)
compose(f, op(g, h)) == compose(compose(f, g), h)
```

##### 적용성 함수자에 대한 결합법칙

```scala
def product[A, B](fa: F[A], fb: f[B]): F[(A, B)] =
  map2(fa, fb)((_, _))

def assoc[A, B, C](p: (A, (B, C))): ((A, B), C) =
  p match { case (a, (b, c)) => ((a, b), c) }

// product, assoc를 이용한 적용성 함수자의 결합법칙 표현
product(product(fa, fb), fc) == map(product(fa, product(fb, fc)))(assoc)
```

### 12.5.3 곱의 자연성 법칙

```scala
val F: Applicative[Option] = ...

case class Employee(name: String, id: Int)
case class Pay(rate: Double, hoursPerYear: Double)

def format(e: Option[Employee], pay: Option[Pay]): Option[String] = 
  F.map2(e, pay) { (e, pay) =>
    s"${e.name} makes ${pay.rate * pay.hoursPerYear}"
  }

val e: Option[Employee] = ...
val pay: Option[Pay] = ...
format(e, pay)
```

`format`이 `Option[Employee]`와 `Option[Pay]` 대신 `Option[String]`과 `Option[Double]`을 받도록 구현하면 `Employee`, `Pay` 자료 형식을 직접적으로 알 필요가 없어진다.

```scala
val F: Applicative[Option] = ...

def format(name: Option[String], pay: Option[Double]): Option[String] = 
  F.map2(name, pay) { (name, pay) => s"$name makes $pay" }

val e: Option[Employee] = ...
val pay: Option[Pay] = ...

format(
  F.map(e)(_.name),
  F.map(pay)(pay => pay.rate * pay.hoursPerYear))
```

Applicative 효과들을 다룰 때에는 `map2`로 값들을 **결합하기 전에** 변환을 적용할 수도 있고 **결합한 후에** 적용할 수도 있는 경우가 많으며, 자연성 법칙은 **어떤 쪽을 선택하든 결과가 같음**을 말해준다.

```scala
def productF[I1, O1, I2, O2](f: I1 => O1, g: I2 => O2): (I1, I2) => (O1, O2) =
  (i1, i2) => (f(i1), g(i2))

map2(a, b)(productF(f, g)) == product(map(a)(f), map(b)(g))
```

적용성 함수자에 대한 법칙들은 `unit`, `map`, `map2`가 일관되고 합리적인 방식으로 작동함을 보장한다.

###### □ 연습문제 12.7

> **어려움**: 모든 모나드가 적용성 함수자임들 증명하라. 만일 모나드 법칙들이 성립한다면 map2와 map의 Monad 구현들이 적용성 법칙들을 만족함을 보이면 된다.

###### ■ 연습문제 12.8

> 두 모노이드 A와 B의 곱(product)을 취하면 모노이드 (A, B)가 나온다. 그와 비슷하게, 두 적용성 함수자의 곱을 산출하는 다음 함수를 구현하라.
> 
> `def product[G[_]](G: Applicative[G]): Applicative[({type f[x] = (F[x], G[x])})#f]`

###### ■ 연습문제 12.9

> **어려움**: 적용성 함수자들은 이런 방식으로도 합성된다. 만일 `F[_]`와 `G[_]`가 적용성 함수자이면 `F[G[_]]`도 적용성 함수자이다. 이 함수를 구현하라.
> 
> `def compose[G[_]](G: Applicative[G]): Applicative[({type F[x] = F[G[x]]})#f]`

###### □ 연습문제 12.10

> **어려움**: 이러한 합성 적용성 함수자가 적용성 법칙들을 만족함을 증명하라. 이는 대단히 도전적인 연습문제이다. 

###### □ 연습문제 12.11

> Monad에 대한 compose를 작성해 보라. 사실 구현이 불가능하지만, 시도해 보고 왜 구현이 불가능한지 이해한다면 많은 것을 배울 수 있을 것이다.
> 
> `def compose[G[_]](G: Monad[G]): Monad[({type f[x] = F[G[x]]})#f]`

