# Kafka Producer Configuration Reference

## 개요

Kafka Producer의 모든 주요 설정값을 한 곳에서 참고할 수 있는 레퍼런스 문서입니다. 각 설정의 목적, 기본값, 유효한 값 범위, 사용 예시를 체계적으로 정리했습니다.

### Producer 설정이란?

KafkaProducer를 생성할 때 Properties 객체에 설정값을 전달하여 Producer의 동작 방식을 제어합니다:

```java
Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

// 추가 설정
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.RETRIES_CONFIG, 3);

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

---

## 필수 설정 (Required Configurations)

모든 Producer가 반드시 설정해야 하는 3개의 필수 설정입니다.

### 1. bootstrap.servers

| 항목 | 값 |
|-----|-----|
| **설정 키** | `bootstrap.servers` |
| **타입** | String (쉼표로 구분된 호스트:포트 목록) |
| **기본값** | "" (필수) |
| **유효 값** | "localhost:9092", "broker1:9092,broker2:9092,..." |
| **중요도** | ⭐⭐⭐ Critical |

**설명:** Kafka 브로커의 주소와 포트를 지정합니다. 하나 이상의 브로커를 지정하면 Producer가 클러스터의 전체 정보를 자동으로 검색합니다.

**코드 예시:**
```java
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
// 또는 다중 브로커
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "broker1:9092,broker2:9092,broker3:9092");
```

**주의사항:** 최소 1개의 브로커를 지정해야 하며, 모든 브로커를 지정할 필요는 없습니다. Producer가 자동으로 클러스터 정보를 조회합니다.

---

### 2. key.serializer

| 항목 | 값 |
|-----|-----|
| **설정 키** | `key.serializer` |
| **타입** | String (클래스명) |
| **기본값** | "" (필수) |
| **유효 값** | "org.apache.kafka.common.serialization.StringSerializer", "org.apache.kafka.common.serialization.IntegerSerializer", 커스텀 Serializer |
| **중요도** | ⭐⭐⭐ Critical |

**설명:** 메시지 Key를 바이트 배열로 변환할 Serializer 클래스를 지정합니다.

**코드 예시:**
```java
// 문자열 Key 사용
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

// 정수 Key 사용
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());

// 커스텀 Serializer
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, CustomKeySerializer.class.getName());
```

**주의사항:** Serializer와 Deserializer의 타입이 일치해야 합니다. Consumer에서도 동일한 타입을 지정해야 정상적으로 역직렬화됩니다.

---

### 3. value.serializer

| 항목 | 값 |
|-----|-----|
| **설정 키** | `value.serializer` |
| **타입** | String (클래스명) |
| **기본값** | "" (필수) |
| **유효 값** | "org.apache.kafka.common.serialization.StringSerializer", "org.apache.kafka.common.serialization.ByteArraySerializer", 커스텀 Serializer |
| **중요도** | ⭐⭐⭐ Critical |

**설명:** 메시지 Value를 바이트 배열로 변환할 Serializer 클래스를 지정합니다.

**코드 예시:**
```java
// 문자열 Value 사용 (일반적)
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

// JSON 문자열로 직렬화
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

// 바이트 배열 직렬화
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
```

**주의사항:** Value Serializer는 복잡한 객체 (JSON, Avro, Protobuf 등)를 직렬화하는 데 사용될 수 있습니다.

---

## 설정별 상세 설명

### Category 1: 신뢰성 & 내구성 (Reliability & Durability)

메시지 전송 보장 수준을 제어하여 데이터 손실을 방지합니다.

#### acks (Acknowledgment Level)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `acks` |
| **타입** | String |
| **기본값** | "1" |
| **유효 값** | "0", "1", "all" (또는 "-1") |
| **중요도** | ⭐⭐⭐ High |
| **관련 Stage** | [Stage 2: acks 설정과 전송 보장](../stages/stage-2-acks.md) |

**설명:** Producer가 메시지 전송 완료로 간주하기 전에 얼마나 많은 브로커의 확인(ACK)을 기다릴지 결정합니다.

- **"0":** 브로커 확인 없음 (최고 처리량, 최저 신뢰성)
- **"1":** Leader만 확인 (균형잡힌 설정)
- **"all" 또는 "-1":** Leader + 모든 In-Sync Replicas 확인 (최고 신뢰성, 최저 처리량)

**코드 예시:**
```java
// 처리량 우선
props.put(ProducerConfig.ACKS_CONFIG, "0");

// 균형잡힌 설정 (기본값)
props.put(ProducerConfig.ACKS_CONFIG, "1");

