# Topic과 Partition

## 📌 핵심 개념

### Topic이란?
- Kafka에서 메시지를 **카테고리별로 분류**하는 논리적 단위
- 데이터베이스의 테이블과 비슷한 역할
- 예: `order-events`, `user-logs`, `payment-transactions`

### Partition이란?
- Topic을 **물리적으로 분할**한 단위
- 각 Partition은 **순서가 보장되는 로그 파일**처럼 동작
- Partition 수 = **병렬 처리 능력**

```
Topic: order-events
├── Partition 0: [msg1] → [msg4] → [msg7] → ...
├── Partition 1: [msg2] → [msg5] → [msg8] → ...
└── Partition 2: [msg3] → [msg6] → [msg9] → ...
```

---

## 🔑 왜 Partition이 필요한가?

### 1. 병렬 처리 (Parallelism)
- Partition이 많을수록 더 많은 Consumer가 **동시에** 메시지 처리 가능
- `Partition 수 >= Consumer 수` 일 때 최적

### 2. 순서 보장 (Ordering)
- **같은 Partition 내에서만** 메시지 순서 보장
- 전체 Topic 레벨에서는 순서 보장 ❌

### 3. 확장성 (Scalability)
- Partition을 여러 Broker에 분산 저장
- 하나의 Broker 부하 분산

---

## 📊 메시지 분배 방식

### Key가 없는 경우 (Round-Robin)
```java
// Key 없이 전송
new ProducerRecord<>("topic", "value")
```
→ Partition 0, 1, 2, 0, 1, 2... 순서로 분배

### Key가 있는 경우 (Hash 기반)
```java
// Key와 함께 전송
new ProducerRecord<>("topic", "user-123", "value")
```
→ `hash(key) % partition_count` 로 Partition 결정
→ **같은 Key는 항상 같은 Partition**으로 이동

---

## 💻 CLI 실습

### Topic 생성
```bash
# Partition 3개인 Topic 생성
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
    --create --topic test-topic --partitions 3 --replication-factor 1
```

### Topic 목록 확인
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Topic 상세 정보
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
    --describe --topic test-topic
```

### 메시지 전송 (Console Producer)
```bash
# Key 없이 전송
docker exec -it kafka kafka-console-producer --bootstrap-server localhost:9092 \
    --topic test-topic

# Key와 함께 전송 (key:value 형식)
docker exec -it kafka kafka-console-producer --bootstrap-server localhost:9092 \
    --topic test-topic --property "parse.key=true" --property "key.separator=:"
```

### 메시지 수신 (Console Consumer)
```bash
# 처음부터 읽기 + Partition 정보 표시
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic test-topic --from-beginning \
    --property print.partition=true --property print.key=true
```

---

## 🧪 직접 풀어볼 문제

### 문제 1: Partition 분배 확인
**목표:** Key 유무에 따른 Partition 분배 차이 확인

1. Partition 3개인 `partition-test` Topic 생성
2. Key 없이 메시지 10개 전송 → 어느 Partition에 저장되는지 확인
3. 같은 Key로 메시지 10개 전송 → 모두 같은 Partition에 저장되는지 확인

```bash
# TODO: 여기에 실행할 명령어 작성
```

**예상 결과:**
- Key 없음: Round-Robin으로 0, 1, 2, 0, 1, 2... 분배
- Key 있음: 모든 메시지가 동일한 Partition에 저장

---

### 문제 2: Partition 수와 Consumer 관계
**목표:** Consumer 수가 Partition 수보다 많을 때 동작 확인

1. Partition 2개인 Topic 생성
2. Consumer 3개 실행 (같은 Group)
3. 어떤 Consumer가 메시지를 받는지 관찰

**예상 결과:**
- Consumer 1개는 Partition을 할당받지 못함 (idle 상태)

---

### 문제 3: 순서 보장 테스트
**목표:** Partition 내 순서 보장 vs Topic 레벨 순서

1. Partition 3개인 Topic에 순서대로 1~10 메시지 전송 (Key 없이)
2. Consumer로 읽었을 때 순서 확인
3. 같은 Key로 1~10 전송 후 순서 확인

**예상 결과:**
- Key 없음: 전체 순서는 뒤섞일 수 있음
- Key 있음: 1, 2, 3, 4... 순서대로 수신

---

## 🔍 핵심 정리

| 항목 | 설명 |
|-----|------|
| Topic | 메시지의 논리적 분류 단위 |
| Partition | Topic의 물리적 분할 단위, 병렬 처리의 기본 |
| Key 없음 | Round-Robin 분배 |
| Key 있음 | Hash 기반 → 같은 Key는 같은 Partition |
| 순서 보장 | Partition 내에서만 보장됨 |

---

## 📝 학습 노트

> 여기에 강의를 들으면서 추가로 배운 내용이나 궁금한 점을 기록하세요.

```
-
-
-
```
