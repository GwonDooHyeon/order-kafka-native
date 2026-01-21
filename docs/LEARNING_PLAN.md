# Kafka 단계별 학습 계획

## 현재 상태
- ✅ SimpleProducer (동기식 전송)
- ✅ SimpleProducerAsync (비동기식 전송 + 콜백)
- ✅ Docker Kafka 인프라 (단일 브로커)
- ✅ Consumer는 kafka-console-consumer로 확인

---

## 학습 방식
각 단계마다:
1. **📖 개념 설명** - 이론 학습
2. **🤔 예상 퀴즈** - 코드 작성 전 동작 예측 (정답은 맨 아래)
3. **💻 구현** - Producer 코드 직접 작성
4. **✅ 결과 확인** - kafka-console-consumer로 검증
5. **💡 핵심 포인트** - 배운 내용 정리

---

# Part 1: Producer 심화

## Stage 1: 파티션 3개 + 키 기반 라우팅
> 목표: 키 값에 따라 메시지가 어떤 파티션으로 가는지 이해

### 📖 개념
- 파티션은 토픽 내 데이터를 분산 저장하는 단위
- 같은 키를 가진 메시지는 항상 같은 파티션으로 간다 (해시 기반)
- 키가 null이면 라운드로빈 또는 Sticky Partitioner 방식으로 분배

### 🤔 예상 퀴즈 (코드 작성 전 예측해보세요!)
```
Q1. 파티션이 3개인 토픽에 key="user-1"로 10개 메시지를 보내면?
    a) 3개 파티션에 골고루 분배된다
    b) 10개 모두 같은 파티션에 들어간다
    c) 랜덤하게 분배된다

Q2. key=null로 10개 메시지를 보내면?
    a) 모두 파티션 0에 들어간다
    b) 여러 파티션에 분배된다
    c) 에러가 발생한다

Q3. key="A", "B", "C"로 각각 1개씩 보내면 파티션 번호는?
    (예측해보세요: A→?, B→?, C→?)
```

### 💻 구현할 파일
`producer/src/main/java/KeyPartitionProducer.java`

### ✅ 검증 방법
```bash
# 1. 파티션 3개 토픽 생성
docker exec kafka kafka-topics --create --topic partitioned_topic \
  --partitions 3 --bootstrap-server localhost:9092

# 2. Producer 실행 (콜백에서 파티션 번호 확인)

# 3. 각 파티션별 메시지 확인
docker exec kafka kafka-console-consumer --topic partitioned_topic \
  --partition 0 --from-beginning --bootstrap-server localhost:9092
```

---

## Stage 2: acks 설정과 전송 보장
> 목표: acks 설정에 따른 성능과 안정성 트레이드오프 이해

### 📖 개념
- `acks=0`: 전송만 하고 확인 안 함 (가장 빠름, 유실 가능)
- `acks=1`: Leader에 저장 확인 (기본값, 빠름)
- `acks=all(-1)`: 모든 ISR에 저장 확인 (가장 안전, 느림)

### 🤔 예상 퀴즈
```
Q1. acks=0으로 설정하고 메시지 전송 직후 브로커가 다운되면?
    a) 메시지가 유실된다
    b) 자동으로 재전송된다
    c) 에러가 발생한다

Q2. acks=1인데 Leader가 Follower에 복제하기 전 다운되면?
    a) 메시지가 유실될 수 있다
    b) Follower가 자동 복구한다
    c) 메시지는 안전하다

Q3. 콜백에서 metadata.partition()과 metadata.offset() 값이
    acks=0일 때 어떻게 될까?
    a) 정상 출력된다
    b) -1 또는 null이 될 수 있다
    c) 에러가 발생한다
```

### 💻 구현할 파일
`producer/src/main/java/AcksProducer.java`

### ✅ 검증 방법
```bash
# acks=0, 1, all 각각 실행해보고 콜백 결과 비교
# 전송 시간 차이도 측정해보기
```

---