// 신뢰성 우선
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.ACKS_CONFIG, "-1"); // 동일
```

**주의사항:**
- `acks=0`: 메시지 손실 가능성이 있음
- `acks=all`: 응답 시간이 길어짐
- `enable.idempotence=true` 설정 시 자동으로 `acks=all`로 변경됨

**권장 사용 사례:**
- 금융 거래, 결제 시스템 → `acks=all`
- 로그, 분석 데이터 → `acks=1`
- 실시간 모니터링, 낮은 우선순위 데이터 → `acks=0`

---

#### enable.idempotence (중복 전송 방지)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `enable.idempotence` |
| **타입** | Boolean |
| **기본값** | true (Kafka 3.0+) / false (Kafka 2.x) |
| **유효 값** | true, false |
| **중요도** | ⭐⭐⭐ High |
| **관련 Stage** | [Stage 4: 멱등성 설정과 정확히 한 번 전송](../stages/stage-4-idempotence.md) |

**설명:** Producer가 동일한 메시지를 여러 번 전송하더라도 Topic에는 한 번만 저장되도록 보장합니다.

**코드 예시:**
```java
// 멱등성 활성화
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

// 멱등성 비활성화 (기본값 - Kafka 2.x 기준)
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
```

**자동 설정:**
- `enable.idempotence=true` 설정 시 다음이 자동으로 설정됨:
  - `acks` → "all"
  - `retries` → Integer.MAX_VALUE
  - `max.in.flight.requests.per.connection` → 1~5 범위 내에서 설정 가능

**주의사항:**
- Kafka 3.0+에서는 기본값이 true로 변경됨
- `acks=0`과 함께 사용할 수 없음 (자동으로 오류 발생)
- Producer가 고유한 ID를 유지해야 함

**권장 사용 사례:**
- 거래 시스템 (중복 방지 필수)
- 정확히 한 번 처리 필요한 경우
- 데이터 무결성이 중요한 경우

---

#### retries (재시도 횟수)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `retries` |
| **타입** | Integer |
| **기본값** | 2147483647 (Integer.MAX_VALUE, ~24.8일) |
| **유효 값** | 0 ~ Integer.MAX_VALUE |
| **중요도** | ⭐⭐ Medium |
| **관련 Stage** | [Stage 3: 재시도 및 오류 처리](../stages/stage-3-retries.md) |

**설명:** 전송 실패 시 자동으로 재시도하는 횟수를 지정합니다. `delivery.timeout.ms` 시간 내에서 최대 이 횟수만큼 재시도합니다.

**코드 예시:**
```java
// 3회 재시도
props.put(ProducerConfig.RETRIES_CONFIG, 3);

// 재시도 안 함
props.put(ProducerConfig.RETRIES_CONFIG, 0);

// 최대 재시도 (멱등성과 함께 사용)
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
```

**주의사항:**
- `enable.idempotence=true` 설정 시 자동으로 Integer.MAX_VALUE로 설정됨
- 실제 재시도 회수는 `delivery.timeout.ms` 제한을 받음
- 각 재시도 사이에 `retry.backoff.ms`만큼 대기

**권장 사용 사례:**
- 일시적 네트워크 오류 대응
- 브로커 재시작 대응
- 명령형 프로그래밍이 필요한 경우

---

#### retry.backoff.ms (재시도 간격)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `retry.backoff.ms` |
| **타입** | Long |
| **기본값** | 100 (밀리초) |
| **유효 값** | 0 ~ Long.MAX_VALUE |
| **중요도** | ⭐⭐ Medium |
| **관련 Stage** | [Stage 3: 재시도 및 오류 처리](../stages/stage-3-retries.md) |

**설명:** 재시도 시 대기하는 시간입니다. 이 시간을 늘리면 Broker의 부하 회복 시간을 줄 수 있습니다.

**코드 예시:**
```java
// 100ms 대기 (기본값)
props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100L);

// 1초 대기 (부하가 있는 Broker의 회복을 기다림)
props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000L);

// 5초 대기
props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 5000L);
```

**주의사항:**
- 이 값을 크게 설정하면 전체 처리량이 감소할 수 있음
- 네트워크 오류와 Broker 오류에 차별화된 대기 시간을 원하면 Custom Retry 전략 사용 추천
- Exponential backoff를 구현하려면 Custom interceptor 사용 필요

**권장 사용 사례:**
- Broker 과부하 시 자동 복구 대기
- 네트워크 혼잡 시간대 자동 대응
- 안정적이지 않은 네트워크 환경

---

#### max.in.flight.requests.per.connection (동시 요청 수)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `max.in.flight.requests.per.connection` |
| **타입** | Integer |
| **기본값** | 5 |
| **유효 값** | 1 ~ 5 (멱등성 활성화 시) / 1 ~ Integer.MAX_VALUE (비활성화 시) |
| **중요도** | ⭐⭐⭐ High |
| **관련 Stage** | [Stage 3: 순서 보장과 멱등성](../stages/stage-3-retries.md) |

**설명:** 응답을 기다리지 않고 브로커에 전송할 수 있는 최대 요청 수입니다. 값이 클수록 처리량이 높지만 메모리 사용량도 증가하고 순서 보장이 어려워집니다.

**코드 예시:**
```java
// 메시지 순서 보장 필요 (멱등성과 함께)
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

// 메시지 순서 보장 필수 (엄격한 순서)
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

