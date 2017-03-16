10.5 접을 수 있는 자료구조
===============


접을 수 있는 자료구조
--------------

- List
- Tree
- Stream
- IndexedSeq

접을 수 있는 자료구조를 처리해야 하는 코드를 작성할 때
---------------
- 구체적인 형식(자료구조의 형태, 지연 여부, 효율적인 임의 접근 능력 등)을 신경 쓸 필요가 없다.
- foldLeft, foldRight, foldMap을 사용한다.

```
trait Foldable[F[_]] {
	def foldRight[A,B](as: F[A])(z: B)(f: (A,B) => B): B
	def foldLeft[A,B](as: F[A])(z: B)(f: (B,A) => B): B
	def foldMap[A,B](as: F[A])(f: A => B)(mb: Monoid[B]): B
	def concatenate[A](as: F[A])(m: Monoid[A]): A =
		foldLeft(as)(m.zero)(m.op)
}
```
- F[_]: 밑줄은 F가 형식이 아니라 하나의 형식 인수를 받는 형식 생성자(type constructor)임을 나타낸다.
- Foldable: 다른 형식 생성자를 인수로 받는 형식 생성자 -> 고차 형식 생성자(higher-order type constructor) or 상위 종류 형식(higher-kinded type)

10.6 모노이드 합성
===
모노이드의 진정한 위력은 그 합성 능력에서 비롯된다.
---
- 형식 A와 B가 모노이드이면 튜플 형식 (A, B)(이것을 두 모노이드의 곱(product)이라고 부른다.) 역시 모노이드임을 뜻한다.

10.6.1 좀 더 복잡한 모노이드 합성
---
- 자료구조에 담긴 요소들의 형식들이 모노이드를 형성한다면 그 자료구조 자체도 흥미로운 모노이드를 형성하기도 한다.
- ex) 키-값 Map의 값 형식이 모노이드면 그런 Map들을 병합하기 위한 모노이드가 존재한다.

목록 10.1 키-값 Map들의 병합

```
def mapMergeMonoid[K,V](V: Monoid[V]): Monoid[Map[K, V]] =
	new Monoid[Map[K, V]] {
		def zero = Map[K,V]()
		def op(a: Map[K, V], b: Map[K, V]) =
			(a.keySet ++ b.keySet).foldLeft(zero) { (acc,k) =>
				acc.updated(k, V.op(a.getOrElse(k, V.zero),
									b.getOrElse(k, V.zero)))
		}
	}
```
이 간단한 조합기를 이용하면 좀 더 복잡한 모노이드를 상당히 수월하게 조립할 수 있다.

```
scala> val M: Monoid[Map[String, Map[String, Int]]] =
     | mapMergeMonoid(mapMergeMonoid(intAddition))
M: Monoid[Map[String, Map[String, Int]]] = $anon$1@21dfac82
```
이에 의해, 추가적인 프로그래밍 없이도 모노이드를 이용해서 중첩된 표현식들을 조합할 수 있게 된다.

```
scala> val m1 = Map("o1" -> Map("i1" -> 1, "i2" -> 2))
m1: Map[String,Map[String,Int]] = Map(o1 -> Map(i1 -> 1, i2 -> 2))

scala> val m2 = Map("o1" -> Map("i2" -> 3))
m2: Map[String,Map[String,Int]] = Map(o1 -> Map(i2 -> 3))

scala> val m3 = M.op(m1, m2)
m3: Map[String,Map[String,Int]] = Map(o1 -> Map(i1 -> 1, i2 -> 5))
```

10.6.2 모노이드 합성을 이용한 순회 융합(Using composed monoids to fuse traversals)
---
- 여러 모노이드를 하나로 합성할 수 있다는 사실은 자료구조를 접을 때 여러 계산을 동시에 수행할 수 있음을 뜻한다.

- ex) 목록의 평균을 구할 때, 다음과 같이 목록의 길이와 합을 동시에 구할 수 있다.

```
scala> val m = productMonoid(intAddition, intAddition)
m: Monoid[(Int, Int)] = $anon$1@8ff557a

scala> val p = listFoldable.foldMap(List(1,2,3,4))(a => (1, a))(m)
p: (Int, Int) = (4, 10)

scala> val mean = p._1 / p._2.toDouble
mean: Double = 2.5
```
- 모노이드를 productMonoid와 foldMap을 이용해서 일일이 조립하는 것이 좀 번거롭다.
- 그 이유는 foldMap의 매핑 함수로부터 Monoid를 구축할 때 형식을 일일이 맞추어 주어야 하기 때문이다.
- 합성된 모노이드들을 조립하는 작업과 병렬화해서 하나의 path로 실행할 수 있는 계산을 훨ㄹ씬 더 편하게 정의할 수 있는 **조합기 라이브러리**를 만들면 된다.
- 웹 부록의 이번 장 참고자료 보세요.

10.7 요약
===
- 모노이드는 결합법칙을 만족하기 때문에 Foldable을 지원하는 자료 형식을 접을 수 있다.
- 접기 연산을 병렬적으로 수행할 수 있는 유연성이 있다.
- 모노이드는 합성이 가능하기 때문에, 모노이드들을 이용해서 선언적이고 재사용 가능한 방식으로 접기 연산을 조립할 수 있다.
- 인수 형식이 하나의 모노이드를 형성한다는 것만 알면 인수에 대한 다른 정보를 몰라도 유용한 함수를 작성할 수 있다.