## Stage 3: 재시도(Retries)와 순서 보장
> 목표: 재시도 설정과 메시지 순서 보장 관계 이해

### 📖 개념
- `retries`: 재시도 횟수 (기본값: 2147483647)
- `retry.backoff.ms`: 재시도 간격 (기본값: 100ms)
- `max.in.flight.requests.per.connection`: 동시 전송 요청 수
- 재시도 시 순서가 바뀔 수 있음! (중요)

### 🤔 예상 퀴즈
```
Q1. 메시지 A, B를 순서대로 보냈는데 A가 실패해서 재시도하면?
    a) 항상 A, B 순서 유지
    b) B가 먼저 저장될 수 있음 (순서 역전)
    c) B도 같이 실패함

Q2. max.in.flight.requests.per.connection=1로 설정하면?
    a) 순서가 보장된다 (but 성능 저하)
    b) 재시도가 안 된다
    c) 에러 발생

Q3. enable.idempotence=true면 max.in.flight는 최대 몇까지 순서 보장?
    a) 1
    b) 5
    c) 무제한
```

### 💻 구현할 파일
`producer/src/main/java/RetryProducer.java`

### ✅ 검증 방법
```bash
# 인위적으로 실패 상황 만들기 어려우므로
# 로그에서 재시도 설정이 적용됐는지 확인
```

---

## Stage 4: Idempotent Producer (멱등성)
> 목표: 중복 전송 방지 메커니즘 이해

### 📖 개념
- 네트워크 오류로 재전송 시 중복 발생 가능
- `enable.idempotence=true`: 중복 전송 방지
- Producer ID + Sequence Number로 브로커가 중복 감지
- Kafka 3.0+에서는 기본값이 true

### 🤔 예상 퀴즈
```
Q1. Idempotent Producer가 같은 메시지를 두 번 보내면?
    a) 두 번 저장된다
    b) 한 번만 저장된다
    c) 에러가 발생한다

Q2. enable.idempotence=true 설정 시 자동으로 변경되는 것은?
    a) acks=all, retries=MAX, max.in.flight≤5
    b) acks=0, retries=0
    c) 아무것도 변경 안 됨

Q3. Idempotence는 어느 범위까지 중복을 방지할까?
    a) 단일 Producer 세션 내
    b) 여러 Producer 간에도
    c) Consumer까지 포함
```

### 💻 구현할 파일
`producer/src/main/java/IdempotentProducer.java`

### ✅ 검증 방법
```bash
# 메시지 전송 후 콜백에서 확인
# 로그에서 ProducerID 할당 확인
```

---

## Stage 5: Batch와 Linger 설정
> 목표: 배치 전송으로 처리량 최적화하기

### 📖 개념
- `batch.size`: 배치 최대 크기 (기본: 16KB)
- `linger.ms`: 배치 대기 시간 (기본: 0ms)
- linger.ms=0이면 즉시 전송, 값이 크면 배치로 모아서 전송
- 처리량 vs 지연시간 트레이드오프

### 🤔 예상 퀴즈
```
Q1. linger.ms=0이고 메시지를 1개씩 빠르게 보내면?
    a) 배치 없이 1개씩 전송
    b) 자동으로 배치됨
    c) 에러 발생

Q2. linger.ms=100으로 설정하고 메시지 10개를 보내면?
    a) 즉시 10번 전송
    b) 100ms 후 한 번에 전송
    c) batch.size에 따라 다름

Q3. batch.size=1000이고 메시지가 500바이트면?
    a) 즉시 전송
    b) linger.ms까지 대기
    c) 메시지 2개가 모일 때까지 대기
```

### 💻 구현할 파일
`producer/src/main/java/BatchProducer.java`

### ✅ 검증 방법
```bash
# linger.ms=0 vs linger.ms=100 비교
# 메시지 100개 전송 시간 측정
```

---

## Stage 6: 압축(Compression)
> 목표: 메시지 압축으로 네트워크/저장 효율화

### 📖 개념
- `compression.type`: none, gzip, snappy, lz4, zstd
- 압축은 배치 단위로 수행됨
- CPU 사용량 vs 네트워크/디스크 절약 트레이드오프

