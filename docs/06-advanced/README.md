# 고급 주제 (Advanced Topics)

> **프로덕션 환경에서 필요한 고급 기능들을 학습합니다.**
>
> 이 섹션은 선택사항이며, 특정 요구사항이 있을 때 참고하면 됩니다.

---

## 📚 고급 주제 로드맵

### Group 1: 데이터 형식 및 스키마

| # | 주제 | 난이도 | 소요 시간 | 상태 |
|---|------|--------|---------|------|
| 1 | JSON 직렬화 심화 | ⭐⭐ | 1시간 | ⏳ 예정 |
| 2 | Avro & Schema Registry | ⭐⭐⭐ | 2시간 | ⏳ 예정 |
| 3 | Protobuf 지원 | ⭐⭐⭐ | 2시간 | ⏳ 예정 |
| 4 | Schema Evolution 전략 | ⭐⭐⭐ | 1.5시간 | ⏳ 예정 |

### Group 2: 저장소 및 로그 관리

| # | 주제 | 난이도 | 소요 시간 | 상태 |
|---|------|--------|---------|------|
| 5 | Log Compaction | ⭐⭐ | 1시간 | ⏳ 예정 |
| 6 | Tiered Storage | ⭐⭐⭐ | 2시간 | ⏳ 예정 |
| 7 | Log Retention 전략 | ⭐ | 1시간 | ⏳ 예정 |
| 8 | 저장소 용량 계획 | ⭐⭐ | 1.5시간 | ⏳ 예정 |

### Group 3: 보안

| # | 주제 | 난이도 | 소요 시간 | 상태 |
|---|------|--------|---------|------|
| 9 | SSL/TLS 암호화 | ⭐⭐ | 1시간 | ⏳ 예정 |
| 10 | SASL 인증 | ⭐⭐⭐ | 1.5시간 | ⏳ 예정 |
| 11 | ACL (Access Control List) | ⭐⭐ | 1시간 | ⏳ 예정 |
| 12 | Quotas & Rate Limiting | ⭐⭐ | 1시간 | ⏳ 예정 |

### Group 4: 모니터링 및 운영

| # | 주제 | 난이도 | 소요 시간 | 상태 |
|---|------|--------|---------|------|
| 13 | JMX Metrics | ⭐⭐ | 1.5시간 | ⏳ 예정 |
| 14 | Prometheus + Grafana | ⭐⭐⭐ | 2시간 | ⏳ 예정 |
| 15 | Kafka Manager / AKHQ | ⭐⭐ | 1시간 | ⏳ 예정 |
| 16 | 로그 분석 및 트러블슈팅 | ⭐⭐⭐ | 2시간 | ⏳ 예정 |

### Group 5: 성능 최적화

| # | 주제 | 난이도 | 소요 시간 | 상태 |
|---|------|--------|---------|------|
| 17 | Producer 성능 튜닝 심화 | ⭐⭐⭐ | 2시간 | ⏳ 예정 |
| 18 | Consumer 성능 튜닝 심화 | ⭐⭐⭐ | 2시간 | ⏳ 예정 |
| 19 | Broker 성능 튜닝 | ⭐⭐⭐⭐ | 2.5시간 | ⏳ 예정 |
| 20 | 처리량 vs 지연시간 최적화 | ⭐⭐⭐ | 1.5시간 | ⏳ 예정 |

### Group 6: 통합 및 확장

| # | 주제 | 난이도 | 소요 시간 | 상태 |
|---|------|--------|---------|------|
| 21 | Kafka Connect | ⭐⭐⭐ | 2시간 | ⏳ 예정 |
| 22 | Kafka Streams | ⭐⭐⭐⭐ | 3시간 | ⏳ 예정 |
| 23 | ksqlDB 소개 | ⭐⭐⭐ | 2시간 | ⏳ 예정 |
| 24 | 마이크로서비스 아키텍처 | ⭐⭐⭐⭐ | 3시간 | ⏳ 예정 |

