# 카프카 완벽 가이드 - 학습 로드맵

> **이 가이드를 읽으면** 이 프로젝트에서 학습할 수 있는 전체 내용과 구조를 파악할 수 있습니다.

---

## 🎯 학습 목표

이 프로젝트의 목표는 **Apache Kafka의 동작 원리를 low-level부터 체계적으로 이해하는 것**입니다.

### 핵심 역량 습득

- ✅ **Kafka 아키텍처** - Topic, Partition, Broker, Consumer Group의 상호작용
- ✅ **Producer 심화** - 성능, 안정성, 순서 보장을 위한 설정 최적화
- ✅ **Consumer 심화** - 정확한 메시지 처리, 재시도 로직, 성능 튜닝
- ✅ **실전 패턴** - 에러 핸들링, DLQ, Exactly-Once 구현
- ✅ **운영 역량** - 클러스터 구성, 모니터링, 장애 대응

---

## 📊 학습 진행 현황

| Phase | 주제 | 완료도 | 난이도 | 예상 시간 |
|-------|------|--------|--------|----------|
| 1 | 기초 개념 | 70% | ⭐ | 1-2시간 |
| 2 | Producer 심화 | 80% | ⭐⭐ | 3-4시간 |
| 3 | Consumer 심화 | 40% | ⭐⭐⭐ | 3-4시간 |
| 4 | 실전 프로젝트 | 10% | ⭐⭐⭐ | 3-4시간 |
| 5 | 클러스터 & 심화 | 30% | ⭐⭐⭐⭐ | 4-6시간 |

---

## 🗂️ 문서 구조 및 역할

### 문서 간 관계도

```
README.md
  ├─ 프로젝트 소개 및 빠른 시작
  └─ 📍 문서 읽는 순서 (YOU ARE HERE)

00-overview/README.md (현재 파일)
  ├─ 전체 학습 로드맵 설명
  ├─ 각 Phase의 목표 및 학습 체크리스트
  └─ 전체 내용 개요

01-fundamentals/
  ├─ topic-partition.md    # Topic과 Partition 개념
  ├─ producer-consumer.md  # Producer/Consumer 기초
  └─ cli-commands.md       # Kafka CLI 명령어 (필수 실습)

LEARNING_PLAN.md
  ├─ Stage별 상세 학습 계획
  ├─ 개념 설명 → 퀴즈 → 구현 → 검증
  └─ 각 Stage 코드 구현 완성

02-producer/ (예정)
  └─ Producer 관련 상세 문서

03-consumer/ (예정)
  └─ Consumer 관련 상세 문서

04-real-world/ (예정)
  └─ 실전 프로젝트 상세 가이드

05-cluster/ (예정)
  └─ 클러스터 운영 및 장애 복구

06-advanced/ (예정)
  └─ Schema Registry, 보안, 모니터링

보조 자료:
  ├─ KAFKA_COMMANDS.md     # CLI 명령어 모음
  ├─ MY_LEARNING_NOTES.md  # 학습 과정 노트
  └─ QUIZ_ANSWERS.md       # 퀴즈 정답
```

---

## 🎯 Phase 1: 기초 개념

**목표:** Kafka의 핵심 개념을 이해하고 CLI로 직접 조작해보기

### 📚 읽어야 할 문서
- `docs/01-fundamentals/topic-partition.md`
- `docs/01-fundamentals/producer-consumer.md`
- `docs/KAFKA_COMMANDS.md`

### 학습 체크리스트

- [ ] Topic과 Partition의 관계 이해
- [ ] Producer/Consumer 기본 동작 흐름 파악
- [ ] docker-compose로 Kafka 실행
- [ ] kafka-console-producer/consumer로 메시지 송수신
- [ ] Consumer Group의 개념과 Rebalancing
- [ ] Offset과 Commit의 의미 이해

### 핵심 질문 (스스로 답해보기)

1. Partition 수를 늘리면 어떤 장점이 있나요?
2. 같은 Key의 메시지는 항상 같은 Partition에 저장되나요? 왜?
3. Consumer Group 내 Consumer 수가 Partition 수보다 많으면?
4. Consumer가 실패했을 때, 다시 시작하면 어디부터 읽나요?
5. Offset을 Commit하지 않으면 어떻게 되나요?

### 완성도: ✅ 70% (기초 개념 완성, 실습 강화 예정)

---

## 🎯 Phase 2: Java Producer 심화

**목표:** Native Kafka Producer를 사용하여 메시지 전송의 모든 측면 이해