### 🤔 예상 퀴즈
```
Q1. gzip vs snappy 차이는?
    a) gzip: 높은 압축률/느림, snappy: 낮은 압축률/빠름
    b) 동일한 성능
    c) snappy가 모든 면에서 우수

Q2. 압축된 메시지를 Consumer가 받으면?
    a) 압축된 상태로 받음
    b) 자동으로 압축 해제됨
    c) 별도 설정 필요

Q3. 압축 효과가 가장 좋은 경우는?
    a) 작은 메시지를 1개씩 보낼 때
    b) 큰 메시지를 배치로 보낼 때
    c) 바이너리 데이터를 보낼 때
```

### 💻 구현할 파일
`producer/src/main/java/CompressionProducer.java`

### ✅ 검증 방법
```bash
# 같은 메시지로 압축 없음 vs gzip vs snappy 비교
# 토픽의 실제 저장 크기 확인
```

---

## Stage 7: JSON 직렬화
> 목표: String 외에 JSON 객체를 전송하기

### 📖 개념
- 기본 StringSerializer 외에 커스텀 Serializer 사용 가능
- Jackson 라이브러리로 Java 객체 ↔ JSON 변환
- 또는 StringSerializer + JSON 문자열로 간단히 처리

### 🤔 예상 퀴즈
```
Q1. Java 객체를 Kafka로 보내려면?
    a) 직접 전송 가능
    b) Serializer로 바이트 변환 필요
    c) String으로만 가능

Q2. kafka-console-consumer로 JSON 메시지를 받으면?
    a) JSON 문자열로 보임
    b) 바이너리로 보임
    c) 파싱된 객체로 보임

Q3. 필드가 추가된 새 버전 객체를 보내면 기존 Consumer는?
    a) 에러 발생
    b) 새 필드 무시 가능 (Jackson 설정에 따라)
    c) 자동으로 대응
```

### 💻 구현할 파일
- `producer/src/main/java/model/User.java`
- `producer/src/main/java/JsonProducer.java`

### ✅ 검증 방법
```bash
# kafka-console-consumer로 JSON 메시지 확인
docker exec kafka kafka-console-consumer --topic json_topic \
  --from-beginning --bootstrap-server localhost:9092
```

---

## Stage 8: Transaction (트랜잭션)
> 목표: 여러 메시지를 원자적으로 전송

### 📖 개념
- `transactional.id` 설정으로 트랜잭션 활성화
- initTransactions() → beginTransaction() → send() → commitTransaction()
- 실패 시 abortTransaction()으로 롤백
- Exactly-Once Semantics의 핵심

### 🤔 예상 퀴즈
```
Q1. 트랜잭션 중 3개 메시지 전송 후 abort하면?
    a) 3개 모두 Consumer에게 전달 안 됨
    b) 일부만 전달됨
    c) 모두 전달됨

Q2. kafka-console-consumer로 abort된 메시지를 볼 수 있을까?
    a) 볼 수 없다
    b) 기본적으로 보인다 (isolation.level=read_uncommitted가 기본)
    c) 에러 발생

Q3. 같은 transactional.id로 두 Producer가 동시에 실행하면?
    a) 둘 다 정상 동작
    b) 먼저 실행된 것이 펜싱(fencing)되어 실패
    c) 나중에 실행된 것이 실패
```

### 💻 구현할 파일
`producer/src/main/java/TransactionalProducer.java`

### ✅ 검증 방법
```bash
# 트랜잭션 commit vs abort 비교

# read_uncommitted (기본) - abort된 메시지도 보임
docker exec kafka kafka-console-consumer --topic tx_topic \
  --from-beginning --bootstrap-server localhost:9092

# read_committed - commit된 메시지만 보임
docker exec kafka kafka-console-consumer --topic tx_topic \
  --from-beginning --bootstrap-server localhost:9092 \
  --isolation-level read_committed
```

---

# Part 2: Consumer 직접 구현 (선택)

