# 실전 프로젝트

> **실제 운영 환경에서 마주칠 수 있는 시나리오를 기반으로 구현합니다.**
>
> 이전의 Stage별 학습에서는 각 개념을 분리해서 학습했다면,
> 여기서는 여러 개념이 함께 작동하는 실전 상황을 경험하게 됩니다.

---

## 📚 프로젝트 구성

| # | 프로젝트 | 난이도 | 소요 시간 | 핵심 학습 |
|---|---------|--------|---------|---------|
| 1 | File Monitoring Producer | ⭐⭐ | 1-2시간 | WatchService, 배치 처리, 에러 핸들링 |
| 2 | DB Writer Consumer | ⭐⭐⭐ | 2-3시간 | JDBC 트랜잭션, Exactly-Once, DLQ |
| 3 | Order Processing System | ⭐⭐⭐⭐ | 3-4시간 | Producer-Consumer 조율, 이벤트 기반 아키텍처 |

---

## 🎯 Project 1: File Monitoring Producer

### 요구사항 명세

```
시스템: 로그 파일 변경 감지 시스템
- 지정된 디렉토리의 파일 변경 모니터링
- 변경된 파일 내용을 Kafka로 발행
- 네트워크 장애 시 재시도
- 메시지 손실 방지 (at-least-once)
```

### 아키텍처

```
┌─────────────────────┐
│  File System        │
│  (변경 감지)         │
└──────────┬──────────┘
           │
           │ WatchService 감지
           ▼
┌─────────────────────┐
│  FileMonitoringTask │
│  (파일 읽기)        │
└──────────┬──────────┘
           │
           │ FileChangeEvent
           ▼
┌─────────────────────┐
│  EventBuffer        │
│  (배치 큐)          │
└──────────┬──────────┘
           │
           │ 주기적 flush (또는 크기 도달)
           ▼
┌─────────────────────┐
│  FileProducer       │
│  (Kafka 전송)       │
└──────────┬──────────┘
           │
           │ + 재시도 로직
           ▼
┌─────────────────────┐
│  Kafka Topic        │
│  (file-changes)     │
└─────────────────────┘
```

### 구현 체크리스트

- [ ] 1. FileChangeEvent DTO 정의
  ```java
  // src/main/java/org/example/producer/model/FileChangeEvent.java
  record FileChangeEvent(
      String filename,
      String operation,      // CREATED, MODIFIED, DELETED
      byte[] content,        // 전체 파일 내용
      long timestamp,
      long fileSize
  ) {}
  ```

- [ ] 2. FileWatcher 구현
  ```java
  // 기능: WatchService로 파일 변경 감지
  // 메서드: startMonitoring(), stopMonitoring()
  // 이벤트 리스너 등록 기능
  ```

- [ ] 3. EventBuffer 구현 (배치 처리)
  ```java
  // 기능: 메모리 큐에 이벤트 저장
  // 크기 또는 시간으로 배치 flush
  // 메서드: add(event), flush()
  ```

- [ ] 4. FileProducer 구현
  ```java
  // 기능: Kafka Producer로 메시지 전송
  // 설정: acks=all, retries=3, enable.idempotence=true
  // 콜백에서 재시도 로직
  ```

- [ ] 5. 통합 테스트
  ```bash
  # 테스트 파일 변경 시뮬레이션
  echo "content" > /watch-dir/test.txt

  # Kafka 메시지 확인
  kafka-console-consumer --topic file-changes ...
  ```

### 핵심 코드 스니펫

```java
// FileMonitor.java - WatchService 사용
try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
    watchedDir.register(watchService,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE);

    WatchKey key;
    while ((key = watchService.poll(1, TimeUnit.SECONDS)) != null) {
        for (WatchEvent<?> event : key.pollEvents()) {
            Path changedFile = (Path) event.context();
            eventBuffer.add(new FileChangeEvent(
                changedFile.toString(),
                event.kind().name(),
                Files.readAllBytes(changedFile),
                System.currentTimeMillis(),
                Files.size(changedFile)
            ));
        }
        key.reset();
    }
}
```

### 예상 문제 및 해결책

| 문제 | 원인 | 해결책 |
|------|------|--------|
| 파일이 매우 자주 변경됨 | 이벤트 폭주 | EventBuffer 크기/시간 조정, 배치 크기 증대 |
| 대용량 파일 처리 | 메모리 부족 | 파일 분할 전송, 스트리밍 방식 사용 |
| 네트워크 장애 시 메시지 유실 | 재시도 없음 | 재시도 로직 + 실패 큐 구현 |
| 중복 메시지 전송 | Idempotence 미설정 | `enable.idempotence=true` 설정 |

### 테스트 시나리오

1. **정상 시나리오**
   - 파일 변경 → Kafka 전송 → Consumer 수신 확인

