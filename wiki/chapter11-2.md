## 11.3 모나드적 조합기

> 연습문제 11.3  
> sequence 조합기와 traverse 조합기는 이제 아주 익숙할 것이다.
> 그리고 이전 장들에서 독자가 작성한 구현들은 서로 아주 비슷할 것이다.
> 이 두 조합기를 Monad[F]에 대해 한 번씩만 작성하라.  
> `def sequence[A](lma: List[F[A]]): F[List[A]]`  
> `def traverse[A,B](la: List[A])(f: A => F[B]): F[List[B]]`  

```scala
def sequence[A](lma: List[F[A]]): F[List[A]] =
  lma.foldRight(unit(List[A]()))((ma, mla) => map2(ma, mla)(_ :: _))
  
def traverse[A,B](la: List[A])(f: A => F[B]): F[List[B]] =
  la.foldRight(unit(List[B]()))((a, mlb) => map2(f(a), mlb)(_ :: _))
```


파서 또는 생성기를 n번 되풀이해서 그 길이의 입력을 인식하는 파서 또는 그 개수만큼의 목록들을 생성하는 생성기를 얻는 데 사용되는 조합기를 Monad 특질에 추가.  
> 연습문제 11.4   
> replicateM을 구현하라.  
> `def replicateM[A](n: Int, ma: F[A]): F[List[A]]`  

```scala
// Recursive version:
def _replicateM[A](n: Int, ma: F[A]): F[List[A]] =
  if (n <= 0) unit(List[A]()) else map2(ma, _replicateM(n - 1, ma))(_ :: _)

// Using `sequence` and the `List.fill` function of the standard library:
def replicateM[A](n: Int, ma: F[A]): F[List[A]] =
  sequence(List.fill(n)(ma))
```


> 연습문제 11.5  
> 여러 구체적인 F에 대해 replicateM이 어떤 식으로 행동할지 생각해 보라. 예를 들어 List 모나드에 대해서는 어떻게 행동할까?
> Option은 어떨까? replicateM의 일반적 의미를 독자의 어법으로 서술하라.  


주어진 생성기 두 개로 쌍들을 생성하는 하나의 생성기를 돌려주는 조합기 product를 모나드 F에 대해 일반적으로 구현.  
`def product[A,B](ma: F[A], mb: F[B]): F[(A, B)] = map2(ma, mb)((_, _))`  


> 연습문제 11.6
> 어려움: 다음은 아직 본 적이 없는 함수의 예이다. 이 함수 filterM을 구현하라. 이 함수는 filter와 비슷하되,
> A => Boolean 형식의 함수가 아니라 A => F[Boolean] 형식의 함수를 사용한다는 점이 다르다.
> (이런 여러 보통 함수들을 모나드적 함수로 대체해 보면 흥미로운 결과가 나오는 경우가 많다.)
> 이 함수를 구현하고, 이것이 여러 자료 형식들에 어떤 의미인지도 생각해 보라.  
> `def filterM[A](ms: List[A])(f: A => F[Boolean]): F[List[A]]`  

```scala
def filterM[A](ms: List[A])(f: A => F[Boolean]): F[List[A]] =
   ms.foldRight(unit(List[A]()))((x,y) =>
     compose(f, (b: Boolean) => if (b) map2(unit(x),y)(_ :: _) else y)(x))
```



## 11.4 모나드 법칙

Monad[F]도 일종의 Functor[F]이므로 함수자 법칙들이 Monad에 대해서도 성립.  


### 11.4.1 결합법칙

Gen 모나드를 이용한 모의 주문 생성기  


```scala
// 목록 11.6 order 클래스의 정의
case class Order(item: Item, quantity: Int)
case class Item(name: String, price: Double)

val genOrder: Gen[Order] = for {
  name <- Gen.stringN(3)
  price <- Gen.uniform.map(_ * 10)
  quantity <- Gen.choose(1, 100)
} yield Order(Item(name, price), quantity)
```


Item을 개별적으로 생성하는 Item 생성기  

```scala
val genItem: Gen[Item] = for {
  name <- Gen.stringN(3)
  price <- Gen.uniform.map(_ * 10)
} yield Item(name, price)
```


위 아이템 생성기를 이용하도록 변경된 genOrder  

```scala
val genOrder: Gen[Order] = for {
  item <- genItem
  quantity <- Gen.choose(1,100)
} yield Order(item, quantity)
```


위 두 구현은 동일하지 않지만 그 결과는 동일함 확인 필요.  
map호출과 flatMap 호출로 전개하여 확인.  

```scala
// 전자의 경우
Gen.nextString.flatMap(name =>
Gen.nextDouble.flatMap(price =>
Gen.nextInt.map(quantity =>
  Order(Item(name, price), quantity))))
```