// 처리량 우선 (순서 보장 불필요)
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 20);
```

**주의사항:**
- `enable.idempotence=true` 설정 시 자동으로 1~5 범위로 제한됨
- 1보다 크게 설정하면 여러 메시지가 동시에 전송되어 순서 보장이 안 될 수 있음
- Broker 재시도 시 메시지 순서가 뒤바뀔 수 있음

**권장 사용 사례:**
- 순서 보장이 중요한 경우 → 1
- 멱등성과 함께 고처리량 필요 → 5
- 순서 무관 고처리량 → 20 이상

---

### Category 2: 성능 최적화 (Performance Optimization)

메시지 배치 처리, 압축 등을 통해 처리량과 네트워크 효율성을 높입니다.

#### batch.size (배치 크기)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `batch.size` |
| **타입** | Integer |
| **기본값** | 16384 (16 KB) |
| **유효 값** | 0 ~ Integer.MAX_VALUE |
| **중요도** | ⭐⭐ Medium |
| **관련 Stage** | [Stage 5: 배치 처리와 압축](../stages/stage-5-batching.md) |

**설명:** 동일한 Partition으로 보낼 메시지들을 이 크기만큼 모아서 한 번에 전송합니다. 크기가 클수록 처리량이 높지만 지연시간이 증가합니다.

**코드 예시:**
```java
// 기본값 (16 KB)
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

// 처리량 우선 (32 KB)
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);

// 지연시간 우선 (1 KB)
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 1024);

// 배치 비활성화 (메시지 1개씩 전송)
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 1);
```

**관련 설정:**
- `linger.ms`: 배치가 도달하지 않아도 최대 대기 시간 후 전송
- `buffer.memory`: 배치를 보관할 버퍼 메모리 크기

**주의사항:**
- 배치 크기를 초과하는 단일 메시지는 그대로 전송됨
- 버퍼 메모리가 부족하면 `max.block.ms` 시간만큼 대기
- Producer 처리 성능에 큰 영향을 미침

**권장 사용 사례:**
- 고처리량 필요 → 32KB 이상
- 일반적인 경우 → 16KB (기본값)
- 저지연 필요 → 1KB ~ 4KB

---

#### linger.ms (배치 대기 시간)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `linger.ms` |
| **타입** | Long |
| **기본값** | 0 (밀리초) |
| **유효 값** | 0 ~ Long.MAX_VALUE |
| **중요도** | ⭐⭐ Medium |
| **관련 Stage** | [Stage 5: 배치 처리와 압축](../stages/stage-5-batching.md) |

**설명:** 배치가 `batch.size`에 도달하지 못해도 이 시간만큼 기다렸다가 메시지를 전송합니다. 배치 처리를 더 효과적으로 하기 위해 더 많은 메시지를 모으는 시간입니다.

**코드 예시:**
```java
// 배치 기다리지 않음 (기본값, 바로 전송)
props.put(ProducerConfig.LINGER_MS_CONFIG, 0L);

// 10ms 대기 (배치 효과)
props.put(ProducerConfig.LINGER_MS_CONFIG, 10L);

// 100ms 대기 (더 많은 배치, 지연시간 증가)
props.put(ProducerConfig.LINGER_MS_CONFIG, 100L);

// batch.size와 함께 사용 (권장)
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
props.put(ProducerConfig.LINGER_MS_CONFIG, 10L);
```

**관련 설정:**
- `batch.size`: 이 크기에 도달하면 즉시 전송 (linger.ms 대기 시간 무시)

**주의사항:**
- 0이면 배치 대기 시간이 없음 (배치 효과 감소)
- 큰 값은 처리량은 높지만 지연시간이 증가
- CPU 사용량이 많으면 배치가 더 빠르게 차요

**권장 사용 사례:**
- 처리량 최대화 → 10~100ms
- 지연시간 중요 → 0ms (기본값)
- 네트워크 효율성 → 10~50ms

---

#### compression.type (압축 알고리즘)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `compression.type` |
| **타입** | String |
| **기본값** | "none" |
| **유효 값** | "none", "gzip", "snappy", "lz4", "zstd" |
| **중요도** | ⭐⭐ Medium |
| **관련 Stage** | [Stage 6: 압축 및 성능 최적화](../stages/stage-6-compression.md) |

**설명:** 배치 메시지를 압축하여 네트워크 대역폭을 절감하고 디스크 사용량을 줄입니다. 각 알고리즘의 특성:

- **none**: 압축 안 함 (기본값)
- **gzip**: 높은 압축률, 느린 속도 (CPU 집약적)
- **snappy**: 중간 압축률, 빠른 속도 (균형)
- **lz4**: 낮은 압축률, 매우 빠른 속도 (처리량 우선)
- **zstd**: 높은 압축률, 빠른 속도 (최적 선택)

**코드 예시:**
```java
// 압축 안 함
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");

// Gzip 압축 (높은 압축률)
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");

// Snappy 압축 (균형)
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

// LZ4 압축 (빠른 속도)
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

