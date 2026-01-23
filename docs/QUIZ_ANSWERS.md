# 퀴즈 정답 (학습 후 확인!)

> ⚠️ 스포일러 주의! 먼저 예상하고 코드를 작성한 후 확인하세요.

---

## Stage 1: 파티션 + 키

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **b** | 같은 키는 항상 같은 파티션으로 (해시 기반) |
| Q2 | **b** | null 키는 여러 파티션에 분배됨 (Sticky Partitioner) |
| Q3 | - | 해시 기반이라 실행해봐야 알 수 있음! 실습으로 확인하세요. |

---

## Stage 2: acks 설정

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **a** | acks=0은 확인 안 하므로 유실 가능 |
| Q2 | **a** | acks=1은 Leader만 확인, 복제 전 다운되면 유실 |
| Q3 | **b** | acks=0이면 메타데이터가 제대로 안 올 수 있음 |

---

## Stage 3: 재시도

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **b** | 재시도 중 순서 역전 가능! (in.flight > 1일 때) |
| Q2 | **a** | in.flight=1이면 순서 보장되나 성능 저하 |
| Q3 | **b** | Idempotent면 max 5까지 순서 보장 |

---

## Stage 4: 멱등성

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **b** | 브로커가 중복 감지하여 한 번만 저장 |
| Q2 | **a** | acks=all, retries=MAX, in.flight≤5로 자동 설정 |
| Q3 | **a** | 단일 Producer 세션 내에서만 보장 (재시작하면 새 PID) |

---

## Stage 5: 배치

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **a** | linger.ms=0이면 배치 없이 즉시 전송 |
| Q2 | **c** | batch.size나 linger.ms 둘 중 하나 만족시 전송 |
| Q3 | **b** | 배치가 안 차면 linger.ms까지 대기 |

---

## Stage 6: 압축

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **a** | gzip은 압축률 높고 느림, snappy는 빠르고 압축률 낮음 |
| Q2 | **b** | Consumer가 자동으로 압축 해제 (투명하게 처리) |
| Q3 | **b** | 배치로 모아서 보낼 때 압축 효과 극대화 |

---

## Stage 7: JSON

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **b** | Serializer로 바이트 배열 변환 필요 |
| Q2 | **a** | JSON 문자열로 보임 (StringDeserializer 사용시) |
| Q3 | **b** | Jackson의 `FAIL_ON_UNKNOWN_PROPERTIES=false` 설정 |

---

## Stage 8: 트랜잭션

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **a** | abort하면 모두 전달 안 됨 (원자성) |
| Q2 | **b** | console-consumer는 기본이 read_uncommitted라 보임! |
| Q3 | **b** | 새 Producer가 기존 것을 펜싱(zombie fencing) |

---

## Stage 9: Consumer 기본

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **c** | auto.offset.reset 설정에 따라 다름 (earliest/latest) |
| Q2 | **a** | 1초 동안 대기하며 메시지 수집 |

---

## Stage 10: Consumer Group

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **a** | Consumer1: 1개, Consumer2: 2개 파티션 담당 (또는 그 반대) |
| Q2 | **b** | 1개는 메시지를 받지 못한다 (파티션 수 < Consumer 수) |

---

## Stage 11: 수동 커밋

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **c** | 마지막 커밋된 오프셋부터 읽는다 (중복 가능) |
| Q2 | **a** | 해당 메시지를 다시 받을 수 있다 (at-least-once) |

---

## Stage 12: Rebalance Listener

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **a** | onPartitionsRevoked() → Rebalancing 진행 → onPartitionsAssigned() 순서로 호출됨 |
| Q2 | **b** | 이전 상태 복구 (메모리 캐시, DB 연결 등) - 오프셋 설정은 자동으로 처리됨 |
| Q3 | **c** | a, b 모두 - Consumer 1이 모든 파티션을 다시 받으며, 마지막 커밋된 오프셋부터 처리 재개 |

---

## Stage 13: Seek 메서드

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **b** | seek()로 처음 오프셋(0)으로 이동 - 가장 간단하고 효율적 |
| Q2 | **a** | 어느 때든 상관없음 - partition이 할당된 후라면 언제든 seek 가능 |
| Q3 | **b** | 새 메시지가 들어올 때까지 대기 - seekToEnd()는 latest offset을 의미 |

---

## Stage 14: Consumer 성능 튜닝

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **b** | 100ms 후 100bytes 반환 - fetch.min.bytes 미충족 시 fetch.max.wait.ms까지 대기 |
| Q2 | **b** | max.poll.interval.ms=5초 초과 시 Rebalancing 발생 - 메시지 처리가 제한 시간을 넘으면 안 됨 |
| Q3 | **b** | GC로 Consumer 스레드 정지 → 하트비트 전송 지연 → session.timeout.ms 초과 가능 |

---

## Stage 15: Consumer Lag 모니터링

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **a** | Lag = Latest Offset - Consumer Offset = 1000 - 950 = 50 |
| Q2 | **c** | a, b 둘 다 가능 - Producer 속도가 빠르거나 Consumer가 느리거나 둘 다 가능 |
| Q3 | **c** | 변화 없음 (Partition 수에 따라 다름) - Consumer 수 > Partition 수면 일부 유휴 상태 |

---

