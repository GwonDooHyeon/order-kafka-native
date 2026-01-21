# Consumer 실습 문제

## [C-01] Consumer와 Partition 분배

**목표:** 2개의 Consumer로 4개 Partition 구독 시 분배 확인

**요구사항:**
1. Partition 4개인 Topic 생성
2. 같은 Consumer Group으로 Consumer 2개 실행
3. 각 Consumer가 어떤 Partition을 담당하는지 확인

**예상 결과:**
- Consumer 1: Partition 0, 1
- Consumer 2: Partition 2, 3

---

## [C-02] Rebalancing 관찰

**목표:** Consumer 장애 시 Rebalancing 시간 측정

**요구사항:**
1. Consumer 3개로 Partition 3개 구독
2. 하나의 Consumer 강제 종료
3. 남은 Consumer들이 재분배 받는 시간 측정

**확인 포인트:**
- `session.timeout.ms` 설정값
- Rebalancing 동안 메시지 처리 중단 여부

---

## [C-03] 수동 커밋으로 정확히 한 번 처리

**목표:** 메시지 처리 완료 후 커밋하여 중복 방지

**요구사항:**
1. `enable.auto.commit=false` 설정
2. 메시지 처리 성공 시에만 `commitSync()` 호출
3. 처리 중 예외 발생 시 커밋하지 않음 → 재처리 확인

**시나리오:**
```java
for (ConsumerRecord<String, String> record : records) {
    try {
        process(record);
        consumer.commitSync();
    } catch (Exception e) {
        // 커밋하지 않음 → 다음 poll에서 재처리
    }
}
```