// Zstd 압축 (권장)
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "zstd");
```

**성능 비교:**
| 알고리즘 | 압축률 | 속도 | CPU | 추천 |
|---------|-------|------|-----|------|
| gzip | ⭐⭐⭐⭐⭐ | ⭐ | 높음 | 네트워크 대역폭 제한 |
| snappy | ⭐⭐⭐ | ⭐⭐⭐⭐ | 중간 | 일반적 경우 |
| lz4 | ⭐⭐ | ⭐⭐⭐⭐⭐ | 낮음 | 고처리량 |
| zstd | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 중간 | 최우선 추천 |

**주의사항:**
- 압축은 배치 단위로 수행되므로 배치 크기가 작으면 효과 감소
- 이미 압축된 메시지 (이미지, 비디오)는 압축 효과 없음
- CPU 사용량 증가, 메모리 사용량 증가

**권장 사용 사례:**
- 네트워크 대역폭 제한 → gzip
- 일반적인 경우 → snappy 또는 zstd
- 높은 처리량 필요 → lz4
- JSON/텍스트 데이터 → zstd

---

#### buffer.memory (버퍼 메모리)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `buffer.memory` |
| **타입** | Long |
| **기본값** | 33554432 (32 MB) |
| **유효 값** | 0 ~ Long.MAX_VALUE |
| **중요도** | ⭐ Low |

**설명:** Producer가 배치 처리를 위해 예약할 총 메모리 크기입니다. 이 메모리가 가득 차면 `max.block.ms` 시간만큼 대기한 후 Exception 발생합니다.

**코드 예시:**
```java
// 기본값 (32 MB)
props.put(ProducerConfig.BUFFER_MEMORY, 33554432L);

// 64 MB (고처리량)
props.put(ProducerConfig.BUFFER_MEMORY, 67108864L);

// 16 MB (메모리 제약)
props.put(ProducerConfig.BUFFER_MEMORY, 16777216L);
```

**계산 방법:**
```
필요 메모리 = batch.size × max.in.flight.requests.per.connection × partition_count
예시: 16KB × 5 × 10개 파티션 = 800KB (충분히 설정하면 안 됨)
권장: batch.size × max.in.flight.requests.per.connection × 2 ~ 4
```

**주의사항:**
- 메모리가 가득 차면 send() 메서드가 블로킹됨
- 낮게 설정하면 처리량 감소, 높게 설정하면 메모리 낭비
- 멀티 Producer 실행 시 합산 메모리 고려

**권장 사용 사례:**
- 일반적인 경우 → 32MB (기본값)
- 고처리량 → 64MB 이상
- 메모리 제약 환경 → 16MB

---

### Category 3: 타임아웃 & 제한 (Timeouts & Limits)

타임아웃과 블로킹 시간을 제어하여 응답 시간을 관리합니다.

#### max.block.ms (최대 블로킹 시간)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `max.block.ms` |
| **타입** | Long |
| **기본값** | 60000 (60초) |
| **유효 값** | 0 ~ Long.MAX_VALUE |
| **중요도** | ⭐⭐ Medium |

**설명:** `send()` 메서드가 버퍼 메모리 부족으로 블로킹될 수 있는 최대 시간입니다. 이 시간을 초과하면 `TimeoutException`이 발생합니다.

**코드 예시:**
```java
// 60초 대기 (기본값)
props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000L);

// 5초 대기 (빠른 응답 필요)
props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000L);

// 30초 대기
props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30000L);
```

**주의사항:**
- 버퍼 메모리 부족으로 인한 블로킹만 영향 (네트워크 요청 timeout이 아님)
- 이 값이 `delivery.timeout.ms`보다 커야 함
- 너무 작으면 `TimeoutException` 발생 가능성 증가

**권장 사용 사례:**
- 응답 시간 중요 → 5~10초
- 일반적인 경우 → 60초 (기본값)
- 느린 네트워크 → 120~300초

---

#### request.timeout.ms (요청 응답 대기 시간)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `request.timeout.ms` |
| **타입** | Integer |
| **기본값** | 30000 (30초) |
| **유효 값** | 0 ~ Integer.MAX_VALUE |
| **중요도** | ⭐ Low |

**설명:** Producer가 Broker로부터 응답을 기다리는 최대 시간입니다. 이 시간을 초과하면 재시도를 시작합니다.

**코드 예시:**
```java
// 30초 대기 (기본값)
props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

// 5초 대기 (빠른 재시도)
props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

// 60초 대기 (느린 네트워크)
props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60000);
```

**주의사항:**
- 네트워크 라운드 트립 시간 고려
- `max.block.ms`와 함께 고려해야 함
- WAN 환경에서는 더 큰 값 필요

**권장 사용 사례:**
- LAN 환경 → 30초 (기본값)
- 불안정한 네트워크 → 60초
- 빠른 응답 필요 → 10초

---

#### delivery.timeout.ms (전체 전송 제한 시간)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `delivery.timeout.ms` |
| **타입** | Integer |
| **기본값** | 120000 (120초) |
| **유효 값** | 0 ~ Integer.MAX_VALUE |
| **중요도** | ⭐⭐ Medium |

**설명:** `send()` 메서드가 처음 호출되는 순간부터 브로커가 메시지를 받거나 에러를 반환할 때까지의 최대 시간입니다. 이 시간 내에 모든 재시도가 완료되어야 합니다.

**관계식:**
```
delivery.timeout.ms >= max.block.ms + request.timeout.ms + (retry.backoff.ms × retries)
```

**코드 예시:**
```java
// 120초 (기본값)
props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

