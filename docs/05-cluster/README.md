# 클러스터 운영

> **멀티 브로커 환경에서 Kafka의 분산 특성을 직접 경험합니다.**
>
> 이전까지는 단일 브로커에서 학습했다면,
> 이제는 실제 프로덕션 환경처럼 여러 브로커가 협력하는 상황을 다룹니다.

---

## 🎯 학습 목표

- ✅ 3-Broker 클러스터 docker-compose로 구성
- ✅ Replication Factor와 데이터 안정성 관계 이해
- ✅ Leader Election 과정 직접 관찰
- ✅ ISR (In-Sync Replicas) 상태 추적
- ✅ 실패 시나리오 테스트 (Broker 중단, 복구)
- ✅ 운영 명령어 습숙
- ✅ 성능 비교 (복제도에 따른 처리량)

---

## 📋 파일 구조

```
docs/05-cluster/
├── README.md (현재 파일)           # 전체 개요 및 실습 가이드
├── docker-compose-3broker.yml      # 3-Broker 클러스터 설정
├── setup-cluster.sh                # 클러스터 자동 설정 스크립트
└── scenarios/
    ├── 01-basic-cluster.md         # 기본 클러스터 구성
    ├── 02-replication.md           # 복제 실습
    ├── 03-failure-recovery.md      # 장애 복구 시나리오
    └── 04-performance-tuning.md    # 성능 튜닝
```

---

## 🚀 빠른 시작: 3-Broker 클러스터 구성

### 1단계: docker-compose 파일 준비

```yaml
# docker-compose-3broker.yml
version: '3'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka-broker-1:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-broker-1:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'false'
    ports:
      - "9092:9092"

  kafka-broker-2:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 2
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-broker-2:29092,PLAINTEXT_HOST://localhost:9093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'false'
    ports:
      - "9093:9092"

  kafka-broker-3:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 3
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-broker-3:29092,PLAINTEXT_HOST://localhost:9094
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'false'
    ports:
      - "9094:9092"
```

### 2단계: 클러스터 시작

```bash
# docker-compose 파일 위치로 이동
cd docs/05-cluster

# 클러스터 시작
docker-compose -f docker-compose-3broker.yml up -d

# 상태 확인
docker-compose -f docker-compose-3broker.yml ps
```

### 3단계: 토픽 생성 (복제도 3)

```bash
# 3개 파티션, 복제도 3인 토픽 생성
docker exec kafka-broker-1 kafka-topics --create \
  --topic test-cluster \
  --partitions 3 \
  --replication-factor 3 \
  --bootstrap-server localhost:9092

# 토픽 상세 정보 확인
docker exec kafka-broker-1 kafka-topics --describe \
  --topic test-cluster \
  --bootstrap-server localhost:9092
```

### 4단계: 메시지 전송 및 복제 확인

```bash
# Producer로 메시지 전송 (1000개)
for i in {1..1000}; do
  echo "message-$i" | docker exec -i kafka-broker-1 \
    kafka-console-producer \
    --broker-list localhost:9092 \
    --topic test-cluster
done

# Consumer로 수신 확인
docker exec kafka-broker-1 kafka-console-consumer \
  --topic test-cluster \
  --from-beginning \
  --bootstrap-server localhost:9092 | head -20
```

---

## 🔍 클러스터 상태 모니터링

### 토픽 정보 조회

```bash
# 토픽의 파티션별 복제본 상태
docker exec kafka-broker-1 kafka-topics --describe \
  --topic test-cluster \
  --bootstrap-server localhost:9092

# 출력 예시:
# Topic: test-cluster     Partition: 0    Leader: 1    Replicas: 1,2,3    Isr: 1,2,3
# Topic: test-cluster     Partition: 1    Leader: 2    Replicas: 2,3,1    Isr: 2,3,1
# Topic: test-cluster     Partition: 2    Leader: 3    Replicas: 3,1,2    Isr: 3,1,2
```

### 메타데이터 상세 분석

```bash
# 각 파티션의 상세 정보
docker exec kafka-broker-1 kafka-topics --describe \
  --topic test-cluster \
  --under-replicated-partitions \
  --bootstrap-server localhost:9092

# Leader 변경 감시 (실시간)
watch -n 1 'docker exec kafka-broker-1 kafka-topics --describe \
  --topic test-cluster \
  --bootstrap-server localhost:9092'
```

---

## 📊 실습 시나리오

### 시나리오 1: 정상 클러스터 상태

**목표**: 클러스터가 정상으로 작동하는지 확인

```bash
# 1. 토픽 정보 확인
docker exec kafka-broker-1 kafka-topics --describe \
  --topic test-cluster \
  --bootstrap-server localhost:9092

# 2. ISR이 모두 동일한지 확인
# 각 파티션의 Isr이 모든 레플리카를 포함해야 함

# 3. Leader 분산 확인
# Partition 0: Leader 1, Partition 1: Leader 2, Partition 2: Leader 3
# (라운드 로빈으로 분산되어야 함)
```

