# 11.5 도대체 모나드란 무엇인가?
- Monad는 Monoid처럼 좀 더 추상적이고 순수 대수적인 인터페이스
- Monad 조합기들은 주어진, 그리고 모나드가 될 수 있는 자료 형식에 대한 전체 API 중 일부만 차지
- Monad는 한두 형식을 일반화하는 것이 아니라, Moand 인터페이스와 법칙을 만족할 수 있는 아주 다양하고 많은 자료 형식을 일반화한다.

### 모나드의 조건
	- 기본 Monad 조합기들의 최소 집합 세 가지 중 하나의 구현들을 제공한다.
		- unit과 flatMap
		- unit과 compose
		- unit, map, join
	- 결합법칙과 항등법칙을 만족한다.

## 11.5.1 항등 모나드
### 항등 모나드의 형식
```
case class Id[A](value: A)
```
- Id는 그냥 간단한 wrapper다. 특별히 하는 일은 없다.
- Id를 A에 적용하는 것은 항등 연산이다. 감싸인 형식과 감싸이지 않은 형식이 완전히 동형(isomorphic)이기 때문이다(즉, 한 형식에서 다른 형식으로 갔다가 다시 원래의 형식으로 돌아와도 정보가 전혀 소실되지 않는다).

### 항등 모나드를 REPL에서 시험
```
scala> Id("Hello, ") flatMap (a =>     |   Id("monad!") flatMap (b =>     |     Id(a + b)))res0: Id[java.lang.String] = Id(Hello, monad!)
```
### 동일한 작업을 for-함축으로 표현
```scala> for {     |   a <- Id("Hello, ")     |   b <- Id("monad!")     | } yield a + bres1: Id[java.lang.String] = Id(Hello, monad!)
```
- 항등 모나드에 대한 flatMap의 동작(action)은 그냥 변수 치환이다.
- 변수 a와 b가 각각 "Hello, "와 "monad!"에 묶인 후 표현식 a + b로 대입된다.

### 같은 동작을 Id wrapper 없이 스칼라 자체의 변수를 이용해서 작성
```
scala> val a = "Hello, "a: java.lang.String = "Hello, "scala> val b = "monad!"b: java.lang.String = monad!scala> a + bres2: java.lang.String = Hello, monad!
```
- Id wrapper 말고는 차이가 없다.
- **즉, 모나드는 변수의 도입과 binding, 그리고 변수 치환 수행을 위한 문맥을 제공한다.**

## 11.5.2 State 모나드와 부분 형식 적용

### 제6장에서 논의한 State 자료 형식 복습
```case class State[S, A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] =    State(s => {      val (a, s1) = run(s)
      (f(a), s1)    })  def flatMap[B](f: A => State[S, B]): State[S, B] =    State(s => {
	  val (a, s1) = run(s)      f(a).run(s1)    })}
```
- 형태로 볼 떄 State는 모나드가 되기에 충분해 보인다.
- 그러나 State의 형식 생성자는 형식 인수 두 개를 받지만 Monad는 인수가 하나인 형식 생성자를 요구하므로, 그냥 Monad[State]라고 표기할 수 없다.
- 하지만 어떤 구체적인 S에 대한 State[S, _]는 Monad가 받아들일 수 있는 형태이다. 이는 State에 단 하나의 모나드 인스턴스가 아닌 여러 인스턴스들(S마다 하나씩)의 family가 있음을 뜻한다.
- 형식 인수 S가 어떤 구체적인 형식으로 고정되도록 State를 부분적으로 적용할 수 있다면 좋을 것이다.
- 예를 들어 **IntState 형식 생성자**를 만들 수 있다. InstState 형식 생성자는 첫 형식 인수가 Int로 고정된 State의 별칭이다.

### IntState 형식 생성자
```type IntState[A] = State[Int, A]```
- IntState는 Monad로 만들기에 딱 맞는 형태다.