// 60초 (빠른 실패)
props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 60000);

// 300초 (매우 느린 네트워크)
props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 300000);
```

**주의사항:**
- 이 값은 실제 재시도 횟수(`retries`)를 제한함
- 멱등성과 함께 설정할 때 중요
- 너무 짧으면 재시도 기회 감소

**권장 사용 사례:**
- 일반적인 경우 → 120초 (기본값)
- 빠른 응답 필요 → 60초
- 느린 네트워크 → 300초 이상

---

### Category 4: 트랜잭션 (Transactions)

Exactly-Once Semantics를 구현하여 정확히 한 번 전송을 보장합니다.

#### transactional.id (트랜잭션 식별자)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `transactional.id` |
| **타입** | String |
| **기본값** | null (트랜잭션 비활성화) |
| **유효 값** | 문자열 (예: "producer-1", "order-producer") |
| **중요도** | ⭐⭐⭐ High (트랜잭션 사용 시) |
| **관련 Stage** | [Stage 8: 트랜잭션과 Exactly-Once](../stages/stage-8-transactions.md) |

**설명:** 이 값을 설정하면 Producer가 트랜잭션 기능을 활성화하고, 같은 ID를 가진 Producer의 이전 미완료 트랜잭션을 자동으로 정리합니다. 중복 전송을 방지하고 정확히 한 번 전송을 보장합니다.

**코드 예시:**
```java
// 트랜잭션 활성화
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-transactional-producer");

// 각 Producer 인스턴스마다 고유한 ID 필요
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "order-producer-" + instanceId);

// 트랜잭션 비활성화
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, null);

KafkaProducer<String, String> producer = new KafkaProducer<>(props);

// 트랜잭션 초기화 (필수)
producer.initTransactions();

// 트랜잭션 시작
producer.beginTransaction();
try {
    producer.send(new ProducerRecord<>("topic1", "message1"));
    producer.send(new ProducerRecord<>("topic2", "message2"));
    producer.commitTransaction();
} catch (Exception e) {
    producer.abortTransaction();
}
```

**자동 설정:**
- `transactional.id` 설정 시 `enable.idempotence`가 자동으로 `true`로 설정됨
- 따라서 `acks=all`, `retries=Integer.MAX_VALUE`도 자동 적용

**주의사항:**
- 반드시 `initTransactions()` 호출 필요
- 같은 ID를 가진 여러 Producer가 동시에 실행되면 안 됨
- Broker 메모리 사용량 증가
- `transactional.id` 값은 Broker에 영구 저장됨

**권장 사용 사례:**
- 여러 Topic에 동시 쓰기 (All-or-Nothing)
- 정확히 한 번 처리 필수
- 금융 거래, 순서 보장 필요

**Fencing 메커니즘:**
```java
// 같은 ID로 새로운 Producer가 시작되면
// Broker가 이전 Producer의 미완료 트랜잭션을 자동으로 abort함
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "producer-1");
KafkaProducer<String, String> producer1 = new KafkaProducer<>(props);
producer1.initTransactions();
producer1.beginTransaction();
producer1.send(new ProducerRecord<>("topic", "message"));
// producer1이 비정상 종료되고 새로운 Producer 시작
KafkaProducer<String, String> producer2 = new KafkaProducer<>(props);
producer2.initTransactions(); // 자동으로 producer1의 트랜잭션을 abort함
```

---

#### transaction.timeout.ms (트랜잭션 타임아웃)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `transaction.timeout.ms` |
| **타입** | Integer |
| **기본값** | 60000 (60초) |
| **유효 값** | 0 ~ Integer.MAX_VALUE |
| **중요도** | ⭐⭐ Medium |
| **관련 Stage** | [Stage 8: 트랜잭션과 Exactly-Once](../stages/stage-8-transactions.md) |

**설명:** 트랜잭션이 시작된 후 이 시간 내에 `commitTransaction()` 또는 `abortTransaction()`을 호출하지 않으면 Broker가 자동으로 트랜잭션을 abort합니다.

**코드 예시:**
```java
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-producer");
props.put(ProducerConfig.TRANSACTION_TIMEOUT_MS_CONFIG, 60000);

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
producer.initTransactions();

producer.beginTransaction();
// 60초 내에 commitTransaction()을 호출해야 함
for (int i = 0; i < 1000; i++) {
    producer.send(new ProducerRecord<>("topic", "message-" + i));
}
producer.commitTransaction(); // 성공

// 타임아웃 예시
producer.beginTransaction();
Thread.sleep(65000); // 65초 대기 → 트랜잭션 자동 abort
try {
    producer.commitTransaction(); // IllegalStateException 발생
} catch (IllegalStateException e) {
    System.out.println("트랜잭션이 이미 종료됨");
}
```

**주의사항:**
- 네트워크 지연이나 처리 시간을 고려하여 설정
- `delivery.timeout.ms`와는 다른 개념
- Broker 메모리에 영향

**권장 사용 사례:**
- 일반적인 경우 → 60초 (기본값)
- 복잡한 트랜잭션 → 300초
- 빠른 응답 필요 → 30초

---

### Category 5: 기타 (Miscellaneous)

디버깅, 로깅, 식별용 설정입니다.

#### client.id (Producer 식별자)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `client.id` |
| **타입** | String |
| **기본값** | "" (자동 생성) |
| **유효 값** | 문자열 (예: "order-producer-1", "analytics") |
| **중요도** | ⭐ Low |

**설명:** Producer를 식별하는 문자열입니다. 로그, 메트릭, 모니터링에 사용되며, Broker의 요청 로그에도 기록됩니다.

**코드 예시:**
```java
// 명시적 설정
props.put(ProducerConfig.CLIENT_ID_CONFIG, "order-producer-1");