## Stage 16: Producer 에러 처리

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **a** | RetriableException (자동 재시도됨) - 네트워크 오류, 브로커 일시적 장애 등 |
| Q2 | **b** | 실패 (exception 확인) - callback에서 exception이 null이 아니면 실패 처리 필요 |
| Q3 | **a** | 재시도 안 함 (바로 실패) - retries=0으로 설정하면 즉시 실패 반환 |

---

## Stage 17: Consumer 에러 처리 & DLQ 패턴

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **c** | 수동으로 처리 (일반적으로 DLQ로 전송) - DB 제약 조건, 검증 실패 등 처리 불가 에러 |
| Q2 | **a** | 영구 보관 (나중에 수동 처리) - 별도 모니터링 프로세스로 후속 처리 |
| Q3 | **b** | 문제 (근본 원인 파악 필요) - 같은 메시지 반복 실패 = 체계적 오류, 알람 필요 |

---

## Stage 18: Exactly-Once Semantics (심화)

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **b** | Consumer 로직에서 중복 제거 필요 - Producer EOS와 Consumer EOS 독립적, Consumer 실패 시 재처리 불가피 |
| Q2 | **a** | 받음 (설정에 따라) - isolation.level=read_uncommitted (기본값)는 abort된 메시지도 읽음 |
| Q3 | **b** | 모두 롤백 (원자성) - 트랜잭션의 핵심은 All-or-Nothing 원칙 |

---

## Stage 19: Circuit Breaker & 복구 패턴

| 문제 | 정답 | 설명 |
|------|------|------|
| Q1 | **a** | Timeout까지 계속 재시도 (리소스 낭비) - Connection Pool 고갈, 시간 낭비 |
| Q2 | **b** | 모든 요청을 즉시 실패 (Fail Fast) - Open 상태는 외부 의존성이 죽었음을 의미 |
| Q3 | **b** | Half-Open으로 자동 전환 (일부 요청 시도) - 복구 감지 후 Closed로 변경 (자동 복구 패턴) |

---

## 핵심 개념 정리

### Producer 설정 트레이드오프

| 설정 | 성능 ↑ | 안정성 ↑ |
|------|--------|----------|
| acks | 0 | all |
| retries | 0 | MAX |
| linger.ms | 높음 | 낮음 |
| compression | snappy/lz4 | none |

### Consumer 보장 수준

| 패턴 | 설명 | 구현 |
|------|------|------|
| At-most-once | 유실 가능, 중복 없음 | 처리 전 커밋 |
| At-least-once | 유실 없음, 중복 가능 | 처리 후 커밋 |
| Exactly-once | 유실/중복 없음 | 트랜잭션 사용 |

---

## Consumer 심화 개념

### Rebalancing 프로세스
```
onPartitionsRevoked()
    ↓
[리밸런싱 중 - 메시지 처리 중단]
    ↓
onPartitionsAssigned()
    ↓
poll() 재개
```
- **Stop-the-World**: 리밸런싱 중 모든 Consumer 멈춤
- **Graceful Shutdown**: revoked() 단계에서 오프셋 저장 필수
- **Timeout**: max.poll.interval.ms 초과 시 자동 탈출

### Lag 관리 전략

| 상황 | 원인 | 해결책 |
|------|------|---------|
| Lag 증가 | Consumer 느림 | Consumer 인스턴스 추가 (max = Partition 수) |
| Lag 증가 | Producer 빠름 | 자연스러운 흐름 (괜찮음) |
| Lag 지속 0 | 동시 처리 | Consumer 프로세싱 최적화 필요 |

### 에러 처리 패턴 선택

| 에러 타입 | 재시도 | DLQ | 조치 |
|----------|--------|-----|------|
| 일시적 (DB 연결 timeout) | ✅ | ❌ | exponential backoff |
| 영구적 (제약 조건 위반) | ❌ | ✅ | 수동 분석 후 처리 |
| 비즈니스 로직 (검증 실패) | ⚠️ | ✅ | 정책에 따라 다름 |

### Circuit Breaker 상태 전환

```
Closed (정상)
    ↓ [연속 실패 임계값 초과]
Open (차단)
    ↓ [일정 시간 경과]
Half-Open (테스트)
    ↓ [요청 성공] → Closed
    ↓ [요청 실패] → Open
```

---

## 학습 로드맵 정리

### Part 1: Producer 기초 → 심화 (1-8 Stage)
- **기초**: 파티션, acks, 배치
- **심화**: 재시도 + 순서, 멱등성, 트랜잭션

### Part 2: Consumer 구현 (9-15 Stage)
- **기본**: Consumer 그룹, 오프셋 관리
- **심화**: 리밸런싱, seek, Lag 모니터링

### Part 3: 프로덕션 안정성 (16-19 Stage)
- **에러 처리**: 재시도, DLQ, Circuit Breaker
- **안정성**: Exactly-Once, 장애 격리

---

## ⭐ 가장 중요한 3가지

1. **Consumer 실패 = Rebalancing 유발** (Stage 12, 14)
   - max.poll.interval.ms 초과 주의
   - 하트비트 주기보다 처리가 오래 걸리면 위험

2. **에러는 종류에 따라 다르게 처리** (Stage 16-17)
   - 일시적 에러 → 자동 재시도
   - 영구적 에러 → DLQ로 분류

3. **Exactly-Once는 양쪽 모두 필요** (Stage 18)
   - Producer EOS + Consumer EOS
   - 하지만 application 레벨 중복 제거 여전히 필요
