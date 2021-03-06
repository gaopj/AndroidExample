package com.example.android.activenotifications;
/*
* Copyright 2015 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.common.logger.Log;

/**
 * A fragment that allows notifications to be enqueued.
 * 一个允许notifications入队的fragment
 */
public class ActiveNotificationsFragment extends Fragment {

    /**
     * The request code can be any number as long as it doesn't match another request code used
     * in the same app.
     * REQUEST_CODE 只要不重复都行
     */
    private static final int REQUEST_CODE = 2323;

    private static final String TAG = "ActiveNotificationsFragment";

    private static final String NOTIFICATION_GROUP =
            "com.example.android.activenotifications.notification_type";

    private static final int NOTIFICATION_GROUP_SUMMARY_ID = 1;

    private NotificationManager mNotificationManager;

    private TextView mNumberOfNotifications;

    // Every notification needs a unique ID otherwise the previous one would be overwritten. This
    // variable is incremented when used.
    // 每个通知都需要一个唯一的ID，否则前一个会被覆盖。
    // 这个变量在使用时递增。
    private static int sNotificationId = NOTIFICATION_GROUP_SUMMARY_ID + 1;

    private PendingIntent mDeletePendingIntent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_builder, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNumberOfNotifications();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNotificationManager = (NotificationManager) getActivity().getSystemService(
                Context.NOTIFICATION_SERVICE);
        mNumberOfNotifications = (TextView) view.findViewById(R.id.number_of_notifications);

        // Supply actions to the button that is displayed on screen.
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.add_notification: {
                        addNotificationAndUpdateSummaries();
                        break;
                    }
                }
            }
        };
        view.findViewById(R.id.add_notification).setOnClickListener(onClickListener);

        // [BEGIN create_pending_intent_for_deletion]
        // Create a PendingIntent to be fired upon deletion of a Notification.
        Intent deleteIntent = new Intent(ActiveNotificationsActivity.ACTION_NOTIFICATION_DELETE);
        mDeletePendingIntent = PendingIntent.getBroadcast(getActivity(),
                REQUEST_CODE, deleteIntent, 0);
        // [END create_pending_intent_for_deletion]
    }

    /**
     * Adds a new {@link Notification} with sample data and sends it to the system.
     * Then updates the current number of displayed notifications for this application and
     * creates a notification summary if more than one notification exists.
     */
    private void addNotificationAndUpdateSummaries() {
        // [BEGIN create_notification]
        // Create a Notification and notify the system.
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.sample_notification_content))
                .setAutoCancel(true)
                .setDeleteIntent(mDeletePendingIntent)
                .setGroup(NOTIFICATION_GROUP);

        final Notification notification = builder.build();
        mNotificationManager.notify(getNewNotificationId(), notification);
        // [END create_notification]
        Log.i(TAG, "Add a notification");

        updateNotificationSummary();
        updateNumberOfNotifications();
    }

    /**
     * Adds/updates/removes the notification summary as necessary.
     */
    protected void updateNotificationSummary() {
        int numberOfNotifications = getNumberOfNotifications();

        if (numberOfNotifications > 1) {
            // Add/update the notification summary.
            String notificationContent = getString(R.string.sample_notification_summary_content,
                    numberOfNotifications);
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
                    .setSmallIcon(R.mipmap.ic_notification)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setSummaryText(notificationContent))
                    .setGroup(NOTIFICATION_GROUP)
                    .setGroupSummary(true);
            final Notification notification = builder.build();
            mNotificationManager.notify(NOTIFICATION_GROUP_SUMMARY_ID, notification);
        } else {
            // Remove the notification summary.
            mNotificationManager.cancel(NOTIFICATION_GROUP_SUMMARY_ID);
        }
    }

    /**
     * Requests the current number of notifications from the {@link NotificationManager} and
     * display them to the user.
     * 请求来自{@link NotificationManager}，当前通知数量并将其显示给用户。

     */
    protected void updateNumberOfNotifications() {
        final int numberOfNotifications = getNumberOfNotifications();
        mNumberOfNotifications.setText(getString(R.string.active_notifications,
                numberOfNotifications));
        Log.i(TAG, getString(R.string.active_notifications, numberOfNotifications));
    }

    /**
     * Retrieves a unique notification ID.
     */
    public int getNewNotificationId() {
        int notificationId = sNotificationId++;

        // Unlikely in the sample, but the int will overflow if used enough so we skip the summary
        // ID. Most apps will prefer a more deterministic way of identifying an ID such as hashing
        // the content of the notification.
        // 在本例中不太可能，但是如果使用足够的话，int会溢出，所以我们跳过摘要ID。
        // 大多数应用程序会选择更确定的方式来识别ID，例如哈希通知的内容。
        if (notificationId == NOTIFICATION_GROUP_SUMMARY_ID) {
            notificationId = sNotificationId++;
        }
        return notificationId;
    }

    private int getNumberOfNotifications() {
        // [BEGIN get_active_notifications]
        // Query the currently displayed notifications.
        // 查询当前显示的通知。
        final StatusBarNotification[] activeNotifications = mNotificationManager
                .getActiveNotifications();
        // [END get_active_notifications]

        // Since the notifications might include a summary notification remove it from the count if
        // it is present.
        // 由于通知可能包含摘要通知，因此如果它存在，请将其从计数中删除。
        for (StatusBarNotification notification : activeNotifications) {
            if (notification.getId() == NOTIFICATION_GROUP_SUMMARY_ID) {
                return activeNotifications.length - 1;
            }
        }
        return activeNotifications.length;
    }
}
