# Stage 3 테스트 가이드: 재시도와 순서 역전

## 1. 개요

### 학습 목표
이 단계에서는 Kafka Producer의 **재시도(Retries)** 설정과 **메시지 순서 보장(Order Guarantee)**의 관계를 이해합니다.

**핵심 개념:**
- `retries`: 전송 실패 시 재시도 횟수 설정
- `retry.backoff.ms`: 재시도 간격 (기본값: 100ms)
- `max.in.flight.requests.per.connection`: 동시에 진행 가능한 요청 수

### 순서 역전 문제 (Out-of-Order Problem)

```
시나리오: 메시지 A, B를 순서대로 전송

========== max.in.flight = 1인 경우 (순서 보장) ==========
시간    메시지A         메시지B         Broker 상태
t1     전송중          -               -
t2     -               대기중          -
t3     전송 성공       -               [A]
t4     -               전송 성공       [A, B]

========== max.in.flight > 1인 경우 (순서 역전!) ==========
시간    메시지A         메시지B         Broker 상태
t1     전송중          -               -
t2     전송 실패       -               -
t3     -               전송 성공       [B]
t4     재시도 중       -               [B]
t5     재시도 성공     -               [B, A]  ← 순서 역전!
```

---

## 2. 사전 준비

### 2.1 토픽 생성

**중요**: 파티션을 **1개**로 설정하는 이유:
- Kafka는 **같은 파티션 내에서만** 메시지 순서를 보장합니다

```bash
docker exec kafka-1 kafka-topics --create \
  --topic retry-topic \
  --partitions 1 \
  --replication-factor 3 \
  --bootstrap-server localhost:9092
```

### 2.2 프로젝트 빌드

```bash
cd /Users/doohyeon/Desktop/Project/order-native-kafka/producer
mvn clean package -DskipTests
```

### 2.3 순서 역전을 실제로 보는 방법

**문제:** 로컬 Docker 환경에서는 1500ms 안에 모든 메시지가 전송되므로, 외부 개입(Broker 중지)이 어렵습니다.

**해결책:** 코드 수정으로 의도적 실패를 만듭니다.

#### ✅ 추천 방법: 특정 메시지에 의도적 지연 추가

**RetryProducer.java 수정 - send() 호출 부분에 지연 추가:**

```java
for (int seq = 0; seq < totalMessages; seq++) {
    final String value = "msg-" + seq;
    ProducerRecord<String, String> record = new ProducerRecord<>(topic, value);

    // 매 100개마다 200ms 대기 → Broker 중지할 시간 확보
    if (seq % 100 == 0 && seq > 0) {
        try {
            Thread.sleep(200);  // 200ms 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    producer.send(record, (metadata, exception) -> { ... });
}
```

이제 전송 시간이 약 3초 이상 소요되어 **Broker를 중지할 충분한 시간** 확보됩니다!

#### 🔧 실행 절차

```bash
# 1. RetryProducer.java 수정 (위 코드로 지연 추가)
# 2. 빌드
mvn clean package -DskipTests

# 3. 터미널 1: Producer 시작
java -cp target/producer-0.0.1-SNAPSHOT.jar:target/classes \
  org.example.producer.RetryProducer

# 4. 터미널 2: 약 1초 후 Broker 중지 (이제 가능!)
# 로그에서 "송신 큐 적재 시간" 시작되면:
docker stop kafka-1

# 5. 약 5초 대기
sleep 5

# 6. Broker 재시작
docker start kafka-1

# Producer는 자동으로 재시도 → 순서 역전 발생!
```

---

## 3. Scenario: 높은 처리량, 순서 보장 없음 (순서 역전 발생)

### 3.1 코드 수정 (필수!)

**Step 1: RetryProducer.java 수정 - send() 부분**

위의 2.3 섹션 "추천 방법"의 지연 코드를 추가합니다:

```java
for (int seq = 0; seq < totalMessages; seq++) {
    final String value = "msg-" + seq;
    ProducerRecord<String, String> record = new ProducerRecord<>(topic, value);

    // ✅ 매 100개마다 200ms 대기 추가
    if (seq % 100 == 0 && seq > 0) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    producer.send(record, (metadata, exception) -> { ... });
}
```

**Step 2: Producer 설정 활성화**

```java
// === 순서 역전이 발생하는 설정 ===
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
```

### 3.2 실행

**Step 1: 빌드**

```bash
mvn clean package -DskipTests
```

**Step 2: 터미널 1 - Producer 시작**

```bash
java -cp target/producer-0.0.1-SNAPSHOT.jar:target/classes \
  org.example.producer.RetryProducer
```

**Step 3: 터미널 2 - Broker 중지 (즉시)**

Producer 로그에서 다음 메시지가 나타나면:

```
⏰ 메시지 전송 큐 준비 완료!
✅ 이 시점에서 Broker를 중지하세요:
   docker stop kafka-1 && sleep 5 && docker start kafka-1
📢 30초 동안 대기 중입니다...
   (일부 메시지는 아직 ACK를 받지 못한 상태)
```

이 메시지가 보인 즉시 다른 터미널에서 명령을 실행하세요:

```bash
docker stop kafka-1

# 5초 대기
sleep 5

# Broker 재시작
docker start kafka-1
```

⚠️ **중요**:
- 메시지가 보인 후 **최대 30초 이내**에 명령을 실행해야 합니다
- Broker가 다시 시작되면, 미완료된 메시지들이 재시도되면서 **순서 역전이 발생**합니다

### 3.3 결과 확인

**Producer 로그 (끝 부분):**
```
========================================
순서 보장 검증
========================================
수신된 최대 시퀀스 번호: 99999
순서 역전 발생 횟수: 12      ← ✓ 순서 역전 발생!
✗ 순서 역전 발생! (12번)
========================================
```

### 3.4 Consumer로 Broker 실제 상태 확인

```bash
docker exec kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic retry-topic \
  --from-beginning
```

**출력 예:**
```
msg-0
msg-1
...
msg-99
msg-100
msg-101
...
msg-150  ← Broker 중지된 구간의 메시지들
msg-51   ← 뒤바뀜! (msg-51~99가 나중에 도착)
msg-52
...
msg-99
...
```

→ **Broker에 저장된 순서가 뒤바뀌어 있음! (원치 않는 결과)**

---

## 4. 학습 체크포인트

### 핵심 개념

1. **max.in.flight > 1일 때 순서 역전이 발생하는 이유는?**
   - 메시지들이 동시에 처리되어 각각 독립적으로 경쟁
   - 네트워크 지연이나 Broker 처리 시간 차이로 순서 변경 가능
   - 재시도 시 이미 전송된 다른 메시지가 먼저 저장될 수 있음

2. **왜 이것이 문제가 되는가?**
   - 메시지 순서가 중요한 비즈니스 로직 (예: 주문 시스템, 거래 내역)에서 심각한 오류 유발
   - 데이터 일관성 문제 발생

3. **순서 역전을 방지하는 방법은?**
   - `max.in.flight.requests.per.connection` = 1 (낮은 처리량)
   - `enable.idempotence` = true (성능 유지하면서 순서 보장)

---

**생성일**: 2026-01-22
**Stage**: 3 (Retries와 순서 역전)