``` scala
// 후자의 경우
Gen.nextString.flatMap(name =>
Gen.nextInt.map(price =>
  Item(name, price))).flatMap(item =>
  Gen.nextInt.map(quantity =>
    Order(item, quantity)))
```

위 두 구현의 전개는 flatMap이 결합법칙을 만족한다는 가정하에 정확히 동일한 일을 하리라고 가정함이 합당하다.  

`x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))`  

이 법칙은 Gen 뿐만 아니라 Parser나 Option을 비롯한 모든 모나드에 성립.  



### 11.4.2 특정 모나드의 결합법칙 성립 증명  

Option에 대해 성립함 증명.  

- x가 None이라고 가정할 때.  

`None.flatMap(f).flatMap(g) == None.flatMap(a => f(a).flatMap(g))`  

위 등식을 줄이면 `None == None`  

즉, x가 None일 때 결합법칙이 성립한다.  


- x가 Some(v)이라고 가정할 때.  

```scala
Some(v).flatMap(f).flatMap(g) == Some(v).flatMap(a => f(a).flatMap(g))
f(v).flatMap(g) == (a => f(a).flatMap(g))(v)
f(v).flatMap(g) == f(v).flatMap(g)
```

이 법칙은 x가 임의의 v에 대한 Some(v)일 때에도 성립한다. 이로써 이 법칙이 x가 None일 때와 x가 Some일 때 성립함이 증명되었다.  
그리고 Option에 대해서는 그 두 가지 가능성밖에 없으므로, 이 법칙은 Option에 항상 성립한다.  


- 크라이슬리 화살표(Kleisli arrow)  
결합법칙을 만족하는 `A => F[B]` 같은 형식의 모나드적 함수  
크라이슬리 화살표들은 합성이 가능.  

`def compose[A,B,C](f: A => F[B], g: B => F[C]): A => F[C]`


> 연습문제 11.7  
> 크라이슬리 합성 함수 compose를 구현하라.  

```scala
def compose[A,B,C](f: A => F[B], g: B => F[C]): A => F[C] =
  a => flatMap(f(a))(g)
```

compose 함수를 이용하면 모나드에 관한 결합법칙을 훨씬 대칭적인 형태로 표현할 수 있다.  

`compose(compose(f, g), h) == compose(f, compose(g, h))`  
 

> 연습문제 11.8  
> 어려움: flatMap을 compose를 이용해서 구현하라. 이 구현이 가능하므로, compose와 unit은 모나드 조합기들의 또 다른 최소 집합이다.  

```scala
def _flatMap[A,B](ma: F[A])(f: A => F[B]): F[B] =
  compose((_:Unit) => ma, f)(())
```


> 연습문제 11.9  
> 결합법칙의 두 표현, 즉 flatMap을 사용한 표현과 compose를 사용한 표현이 동치임을 증명하라.  


### 11.4.3 항등법칙

모나드에서 compose에 대한 항등원이 존재.  
unit이 바로 그 항등원.  

`def unit[A](a: => A): F[A]`  

왼쪽 항등법칙 : `compose(f, unit) == f`  
오른쪽 항등법칙 : `compose(unit, f) == f`  

이 법칙들을 flatMap으로 표현.  
`flatMap(x)(unit) == x`  
`flatMap(unit(y))(f) == f(y)`  


> 연습문제 11.10  
> 항등법칙의 이 두 표현이 서로 동치임을 증명하라.  


> 연습문제 11.11  
> 모나드를 하나 선택해서, 그 모나드에 대해 항등법칙이 성립함을 증명하라.  


> 연습문제 11.12  
> 또 다른(셋째) 모나드 조합기 최소 집합으로 map, unit, join이 있다. join을 flatMap을 이용해서 구현하라.  
> `def join[A](mma: F[F[A]]): F[A]`  

```scala
def join[A](mma: F[F[A]]): F[A] = flatMap(mma)(ma => ma)
```


> 연습문제 11.13
> flatMap이나 compose를 join과 map을 이용해서 구현하라.  

```scala
def flatMap[A,B](ma: F[A])(f: A => F[B]): F[B] =
    join(map(ma)(f))
```


> 연습문제 11.14  
> 모나드 법칙들을 join과 map, unit만으로 표현하라.  


> 연습문제 11.15  
> Par와 Parser에 대한 결합법칙이 무엇을 뜻하는지를 독자의 어법으로 설명하라.  


> 연습문제 11.16  
> 항등법칙이 Gen과 List에 대해 구체적으로 어떤 의미인지를 독자의 어법으로 설명하라.  