2. **네트워크 장애**
   - Broker 중단 → 재시도 동작 확인 → Broker 재시작 → 메시지 전송 재개

3. **대용량 파일**
   - 100MB 파일 변경 → 메모리 사용량 모니터링 → 전송 시간 측정

4. **고빈도 변경**
   - 초당 100회 파일 변경 → 배치 효율성 측정 → 처리량 확인

---

## 🎯 Project 2: DB Writer Consumer

### 요구사항 명세

```
시스템: 주문 이벤트 → DB 저장 시스템
- Kafka에서 주문 메시지 수신
- 데이터베이스에 저장
- 중복 처리 방지 (Exactly-Once)
- 처리 실패 시 DLQ로 이동
- Consumer Lag 모니터링
```

### 아키텍처

```
┌──────────────────────┐
│  Kafka Topic         │
│  (order-events)      │
└──────────┬───────────┘
           │
           │ poll()
           ▼
┌──────────────────────┐
│  OrderConsumer       │
│  (메시지 수신)       │
└──────────┬───────────┘
           │
           │ 유효성 검증
           ▼
┌──────────────────────┐
│  OrderValidator      │
│  (검증 성공/실패)    │
└──┬────────────────┬──┘
   │ Success         │ Failure
   ▼                 ▼
┌──────────────┐  ┌──────────────┐
│ OrderDAO     │  │ DLQProducer  │
│ (DB 저장)    │  │ (DLQ 전송)   │
└──────┬───────┘  └──────────────┘
       │
       │ Commit Offset
       ▼
┌──────────────────────┐
│  __consumer_offsets  │
│  (오프셋 저장)       │
└──────────────────────┘
```

### 구현 체크리스트

- [ ] 1. OrderEvent DTO 정의
  ```java
  // src/main/java/org/example/common/dto/OrderEvent.java
  record OrderEvent(
      String orderId,
      String customerId,
      List<OrderItem> items,
      BigDecimal totalAmount,
      long timestamp
  ) {}
  ```

- [ ] 2. OrderValidator 구현
  ```java
  // 기능: 주문 유효성 검증
  // 검증 항목: orderId 존재, customerId 존재, amount > 0
  // 결과: ValidationResult(success, errorMessage)
  ```

- [ ] 3. OrderDAO 구현
  ```java
  // 기능: 데이터베이스 CRUD
  // 설정: 트랜잭션 처리, 중복 키 체크
  // 메서드: save(order), exists(orderId)
  ```

- [ ] 4. OrderConsumer 구현
  ```java
  // 기능: Kafka Consumer + 처리 로직
  // 설정: auto.commit=false (수동 커밋)
  // 처리: poll → validate → save → commit
  ```

- [ ] 5. DLQ 처리
  ```java
  // 기능: 실패 메시지를 별도 토픽으로 전송
  // 토픽명: order-events-dlq
  // 저장 정보: 원본 메시지 + 실패 사유
  ```

- [ ] 6. Lag 모니터링
  ```bash
  # Consumer Lag 조회
  kafka-consumer-groups --describe --group order-consumer-group
  ```

### 핵심 코드 스니펫

```java
// OrderConsumer.java - Exactly-Once 처리
consumer.subscribe(Collections.singletonList("order-events"));

while (true) {
    ConsumerRecords<String, OrderEvent> records = consumer.poll(Duration.ofMillis(1000));

    for (ConsumerRecord<String, OrderEvent> record : records) {
        try {
            OrderEvent event = record.value();

            // 1. 유효성 검증
            ValidationResult result = validator.validate(event);
            if (!result.success()) {
                dlqProducer.send(event, result.error());
                continue;
            }

            // 2. DB 저장 (트랜잭션)
            if (!orderDAO.exists(event.orderId())) {
                orderDAO.save(event);
            } else {
                // 중복 처리: 이미 저장된 주문은 스킵
                logger.info("Duplicate order: {}", event.orderId());
            }

            // 3. 오프셋 커밋 (성공 후에만)
            consumer.commitSync();

        } catch (Exception e) {
            // 예기치 않은 에러: DLQ로 전송
            dlqProducer.send(record.value(), e.getMessage());
            consumer.commitSync(); // 실패한 메시지도 커밋 (중복 처리 방지)
        }
    }
}
```

### 예상 문제 및 해결책

| 문제 | 원인 | 해결책 |
|------|------|--------|
| 중복 주문 저장 | 자동 커밋 미설정 | `enable.auto.commit=false` + 수동 커밋 |
| Consumer 다운 시 메시지 유실 | 커밋 전 실패 | Exception 발생 시에도 커밋 + DLQ 활용 |
| DB 장애로 저장 실패 | 재시도 없음 | 재시도 로직 + Circuit Breaker |
| Lag 증가 | 처리 느림 | Consumer 인스턴스 추가, fetch 설정 조정 |