> kafka-console-consumer로 충분히 이해했다면, 직접 Consumer 코드를 작성해봅니다.

## Stage 9: Consumer 기본 구현
> 목표: Java Consumer 코드로 메시지 받기

### 📖 개념
- Consumer는 토픽을 구독(subscribe)하고 poll()로 메시지를 가져옴
- `group.id`는 Consumer Group을 식별하는 필수 설정
- `auto.offset.reset`: earliest(처음부터) / latest(최신부터)

### 🤔 예상 퀴즈
```
Q1. Consumer가 시작되기 전에 Producer가 보낸 메시지는?
    a) 사라진다
    b) Consumer가 시작되면 받을 수 있다
    c) auto.offset.reset 설정에 따라 다르다

Q2. poll(Duration.ofMillis(1000))의 의미는?
    a) 1초 동안 대기하며 메시지 수집
    b) 1초마다 한 번씩 호출해야 함
    c) 1초 후 자동 종료
```

### 💻 구현할 파일
`consumer/src/main/java/SimpleConsumer.java`

---

## Stage 10: Consumer Group과 파티션 분배
> 목표: 여러 Consumer가 파티션을 나눠 처리하는 방식 이해

### 🤔 예상 퀴즈
```
Q1. 파티션 3개, Consumer 2개(같은 그룹)이면?
    a) Consumer1: 1개, Consumer2: 2개 파티션 담당
    b) 둘 다 3개 파티션 모두 읽음
    c) 에러 발생

Q2. 파티션 3개, Consumer 4개(같은 그룹)이면?
    a) 4개 모두 메시지를 받는다
    b) 1개는 메시지를 받지 못한다 (놀고 있음)
    c) 에러 발생
```

### 💻 구현할 파일
`consumer/src/main/java/GroupConsumer.java`

---

## Stage 11: 수동 오프셋 커밋
> 목표: 정확한 메시지 처리 보장하기

### 🤔 예상 퀴즈
```
Q1. Consumer가 오프셋 5까지 읽고 커밋 없이 종료되면, 재시작 시?
    a) 오프셋 0부터 다시 읽는다
    b) 오프셋 6부터 읽는다
    c) 마지막 커밋된 오프셋부터 읽는다

Q2. 메시지 처리 중 에러 발생 시, 커밋을 안 하면?
    a) 해당 메시지를 다시 받을 수 있다
    b) 해당 메시지는 유실된다
    c) Consumer가 종료된다
```

### 💻 구현할 파일
`consumer/src/main/java/ManualCommitConsumer.java`

---

## Stage 12: Rebalance Listener 구현
> 목표: Consumer Rebalancing 이벤트를 감지하고 처리하기

### 📖 개념
- `ConsumerRebalanceListener` 인터페이스로 rebalancing 이벤트 감지
- `onPartitionsAssigned()`: 새로운 파티션이 할당되는 시점
- `onPartitionsRevoked()`: 파티션이 회수되는 시점 (현재 오프셋 저장)
- Rebalance 중 상태 정리 및 복구 로직 필요

### 🤔 예상 퀴즈
```
Q1. Consumer 1이 오프셋 10까지 읽다가 Consumer 2가 같은 그룹에
    조인하면 rebalancing이 발생한다. 이 시점에?
    a) onPartitionsRevoked()가 호출되고 오프셋 저장
    b) 바로 메시지 처리 계속함
    c) Consumer가 일시 정지됨

Q2. onPartitionsAssigned()에서 해야 할 작업은?
    a) 새로운 파티션에서 읽을 시작 오프셋 설정
    b) 이전 상태 복구 (예: 메모리 캐시)
    c) a, b 모두

Q3. Consumer 2가 나가면?
    a) Consumer 1이 모든 파티션을 다시 받음 (rebalancing)
    b) 메시지 유실 가능성 있음
    c) a, b 모두 가능
```

### 💻 구현할 파일
`consumer/src/main/java/RebalanceListenerConsumer.java`