### 📚 읽어야 할 문서
- `docs/LEARNING_PLAN.md` (Part 1)
- `docs/02-producer/` (예정)

### 학습 내용 (8 Stages)

| Stage | 주제 | 핵심 개념 | 난이도 |
|-------|------|---------|--------|
| 1 | 파티션 + 키 | key-based routing, partition assignment | ⭐ |
| 2 | acks 설정 | durability vs performance tradeoff | ⭐ |
| 3 | 재시도 | retries, ordering, in-flight requests | ⭐⭐ |
| 4 | 멱등성 | idempotence, PID, sequence number | ⭐⭐ |
| 5 | 배치 | batch.size, linger.ms, throughput | ⭐ |
| 6 | 압축 | compression types, CPU vs network | ⭐ |
| 7 | JSON 직렬화 | custom serializer, schema evolution | ⭐⭐ |
| 8 | 트랜잭션 | Exactly-Once, atomicity | ⭐⭐⭐ |

### 학습 체크리스트

- [ ] 동기 vs 비동기 전송 방식
- [ ] acks 설정에 따른 성능/안정성 트레이드오프
- [ ] 재시도 로직과 메시지 순서 보장
- [ ] Idempotent Producer 이해
- [ ] 배치 설정으로 처리량 최적화
- [ ] 압축으로 네트워크/디스크 절약
- [ ] Custom Serializer 구현
- [ ] 트랜잭션으로 Exactly-Once 보장

### 완성도: ✅ 80% (개념 설명 완성, 코드 구현 진행 중)

---

## 🎯 Phase 3: Java Consumer 심화

**목표:** Consumer의 동작 원리를 이해하고 실무 패턴 구현

### 📚 읽어야 할 문서
- `docs/LEARNING_PLAN.md` (Part 2)
- `docs/03-consumer/` (예정)

### 현재 학습 내용 (3 Stages → 확장 예정 7 Stages)

| Stage | 주제 | 핵심 개념 | 난이도 | 상태 |
|-------|------|---------|--------|------|
| 9 | Consumer 기본 | subscribe, poll, group.id | ⭐ | ✅ |
| 10 | Consumer Group | partition assignment, rebalancing | ⭐⭐ | ✅ |
| 11 | 수동 커밋 | commitSync, offset management | ⭐⭐ | ✅ |
| 12 | Rebalance Listener | onPartitionsAssigned/Revoked | ⭐⭐ | ⏳ |
| 13 | Seek 메서드 | seek, seekToBeginning, timestamp seek | ⭐⭐ | ⏳ |
| 14 | 성능 튜닝 | fetch 설정, poll 최적화 | ⭐⭐ | ⏳ |
| 15 | Lag 모니터링 | consumer lag 측정 및 대응 | ⭐⭐ | ⏳ |

### 학습 체크리스트

- [ ] Consumer 기본 구현 (subscribe, poll)
- [ ] Consumer Group의 파티션 분배 원리
- [ ] 자동 커밋 vs 수동 커밋
- [ ] Rebalancing 관찰 및 이해
- [ ] Consumer Lag 모니터링
- [ ] 성능 튜닝 설정

### 완성도: ⏳ 40% (기본 내용 완성, Stages 12-15 추가 필요)

---

## 🎯 Phase 4: 실전 프로젝트

**목표:** 실무에서 마주칠 수 있는 시나리오 구현

### 📚 읽어야 할 문서
- `docs/04-real-world/` (예정)

### 예정된 프로젝트

#### Project 1: File Monitoring Producer
- 파일 변경 감지 (File Watcher API)
- 변경 내용을 Kafka로 발행
- 배치 처리 및 에러 핸들링

#### Project 2: DB Writer Consumer
- Kafka 메시지 구독
- 데이터베이스에 저장
- 트랜잭션 및 재시도 로직

#### Project 3: Order Processing System
- Producer: 주문 이벤트 발행
- Consumer: 주문 검증 및 처리
- 에러 처리 및 DLQ

### 완성도: 🔴 10% (구조만 정의, 상세 가이드 예정)

---

## 🎯 Phase 5: 클러스터 & 심화 주제

**목표:** 프로덕션 환경에서의 Kafka 운영 이해

### 📚 읽어야 할 문서
- `docs/05-cluster/` (예정)
- `docs/06-advanced/` (예정)

### 학습 내용

#### 5-1. 클러스터 구성
- [ ] 3-Broker 클러스터 docker-compose 구성
- [ ] Replication Factor와 ISR 이해
- [ ] Leader Election 관찰