// 인스턴스 이름 포함
props.put(ProducerConfig.CLIENT_ID_CONFIG, "producer-" + UUID.randomUUID());

// 빈 문자열 (자동 생성)
props.put(ProducerConfig.CLIENT_ID_CONFIG, "");
```

**자동 생성 형식:**
```
producer-[숫자] (예: producer-1, producer-2)
```

**주의사항:**
- 모니터링 및 디버깅에 중요
- 재시작 후에도 동일한 ID를 사용하는 것이 좋음
- Broker 로그에 남으므로 분석 가능

**권장 사용 사례:**
- 여러 Producer 실행 → 고유한 ID 설정
- 모니터링 필요 → 의미있는 이름 설정
- 개발 환경 → 기본값으로 충분

---

#### partitioner.class (파티셔너 클래스)

| 항목 | 값 |
|-----|-----|
| **설정 키** | `partitioner.class` |
| **타입** | String (클래스명) |
| **기본값** | "org.apache.kafka.clients.producer.internals.DefaultPartitioner" |
| **유효 값** | 클래스명 (Partitioner 인터페이스 구현) |
| **중요도** | ⭐⭐ Medium (커스텀 필요 시) |
| **관련 Stage** | [Stage 1: Kafka 기초와 파티션 이해](../stages/stage-1-partitioning.md) |

**설명:** 메시지를 어느 파티션에 보낼지 결정하는 로직을 구현한 클래스입니다. 기본적으로 라운드로빈 또는 key 기반 해싱을 사용합니다.

**기본 Partitioner 동작:**
- **Key가 null:** 라운드로빈으로 모든 파티션에 분산
- **Key가 있음:** Key의 해시값으로 특정 파티션에 고정

**코드 예시:**
```java
// 기본 파티셔너
props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG,
    "org.apache.kafka.clients.producer.internals.DefaultPartitioner");

// 커스텀 파티셔너
props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG,
    "com.example.kafka.CustomPartitioner");

// 커스텀 파티셔너 구현 예시
public class CustomPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes,
                        Cluster cluster) {
        if (key == null) {
            return 0; // 기본 파티션
        }
        // 커스텀 로직: 홀짝수 key를 다른 파티션으로 분배
        int hashValue = key.hashCode();
        return Math.abs(hashValue % cluster.partitionsForTopic(topic).size());
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
```

**관련 설정:**
- Key를 설정하여 특정 파티션으로 메시지 전송

**주의사항:**
- 같은 Key를 가진 메시지는 항상 같은 파티션으로 전송됨
- 커스텀 파티셔너는 성능에 영향 가능
- 파티션 수가 변경되면 Key 분배가 변할 수 있음

**권장 사용 사례:**
- 기본 파티셔너 → 대부분의 경우
- 특정 Key 분배 필요 → 커스텀 파티셔너
- 조회 성능 최적화 → 파티션별 데이터 분배 전략

---

## 시나리오별 권장 설정

### Scenario 1: 최대 처리량 (High Throughput)

높은 처리량이 필요하고 약간의 데이터 손실을 허용하는 경우:

```java
Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

// 신뢰성 설정
props.put(ProducerConfig.ACKS_CONFIG, "1");  // Leader만 확인

// 성능 최적화
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);  // 32 KB
props.put(ProducerConfig.LINGER_MS_CONFIG, 10L);  // 10ms 대기
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");  // 빠른 압축

// 버퍼 설정
props.put(ProducerConfig.BUFFER_MEMORY, 67108864L);  // 64 MB

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

**성능 기대값:**
- 처리량: 100k+ 메시지/초
- 지연시간: 100~500ms
- CPU 사용량: 중간

---

### Scenario 2: 최대 안정성 (Maximum Reliability)

데이터 손실이 절대 없어야 하는 경우:

```java
Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

// 신뢰성 설정
props.put(ProducerConfig.ACKS_CONFIG, "all");  // 모든 replica 확인
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // 중복 방지
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);  // 무한 재시도 (자동)
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);  // 멱등성과 함께

// 타임아웃 설정 (충분한 시간)
props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60000);
props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 300000);

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

**성능 기대값:**
- 처리량: 10k~50k 메시지/초
- 지연시간: 500ms~2초
- CPU 사용량: 낮음

---

### Scenario 3: 최소 지연시간 (Low Latency)

실시간 처리가 중요한 경우:

```java
Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

// 신뢰성 설정 (중간 수준)
props.put(ProducerConfig.ACKS_CONFIG, "1");  // Leader만 확인

