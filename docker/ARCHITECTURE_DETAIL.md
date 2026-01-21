# Kafka Docker Compose 상세 구조도 (멀티 브로커)

> 단일 브로커 구조는 [ARCHITECTURE.md](./ARCHITECTURE.md) 참고

---

## 전체 구조 (3대 브로커)

```mermaid
flowchart TB
    subgraph MAC["내 Mac (호스트)"]
        subgraph DEV["개발 환경"]
            INTELLIJ["IntelliJ<br/>Spring Boot"]
            TERMINAL["터미널<br/>kafka-cli"]
        end
    end

    subgraph DOCKER["Docker 네트워크"]
        subgraph KAFKA1["Kafka Broker 1"]
            HOST_MAC_1["HOST_MAC 문<br/>localhost:9092<br/>(Mac에서 접근)"]
            DOCKER_INTERNAL_1["DOCKER_INTERNAL 문<br/>kafka-1:29092<br/>(Docker 내부 접근)"]
        end

        subgraph KAFKA2["Kafka Broker 2"]
            HOST_MAC_2["HOST_MAC 문<br/>localhost:9093<br/>(Mac에서 접근)"]
            DOCKER_INTERNAL_2["DOCKER_INTERNAL 문<br/>kafka-2:29093<br/>(Docker 내부 접근)"]
        end

        subgraph KAFKA3["Kafka Broker 3"]
            HOST_MAC_3["HOST_MAC 문<br/>localhost:9094<br/>(Mac에서 접근)"]
            DOCKER_INTERNAL_3["DOCKER_INTERNAL 문<br/>kafka-3:29094<br/>(Docker 내부 접근)"]
        end

        ZOOKEEPER["Zookeeper<br/>:2181<br/>클러스터 관리"]

        OTHER_CONTAINER["(선택) 다른 앱 컨테이너<br/>kafka-1:29092 사용"]
    end

    INTELLIJ -->|"localhost:9092"| HOST_MAC_1
    TERMINAL -->|"localhost:9092"| HOST_MAC_1
    OTHER_CONTAINER -->|"kafka-1:29092"| DOCKER_INTERNAL_1

    DOCKER_INTERNAL_1 <-->|"브로커 간 통신"| DOCKER_INTERNAL_2
    DOCKER_INTERNAL_2 <-->|"브로커 간 통신"| DOCKER_INTERNAL_3
    DOCKER_INTERNAL_1 <-->|"브로커 간 통신"| DOCKER_INTERNAL_3

    KAFKA1 & KAFKA2 & KAFKA3 <-->|"zookeeper:2181"| ZOOKEEPER
```

---

## 포트 매핑

| 브로커 | 호스트 (Mac) | Docker 내부 | BROKER_ID |
|--------|-------------|-------------|-----------|
| kafka-1 | localhost:9092 | kafka-1:29092 | 1 |
| kafka-2 | localhost:9093 | kafka-2:29093 | 2 |
| kafka-3 | localhost:9094 | kafka-3:29094 | 3 |

---

## 리스너 설정 (kafka-1 예시)

```yaml
# 문(리스너) 이름 -> 보안 프로토콜
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: DOCKER_INTERNAL:PLAINTEXT,HOST_MAC:PLAINTEXT

# 각 문의 주소
KAFKA_ADVERTISED_LISTENERS: DOCKER_INTERNAL://kafka-1:29092,HOST_MAC://localhost:9092

# 브로커 간 통신용 문
KAFKA_INTER_BROKER_LISTENER_NAME: DOCKER_INTERNAL
```

| 문 이름 | 주소 | 누가 사용? |
|--------|------|-----------|
| DOCKER_INTERNAL | kafka-X:2909X | Docker 내부 컨테이너, 브로커 간 통신 |
| HOST_MAC | localhost:909X | 내 Mac |
| PLAINTEXT | - | 암호화 없음 (개발용) |

---

## 단일 vs 멀티 브로커 비교

| 구분 | 단일 브로커 | 멀티 브로커 (3대) |
|------|-----------|-----------------|
| 복제 (Replication) | ❌ 불가능 | ✅ 가능 |
| 장애 대응 | ❌ 불가능 | ✅ 자동 Leader 선출 |
| acks=all 효과 | acks=1과 동일 | ISR 전체 복제 보장 |
| 용도 | 개발/학습 | 개발/운영 |

---

## 복제(Replication) 구조

```mermaid
flowchart LR
    subgraph PARTITION0["Partition 0"]
        P0_L["kafka-1<br/>(Leader)"]
        P0_F1["kafka-2<br/>(Follower)"]
        P0_F2["kafka-3<br/>(Follower)"]

        P0_L -->|"복제"| P0_F1
        P0_L -->|"복제"| P0_F2
    end
```

### ISR (In-Sync Replicas)

```mermaid
flowchart TB
    subgraph ISR["ISR = In-Sync Replicas"]
        direction LR
        L["Leader<br/>kafka-1"]
        F1["Follower<br/>kafka-2"]
        F2["Follower<br/>kafka-3"]
    end

    NOTE["모든 복제본이 동기화됨<br/>ISR = [1, 2, 3]"]
```

| acks 설정 | 동작 | 응답 시점 |
|-----------|------|----------|
| `0` | 확인 안 함 | 즉시 |
| `1` | Leader만 저장 | Leader 저장 후 |
| `all` (`-1`) | **ISR 전체** 저장 | 모든 ISR 복제 완료 후 |

---

## 메시지 흐름 (acks=all)

```mermaid
sequenceDiagram
    participant P as Producer
    participant L as Leader (kafka-1)
    participant F1 as Follower (kafka-2)
    participant F2 as Follower (kafka-3)

    P->>L: 1. 메시지 전송
    L->>L: 2. 로컬 저장
    L->>F1: 3. 복제 요청
    L->>F2: 3. 복제 요청
    F1-->>L: 4. 복제 완료
    F2-->>L: 4. 복제 완료
    L-->>P: 5. acks 응답 (성공)
```

---

## 장애 시나리오

### kafka-1이 다운되면?

```mermaid
flowchart LR
    subgraph BEFORE["장애 전"]
        B_L["kafka-1 (Leader)"]
        B_F1["kafka-2"]
        B_F2["kafka-3"]
    end

    subgraph AFTER["장애 후"]
        A_DOWN["kafka-1 ❌"]
        A_L["kafka-2 (New Leader)"]
        A_F["kafka-3"]
    end

    BEFORE --> |"kafka-1 다운"| AFTER
```

- Zookeeper가 감지 → 새 Leader 선출
- ISR 중 하나가 Leader로 승격
- 클라이언트는 자동으로 새 Leader에 연결

---

## 연결 방법

### Mac에서 Spring Boot 앱 실행 시

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

> **Bootstrap Server**: 하나만 지정해도 클러스터 전체 메타데이터를 받아옴

### Docker 내부 다른 컨테이너에서 연결 시

```yaml
environment:
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-1:29092,kafka-2:29093,kafka-3:29094
```
