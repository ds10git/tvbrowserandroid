package org.tvbrowser;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import org.tvbrowser.utils.CompatUtils;

import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.UiUtils;

public final class App extends Application {
  public static final int TYPE_NOTIFICATION_DEFAULT = 0;
  public static final int TYPE_NOTIFICATION_REMINDER_DAY = 1;
  public static final int TYPE_NOTIFICATION_REMINDER_WORK = 2;
  public static final int TYPE_NOTIFICATION_REMINDER_NIGHT = 3;

	private static App INSTANCE = null;

	/**
	 * <p>Single instance of this {@link Application} (per process).</p>
	 *
	 * <p>
	 * Note: Dedicated processes having their own application context: examples are
	 * {@link android.widget.RemoteViews} running in the launcher, {@link android.app.Service}
	 * instances with a configured name in the {@link android.Manifest}.
	 * </p>
	 */
	public static App get() {
		return INSTANCE;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		INSTANCE = this;

		if (CompatUtils.isAtLeastAndroidO()) {
			createNotificationChannel();
			UiUtils.updateImportantProgramsWidget(getApplicationContext());
			UiUtils.updateRunningProgramsWidget(getApplicationContext());
		}
	}

	public static boolean isInititalized() {
		return INSTANCE != null;
	}

	/**
	 * Returns the apps's appropriate priority global notification channel identifier
	 * @param type The type of the notification, one of {@link #TYPE_NOTIFICATION_DEFAULT},
	  * {@link #TYPE_NOTIFICATION_REMINDER_DAY}, {@link #TYPE_NOTIFICATION_REMINDER_WORK}, {@link #TYPE_NOTIFICATION_REMINDER_NIGHT}.
	 */
	public String getNotificationChannelId(final int type) {
	  final StringBuilder builder = new StringBuilder(getPackageName());

	  switch(type) {
	    case TYPE_NOTIFICATION_REMINDER_DAY:builder.append(".reminderDayMode");break;
	    case TYPE_NOTIFICATION_REMINDER_WORK:builder.append(".reminderWorkMode");break;
	    case TYPE_NOTIFICATION_REMINDER_NIGHT:builder.append(".reminderNightMode");break;
	    // the default notification channel has the package name, therefor
			// TYPE_NOTIFICATION_DEFAULT doesn't need to be handled here
    }

    return builder.toString();
  }

  public static String getNotificationChannelIdDefault(final Context context) {
		return context.getPackageName();
	}

	/**
	 * Returns the app's global high priority notification channel name
	 */
	private String getNotificationChannelName(final int type) {
	  final StringBuilder builder = new StringBuilder(getString(R.string.app_name));

	  switch(type) {
	    case TYPE_NOTIFICATION_REMINDER_DAY:builder.append(": ").append(getString(R.string.notification_channel_reminder_day));break;
	    case TYPE_NOTIFICATION_REMINDER_WORK:builder.append(": ").append(getString(R.string.notification_channel_reminder_work));break;
	    case TYPE_NOTIFICATION_REMINDER_NIGHT:builder.append(": ").append(getString(R.string.notification_channel_reminder_night));break;
    }

		return builder.toString();
	}

	/**
	 * Each foreground service or notification requires a {@link NotificationChannel} starting with
	 * Android 8.0 Oreo (SDK 26). For categorization or grouping, additional channels are possible.
	 *
	 * @see <a href="https://developer.android.com/guide/topics/ui/notifiers/notifications.html#ManageChannels">Notifications Overview</a>
	 * @see <a href="https://developer.android.com/about/versions/oreo/android-8.0.html#notifications">Android 8.0 Features and APIs</a>
	 */
	@RequiresApi(Build.VERSION_CODES.O)
	private void createNotificationChannel() {
		final long[] vibrationPattern = new long[] {1000,200,1000,400,1000,600};

	  final NotificationChannel notificationChannelDefault = new NotificationChannel(getNotificationChannelId(TYPE_NOTIFICATION_DEFAULT),getNotificationChannelName(TYPE_NOTIFICATION_DEFAULT), NotificationManager.IMPORTANCE_DEFAULT);
	  notificationChannelDefault.setDescription(getString(R.string.notification_channel_default_description));
		notificationChannelDefault.setVibrationPattern(null);
		notificationChannelDefault.setSound(null,null);

		final NotificationChannel notificationChannelReminderDay = new NotificationChannel(getNotificationChannelId(TYPE_NOTIFICATION_REMINDER_DAY),getNotificationChannelName(TYPE_NOTIFICATION_REMINDER_DAY), NotificationManager.IMPORTANCE_DEFAULT);
		notificationChannelReminderDay.setVibrationPattern(vibrationPattern);

		final NotificationChannel notificationChannelReminderWork = new NotificationChannel(getNotificationChannelId(TYPE_NOTIFICATION_REMINDER_WORK),getNotificationChannelName(TYPE_NOTIFICATION_REMINDER_WORK), NotificationManager.IMPORTANCE_DEFAULT);
		notificationChannelReminderWork.setVibrationPattern(vibrationPattern);

		final NotificationChannel notificationChannelReminderNight = new NotificationChannel(getNotificationChannelId(TYPE_NOTIFICATION_REMINDER_NIGHT),getNotificationChannelName(TYPE_NOTIFICATION_REMINDER_NIGHT), NotificationManager.IMPORTANCE_DEFAULT);
		notificationChannelReminderNight.setVibrationPattern(vibrationPattern);

		final NotificationManager service = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		if (service != null) {
			service.createNotificationChannel(notificationChannelDefault);
			service.createNotificationChannel(notificationChannelReminderDay);
			service.createNotificationChannel(notificationChannelReminderWork);
			service.createNotificationChannel(notificationChannelReminderNight);
		}
	}
}
