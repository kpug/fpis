# 12 적용성 함수자와 순회 가능 함수자

적용성 함수자(applicative functor)
순회 가능 함수자(traversable functor)

## 12.1 모나드의 일반화

```scala
def sequence[A](lfa: List[F[A]]): F[List[A]]
  traverse(lfa)(fa => fa)

def traverse[A,B](as: List[A])(f: A => F[B]): F[List[B]]
  as.foldRight(unit(List[B]()))((a, mbs) => map2(f(a), mbs)(_ :: _))

def map2[A,B,C](ma: F[A], mb: F[B])(f: (A,B) => C): F[C] =
  flatMap(ma)(a => map(mb)(b => f(a,b)))
```

Monad에 대한 수많은 유용한 조합기들을 unit과 map2로 정의할 수 있다.    
traverse 조합기가 그러한 조합기의 예.  

Monad에 대한 하나의 변종으로써, unit과 map2를 기본수단으로 두는 새로운 추상을 **적용성 함수자**라고 부름.  
