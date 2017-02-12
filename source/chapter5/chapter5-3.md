## 5.3 프로그램 서술과 평가의 분리 (Separating program description from evaluation)  

* 관심사의 분리(separation of concerns)  
: 이 책에서는 계산의 서술(description)을 그 계산의 실제 실행과 분리하는 것을 이야기함.  

- examples  
> 일급 함수는 일부 계산을 자신의 본문에 담고 있으나, 그 계산은 오직 인수들이 전달되어야 실행된다.  
> Option은 오류가 발생했다는 사실을 담고 있을 뿐, 오류에 대해 무엇을 수행할 것인가는 그와는 분리된 관심사.  
> Stream을 이용하면 요소들의 순차령릉 생성하는 계산을 구축하되 계산 단계들의 실행은 실제로 요소가 필요할 때 까지 미룰 수 있다.  

나태성(laziness)을 통해서 표현식의 서술을 그 표현식의 평가와 분리.

```scala
def exists(p: A => Boolean): Boolean = this match {
  case Cons(h, t) => p(h()) || t().exists(p)
  case _ => false
}
```

두번째 줄의 `||`는 두번째 인수에 대해 엄격하지 않음. p(h())가 true를 리턴하는 경우 t().exists(p)는 연산하지 않고 true를 리턴.  
stream의 tail이 lazy val 이므로 Stream의 traversal이 일찍 종료될 뿐 아니라 tail의 평가는 수행되지 않는다.

```scala
// 인수 형식 B 앞의 화살표 => 는 함수 f가 그 인수(둘째 인수)를 이름으로 받으며,
// 경우에 따라서는 그것을 평가하지 않을 수 있다는 뜻이다.
def foldRight[B](z: => B)(f: (A, => B) => B): B = this match {
  // 만일 f가 둘째 인수를 평가하지 않으면 재귀는 결코 일어나지 않는다.
  case Cons(h, t) => f(h(), t().foldRight(z)(f))
  case _ => z
}
```

결합 함수 f가 두번째 매개변수에 대해 엄격하지 않다(non-strictness).  


```
// foldRight로 exists 구현 
def exists(p: A => Boolean): Boolean = 
  foldRight(false)((a, b) => p(a) || b)
```

여기서 b는 평가되지 않을 수 있음. (a가 true를 리턴할 경우.)   
이러한 함수들을 엄격하게 작성할 경우, 조기종료를 처리하는 코드를 작성해야 함.  
laziness를 통해 코드 재사용성 향상.  



* 목록 5.3 stream에 대한 프로그램 추적  

```
Stream(1, 2, 3, 4).map(_ + 10).filter(_ % 2 == 0).toList
```

stream의 elements에 대해, map과 filter 계산이 번갈아 수행됨.  
변환 논리를 엇갈려 수행(interleave)  
map에서 비롯된 중간 스트림이 완전하게 인스턴스화되지는 않음.  

중간 스트림들이 인스턴스화 되지 않음.  

```
// 주어진 술어를 만족하는 첫 요소를 돌려주는 find.
// filter를 재사용해서 구현.
def find(p: A => Boolean): Option[A] =
  filter(p).headOption
```

filter가 전체 스트림을 변환하긴 하나 그 변환은 게으르게 일어나므로, find는 부합하는 요소를 발견하는 즉시 종료.  


* 스트림 변환(stream transformations)의 점진적 본성(incremental nature)의 메모리 사용 영향.  
위 목록 5.3과 같은 예에서 garbage collector는 map이 산출한 11, 13에 대해 filter가 그 값이 필요하지 않다고 결정한 즉시 수거가 가능하다.  
객체가 더 큰 경우에, 이러한 메모리를 일찍 reclaim할 수 있다면 전체적인 메모리 사용량을 줄일 수 있다.

