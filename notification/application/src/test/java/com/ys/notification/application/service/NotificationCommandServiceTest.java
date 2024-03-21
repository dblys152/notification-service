package com.ys.notification.application.service;

import com.ys.notification.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {
    private static final NotificationId NOTIFICATION_ID = NotificationId.of(9999999L);
    private static final NotificationId NOTIFICATION_ID2 = NotificationId.of(9999998L);

    @InjectMocks
    private NotificationCommandService sut;

    @Mock
    private RecordNotificationPort recordNotificationPort;
    @Mock
    private LoadNotificationPort loadNotificationPort;

    @Test
    void 알림을_예약한다() {
        CreateNotificationCommand command = mock(CreateNotificationCommand.class);

        when(recordNotificationPort.save(any(Notification.class))).thenReturn(mock(Notification.class));
        Notification actual = sut.reserve(command);

        assertAll(
                () -> assertThat(actual).isNotNull(),
                () -> then(recordNotificationPort).should().save(any(Notification.class))
        );
    }

    @Test
    void 예약된_알림을_대기중_상태로_변경한다() {
        Notifications reservedNotifications = mock(Notifications.class);
        given(loadNotificationPort.findAllByStatusAndSentAtLessThanEqual(eq(NotificationStatus.RESERVED), any(LocalDateTime.class)))
                .willReturn(reservedNotifications);

        when(recordNotificationPort.saveAll(reservedNotifications)).thenReturn(mock(Notifications.class));
        Notifications actual = sut.changeReservedToWaiting();

        assertAll(
                () -> assertThat(actual).isNotNull(),
                () -> then(loadNotificationPort).should().findAllByStatusAndSentAtLessThanEqual(eq(NotificationStatus.RESERVED), any(LocalDateTime.class)),
                () -> then(reservedNotifications).should().toWaiting(),
                () -> then(recordNotificationPort).should().saveAll(reservedNotifications)
        );
    }

    @Test
    void 전송_결과를_처리한다() {
        List<ProcessSendingResultCommand> commandList = List.of(getProcessSendingResultCommand(NOTIFICATION_ID), getProcessSendingResultCommand(NOTIFICATION_ID2));
        List<NotificationId> commandIds = List.of(NOTIFICATION_ID, NOTIFICATION_ID2);
        Notifications notifications = mock(Notifications.class);
        given(loadNotificationPort.findAllByIds(commandIds)).willReturn(notifications);

        Notifications processedNotifications = mock(Notifications.class);
        when(notifications.processSendingResults(commandList)).thenReturn(processedNotifications);
        when(recordNotificationPort.saveAll(processedNotifications)).thenReturn(mock(Notifications.class));
        Notifications actual = sut.processSendingResults(commandList);

        assertAll(
                () -> assertThat(actual).isNotNull(),
                () -> then(loadNotificationPort).should().findAllByIds(commandIds),
                () -> then(notifications).should().processSendingResults(commandList),
                () -> then(recordNotificationPort).should().saveAll(processedNotifications)
        );
    }

    private ProcessSendingResultCommand getProcessSendingResultCommand(NotificationId notificationId) {
        return new ProcessSendingResultCommand(notificationId, NotificationStatus.SUCCEEDED);
    }
}