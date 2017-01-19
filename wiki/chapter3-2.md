##3.3 함수적 자료구조의 자료 공유

####자료공유(data sharing)
> 실제 자료는 불변(immutuable). 복사나 수정없이 목록 자료를 재사용하면 된다.
자료구조에 연산이 가해져도 기존 참조들은 변하지 않는다.

> *함수적 자료구조는 영속적(persistent)*

```
	head -> (a, link) -> (b, link) -> (c, link) -> (d, Nil)


	val first = head	
			=>	List(a, b, c, d)
	val second = head.link 	
			=>	List(b, c, d)
```

##3.3.1 자료 공유의 효율성

\[ *효율적인 예* \]
```
def append[A](a1: List[A], a2: List[A]): List[A] =
	a1 match {
		case Nil => a2
		case Cons(h,t) => Cons(h, apeend(t, a2))
	}
```

>해석 : 현재 함수의 실행 시간꽈 메모리 사용량은 오직 a1 의 길이에 의존. 이후는 단순 a2 를 가르킨다.

\[ *비효율적인 예* \]
```
def init[A](l: List[A]): List[A] = {
	l match {
		case Cons(h, Nil) => Nil
		case Cons(h, t) => Cons(h, init(t))
	}
}
```
>해석 : Cons의 tail 을 치환할 때마다 반드시 이전의 모든 Cons 객체를 복사해야 한다.

##3.3.2 고차 함수를 위한 형식 추론 개선

####정의
```
	def dropWhile[A](l: List[A], f: A => Boolean): List[A]
```

```
	dropWhile(xs, (x: Int) => x < 4)
	=> x 의 parameter 정의가 필요함.
```

####새롭게 정의
```
	def dropWhile[A](as: List[A])(f: A => Boolean): List[A]
	(using curring)
```

```
	=> dropWhile(xs)(x => x < 4)

	(dropWhile(xs))(x => x < 4)
	(result)(x => x < 4)
```
> 해석 : result 에서 generic A 가 이미 정의되었음.


##3.4 목록에 대한 재귀와 고차 함수로의 일반화
```
	def foldRight[A, B](as: List[A], z: B)(f: (A, B) => B): B =
		as match {
			case Nil => z
			case Cons(x, xs) => f(x, fildRight(xs, z)(f))
		}

	def sum2(ns: List[Int]) = 
		foldRight(ns, 0)((x,y) => x + y)

	def product2(ns: List[Double]) = 
		foldRight(ns, 1.0)(_ * _)
```