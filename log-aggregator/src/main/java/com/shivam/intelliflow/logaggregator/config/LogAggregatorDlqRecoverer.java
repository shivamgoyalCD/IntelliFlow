package com.shivam.intelliflow.logaggregator.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

@Component
public class LogAggregatorDlqRecoverer implements ConsumerRecordRecoverer {
    private static final Logger log = LoggerFactory.getLogger(LogAggregatorDlqRecoverer.class);

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        log.warn(
                "Log aggregator consumer failure captured for topic={}, partition={}, offset={}; DLQ routing will be added later",
                record.topic(),
                record.partition(),
                record.offset(),
                exception
        );
    }
}