### 테스트 시나리오

1. **정상 처리**
   - 주문 메시지 생성 → Consumer 수신 → DB 저장 → Lag 확인

2. **중복 처리**
   - 같은 orderId로 2번 메시지 전송
   - DB에 1개만 저장되는지 확인
   - Consumer 로그에 "Duplicate order" 메시지 확인

3. **검증 실패**
   - 잘못된 주문 데이터 전송
   - DLQ 토픽에 메시지가 저장되는지 확인
   - Main 토픽에서는 스킵되는지 확인

4. **DB 장애 복구**
   - DB 다운 → Consumer 실패 → DB 재시작
   - Consumer가 자동으로 재시도하는지 확인
   - 메시지가 유실되지 않는지 확인

---

## 🎯 Project 3: Order Processing System (통합)

### 요구사항 명세

```
시스템: 주문 생성 → 이벤트 발행 → 주문 처리 → 완료 알림
- REST API로 주문 생성
- Producer에서 order-created 이벤트 발행
- Consumer에서 주문 검증 및 DB 저장
- 처리 완료 이벤트 발행
- 실패 이벤트는 DLQ로 처리
```

### 아키텍처 (이벤트 기반)

```
┌──────────────────┐
│  REST API        │  POST /orders
│  (OrderController)
└────────┬─────────┘
         │
         ▼
┌──────────────────────────┐
│  OrderService            │
│  (비즈니스 로직)         │
└────────┬────────┬────────┘
         │        │
         │        └─→ Producer
         │             (order-created)
         │
    DB 저장
         │
         ▼
┌──────────────────┐
│  order-created   │  Kafka Topic
│  (이벤트)        │
└────────┬─────────┘
         │
         │ Consumer
         ▼
┌──────────────────────────┐
│  OrderEventListener      │
│  (order-created 구독)    │
└────────┬──────────┬──────┘
         │          │
    검증 성공    검증 실패
         │          │
         ▼          ▼
    DB 저장    order-dlq
         │
         ▼
    order-completed
    (이벤트 발행)
```

### 구현 체크리스트

- [ ] 1. REST API (OrderController)
  ```java
  @PostMapping("/orders")
  public ResponseEntity<OrderResponse> createOrder(
      @RequestBody OrderRequest request) {
      OrderEvent event = orderService.createOrder(request);
      return ResponseEntity.ok(new OrderResponse(event.orderId()));
  }
  ```

- [ ] 2. Producer 통합
  ```java
  // OrderService에서 order-created 이벤트 발행
  public OrderEvent createOrder(OrderRequest request) {
      OrderEvent event = new OrderEvent(...);
      kafkaProducer.send(event);  // 비동기 발행
      return event;
  }
  ```

- [ ] 3. Consumer 구현
  ```java
  // OrderEventListener에서 order-created 구독
  // 검증 → 저장 → order-completed 이벤트 발행
  ```

- [ ] 4. 통합 테스트
  ```bash
  # 1. 주문 생성
  curl -X POST http://localhost:8080/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId": "cust-001", "items": [...]}'

  # 2. 토픽 확인
  kafka-console-consumer --topic order-created ...
  kafka-consumer-groups --describe --group order-processor
  ```

### 학습 포인트

- **이벤트 기반 아키텍처**: 느슨한 결합, 확장성
- **비동기 처리**: 응답 속도 개선
- **신뢰성**: 중복 방지, 재시도, DLQ
- **모니터링**: Lag 추적, 에러 감시

---

## 📊 프로젝트별 비교

| 항목 | Project 1 | Project 2 | Project 3 |
|------|-----------|-----------|-----------|
| Producer 복잡도 | 중 | - | 낮음 |
| Consumer 복잡도 | - | 높음 | 중 |
| 신뢰성 요구도 | 중 | 높음 | 높음 |
| 학습 포인트 | 배치, 파일 I/O | 트랜잭션, Lag | 이벤트 기반 설계 |

---

## 🚀 프로젝트 수행 순서

1. **Project 1 완성** (File Monitoring)
   - 파일 I/O와 Kafka Producer 이해
   - 배치 처리 경험

2. **Project 2 완성** (DB Writer)
   - Consumer 심화 이해
   - 트랜잭션과 Exactly-Once 구현

3. **Project 3 완성** (통합 시스템)
   - 전체 시스템을 조율하는 경험
   - 실무 패턴 적용

---

## 💡 추가 학습 자료

- [Project 1 상세 가이드](./file-monitoring-producer.md)
- [Project 2 상세 가이드](./db-consumer.md)
- [Project 3 상세 가이드](./order-processing-system.md)