### ✅ 검증 방법
```bash
# Consumer 1 실행
java RebalanceListenerConsumer

# 다른 터미널에서 Consumer 2 실행 (같은 group.id)
java RebalanceListenerConsumer

# Consumer 2를 종료하면 rebalancing 로그 관찰
# onPartitionsRevoked → onPartitionsAssigned 순서 확인
```

---

## Stage 13: Seek 메서드 활용
> 목표: 특정 오프셋이나 타임스탬프부터 메시지 읽기

### 📖 개념
- `seek(TopicPartition, long offset)`: 특정 오프셋으로 이동
- `seekToBeginning()`: 맨 처음부터 읽기
- `seekToEnd()`: 맨 끝으로 이동 (새 메시지 대기)
- `offsetsForTimes()`: 타임스탬프 기반 오프셋 조회
- 재처리, 특정 시간대 데이터 조회 등에 활용

### 🤔 예상 퀴즈
```
Q1. Consumer가 이미 메시지를 모두 읽은 후,
    다시 처음부터 읽으려면?
    a) Consumer 종료 후 다시 시작
    b) seek()로 처음 오프셋으로 이동
    c) 새로운 group.id로 시작

Q2. poll() 호출 전/후에 seek()을 호출하면?
    a) 어느 때든 상관없음
    b) poll() 후에 호출해야 함
    c) 반드시 poll() 전에 호출해야 함

Q3. seekToEnd() 후 poll()을 호출하면?
    a) 즉시 메시지를 받음
    b) 새 메시지가 들어올 때까지 대기
    c) 에러 발생
```

### 💻 구현할 파일
`consumer/src/main/java/SeekConsumer.java`

### ✅ 검증 방법
```bash
# 처음부터 다시 읽기
java SeekConsumer --from-beginning

# 특정 시간 이후 메시지 읽기
java SeekConsumer --timestamp 2024-01-21T10:00:00
```

---

## Stage 14: Consumer 성능 튜닝
> 목표: Consumer 설정으로 처리량 최적화하기

### 📖 개념
- `fetch.min.bytes`: 최소 데이터 크기 (배치 효율)
- `fetch.max.wait.ms`: 최대 대기 시간
- `max.poll.records`: 한 번에 가져올 최대 메시지 수
- `max.poll.interval.ms`: poll() 호출 간 최대 간격
- `session.timeout.ms`: 하트비트 없이 살아있는 시간
- `heartbeat.interval.ms`: 하트비트 전송 간격

### 🤔 예상 퀴즈
```
Q1. fetch.min.bytes=1KB, 실제 데이터 100bytes 들어옴
    fetch.max.wait.ms=100ms일 때?
    a) 즉시 100bytes 반환
    b) 100ms 후 100bytes 반환
    c) 1KB 도달할 때까지 대기

Q2. max.poll.records=100인데 poll()에서 50개만 처리하고
    2초 후 다시 poll()하면?
    a) 정상 (나머지 50개 버짐)
    b) 에러 (Rebalancing 발생)
    c) 50개 메시지 반환

Q3. 하트비트 간격 < 메시지 처리 시간이면?
    a) 정상 동작
    b) Consumer가 Dead로 인식 (Rebalancing)
    c) 자동으로 조정됨
```

### 💻 구현할 파일
`consumer/src/main/java/TuningConsumer.java`

### ✅ 검증 방법
```bash
# 처리량 측정: 메시지 100개 처리 시간
# 느린 처리 시뮬레이션 (max.poll.interval.ms 초과 시 Rebalancing)
```

---

## Stage 15: Consumer Lag 모니터링
> 목표: Consumer Lag을 측정하고 병목 원인 파악하기

### 📖 개념
- `Consumer Lag = Latest Offset - Consumer Offset`
- Lag 증가 = Consumer가 메시지 처리를 못 따라감
- kafka-consumer-groups로 Lag 확인
- JMX 메트릭으로 프로그래매틱 모니터링
- Lag이 크면 Consumer 인스턴스 추가 고려