---

## 🎯 학습 가이드

### 권장 학습 순서

**최소 학습 경로** (4시간):
1. Group 1-1: JSON 직렬화 심화
2. Group 3-9: SSL/TLS 암초화
3. Group 4-13: JMX Metrics
4. Group 5-17: Producer 성능 튜닝

**표준 학습 경로** (12시간):
1. Group 1 전체 (Schema, Avro, Evolution)
2. Group 2 전체 (Log 관리, 저장소)
3. Group 3 전체 (보안)
4. Group 4-13-14-15 (모니터링)

**전체 학습 경로** (25시간):
- 모든 그룹 학습 순서대로

### 선택 기준

| 상황 | 학습 순서 |
|------|---------|
| 빠른 시작 원할 때 | 최소 경로 |
| 안정성 중시 | Group 1 + Group 2 + Group 3 |
| 성능 중시 | Group 4 + Group 5 |
| 운영 중시 | Group 2 + Group 3 + Group 4 |
| 프로덕션 준비 | 전체 학습 경로 |

---

## 📋 각 주제별 학습 자료 구조

각 주제는 다음과 같이 구성됩니다:

### 1. 개념 설명
```markdown
## 개념
- 정의 및 필요성
- 사용 사례
- 장단점
```

### 2. 실습 가이드
```markdown
## 실습
- 단계별 구현
- 코드 예제
- 검증 방법
```

### 3. 성능 비교
```markdown
## 성능 비교
- 설정별 성능 측정
- 벤치마크 결과
- 최적값 도출
```

### 4. 트러블슈팅
```markdown
## 문제 해결
- 흔한 에러
- 해결 방법
- 디버깅 팁
```

---

## 🚀 즉시 활용 가능한 가이드

### 🔒 보안 설정 (Group 3)
- SSL/TLS 설정으로 통신 암호화
- SASL로 클라이언트 인증
- ACL로 접근 권한 제어

### 📊 모니터링 구성 (Group 4)
- Prometheus로 메트릭 수집
- Grafana로 대시보드 시각화
- 알림 규칙 설정

### ⚡ 성능 최적화 (Group 5)
- Producer 처리량 2-3배 개선
- Consumer Lag 감소
- 지연시간 최소화

### 🔌 통합 시스템 (Group 6)
- Kafka Connect로 외부 시스템 연동
- Kafka Streams로 실시간 처리
- 마이크로서비스 간 통신

---

## 💡 고급 주제 선택 팁

### 시나리오별 추천

#### 금융 시스템
→ Group 1 (Schema), Group 2 (저장소), Group 3 (보안)

#### 실시간 데이터 파이프라인
→ Group 1 (Schema), Group 4 (모니터링), Group 6 (Kafka Streams)

#### 전자상거래
→ Group 2 (저장소), Group 5 (성능), Group 6 (Kafka Connect)

#### IoT 센서 수집
→ Group 5 (성능), Group 6 (Kafka Connect), Group 4 (모니터링)

#### 데이터 분석
→ Group 1 (Schema), Group 2 (저장소), Group 6 (ksqlDB)

---

## 📚 관련 학습 자료

### 공식 문서
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Kafka Documentation](https://docs.confluent.io/)

### 추천 도서
- "Kafka: The Definitive Guide" (Neha Narkhede, Gwen Shapira)
- "Event Streaming with Kafka" (Confluent)

### 온라인 코스
- Confluent Kafka Courses
- Udemy Kafka 과정

---

## 🎓 다음 단계

### 이전 단계 복습
- Phase 1-5의 모든 내용이 충분히 이해되었는지 확인
- 실전 프로젝트 완성도 검증

### 실무 적용
- 회사의 실제 Kafka 환경 분석
- 성능 문제 식별 및 최적화
- 보안 정책 수립

### 심화 학습
- Kafka KIP (Kafka Improvement Proposals) 읽기
- 오픈소스 Kafka 컨트리뷰션
- 커뮤니티 활동 참여
