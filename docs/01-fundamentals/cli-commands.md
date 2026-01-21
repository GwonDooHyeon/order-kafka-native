# Kafka CLI 명령어 정리

## 📌 실행 환경 설정

이 프로젝트는 Docker로 Kafka를 실행합니다. 모든 CLI 명령은 컨테이너 내부에서 실행해야 합니다.

```bash
# 방법 1: docker exec로 직접 실행
docker exec -it kafka <명령어>

# 방법 2: 컨테이너 쉘 접속 후 실행
docker exec -it kafka bash
```

---

## 🗂️ Topic 관리

### Topic 생성
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
    --create \
    --topic <토픽명> \
    --partitions <파티션수> \
    --replication-factor <복제계수>

# 예시: partition 3개인 my-topic 생성
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
    --create --topic my-topic --partitions 3 --replication-factor 1
```

### Topic 목록 조회
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Topic 상세 정보
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
    --describe --topic <토픽명>

# 출력 예시:
# Topic: my-topic  TopicId: xxx  PartitionCount: 3  ReplicationFactor: 1
#   Partition: 0  Leader: 1  Replicas: 1  Isr: 1
#   Partition: 1  Leader: 1  Replicas: 1  Isr: 1
#   Partition: 2  Leader: 1  Replicas: 1  Isr: 1
```

### Topic 삭제
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
    --delete --topic <토픽명>
```

### Topic 설정 변경
```bash
# Partition 수 늘리기 (줄이기는 불가!)
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
    --alter --topic <토픽명> --partitions <새파티션수>
```

---

## 📤 Console Producer

### 기본 메시지 전송
```bash
docker exec -it kafka kafka-console-producer \
    --bootstrap-server localhost:9092 \
    --topic <토픽명>

# 실행 후 메시지 입력 (Ctrl+C로 종료)
> hello
> world
```

### Key와 함께 전송
```bash
docker exec -it kafka kafka-console-producer \
    --bootstrap-server localhost:9092 \
    --topic <토픽명> \
    --property "parse.key=true" \
    --property "key.separator=:"

# key:value 형식으로 입력
> user1:order created
> user1:order paid
> user2:order created
```

### 파일에서 메시지 읽어서 전송
```bash
# 파일을 먼저 컨테이너에 복사
docker cp messages.txt kafka:/tmp/messages.txt

# 파일 내용 전송
docker exec -it kafka bash -c "cat /tmp/messages.txt | kafka-console-producer \
    --bootstrap-server localhost:9092 --topic <토픽명>"
```

---

## 📥 Console Consumer

### 새 메시지만 수신
```bash
docker exec -it kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic <토픽명>
```

### 처음부터 모든 메시지 수신
```bash
docker exec -it kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic <토픽명> \
    --from-beginning
```

### Key, Partition, Offset 정보 포함
```bash
docker exec -it kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic <토픽명> \
    --from-beginning \
    --property print.key=true \
    --property print.partition=true \
    --property print.offset=true

# 출력 예시:
# Partition:0  Offset:0  Key:user1  Value:hello
```

### Consumer Group 지정
```bash
docker exec -it kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic <토픽명> \
    --group <그룹ID>
```

### 특정 Partition만 읽기
```bash
docker exec -it kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic <토픽명> \
    --partition 0 \
    --from-beginning
```

---

## 👥 Consumer Group 관리

### Consumer Group 목록 조회
```bash
docker exec -it kafka kafka-consumer-groups \
    --bootstrap-server localhost:9092 --list
```

### Consumer Group 상세 정보 (Lag 확인)
```bash
docker exec -it kafka kafka-consumer-groups \
    --bootstrap-server localhost:9092 \
    --describe --group <그룹ID>

# 출력 예시:
# GROUP     TOPIC      PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# my-group  my-topic   0          100             150             50
# my-group  my-topic   1          80              80              0
```

### Consumer Group Offset 리셋
```bash
# 처음으로 리셋
docker exec -it kafka kafka-consumer-groups \
    --bootstrap-server localhost:9092 \
    --group <그룹ID> \
    --topic <토픽명> \
    --reset-offsets --to-earliest \
    --execute

# 특정 오프셋으로 리셋
docker exec -it kafka kafka-consumer-groups \
    --bootstrap-server localhost:9092 \
    --group <그룹ID> \
    --topic <토픽명> \
    --reset-offsets --to-offset <오프셋> \
    --execute
```

### Consumer Group 삭제
```bash
docker exec -it kafka kafka-consumer-groups \
    --bootstrap-server localhost:9092 \
    --delete --group <그룹ID>
```

---

## 📊 유용한 조합 명령어

### Topic의 메시지 수 확인
```bash
docker exec -it kafka kafka-run-class kafka.tools.GetOffsetShell \
    --broker-list localhost:9092 \
    --topic <토픽명>
```

### 특정 Key의 메시지만 필터링 (Consumer 실행 후)
```bash
# Consumer 출력에서 grep으로 필터링
docker exec -it kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic <토픽명> \
    --from-beginning \
    --property print.key=true | grep "user1"
```

---

## 🧪 직접 풀어볼 문제

### 문제 1: Topic CRUD 실습
**목표:** Topic 생명주기 관리 익히기

1. `cli-test` Topic 생성 (Partition 2개)
2. Topic 정보 확인
3. Partition을 3개로 늘리기
4. 다시 정보 확인
5. Topic 삭제

```bash
# TODO: 명령어 작성
```

---

### 문제 2: Key 기반 메시지 분배 확인
**목표:** 같은 Key의 메시지가 같은 Partition에 저장되는지 확인

1. `key-test` Topic 생성 (Partition 3개)
2. Key와 함께 메시지 전송:
   - `order-1:created`
   - `order-1:paid`
   - `order-1:shipped`
   - `order-2:created`
   - `order-2:cancelled`
3. Partition 정보 포함하여 Consumer로 확인

**예상 결과:**
- `order-1` 메시지들은 모두 같은 Partition에
- `order-2` 메시지들은 모두 같은 Partition에 (order-1과 다를 수 있음)

---

### 문제 3: Consumer Group Lag 모니터링
**목표:** Lag의 의미 이해하기

1. `lag-test` Topic 생성
2. 메시지 100개 전송 (for loop 사용)
3. Consumer Group `lag-group`으로 30개만 읽고 종료
4. Consumer Group 상세 정보에서 Lag 확인

**예상 결과:**
- LAG = 70 (100 - 30)

---

## 🔍 명령어 요약 표

| 용도 | 명령어 |
|-----|-------|
| Topic 생성 | `kafka-topics --create --topic <name> --partitions <n>` |
| Topic 목록 | `kafka-topics --list` |
| Topic 정보 | `kafka-topics --describe --topic <name>` |
| 메시지 전송 | `kafka-console-producer --topic <name>` |
| 메시지 수신 | `kafka-console-consumer --topic <name>` |
| Group 목록 | `kafka-consumer-groups --list` |
| Group 정보 | `kafka-consumer-groups --describe --group <id>` |

---

## 📝 학습 노트

> 여기에 자주 사용하는 명령어 조합이나 팁을 기록하세요.

```
-
-
-
```
