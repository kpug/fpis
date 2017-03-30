### 12.6 순회 가능 함수자

* traverse 함수와 sequence 함수가 flatMap에 직접 의존하지 않는다.
* 적용자 함수를 이용해 traverse, sequence를 다시 한번 일반화 해서 또 다른 추상을 찾아보자.
```scala
def traverse[F[_], A, B](as: List[A])(f: A => F[B]): F[List[B]]
def sequence[F[_], A](fas: List[F[A]]): F[List[A]]
```
* 항상 List에 한정적으로만 사용해야 되는가?  

---
##### `문제 12.12` sequence를 Map에 대해 구현하라.
```scala
def sequenceMap[K,V](ofa: Map[K,F[V]]): F[Map[K,V]] =
    (ofa foldLeft unit(Map.empty[K,V])) { case (acc, (k, fv)) =>
      map2(acc, fv)((m, v) => m + (k -> v))
    }
```
---

* 순회 가능한 자료형식들 모두 사용 가능하도록 추상화를 하자.
```scala
trait Traverse[F[_]] {
  def traverse[G[_]:Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]] =
    sequence(map(fa)(f))
  def sequence[G[_]:Applicative, A](fga: F[G[A]]): G[F[A]] =
    traverse(fga)(ga => ga)
```
> sequence 서명에서 G가 적용자 함수이면, F[G[A]] => G[F[A]] 이다.  
**이것이 뜻하는 바는 잠시 후에 논의**

---
##### `문제 12.13` Listdhk Option, Tree에 대한 Traverse 인스턴스를 작성하라.
```scala
val listTraverse = new Traverse[List] {
  override def traverse[G[_],A,B](as: List[A])(f: A => G[B])(implicit G: Applicative[G]): G[List[B]] =
    as.foldRight(G.unit(List[B]()))((a, fbs) => G.map2(f(a), fbs)(_ :: _))
}

val optionTraverse = new Traverse[Option] {
  override def traverse[G[_],A,B](oa: Option[A])(f: A => G[B])(implicit G: Applicative[G]): G[Option[B]] =
    oa match {
      case Some(a) => G.map(f(a))(Some(_))
      case None    => G.unit(None)
    }
}

val treeTraverse = new Traverse[Tree] {
  override def traverse[G[_],A,B](ta: Tree[A])(f: A => G[B])(implicit G: Applicative[G]): G[Tree[B]] =
    G.map2(f(ta.head), listTraverse.traverse(ta.tail)(a => traverse(a)(f)))(Tree(_, _))
}
```
---

* Applicative인 Option, Par와 Traverse인 List, Tree, Map의 sequence 동작
  * `List[Option[A]] => Option[List[A]]`  
    -> 좌측의 List의 요소 중 하나라도 None이면 None, 그렇지 않으면 List를 Some로 감싼 결과를 리턴.

  * `Tree[Option[A]] => Option[Tree[A]]`  
    -> 좌측의 Tree의 노드 중 하나라도 None이면 None, 그렇지 않으면 List를 Some로 감싼 결과를 리턴.

  * `Map[K, Par[A]] => Par[Map[K, A]]`  
    -> Map에 있는 모든 값을 병렬로 평가하는 병렬 계산을 산출한다.
    
* sequence와 traverse를 이용해 가장 일반적인 방식으로 정의할 수 있는 연산들이 놀랄 만큼 많다.  
  **(다음 절에서 살펴볼 것이다.)**
* 순회 가능 함수자는 자료구조와 함수를 받고 자료구조를 받고, 자료구조에 담긴 자료에 함수를 적용해서 결과를 산출하는 접기(fold) 연산과 비슷하다. 그러나 traverse는 **원래의 구조를 보존**하지만, foldMap은 구조를 폐기하고 그 자리를 모노이드에 대한 연산들로 대신한다. 
```scala
def traverse[F[_], A, B](as: List[A])(f: A => F[B]): F[List[B]]
def foldMap[A, B](as: List[A], m: Monoid[B])(f: A => B): B
```