### 🤔 예상 퀴즈
```
Q1. Topic의 Latest Offset=1000, Consumer Offset=950이면
    Lag은?
    a) 50
    b) 950
    c) 1000

Q2. Lag이 지속적으로 증가한다면?
    a) Producer 속도가 Consumer보다 빠름
    b) Consumer가 느림
    c) a, b 둘 다 가능

Q3. Consumer를 2배로 늘리면 Lag이?
    a) 절반으로 감소
    b) 약간 감소 (오버헤드 있음)
    c) 변화 없음 (Partition 수에 따라 다름)
```

### 💻 구현할 파일
`consumer/src/main/java/LagMonitoringConsumer.java`

### ✅ 검증 방법
```bash
# Consumer Group의 Lag 확인
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group my-group \
  --describe

# JMX로 메트릭 수집 (선택)
```

---

# Part 3: 에러 처리 & 실전 패턴

> 프로덕션 환경에서 가장 중요한 에러 처리 및 신뢰성 확보 방법

## Stage 16: Producer 에러 처리
> 목표: Producer 실패 시나리오 대응하기

### 📖 개념
- `RetriableException` vs `Non-RetriableException`
- Callback에서 exception 처리
- 자동 재시도 vs 수동 재시도
- 재시도 불가 에러 처리 (별도 저장소)
- Circuit Breaker 패턴으로 과부하 방지

### 🤔 예상 퀴즈
```
Q1. Producer.send()에서 발생한 TimeoutException은?
    a) RetriableException (자동 재시도됨)
    b) Non-RetriableException (바로 실패)
    c) 설정에 따라 다름

Q2. Callback의 exception이 non-null이지만 message가 저장되면?
    a) 성공 (exception 무시)
    b) 실패 (exception 확인)
    c) 재시도

Q3. Producer 재시도 설정을 0으로 하면?
    a) 재시도 안 함 (바로 실패)
    b) 기본값 사용
    c) 에러 발생
```

### 💻 구현할 파일
`producer/src/main/java/ErrorHandlingProducer.java`

### ✅ 검증 방법
```bash
# Broker를 잠시 중단했다가 재시작
docker pause kafka
sleep 5
docker unpause kafka

# 재시도 로그 관찰
```

---

## Stage 17: Consumer 에러 처리 & DLQ 패턴
> 목표: 처리 실패한 메시지를 Dead Letter Queue로 분류

### 📖 개념
- Consumer 처리 실패 시나리오 (DB 에러, 검증 실패 등)
- DLQ (Dead Letter Queue): 실패 메시지 별도 저장
- 재시도 횟수 제한 (무한 루프 방지)
- 실패 메시지 모니터링 및 복구 프로세스

### 🤔 예상 퀴즈
```
Q1. Consumer가 DB 저장 중 UniqueConstraintException 발생하면?
    a) 자동으로 재시도
    b) 메시지 버림
    c) 수동으로 처리 (일반적으로 DLQ로 전송)

Q2. DLQ에 저장된 메시지는?
    a) 영구 보관 (나중에 수동 처리)
    b) 일정 시간 후 자동 삭제
    c) 특별한 대책 없음

Q3. 같은 메시지가 DLQ에 100번 들어오면?
    a) 정상 (각각 처리 가능)
    b) 문제 (근본 원인 파악 필요)
    c) 자동으로 중복 제거됨
```

### 💻 구현할 파일
- `consumer/src/main/java/DLQConsumer.java`
- 메인 토픽 실패 → DLQ 토픽으로 전송

### ✅ 검증 방법
```bash
# 의도적 에러 발생 (예: 특정 customerId 거부)
# Main 토픽 메시지 → 처리 실패 → DLQ 토픽 이동 확인

docker exec kafka kafka-console-consumer --topic order-dlq \
  --from-beginning --bootstrap-server localhost:9092
```

---

## Stage 18: Exactly-Once Semantics (EOS) 심화
> 목표: 메시지가 정확히 한 번 처리되도록 보장

