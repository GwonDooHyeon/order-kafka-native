# Kafka 명령어 모음

> Docker 컨테이너 안에서 실행하는 명령어입니다.

## 컨테이너 정보

| 컨테이너명 | 호스트 포트 | 용도 |
|-----------|------------|------|
| kafka-1 | 9092 | Broker 1 |
| kafka-2 | 9093 | Broker 2 |
| kafka-3 | 9094 | Broker 3 |

```bash
# 컨테이너 진입
docker exec -it kafka-1 bash
```

---

## 토픽 관리

### 토픽 생성
```bash
kafka-topics --bootstrap-server localhost:9092 \
    --create \
    --topic <토픽명> \
    --partitions <파티션수> \
    --replication-factor <복제수>

# 예시: 파티션 3개, 복제 3개 토픽 생성
kafka-topics --bootstrap-server localhost:9092 \
    --create \
    --topic my-topic \
    --partitions 3 \
    --replication-factor 3
```

### 토픽 목록 조회
```bash
kafka-topics --bootstrap-server localhost:9092 --list
```

### 토픽 상세 정보
```bash
kafka-topics --bootstrap-server localhost:9092 \
    --describe \
    --topic <토픽명>
```

출력 예시:
```
Topic: my-topic  PartitionCount: 3  ReplicationFactor: 3
    Partition: 0  Leader: 1  Replicas: 1,2,3  Isr: 1,2,3
    Partition: 1  Leader: 2  Replicas: 2,3,1  Isr: 2,3,1
    Partition: 2  Leader: 3  Replicas: 3,1,2  Isr: 3,1,2
```

### 토픽 삭제
```bash
kafka-topics --bootstrap-server localhost:9092 \
    --delete \
    --topic <토픽명>
```

---

## Consumer (메시지 확인)

### 전체 메시지 확인 (처음부터)
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic <토픽명> \
    --from-beginning
```

### 특정 파티션만 확인
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic <토픽명> \
    --partition <파티션번호> \
    --from-beginning

# 예시: 파티션 0 확인
kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic my-topic \
    --partition 0 \
    --from-beginning
```

### 키와 함께 메시지 확인
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic <토픽명> \
    --from-beginning \
    --property print.key=true \
    --property key.separator=":"
```

---

## Producer (메시지 전송)

### 간단한 메시지 전송
```bash
kafka-console-producer --bootstrap-server localhost:9092 \
    --topic <토픽명>
# 이후 메시지 입력, Ctrl+C로 종료
```

### 키와 함께 메시지 전송
```bash
kafka-console-producer --bootstrap-server localhost:9092 \
    --topic <토픽명> \
    --property parse.key=true \
    --property key.separator=":"
# 입력 형식: key:value
```

---

## Consumer Group 관리

### Consumer Group 목록
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

### Consumer Group 상세 (오프셋, lag 확인)
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
    --describe \
    --group <그룹ID>
```

---

## 컨테이너 외부에서 실행

컨테이너 밖에서 실행하려면 앞에 `docker exec kafka-1` 붙이기:

```bash
# 토픽 목록
docker exec kafka-1 kafka-topics --bootstrap-server localhost:9092 --list

# 토픽 생성
docker exec kafka-1 kafka-topics --bootstrap-server localhost:9092 \
    --create --topic my-topic --partitions 3 --replication-factor 3

# 토픽 상세
docker exec kafka-1 kafka-topics --bootstrap-server localhost:9092 \
    --describe --topic my-topic
```

---

## 자주 하는 실수

| 실수 | 올바른 것 |
|------|----------|
| `kakfa-topics` | `kafka-topics` |
| `--partition 3` | `--partitions 3` (s 필요) |
| `docker exec kafka` | `docker exec kafka-1` (멀티 브로커) |
| `--replication-factor 3` (단일 브로커) | 브로커 수보다 클 수 없음 |

---

## 클러스터 상태 확인

### 브로커 목록 확인
```bash
kafka-broker-api-versions --bootstrap-server localhost:9092 | head -1
```

### 클러스터 ID 확인
```bash
kafka-metadata --snapshot /var/lib/kafka/data/__cluster_metadata-0/00000000000000000000.log --cluster-id
```
