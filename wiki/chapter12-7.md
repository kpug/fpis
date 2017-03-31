# 12.7 Traverse의 용도

- 연습문제 12.14
	
```	
type Id[A] = A

val idMonad = new Monad[Id] {
	def unit[A](a: => A) = a
	override def flatMap[A,B](a: A)(f: A => B): B = f(a)
}

def map[A,B](fa: F[A])(f: A => B): F[B] =
	traverse[Id, A, B](xs)(f)(idMonad)

trait Traverse[F[_]] extends Functor[F] {
	def traverse[G[_]:Applicative,A,B](fa: F[A])(f: A => G[B]): G[F[B]] =
		sequence(map(fa)(f))

	def sequence[G[_]:Applicative,A](fga: F[G[A]]): G[F[A]] =
		traverse(fga)(ga => ga)

	def map[A,B](fa: F[A])(f: A => B): F[B] =
		traverse[Id, A, B](fa)(f)(idMonad)
}

```

## 12.7.1 모노이드에서 적용성 함수자로

traverse가 map보다 더 일반적이라는 사실을 배웠다. 이번 절에서는 traverse로 foldMap을 표현할 수 있으며, 그럼으로써 foldLeft와 foldRight도 표현할 수 있음을 배울 것이다.
traverse의 서명을 다시 살펴보자.

`
def traverse[G[_]]: Applicative, A ,B](fa: F[A])(F: A=> G[B]): G[F[B]]
`

G가 임의의 형식을 Int로 사상하는 형식 생성자 ConstInt라고 하자.
즉, ConstInt[A]는 자신의 형식 인수 A를 버리고 그냥 Int만 돌려준다.

`
type ConstInt[A] = Int
`

traverse의 형식 서명에서 G를 ConstInt로 인스턴스화하면 다음과 같은 형식 서명이 나온다.

`
def traverse[A,B](fa: F[A])(f: A => Int): Int
`

이는 Foldable의 foldMap과 아주 비슷한 모습이다. 만일 F가 List같은 것이라면, 이 서명을 구현하는 데 필요한 것은 f가 돌려준 Int값들을 목록의 각 요소와 결합하는 수단과 빈 목록을 처리하기 위한 '시작' 값 뿐이다.
다른 말로 하면 Monoid[Int]만 있으면 된다. 그리고 그런 Monoid[Int]를 마련하는 것은 쉬운 일이다.
 사실, 앞에서 언급한 상수 함수자가 주어진다면, 임의의 Monoid를 Applicative로 변환할 수 있다.


```
type Const[M, B] = M  // 임의의 M에 대해 일반화된 ConstInt이다.

implicit def monoidApplicative[M](M: Monoid[M]) =
	new Applicative[({ type f[x] = Const[M, x] })#f] {
		def unit[A](a: => A): M = M.zero
		def map2[A,B,C](m1: M, m2: M)(f: (A, B) => C): M = M.op(m1, m2)
    }

```
 이는 Traverse가 Foldable을 확장할 수 있으며 foldMap의 기본 구현을 traverse를 이용해서 제공할 수 있음을 제공한다.


```
trait Traverse[F[_]] extends Functor[F] with Foldable[F] {
  ...
  def foldMap[A,M](as: F[A])(f: A => M)(mb: Monoid[M]): M =
    traverse[({type f[x] = Const[M, x]})#f, A, Nothing](as)(f)(monoidApplictive(mb))
}
```

Traverse는 이제 Foldable 뿐만 아니라 Functor도 확장함을 주목하기 바란다. 여기서 중요한 점은, Foldable 자체는 Funcotr를 확장하지 못한다는 것이다. List같은 가장 구체적인 Foldable 자료구조에 대한 접기 연산으로 map을 작성하는 것은 가능하다 해도, 일반적으로는 불가능하다.


- 연습문제 12.15
```
case class Iteration[A](a: A, f: A => A, n: Int) {
	def foldMap[B](g: A => B)(M: Monoid[B]): B = {
	def iterate(n: Int, b: B, c: A): B =
		if (n <= 0) b else iterate(n-1, g(c), f(a))
	iterate(n, M.zero, a)
	}
}

foldRight, foldLeft, foldMap은 foldable 타입의 값을 만들 수 있는 방법이 없기 때문이다.
구조체를 매핑하기 위해서는 새로운 구조체를 생성 할 수 있어야 하는데 (예를들어 List의 경우 Nil과 Cons 같은)
하지만 Traverse는 원래 구조를 보존하기 때문에 Functor를 확장이 가능하다.
```

## 12.7.2 상태있는 순회
State 동작과 traverse를 이용하면, 순회과정에서 일종의 내부 상태를 유지하면서 콜렉션을 훑는 코드를 작성 할 수 있다. 그런데 안타깝게도 State를 적절하게 부분 적용하려면 상당히 많은 형식 주해가 필요하다.
그래서 이를 위한 특별한 메서드를 만드는 것이 좋다.