#### 5-2. 장애 시나리오
- [ ] Broker 다운 시뮬레이션
- [ ] Leader 변경 관찰
- [ ] 데이터 복구 프로세스

#### 5-3. 고급 주제 (선택)
- [ ] Schema Registry & Avro
- [ ] SSL/TLS 보안 설정
- [ ] SASL 인증
- [ ] JMX 모니터링
- [ ] Prometheus + Grafana

#### 5-4. 에러 처리 & 패턴 (신규)
- [ ] Producer 에러 분류
- [ ] Consumer 에러 처리 및 DLQ
- [ ] Circuit Breaker 패턴
- [ ] Exactly-Once 심화

### 완성도: 🔴 30% (개념만 정의, 실습 자료 예정)

---

## 📋 학습 진행 체크리스트

### Phase 1: 기초 개념
- [x] Topic/Partition 개념 문서
- [x] Producer/Consumer 기본 문서
- [x] CLI 명령어 가이드
- [ ] 실습 문제 추가

### Phase 2: Producer 심화
- [x] Stage 1-8 개념 설명
- [x] 각 Stage 퀴즈
- [ ] 각 Stage 코드 구현 예제
- [ ] 검증 방법 상세화

### Phase 3: Consumer 심화
- [x] Stage 9-11 개념 설명
- [ ] Stage 12-15 추가 (Rebalance, Seek, 튜닝, Lag)
- [ ] 각 Stage 코드 구현 예제
- [ ] 실습 문제 추가

### Phase 4: 실전 프로젝트
- [ ] File Monitoring Producer 상세 가이드
- [ ] DB Writer Consumer 상세 가이드
- [ ] Order Processing 시스템 구현
- [ ] 테스트 시나리오

### Phase 5: 클러스터 & 심화
- [ ] docker-compose 멀티 브로커 구성
- [ ] 클러스터 실습 시나리오
- [ ] 에러 처리 패턴 (3개 추가 Stage)
- [ ] 고급 주제 (Schema Registry, 보안, 모니터링)

---

## 🚀 빠른 시작

```bash
# 1. Kafka 시작
docker-compose up -d

# 2. 메시지 전송 (Producer)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "cust-001", "items": 5}'

# 3. 메시지 확인 (Consumer)
docker exec kafka kafka-console-consumer \
  --topic order-created \
  --from-beginning \
  --bootstrap-server localhost:9092
```

---

## 💡 학습 팁

### 1. 순서대로 학습하기
- Foundation 없이 심화 주제를 하면 이해가 어렵습니다
- Phase 1 → 2 → 3 → 4 순으로 진행하세요

### 2. 각 Stage마다 손으로 직접 타이핑하기
- 복사/붙여넣기보다 직접 타이핑하면 이해도가 높습니다
- CLI 명령어도 직접 입력해보세요

### 3. kafka-console-consumer로 결과 확인하기
- 매 Stage마다 메시지를 직접 확인하세요
- 예상과 실제 결과의 차이를 관찰하세요

### 4. 퀴즈에 먼저 답해보기
- 코드를 보기 전에 먼저 예상 퀴즈에 답해보세요
- 틀려도 상관없습니다. 배우는 과정입니다

### 5. 문서 링크 따라가기
- 각 문서 간 링크를 따라가며 깊이 있게 학습하세요

---

## 🧪 실습 문제

### Producer 문제
- [P-01] 동일 Key 100개 메시지 → 같은 Partition 확인
- [P-02] acks=all vs acks=1 처리량 비교
- [P-03] VIP 고객 전용 Partition 라우팅 (Custom Partitioner)

### Consumer 문제
- [C-01] 2 Consumer + 4 Partition 분배 확인
- [C-02] Consumer 장애 시 Rebalancing 관찰
- [C-03] 수동 커밋으로 정확히 한 번 처리 (Exactly-Once)

### 통합 문제
- [I-01] Producer vs Consumer 처리량 병목 분석
- [I-02] 메시지 순서 보장 시나리오 구현
- [I-03] 에러 발생 시 DLQ 처리

---

## 📞 도움말

- **개념이 어려울 때**: 해당 Phase의 기초 문서부터 다시 읽기
- **코드가 안 돌아갈 때**: `docs/KAFKA_COMMANDS.md`에서 디버깅 명령어 찾기
- **더 심화 학습**: Phase 5의 고급 주제 참고
- **피드백**: 학습 과정을 `MY_LEARNING_NOTES.md`에 기록하기