**예상 출력**:
```
Topic: test-cluster     Partition: 0    Leader: 1    Replicas: 1,2,3    Isr: 1,2,3
Topic: test-cluster     Partition: 1    Leader: 2    Replicas: 2,3,1    Isr: 2,3,1
Topic: test-cluster     Partition: 2    Leader: 3    Replicas: 3,1,2    Isr: 3,1,2
```

**학습 포인트**:
- `Replicas`: 해당 파티션이 저장된 브로커 목록
- `Leader`: 현재 읽기/쓰기를 담당하는 브로커
- `ISR`: Leader + 동기화된 Follower들 (In-Sync Replicas)

### 시나리오 2: Broker 장애 시뮬레이션

**목표**: Broker 중단 시 Leader 변경 관찰

```bash
# 1. 시작 전 상태 확인
docker exec kafka-broker-1 kafka-topics --describe \
  --topic test-cluster --bootstrap-server localhost:9092

# 2. Broker 2 중단
docker-compose -f docker-compose-3broker.yml pause kafka-broker-2

# 3. 즉시 상태 확인 (변화 관찰)
docker exec kafka-broker-1 kafka-topics --describe \
  --topic test-cluster --bootstrap-server localhost:9092

# 5초 후 다시 확인
sleep 5
docker exec kafka-broker-1 kafka-topics --describe \
  --topic test-cluster --bootstrap-server localhost:9092

# 출력: ISR에서 2가 제거됨
# Topic: test-cluster     Partition: 0    Leader: 1    Replicas: 1,2,3    Isr: 1,3
# Topic: test-cluster     Partition: 1    Leader: 3    Replicas: 2,3,1    Isr: 3,1    ← Leader 변경!
# Topic: test-cluster     Partition: 2    Leader: 3    Replicas: 3,1,2    Isr: 3,1
```

**학습 포인트**:
- Broker 중단 → ISR에서 제거
- Broker 2가 Leader였던 파티션 → 새 Leader 선출
- Rebalancing 발생 (자동으로 진행)

### 시나리오 3: Broker 복구 및 ISR 복원

**목표**: 다운된 Broker 복구 후 ISR 자동 복원 관찰

```bash
# 1. Broker 2 복구
docker-compose -f docker-compose-3broker.yml unpause kafka-broker-2

# 2. 상태 모니터링 (복구 과정)
for i in {1..10}; do
  echo "=== Check $i ==="
  docker exec kafka-broker-1 kafka-topics --describe \
    --topic test-cluster --bootstrap-server localhost:9092
  sleep 2
done

# 3. 최종 상태 확인 (ISR 복원)
docker exec kafka-broker-1 kafka-topics --describe \
  --topic test-cluster --bootstrap-server localhost:9092

# 출력: ISR이 다시 모두 포함됨
# Topic: test-cluster     Partition: 0    Leader: 1    Replicas: 1,2,3    Isr: 1,2,3
```

**학습 포인트**:
- Broker 복구 → 자동으로 복제 시작
- 복제 완료 → ISR에 다시 추가
- Producer/Consumer는 계속 정상 작동 (다른 Broker 사용)

### 시나리오 4: 동시 다중 Broker 장애

**목표**: 복제도가 생명인 이유 이해

```bash
# 1. Broker 2, 3 동시 중단
docker-compose -f docker-compose-3broker.yml pause kafka-broker-2 kafka-broker-3

# 2. 상태 확인
docker exec kafka-broker-1 kafka-topics --describe \
  --topic test-cluster --bootstrap-server localhost:9092

# 출력: ISR에 Broker 1만 남음
# Topic: test-cluster     Partition: 0    Leader: 1    Replicas: 1,2,3    Isr: 1
# Topic: test-cluster     Partition: 1    Leader: 1    Replicas: 2,3,1    Isr: 1
# Topic: test-cluster     Partition: 2    Leader: 1    Replicas: 3,1,2    Isr: 1

# 3. Consumer 계속 읽기 가능 (Broker 1에서)
docker exec kafka-broker-1 kafka-console-consumer \
  --topic test-cluster \
  --max-messages 10 \
  --bootstrap-server localhost:9092

# 4. Producer는 acks=all일 때 대기 상태 (ISR이 충분하지 않음)
# acks=1 또는 acks=0일 때는 계속 전송

# 5. Broker 복구
docker-compose -f docker-compose-3broker.yml unpause kafka-broker-2 kafka-broker-3
```

**학습 포인트**:
- 복제도 3 → 2개 동시 장애에도 데이터 손실 없음
- 복제도 1 → 1개 장애 시 데이터 유실 위험
- `acks=all` 설정 시 중요함 (Producer가 기다려야 함)

### 시나리오 5: 성능 비교 (복제도에 따른 영향)

**목표**: 복제도에 따른 처리량 차이 측정

