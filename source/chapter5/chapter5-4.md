Chapter 5. Strictness and laziness
==

작성자: 고재도 / 일시: 2017년 2월 8일

## 5.4. INFINITE STREAMS AND CORECURSION

````scala
val ones: Stream[Int] = Stream.cons(1, ones)
````

ones가 무한이라도 위에서 만든 take나 exists 함수들은 스트림의 일부만 살펴볼수 있다

```scala
ones.take(5).toList
```

```scala
ones.exists(_ % 2 != 0)
```

![stream image](https://www.safaribooksonline.com/library/view/functional-programming-in/9781617290657/074fig01_alt.jpg)

Exercise 5.8
```scala
  def constant[A](a: A): Stream[A]
```
Exercise 5.9
```scala
def from(n: Int): Stream[Int]
```

Exercise 5.10

![파보나치 수](https://wikimedia.org/api/rest_v1/media/math/render/svg/00008893a71eebbf4e7d89a0c162fe6359f5ac8c)

Exercise 5.11

```scala
def unfold[A, S](z: S)(f: S => Option[(A, S)]): Stream[A]
```

Option은 Stream이 종료되는 시점에 쓰의고 unfold는 Stream을 생성할때 일반적으로 사용
unfold는 공재귀 corecursive 이다

재귀가 자료를 소비하면 공재귀는 자료를 생산한다
 
Corecursion is also sometimes called guarded recursion, and productivity is also sometimes called 공종료(cotermination)