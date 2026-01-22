# Kafka 학습 노트

> 학습하면서 새로 알게 된 내용들을 Stage별로 정리합니다.

---

## 📚 학습 Stage 목록

### [Stage 1: 파티션 3개 + 키 기반 라우팅](./stages/stage-1-partitioning.md)

**학습 주제:**
- Sticky Partitioner vs Round Robin
- batch.size와 linger.ms의 역할
- Hash Partitioner를 통한 순서 보장

**핵심 개념:**
- 키가 없을 때: Sticky Partitioner (배치 최적화)
- 키가 있을 때: Hash Partitioner (순서 보장)

---

### [Stage 2: acks 설정과 전송 보장](./stages/stage-2-acks.md)

**학습 주제:**
- Leader Partition 할당 방식 (Round-Robin)
- acks=0/1/all의 차이
- min.insync.replicas와 데이터 안정성

**핵심 개념:**
- Leader vs Follower
- acks=0: offset=-1 (확인 안 함)
- acks=all: 모든 ISR에 복제 확인
- **acks 기본값 변경 (Kafka 3.0+)**:
  - 3.0 이전: acks=1 (Leader만 확인)
  - 3.0 이후: acks=-1 (all, 모든 ISR 확인)
  - 이유: 데이터 안정성 강화를 위한 기본값 변경

---

### Stage 3: 브로커 장애 복구 및 ISR (예정)

**예정 학습 주제:**
- ISR (In-Sync Replicas) 관리
- Leader Election 과정
- 브로커 장애 시 복구 메커니즘

---

### Stage 4: Consumer Group & Rebalancing (예정)

**예정 학습 주제:**
- Consumer Group 개념
- Partition 재할당 (Rebalancing)
- Offset 관리

---

## 📖 학습 가이드

### 파일 구조
```
docs/
├── MY_LEARNING_NOTES.md           ← 이 파일 (인덱스)
└── stages/
    ├── stage-1-partitioning.md    ← Stage 1 상세 내용
    ├── stage-2-acks.md             ← Stage 2 상세 내용
    └── ...                         ← 추후 Stage 추가
```

### 학습 순서
1. Stage 1부터 순서대로 학습
2. 각 Stage는 독립적인 파일로 관리
3. 실습 코드는 `producer/src/.../` 경로에 위치

---

## 💡 Tip

- 각 Stage 파일은 독립적으로 읽을 수 있습니다
- 검색 시: IDE의 전체 검색 기능 활용 (Cmd+Shift+F)
- 새로운 Stage 추가 시: 이 인덱스 파일에 링크 추가
