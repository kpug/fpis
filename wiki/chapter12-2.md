12.2 Applicative 특질
=====
적용성 함수자라는 개념을, map2와 unit을 기본수단으로 하는 Applicative라는 새 인터페이스로 구체화해 보자.

목록12.1 Applicative 인터페이스의 정의

```
trait Applicative[F[_]] extends Functor[F] {
    // 기본수단 조합기들
    def map2[A,B,C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C]
    def unit[A](a: => A): F[A]
    
    // 파생된 조합기들
    // map을 unit과 map2를 이용해서 구현할 수 있다.
    def map[B](fa: F[A])(f: A => B): F[B] =
    	// ()는 Unit 형식의 유일하 값임을 기억하기 바란다.
    	// 따라서 unit(())는 그러한 가짜 값 ()로 unit을 호출하는 것에 해당한다.
        map2(fa, unit(()))((a, _) => f(a))

     // traverse의 정의는 이전과 동일하다.
￼￼￼     def traverse[A,B](as: List[A])(f: A => F[B]): F[List[B]]
        as.foldRight(unit(List[B]()))((a, fbs) => map2(f(a), fbs)(_ :: _))
}
```
- 이 특질은 "**모든 적용성 함수자는 함수자이다**"라는 명제를 확립한다.
- 이 특질에서 map은 map2와 unit으로 구현된다.
- traverse의 구현은 이전과 바뀐 점이 없다.
- flatMap이나 join에 직접 의존하지 않는 다른 조합기들도 이와 비슷하게 Applicative에 직접 옮겨놓을 수 있다.

목록 12.2 Monad를 Applicative의 하위 형식으로 정의

```
trait Monad[F[_]] extends Applicative[F] {
    //Monad의 최소한의 구현은 반드시 unit을 구현해야 하며, flatMap 또는 join과 map을 재정의해야 한다.
    def flatMap[A,B](fa: F[A])(f: A => F[B]): F[B] = join(map(fa)(f))

    def join[A](ffa: F[F[A]]): F[A] = flatMap(ffa)(fa => fa) 
    
    def compose[A,B,C](f: A => F[B], g: B => F[C]): A => F[C] =
        a => flatMap(f(a))(g)

    def map[B](fa: F[A])(f: A => B): F[B] =
        flatMap(fa)((a: A) => unit(f(a)))

    def map2[A,B,C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
        flatMap(fa)(a => map(fb)(b => f(a,b)))
}
```
- flatMap을 이용한 map2의 기본 구현을 제공함으로써, Monad[F]를 Applicative[F]의 하위 형식으로 만들 수 있다.
- 이는 **모든 모나드는 적용 함수자**임을 뜻한다.
- 즉, 이미 모나드인 자료 형식이라면 그 어떤 것이든 따로 Applicative 인스턴스를 제공할 필요가 없다.