### 📖 개념
- **At-Least-Once**: 메시지가 최소 1번 처리 (중복 가능)
- **At-Most-Once**: 메시지가 최대 1번 처리 (유실 가능)
- **Exactly-Once**: 메시지가 정확히 1번 처리 (트랜잭션 필수)
- Producer: `enable.idempotence=true` + `transactional.id`
- Consumer: `isolation.level=read_committed` + 수동 커밋
- Producer-Consumer 간 트랜잭션 조율

### 🤔 예상 퀴즈
```
Q1. Producer와 Consumer 모두 EOS 설정했는데,
    Consumer가 실패해서 같은 메시지를 2번 처리하면?
    a) Exactly-Once 보장 (불가능)
    b) Consumer 로직에서 중복 제거 필요
    c) Idempotence로 자동 처리

Q2. Transactional Producer의 abort된 메시지를
    isolation.level=read_uncommitted Consumer가 받으면?
    a) 받음 (설정에 따라)
    b) 안 받음 (자동 필터링)
    c) 에러 발생

Q3. 다중 파티션에 메시지를 쓰는 트랜잭션 중 실패하면?
    a) 일부만 저장됨 (부분 성공)
    b) 모두 롤백 (원자성)
    c) 일부는 저장, 일부는 롤백
```

### 💻 구현할 파일
`producer/src/main/java/ExactlyOnceProducer.java`

### ✅ 검증 방법
```bash
# 중복 메시지 발생 시나리오 테스트
# 트랜잭션 abort 후 메시지 상태 확인

docker exec kafka kafka-console-consumer --topic tx-topic \
  --isolation-level read_committed \
  --from-beginning --bootstrap-server localhost:9092
```

---

## Stage 19: Circuit Breaker & 복구 패턴
> 목표: 장애 상황에서 시스템 안정성 유지하기

### 📖 개념
- **Circuit Breaker**: 연속 실패 시 요청 차단 (Fail Fast)
- Open 상태: 요청 차단 → Timeout 없이 빠르게 실패
- Half-Open 상태: 일부 요청 허용 (복구 감지)
- Closed 상태: 정상 → 모든 요청 허용
- 타임아웃 설정으로 Dead Connection 회피

### 🤔 예상 퀴즈
```
Q1. DB가 완전히 다운되었을 때,
    Circuit Breaker가 없으면?
    a) Timeout까지 계속 재시도 (리소스 낭비)
    b) 빠르게 실패 (정상)
    c) 자동 복구

Q2. Circuit Breaker가 Open 상태면?
    a) 요청을 처리함 (평상시처럼)
    b) 모든 요청을 즉시 실패 (Fail Fast)
    c) 요청을 대기 중

Q3. DB가 복구됐는데 Circuit Breaker는 여전히 Open이면?
    a) 영구적으로 Open (수동 재설정 필요)
    b) Half-Open으로 자동 전환 (일부 요청 시도)
    c) Closed로 즉시 변경
```

### 💻 구현할 파일
`consumer/src/main/java/CircuitBreakerConsumer.java`

### ✅ 검증 방법
```bash
# DB 연결 실패 시뮬레이션
# Circuit Breaker 상태 전환 관찰 (Open → Half-Open → Closed)
# 로그에서 상태 변화 확인
```

---

# 학습 순서 요약

## Part 1: Producer 심화 (kafka-console-consumer로 검증)

| Stage | 주제 | 핵심 키워드 | 난이도 |
|-------|------|------------|--------|
| 1 | 파티션 + 키 | partition, key routing, hash | ⭐ |
| 2 | acks 설정 | acks=0/1/all, durability | ⭐ |
| 3 | 재시도 | retries, ordering, in.flight | ⭐⭐ |
| 4 | 멱등성 | enable.idempotence, PID | ⭐⭐ |
| 5 | 배치 | batch.size, linger.ms | ⭐ |
| 6 | 압축 | compression.type | ⭐ |
| 7 | JSON | Serializer, Jackson | ⭐⭐ |
| 8 | 트랜잭션 | transactional.id, EOS | ⭐⭐⭐ |

