package org.example.common.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class KafkaConstants {

    // 브로커 설정
    public final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";

    // Consumer 설정
    public final String DEFAULT_GROUP_ID = "order-consumer-group";
    public final String AUTO_OFFSET_RESET_EARLIEST = "earliest";
    public final String AUTO_OFFSET_RESET_LATEST = "latest";

}