```
def traverseS[S,A,B](fa: F[A])(f: A => State[S,B]): State[S, F[B]] = 
    traverse[({type f[x] = State[S,x]})#f, A, B](fa)(f)(Monad.stateMonad)
```
모든 요소에 그 요소의 위치를 설정하는 state 순회 함수를 살펴보자. 이 함수는 내부적으로 정수 상태 하나를 유지한다. (그 상태는 0에서 시작하여 단계마다 1씩 증가한다.)

```
// 순회 가능한 자료구조 안의 요소들에 번호 매기기기
def zipWithIndex_[A](ta: F[A]): F[(A,Int)] =
	traverseS(ta)((a: A) => (for {
		i <- get[Int]
		_ <- set(i + 1)
	} yield (a, i))).run(0)._1

이 정의는 List와 Tree를 비롯한 임의의 순회 가능 형식에 유효하다.
순회 도중 List[A] 형식의 상태를 유지하면서,
임의의 순회 가능 함수자를 하나의 List로 바꾸는 것도 가능하다.
```

```
// 순회 가능 함수자를 목록으로 변환
def toList_[A](fa: F[A]): List[A] =
	traverseS(fa)((a: A) => (for {
		as <- get[List[A]]	//현재 상태를 얻는다.
		_  <- set(a :: as) // 현재 요소를 추가하고, 새 목록을 새 상태로서 설정한다.
	} yield ())).run(Nil)._2.reverse

초기 상태에는 Nil로 시작하여 순회 도중 만나는 요소마다 그 요소를 누적 목록의 앞에 추가한다. 
그러면 목록이 순회 순서의 역순으로 구축되므로, 상태 동작이 완료되어서 목록이 완성되면 그것을 뒤집는다.
yeid() 호출은 이 인스턴스가 상태 이외에는 어떤 값도 돌려주지 않도록 하기 위한 것이다.
```

zipwithIndex와 toList의 코드가 거의 비슷하다. 그리고 State를 이용한 순회들은 대부분 이와 동일한 패턴, 즉 현재 상태를 얻고, 다음 상태를 계산하고, 그것을 현재 상태로 설정하고, 어떤 값을 산출(yield)하는 패턴을 따른다. 따라서 이 패턴을 하나의 함수로 갈무리해야 마땅하다.

```
// 공통의 패턴을 추출해서 만든 mapAccum 함수

def mapAccum[S,A,B](fa: F[A], s: S)(f: (A, S) => (B, S)): (F[B], S) =
	traverseS(fa)((a: A) => (for {
		s1 <- get[S]
		(b, s2) = f(a, s1)
		_  <- set(s2)
	} yield b)).run(s)

override def toList[A](fa: F[A]): List[A] =
	mapAccum(fa, List[A]())((a, s) => ((), a :: s))._2.reverse

def zipWithIndex[A](fa: F[A]): F[(A, Int)] =
	mapAccum(fa, 0)((a, s) => ((a, s), s + 1))._1
```

- 연습문제 12.16
```
def reverse[A](fa: F[A]): F[A] =
  mapAccum(fa, toList(fa).reverse)((_, as) => (as.head, as.tail))._1
```

- 연습문제 12.17
```
override def foldLeft[A,B](fa: F[A])(z: B)(f: (B, A) => B): B =
  mapAccum(fa, z)((a, b) => ((), f(b, a)))._2
```

## 12.7.3 순회 가능 구조의 조합
순회 가능 함수자는 반드시 인수의 형태를 유지해야 한다는 성질을 가지고 있다. 
이는 순회 가능 함수자의 강점이자 약점이다.
Traverse[F]가 주어졌을 때, 어떤 형식 F[A]의 값을 어떤 형식 F[B]의 값과 조합해서 F[C]를 얻을 수 있을까? mapAccum을 이용해서 zip의 일반적 버전을 작성해 보자.