## Part 2: Consumer 직접 구현 (기본 3 Stages → 확장 7 Stages)

| Stage | 주제 | 핵심 키워드 | 난이도 | 상태 |
|-------|------|------------|--------|------|
| 9 | Consumer 기본 | subscribe, poll, group.id | ⭐ | ✅ |
| 10 | Consumer Group | 파티션 분배, 리밸런싱 | ⭐⭐ | ✅ |
| 11 | 수동 커밋 | commitSync, enable.auto.commit | ⭐⭐ | ✅ |
| 12 | Rebalance Listener | onPartitionsAssigned/Revoked | ⭐⭐ | ⏳ |
| 13 | Seek 메서드 | seek, seekToBeginning, timestamp | ⭐⭐ | ⏳ |
| 14 | 성능 튜닝 | fetch 설정, poll 최적화 | ⭐⭐ | ⏳ |
| 15 | Lag 모니터링 | consumer lag, 병목 분석 | ⭐⭐ | ⏳ |

## Part 3: 에러 처리 & 실전 패턴 (신규 4 Stages)

| Stage | 주제 | 핵심 키워드 | 난이도 |
|-------|------|------------|--------|
| 16 | Producer 에러 처리 | RetriableException, callback | ⭐⭐⭐ |
| 17 | DLQ 패턴 | Dead Letter Queue, 재시도 | ⭐⭐⭐ |
| 18 | Exactly-Once (심화) | EOS, 트랜잭션, idempotence | ⭐⭐⭐⭐ |
| 19 | Circuit Breaker | 장애 격리, 복구 패턴 | ⭐⭐⭐ |

---

# 수정할 파일 목록

## Part 1: Producer (8개 Stage)
```
producer/src/main/java/
├── KeyPartitionProducer.java    # Stage 1
├── AcksProducer.java            # Stage 2
├── RetryProducer.java           # Stage 3
├── IdempotentProducer.java      # Stage 4
├── BatchProducer.java           # Stage 5
├── CompressionProducer.java     # Stage 6
├── JsonProducer.java            # Stage 7
├── model/User.java              # Stage 7
├── TransactionalProducer.java   # Stage 8
└── ErrorHandlingProducer.java   # Stage 16
```

## Part 2: Consumer 기본 (3개 Stage - 기존)
```
consumer/src/main/java/
├── SimpleConsumer.java          # Stage 9
├── GroupConsumer.java           # Stage 10
└── ManualCommitConsumer.java    # Stage 11
```

## Part 2 확장: Consumer 심화 (4개 Stage - 신규)
```
consumer/src/main/java/
├── RebalanceListenerConsumer.java  # Stage 12
├── SeekConsumer.java               # Stage 13
├── TuningConsumer.java             # Stage 14
└── LagMonitoringConsumer.java      # Stage 15
```

## Part 3: 에러 처리 & 패턴 (4개 Stage - 신규)
```
consumer/src/main/java/
├── DLQConsumer.java            # Stage 17
├── ExactlyOnceProducer.java    # Stage 18 (Producer 영역)
└── CircuitBreakerConsumer.java # Stage 19
```

## 수정할 파일
- `settings.gradle` - consumer 모듈 추가 (Part 2)
- `producer/build.gradle` - Jackson 의존성 추가 (Stage 7)
- `producer/build.gradle` - Resilience4j 추가 (Stage 19)

---

## 📊 커리큘럼 확장 요약

### Before (기존)
- Producer: 8 Stages
- Consumer: 3 Stages
- Error Handling: None
- **총 11 Stages**

### After (개선안)
- Producer: 8 Stages + 1 (Error Handling)
- Consumer: 7 Stages (3 → 3 + 4 확장)
- Error Handling: 3 Stages (Circuit Breaker 포함)
- **총 19 Stages**

### 예상 학습 시간
- Part 1 (Producer): 3-4시간
- Part 2 (Consumer): 4-5시간
- Part 3 (Error Handling): 2-3시간
- **총 9-12시간**
