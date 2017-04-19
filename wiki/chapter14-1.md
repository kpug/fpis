# 14 지역 효과와 변이 가능 상태



## 14.1 순수 함수적 변이 가능 상태

지금까지는 순수 함수형 프로그래밍에서 변이 가능(mutable) 상태를 사용할 수 없다는 인상을 받았을 것이다. 그러나 참조 투명성과 순수성의 정의를 자세히 들여다보면 **지역(local)** 상태의 변이를 금지하는 사항은 전혀 없음을 알 수 있다.

### 참조 투명성

```
만일 모든 프로그램 p에 대해 표현식 e의 모든 출현(occurrence)을 e의 평가 결과로 치환해도 p의 의미에 아무런 영향을 미치지 않는다면, 그 표현식 e는 참조에 투명하다(referentially transparent).
```

### 순수성

```
만일 표현식 f(x)가 참조에 투명한 모든 x에 대해 참조에 투명하다면, 함수 f는 순수하다(pure).
```



### 예시: 변이 가능 배열을 이용한 제자리 quicksort 함수

```scala

def quicksort(xs: List[Int]): List[Int] = if (xs.isEmpty) xs else { 
  val arr = xs.toArray
  def swap(x: Int, y: Int) = { 
    val tmp = arr(x)
    arr(x) = arr(y)
    arr(y) = tmp
  }
  def partition(n: Int, r: Int, pivot: Int) = {
    val pivotVal = arr(pivot)
    swap(pivot, r)
    var j = n
    for (i <- n until r) if (arr(i) < pivotVal) {
      swap(i, j)
      j += 1
    }
    swap(j, r)
    j 
  }
  def qs(n: Int, r: Int): Unit = if (n < r) {
    val pi = partition(n, r, n + (n - r) / 2) qs(n, pi - 1)
    qs(pi + 1, r)
  }
  qs(0, arr.length - 1)
  arr.toList
}
```

- [quicksort 설명](http://coderkoo.tistory.com/7)
- 위 함수는 for 루프와 갱신 가능한 var, 변이 가능 배열을 사용하지만, 정의에 의하면 순수 함수다.
- 이 함수를 호출하는 쪽에서는 quicksort 본문 내부에 있는 개별 부분 표현식이 참조에 투명하지 않다는, 다시 말해 지역 메서드  swap과 partition, qs가 순수 함수가 아니라는 점을 알지 못한다. quick sort 함수 외부에는 변이 가능 배열에 대한 참조가 전혀 없기 때문이다.
- 이 함수의 모든 변이는 지역 범위 안에서 일어나므로 전체적인 함수는 순수하다. 즉, 만일 List[Int] 형식의 어떤 표현식 xs가 참조에 투명하면  표현식 quicksort(xs)도 항상 참조에 투명하다.



### 지역 효과

![지역 효과](http://i68.tinypic.com/2ivduo2.jpg)

함수 안에서 변이가 발생해도, 변이된 객체를 함수 외부에서 전혀 참조하지 않는다면 그 변이는 부수 효과가 아니다.



### 결론

- 어떤 함수가 내부적으로 부수 효과가 있는 구성요소를 사용하더라도 호출자에게 순수한 외부 인터페이스를 제공한다면, 그런 함수를 사용하는 것은 함수형 프로그래밍의 원리를 위반하는 것이 아니다.
- 원칙적으로, 구현에서 지역 부수 효과를 사용하는 순수 함수를 만드는 것에는 아무런 문제도 없다.