```
// 서로 다른 두 구조 형식의 조합
def zip[A,B](fa: F[A], fb: F[B]): F[(A, B)] =
	(mapAccum(fa, toList(fb)) {
	case (a, Nil) => sys.error("zip: Incompatible shapes.")
	case (a, b :: bs) => ((a, b), bs)
})._1

이 버전의 zip이 '형태'가 다른 인수들은 처리하지 못함을 주목하기 바란다.
예를들어 F가 List일 때 이 버전은 길이가 다른 목록들은 재대로 처리하지 못한다.
이 구현에서 목록 fb는 반드시 fa와 길이가 같거나 더 길어야한다. 
F가 Tree이면 모든 수준에서 fb의 가지 개수가 fa의 가지 개수와 같거나 더 많아야 한다.

일반적 zip을 조금 변경해서 왼쪽 인수를 중심으로 한 버전과
오른쪽 인수를 중심으로 한 버전을 따로 만들면 이 문제를 해결 할 수 있다.
```
```
// zip의 좀 더 유연한 구현
def zipL[A,B](fa: F[A], fb: F[B]): F[(A, Option[B])] =
	(mapAccum(fa, toList(fb)) {
	case (a, Nil) => ((a, None), Nil)
	case (a, b :: bs) => ((a, Some(b)), bs)
})._1

def zipR[A,B](fa: F[A], fb: F[B]): F[(Option[A], B)] =
	(mapAccum(fb, toList(fa)) {
	case (b, Nil) => ((None, b), Nil)
	case (b, a :: as) => ((Some(a), b), as)
})._1

이 구현들은 List나 기타 순차열 형식에 잘 작동한다. 예를들어 List의 경우 zipR의 결과는 fb의 형태를 따른다.
만일 fb가 fa보다 길면 여분의 공간이 None들로 채워진다.
Tree같이 좀 더 흥미로운 형식에는 이 구현들이 그리 합당하지 않을 수 있다.
zipL은 오른쪽 인수를 그냥 List[B]로 평탄화(flattening)하기 때문에 원래의 구조가 사라진다.
Tree의 경우에는 각 노드의 이름표를 전위 순서(preorder)로 순회하는 것에 해당한다.
트리의 경우 zipL과 zipR은 두 트리가 같은 형태를 공유함을 알고 있을 떄 가장 유용하다. 
```


## 12.7.4 순회의 융합

5장에서는 한 자료구조를 여러번 훑는 연산을 한 번의 순회로 융합하는 방법을 논의했다.
10장에서는 모노이드 곱을 이용해서 접기 가능 구조에 대한 여러 번의 계산을 한번의 패스로 수행하는 방법을 살펴보았다. 마찬가지로, 적용성 함수자 곱을 이용하면 순회 가능 구조에 대한 다수의 순회를 융합할 수 있다.

- 연습문제 12.8
```
def fuse[G[_],H[_],A,B](fa: F[A])(f: A => G[B], g: A => H[B])
                       (implicit G: Applicative[G], H: Applicative[H]): (G[F[B]], H[F[B]]) =
  traverse[({type f[x] = (G[x], H[x])})#f, A, B](fa)(a => (f(a), g(a)))(G product H)
```

## 12.7.5 중첩된 순회
적용성 함수자 합성을 이용해서 순회들을 융합하는 것은 물론, 순회 가능 함수자들 자체를 합성할 수도 있다.
Map[K, Option[List[V]]] 같은 중첩된(내포된) 구조가 있을 때, Map, Option, List를 동시에 순회해서 그 안에 들어 있는 V값을 얻는 것은 쉽다. 이는 Map과 Option, List가 모두 순회 가능 구조라서 가능한 일이다.

## 12.7.6 모나드 합성
Applicative 인스턴스들은 항상 합성되지만 Monad 인스턴스들은 그렇지 않다. 이전에 일반적 모나드 합성을 구현하려 시도한 적이 있었지만, 중첩된 모나드 F와 G를 위한 join을 구현하려면 F[G[F[G[A]]]] => F[G[A]] 같은 형식을 작성해야 한다.
그런데 이는 일반적으로는 얻을 수 없는 형식이다. 그러나  G에 대한 Traverse 인스턴스가 존재한다면, sequence를 이용해서 G[F[ _ ]]를 F[G[ _ ]로 바꿀 수 있다. 그러면 F[F[G[G[A]]]]가 나오므로, 인접한 F계층들과 인접한 G계층들을 각각의 Monad 인스턴스를 이용해서 결합하면 된다.

> 자료구조가 표현력이 좋고 강력한 대신 합성 능력과 모듈성은 떨어지는 경우가 있다.
모나드 합성의 문제점을, 모나드마다 합성을 위해 특별하게 작성된 버전을 이용해서 해결하기도 하는데, 그런 것들을 모나드 변환기(monad transformer)라고 부른다.

```
case class optionT[M[_],A](vale: M[Option[A]])(implicit M: Moand[M]) {
	def flatMap[B](f: A => OptionT[M, B]): OptionT[M, B] =
		OptionT(value flatMap {
        case Some(a) => M.unit(None)
        case Some(a) => f(a).vlaue
      }
  }

이 구현은 주어진 함수를 M과 Option 모두에 사상하고, M[Option[M[Option[A]]]] 같은 구조를 그냥 M[Option[A]]로 평평하게 만든다. 그런데 이 구현은 Option에 특화된 것이다.
그리고 Traverse의 장점을 취하는 일반적 전략은 순회 가능 함수자에 적용된다.
예를들어 State(순회 가능이 아닌)와의 합성을 위해서는 특화된 StateT모나드 변환기를 작성해야 한다.
따라서 모든 모나드에 유효한 일반적 합성 전략은 없다.
```