### IntState를 Monad로 만들어 보았습니다.
```object IntStateMonad extends Monad[IntState] {  def unit[A](a: => A): IntState[A] = State(s => (a, s))  def flatMap[A,B](st: IntState[A])(f: A => IntState[B]): IntState[B] =    st flatMap f
}```
- 구체적인 상태 형식마다 이렇게 개별적인 Monad 인스턴스를 작성해야 한다면 코드가 엄청나게 중복될 것이다.
- State[Int, _]라고 표기한다고 해서 저절로 익명 형식 생성자가 만들어지지 않는다.
- 대신, 형식 수준에서 람다 구문과 비슷한 것을 사용할 수 있다.

### IntState를 inline에서 선언
```
object IntStateMonad extends  Monad[({type IntState[A] = State[Int, A]})#IntState] {
  ...}
```
- 괄호 안에서 익명 형식을 선언하는 것이다.
- 익명 형식의 멤버: 형식 별칭 IntState.
- 괄호 밖에서는 # 구문을 이용해서 IntState 멤버에 접근한다(객체 멤버를 value로 접근 할 때 '객체명.멤버명'같이 마침표를 사용하는 것처럼, 형식 수준에서 형식 멤버에 접근할 때에는 # 기호를 사용한다).
- 이렇게 즉석에서 선언된 형식 생성자를 스칼라에서는 **형식 람다(type lambda)**라고 부른다.### State 형식 생성자를 부분 적용하고 StateMonad 특질을 선언```def stateMonad[S] = new Monad[({type f[x] = State[S,x]})#f] {  def unit[A](a: => A): State[S,A] = State(s => (a, s))  def flatMap[A,B](st: State[S,A])(f: A => State[S,B]): State[S,B] =￼￼￼    st flatMap f}```- StateMonad[S]의 인스턴스는 주어진 상태 형식 S에 대한 모나드 인스턴스다.

## Id 모나드와 State 모나드의 차이점

### State에 대한 기본수단 연산```def getState[S]: State[S, S]def setState[S](s: => S): State[S, Unit]```
### for-함축을 이용한 상태 조회 및 설정
```
val F = stateMonad[Int]def zipWithIndex[A](as: List[A]): List[(Int,A)] =  as.foldLeft(F.unit(List[(Int, A)]()))((acc,a) => for {
    xs <- acc    n  <- getState    _  <- setState(n + 1)} yield (n, a) :: xs).run(0)._1.reverse
```
- for 블록 안에서 getState와 setState가 쓰이는 방식에 주목하자.
- Id 모나드와 State 모나드의 공통점: 변수를 binding한다는 점.
- Id 모나드와 State 모나드의 차이점: 행간에서 또 다른 일이 진행된다.
	- for-함축의 각 행에서, flatMap 구현은 현재 상태가 getState에 주어지며 새 상태가 setState 다음의 모든 동작으로 전파됨을 보장한다.

## 결론
- flatMap 호출들의 연쇄는(또는 그에 해당하는 for-함축은) 변수에 값을 배정하는 명령문들로 이루어진 명령식 프로그램과 비슷하며, **모나드는 각 명령문의 경계에서 어떤 일이 일어나는지를 명시한다.**
	-  Id: Id 생성자 안에서의 wrapping 풀기와 다시 wrapping하기 이외에는 아무 일도 일어나지 않는다.
	- State: 가장 최근의 상태가 한 명령문에서 다음 명령문으로 전달된다.
	- Option 모나드: 명령문이 None을 돌려주어서 프로그램이 종료될 수 있다.
	- List 모나드: 명령문이 여러 결과를 돌려줄 수 있으며, 그러면 그다음의 명령문들이 여러 번(결과당 한 번씩) 실행될 수 있다.
- **Monad 계약이 행간에서 무엇이 일어나는지 명시하는 것은 아니다. 단지, 어떤 일이 일어나든 그것이 결합법칙과 항등법칙을 만족함을 명시할 뿐이다.**
