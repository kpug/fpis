## 7.3 - API의 경련


* 연습문제 7.3

* 연습문제 7.4

### 기존의 조합기로 표현 하기

* Par[List[Int]]의 결과가 정렬된 Par[List[Int]]로 변환
```
def sortPar(parList: Par[List[Int]): Par[List[Int]]
```

* parList를 map2의 양변 중 하나에 지정한다면 List의 내부에 접근해서 목록을 정렬
```
def sortPar(parList: Par[List[Int]]): Par[List[Int]] =
   map2(parList, unit(()) )( (a, _) => a.sorted)

```

* A => B 형식의 임의의 함수를, Par[A]를 받고 Par[B]를 돌려주는 함수로 승급

```
def map[A,B](pa>: Par[A])(f: A => B): Par[B] =
 map2(pa, unit(()) )( (a,_) => f(a))
```

```
def sortPar(parList: Par[List[Int]]) = map(parList)(_.sorted)
```

* 하나의 목록에 map을 병렬로 적용
```
def parMap[A,B](ps: List[A])(f: A => B): Par[List[B]]
```

* N개의 병렬 계산을 수월하게 분기하기
* asyncF가 병렬 계산 하나를 분기해서 결과를 산춤함으로써 A => B를 A => Par[B]로 변환
```
def parMap[A,B](ps: List[A])(f: A => B): Par[List[B]] = {
  val fbs: List[Par[B]] = ps.map(asyn(f))

  ...
}
```

* 연습문제 7.5

* 연습문제 7.6


## 7.5 - 조합기들을 가장 일반적인 형태로 정련

```
함수적 설계는 반복적인 과정이다. API의 명세를 만들고 적어도 하나의 prototype을 구현을 작성했다면
그것을 점점 복잡한 또는 현신ㄹ적인 시나리오에 사용해 봐야 한다.
그런데 바로 조합기를 구현해보기 보다는 그 조합기를 가장 일반적인 형태로정련할 수 있는지 살펴 보는 것이 바람직하다.
```

* 두 분기 계산 중 하나를 초기 계산의 결과에 기초해서 선택하는 함수
```
def choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A]
```
```
이 함수는 만일 cond의 결과가 true이면 t를 사용해서 계산을 진행하고 cond의 결과가 false이면 f를 사용해서 계산을 진행한다.
```

* 결과를 이용해서 t나 f의 실행을 결정하는 식
```
def choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] =
  es =>
      if (run(es)(cond).get) t(es)
      else f(es)
```
```
여기서 boolean을 사용하는 것은 다소 자의적이다. 그리고 가능한 두 병렬 계산 t와 f중 하나를 선택하는 것도 사실 자의적이다.
```

* N개의 계산 중 하나를 선택
```
def choiceN[A](n: Par[Int])(choices: List[Par[A]): Par[A]
```

* 연습문제 7.13

* 연습문제 7.14