```bash
# 복제도 1로 토픽 생성
docker exec kafka-broker-1 kafka-topics --create \
  --topic perf-rf1 \
  --partitions 3 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092

# 복제도 3으로 토픽 생성
docker exec kafka-broker-1 kafka-topics --create \
  --topic perf-rf3 \
  --partitions 3 \
  --replication-factor 3 \
  --bootstrap-server localhost:9092

# Producer 성능 테스트 (복제도 1)
time docker exec kafka-broker-1 kafka-producer-perf-test \
  --topic perf-rf1 \
  --num-records 100000 \
  --record-size 1024 \
  --throughput -1 \
  --producer-props bootstrap.servers=localhost:9092

# Producer 성능 테스트 (복제도 3)
time docker exec kafka-broker-1 kafka-producer-perf-test \
  --topic perf-rf3 \
  --num-records 100000 \
  --record-size 1024 \
  --throughput -1 \
  --producer-props bootstrap.servers=localhost:9092 acks=all

# 결과 비교
# RF=1: 더 빠름 (복제 없음)
# RF=3: 더 느림 (복제 대기 중)
# 안정성 vs 성능의 트레이드오프
```

---

## 🛠️ 주요 운영 명령어

### 클러스터 상태 확인

```bash
# 모든 토픽 조회
docker exec kafka-broker-1 kafka-topics --list \
  --bootstrap-server localhost:9092

# 토픽 상세 정보
docker exec kafka-broker-1 kafka-topics --describe \
  --bootstrap-server localhost:9092

# 복제되지 않은 파티션 확인
docker exec kafka-broker-1 kafka-topics --describe \
  --under-replicated-partitions \
  --bootstrap-server localhost:9092

# Broker 메타데이터 확인
docker exec kafka-broker-1 kafka-metadata-shell \
  --snapshot /var/lib/kafka/data/__cluster_metadata-0/00000000000000000000.log
```

### Consumer Group 모니터링

```bash
# Consumer Group 목록
docker exec kafka-broker-1 kafka-consumer-groups \
  --list --bootstrap-server localhost:9092

# Consumer Group 상세 정보
docker exec kafka-broker-1 kafka-consumer-groups \
  --describe --group my-group \
  --bootstrap-server localhost:9092

# Consumer Lag 확인
docker exec kafka-broker-1 kafka-consumer-groups \
  --describe --group my-group --members \
  --bootstrap-server localhost:9092
```

---

## 📈 클러스터 튜닝

### 복제 속도 조정

```properties
# broker 설정 (docker-compose 환경변수)
KAFKA_NUM_REPLICA_FETCHERS: 4           # 복제 스레드 수 (기본: 2)
KAFKA_REPLICA_SOCKET_RECEIVE_BUFFER_BYTES: 102400  # 수신 버퍼
KAFKA_REPLICA_LAG_TIME_MAX_MS: 10000   # ISR 제거 임계값 (기본: 30000)
```

### 성능 최적화

```bash
# Producer 설정 (acks=1로 성능 개선)
kafka-producer-perf-test \
  --topic test \
  --num-records 100000 \
  --record-size 1024 \
  --producer-props \
    bootstrap.servers=localhost:9092 \
    acks=1 \
    compression.type=snappy

# Consumer 배치 최적화
kafka-consumer-perf-test \
  --broker-list localhost:9092 \
  --topic test \
  --messages 100000 \
  --fetch-size 1048576  # 1MB
```

---

## 🧪 테스트 체크리스트

- [ ] 1. 3-Broker 클러스터 정상 기동
- [ ] 2. 토픽 생성 (RF=3) 및 ISR 확인
- [ ] 3. 메시지 전송/수신 정상 작동
- [ ] 4. 단일 Broker 장애 시뮬레이션
- [ ] 5. Leader 변경 관찰
- [ ] 6. Broker 복구 후 ISR 복원
- [ ] 7. 다중 Broker 동시 장애
- [ ] 8. 성능 비교 (RF 1 vs 3)
- [ ] 9. Consumer Lag 모니터링
- [ ] 10. 클러스터 정상 종료

---

## 💡 핵심 학습 내용

### 복제도 선택 기준

| RF | 장점 | 단점 | 사용 사례 |
|----|------|------|---------|
| 1 | 빠름, 저장소 효율 | 장애 시 데이터 유실 | 실시간 로그 (유실 허용) |
| 2 | 안정성 + 성능 | 2개 동시 장애 불가 | 일반 서비스 |
| 3 | 높은 안정성 | 느림, 저장소 낭비 | 금융, 거래 시스템 |

### Leader Election 알고리즘

1. **ISR 중 첫 번째 레플리카** 선출 (Unclean Leader Election 비활성화 시)
2. **모든 ISR 다운** → Unclean Leader Election (데이터 유실 위험)
3. **복구 중** → 가장 빨리 동기화된 Follower 선출

### 모니터링 포인트

- **ISR 크기**: 작으면 위험 (복제본 부족)
- **Under-Replicated**: 있으면 주의 (복제 지연)
- **Consumer Lag**: 크면 처리 지연 (Consumer 추가)

---

## 🚀 다음 단계

1. **고급 토픽**으로 진행 (docs/06-advanced/)
2. **성능 튜닝** 심화 학습
3. **모니터링 도구** 연동 (Prometheus + Grafana)
4. **보안 설정** (SSL/TLS, SASL)