// 성능 최적화 (배치 최소화)
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 1024);  // 1 KB (작음)
props.put(ProducerConfig.LINGER_MS_CONFIG, 0L);  // 대기 시간 없음
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");  // 압축 안 함

// 메모리 제약
props.put(ProducerConfig.BUFFER_MEMORY, 16777216L);  // 16 MB

// 타임아웃 설정 (짧음)
props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

**성능 기대값:**
- 처리량: 30k~80k 메시지/초
- 지연시간: 10~50ms
- CPU 사용량: 높음

---

### Scenario 4: 트랜잭션 (Exactly-Once Semantics)

정확히 한 번 처리가 필수인 경우:

```java
Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

// 트랜잭션 활성화
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-transactional-producer-1");
// 다음이 자동으로 설정됨:
// - enable.idempotence = true
// - acks = all
// - retries = Integer.MAX_VALUE

// 트랜잭션 타임아웃
props.put(ProducerConfig.TRANSACTION_TIMEOUT_MS_CONFIG, 60000);

// 타임아웃 설정
props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

KafkaProducer<String, String> producer = new KafkaProducer<>(props);

// 반드시 initTransactions() 호출
producer.initTransactions();

// 트랜잭션 사용
producer.beginTransaction();
try {
    // 여러 Topic에 메시지 전송
    producer.send(new ProducerRecord<>("orders", "order-123", "data1"));
    producer.send(new ProducerRecord<>("payments", "order-123", "data2"));

    // 모두 성공하면 커밋
    producer.commitTransaction();
} catch (Exception e) {
    // 실패하면 롤백
    producer.abortTransaction();
    throw new RuntimeException(e);
}
```

**성능 기대값:**
- 처리량: 5k~20k 메시지/초 (트랜잭션 오버헤드)
- 지연시간: 1~3초
- 정확성: 100% (Exactly-Once)

---

## 설정 간 의존성 및 자동 설정

### enable.idempotence=true 설정 시 자동 변경

```java
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

// 다음이 자동으로 설정됨:
// ProducerConfig.ACKS_CONFIG → "all"
// ProducerConfig.RETRIES_CONFIG → Integer.MAX_VALUE
// ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION → 1~5 범위 내
```

**의도:** 멱등성을 보장하려면 최고 신뢰성이 필요하므로 자동으로 가장 안전한 설정을 적용합니다.

---

### transactional.id 설정 시 자동 변경

```java
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-producer");

// 다음이 자동으로 설정됨:
// ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG → true
// ProducerConfig.ACKS_CONFIG → "all"
// ProducerConfig.RETRIES_CONFIG → Integer.MAX_VALUE
```

**의도:** 트랜잭션도 멱등성이 필요하므로 위의 모든 자동 설정이 함께 적용됩니다.

---

### 충돌하는 설정 조합

#### ❌ acks=0 + enable.idempotence=true
```java
props.put(ProducerConfig.ACKS_CONFIG, "0");
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
// 결과: ConfigError - 멱등성은 acks=all을 요구
```

**해결책:**
```java
// Option 1: 멱등성만 사용
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
// acks는 자동으로 "all"로 설정됨

// Option 2: acks=0만 사용
props.put(ProducerConfig.ACKS_CONFIG, "0");
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
```

---

#### ❌ transactional.id 설정 + enable.idempotence=false
```java
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-producer");
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
// 결과: ConfigError - 트랜잭션은 멱등성이 필수
```

**해결책:**
```java
props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-producer");
// enable.idempotence는 자동으로 true로 설정됨
```

---

### 추천 우선순위

설정할 때 우선순위:

1. **필수 설정** (반드시)
   - bootstrap.servers
   - key.serializer
   - value.serializer

2. **신뢰성 설정** (비즈니스 요구사항에 따라)
   - acks
   - enable.idempotence 또는 transactional.id

3. **성능 최적화** (모니터링 후)
   - batch.size
   - linger.ms
   - compression.type
   - buffer.memory

4. **타임아웃/제한** (필요 시)
   - request.timeout.ms
   - delivery.timeout.ms
   - max.block.ms

5. **식별/디버깅** (선택)
   - client.id
   - partitioner.class

---

## 설정 검증 도구

### Producer 설정 확인 코드

```java
import org.apache.kafka.clients.producer.ProducerConfig;
import java.util.Properties;

public class ProducerConfigValidator {
    public static void printConfigs(Properties props) {
        System.out.println("=== Producer Configurations ===");
        System.out.println("Bootstrap Servers: " + props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        System.out.println("Acks: " + props.get(ProducerConfig.ACKS_CONFIG));
        System.out.println("Enable Idempotence: " + props.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
        System.out.println("Batch Size: " + props.get(ProducerConfig.BATCH_SIZE_CONFIG));
        System.out.println("Linger MS: " + props.get(ProducerConfig.LINGER_MS_CONFIG));
        System.out.println("Compression Type: " + props.get(ProducerConfig.COMPRESSION_TYPE_CONFIG));
        System.out.println("Max In Flight: " + props.get(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION));
        System.out.println("Transactional ID: " + props.get(ProducerConfig.TRANSACTIONAL_ID_CONFIG));
        System.out.println("Delivery Timeout MS: " + props.get(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG));
        System.out.println("Request Timeout MS: " + props.get(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG));
    }
}
```

