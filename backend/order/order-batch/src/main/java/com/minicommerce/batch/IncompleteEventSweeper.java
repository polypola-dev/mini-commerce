package com.minicommerce.batch;

import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * order-api가 event_publication 아웃박스에 남긴 미발행 이벤트(Kafka 전송 실패 등)를 주기적으로
 * 재시도 발행한다(ADR-005 S4). 5분 이상 지난 건만 대상으로 해 정상 처리 중인 이벤트를 건드리지 않는다.
 * replica가 여러 개여도 한 인스턴스만 실행하도록 @SchedulerLock으로 막는다(SchedulerLockConfig 참고).
 */
@Component
class IncompleteEventSweeper {

    private final IncompleteEventPublications incompleteEventPublications;

    IncompleteEventSweeper(IncompleteEventPublications incompleteEventPublications) {
        this.incompleteEventPublications = incompleteEventPublications;
    }

    @Scheduled(fixedDelay = 300_000)
    @SchedulerLock(name = "incompleteEventSweeper", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    void sweep() {
        incompleteEventPublications.resubmitIncompletePublicationsOlderThan(Duration.ofMinutes(5));
    }
}
