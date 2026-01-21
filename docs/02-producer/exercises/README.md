# Producer 실습 문제

## [P-01] Key 기반 파티셔닝 확인

**목표:** 동일한 Key로 메시지를 전송했을 때 모두 같은 Partition에 저장되는지 확인

**요구사항:**
1. `ProducerWithKey.java` 구현
2. 같은 Key로 100개 메시지 전송
3. Callback에서 Partition 번호 출력
4. 모든 메시지가 같은 Partition에 저장되는지 확인

**힌트:**
```java
ProducerRecord<String, String> record =
    new ProducerRecord<>(topic, "same-key", "message-" + i);
```

---

## [P-02] acks 설정에 따른 처리량 비교

**목표:** `acks=1`과 `acks=all`의 처리량 차이 측정

**요구사항:**
1. 동일한 메시지 1000개를 각각 다른 acks 설정으로 전송
2. 소요 시간 측정
3. 결과 비교 및 분석

**측정 코드 예시:**
```java
long start = System.currentTimeMillis();
// 메시지 전송
long end = System.currentTimeMillis();
System.out.println("소요 시간: " + (end - start) + "ms");
```

---

## [P-03] 커스텀 파티셔너 구현

**목표:** VIP 고객의 메시지를 전용 Partition(0번)으로 라우팅

**요구사항:**
1. `VIPPartitioner.java` 구현
2. Key가 "VIP-"로 시작하면 Partition 0으로
3. 그 외에는 기본 파티셔닝 로직 사용
4. Producer에 커스텀 파티셔너 적용

**인터페이스:**
```java
public class VIPPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        // TODO: 구현
    }
}
```