---

## 참고 자료

### Apache Kafka 공식 문서
- [Kafka Producer Configs](https://kafka.apache.org/documentation/#producerconfigs)
- [Kafka Protocol Documentation](https://kafka.apache.org/documentation/#protocol)

### 관련 Stage 학습 자료
- [Stage 1: Kafka 기초와 파티션 이해](../stages/stage-1-partitioning.md)
- [Stage 2: acks 설정과 전송 보장](../stages/stage-2-acks.md)
- [Stage 3: 재시도 및 순서 보장](../stages/stage-3-retries.md)
- [Stage 4: 멱등성 설정과 정확히 한 번](../stages/stage-4-idempotence.md)
- [Stage 5: 배치 처리와 압축](../stages/stage-5-batching.md)
- [Stage 6: 압축 및 성능 최적화](../stages/stage-6-compression.md)
- [Stage 7: 모니터링 및 디버깅](../stages/stage-7-monitoring.md)
- [Stage 8: 트랜잭션과 Exactly-Once](../stages/stage-8-transactions.md)

### 추가 학습 자료
- [Kafka 학습 계획](./LEARNING_PLAN.md)
- [Kafka 주요 명령어](./KAFKA_COMMANDS.md)
- [Producer 구현 가이드](./02-producer/README.md)

---

## 설정 변경 가이드

### Producer 재시작 없이 변경 불가능한 설정

다음 설정은 Producer 생성 시에만 적용되며, 생성 후에 변경할 수 없습니다:

- `bootstrap.servers`
- `key.serializer`
- `value.serializer`
- `transactional.id`
- `partitioner.class`

### 변경 방법

```java
// 잘못된 방법 (적용 안 됨)
producer.send(...); // 메시지 전송 중
props.put(ProducerConfig.ACKS_CONFIG, "all");
producer.send(...); // 변경되지 않은 설정으로 전송

// 올바른 방법 (새로운 Producer 생성)
producer.close();
props.put(ProducerConfig.ACKS_CONFIG, "all");
producer = new KafkaProducer<>(props);
producer.send(...); // 변경된 설정으로 전송
```

---

## 성능 튜닝 팁

### 1. 배치 처리 최적화

```java
// 처리량과 지연시간 균형
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);  // 16 KB
props.put(ProducerConfig.LINGER_MS_CONFIG, 5L);  // 5ms

// 계산: 평균 메시지 크기가 1KB일 때
// - 16개 메시지가 들어오면 바로 전송
// - 5ms 내에 들어오지 않아도 전송
```

### 2. 압축 효율 최대화

```java
// 압축이 효과적인 경우
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "zstd");
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);  // 더 큰 배치

// 압축이 효과 없는 경우 (이미 압축된 데이터)
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");
```

### 3. 메모리 사용량 최적화

```java
// 메모리 제약이 있는 경우
props.put(ProducerConfig.BUFFER_MEMORY, 16777216L);  // 16 MB
props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000L);

// 고처리량이 필요한 경우
props.put(ProducerConfig.BUFFER_MEMORY, 67108864L);  // 64 MB
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
```

### 4. 네트워크 효율성

```java
// WAN 환경 (높은 레이턴시)
props.put(ProducerConfig.ACKS_CONFIG, "1");  // 빠른 응답
props.put(ProducerConfig.LINGER_MS_CONFIG, 50L);  // 더 많은 배치
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "zstd");  // 대역폭 절감

// LAN 환경
props.put(ProducerConfig.ACKS_CONFIG, "all");  // 신뢰성 우선
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");  // 빠른 압축
```

---

## FAQ

**Q: acks 설정을 변경하고 싶은데, 몇 개의 복제본이 필요한가?**

A: `acks=all`을 사용할 때는 최소 `min.insync.replicas` (기본값 1) 개의 replica가 있어야 합니다. 안전성을 위해 최소 3개의 복제본을 권장합니다 (1 leader + 2 replicas).

---

**Q: 멱등성과 트랜잭션의 차이는?**

A:
- **멱등성**: 단일 Producer에서 동일 메시지의 중복 전송 방지
- **트랜잭션**: 여러 Topic에 메시지 그룹의 All-or-Nothing 보장

---

**Q: 배치 크기를 크게 설정하면 지연시간이 증가하는가?**

A: 네, 배치가 가득 차기를 기다리는 시간이 늘어납니다. `linger.ms`를 사용하여 최대 대기 시간을 제한하세요.

---

**Q: transactional.id가 같은 두 Producer를 동시에 실행해도 되는가?**

A: 아니요. Broker가 동시 실행을 감지하고 이전 Producer를 강제 종료합니다. 각 Producer는 고유한 `transactional.id`를 가져야 합니다.

---

**Q: 압축 알고리즘은 어떻게 선택하는가?**

A:
- **높은 압축률 필요**: gzip, zstd
- **빠른 속도 필요**: lz4, snappy
- **균형**: zstd (추천)

---

마지막 업데이트: 2026-01-22
