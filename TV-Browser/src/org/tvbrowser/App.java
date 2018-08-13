package org.tvbrowser;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.PrefUtils;
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
		SettingConstants.initialize(getApplicationContext());

		if (CompatUtils.isAtLeastAndroidO()) {
			createNotificationChannel();
			UiUtils.updateImportantProgramsWidget(getApplicationContext());
			UiUtils.updateRunningProgramsWidget(getApplicationContext());
		}
	}

	//public static boolean isInititalized() {		return INSTANCE != null;	}

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
		final NotificationManager service = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		if (service != null) {
			final long[] vibrationPattern = new long[] {1000,200,1000,400,1000,600};

			final AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build();

			final NotificationChannel notificationChannelDefault = new NotificationChannel(getNotificationChannelId(TYPE_NOTIFICATION_DEFAULT), getNotificationChannelName(TYPE_NOTIFICATION_DEFAULT), NotificationManager.IMPORTANCE_DEFAULT);
			notificationChannelDefault.setDescription(getString(R.string.notification_channel_default_description));
			notificationChannelDefault.setVibrationPattern(null);
			notificationChannelDefault.enableVibration(false);
			notificationChannelDefault.enableLights(false);
			notificationChannelDefault.setSound(null, attributes);
			service.createNotificationChannel(notificationChannelDefault);

			if(service.getNotificationChannel(getNotificationChannelId(TYPE_NOTIFICATION_REMINDER_DAY)) == null
				|| service.getNotificationChannel(getNotificationChannelId(TYPE_NOTIFICATION_REMINDER_WORK)) == null
				|| service.getNotificationChannel(getNotificationChannelId(TYPE_NOTIFICATION_REMINDER_NIGHT)) == null) {
				final String soundDefault = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString();
				PrefUtils.initialize(getApplicationContext());
				final int colorLED = PrefUtils.getIntValue(R.string.PREF_REMINDER_COLOR_LED, ContextCompat.getColor(getApplicationContext(), R.color.pref_reminder_color_led_default));

				service.createNotificationChannel(createReminderNotificationChannel(TYPE_NOTIFICATION_REMINDER_DAY, vibrationPattern, soundDefault, attributes, colorLED));
				service.createNotificationChannel(createReminderNotificationChannel(TYPE_NOTIFICATION_REMINDER_WORK, vibrationPattern, null, attributes, colorLED));
				service.createNotificationChannel(createReminderNotificationChannel(TYPE_NOTIFICATION_REMINDER_NIGHT, vibrationPattern, null, attributes, colorLED));
			}
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private NotificationChannel createReminderNotificationChannel(final int type, final long[] vibrationPattern, final String soundDefault, final AudioAttributes attributes, final int colorLED) {
		int prefIdSound = 0;
		int prefIdLED = 0;
		int prefIdVibrate = 0;
		int idDefaultLED = 0;
		int idDefaultVibrate = 0;

		switch (type) {
			case TYPE_NOTIFICATION_REMINDER_DAY:
				prefIdSound = R.string.PREF_REMINDER_SOUND_VALUE;
				prefIdLED = R.string.PREF_REMINDER_LED; idDefaultLED = R.bool.pref_reminder_led_default;
				prefIdVibrate = R.string.PREF_REMINDER_VIBRATE; idDefaultVibrate = R.bool.pref_reminder_vibrate_default;
				break;
			case TYPE_NOTIFICATION_REMINDER_WORK:
				prefIdSound = R.string.PREF_REMINDER_WORK_MODE_SOUND_VALUE;
				prefIdLED = R.string.PREF_REMINDER_WORK_MODE_LED; idDefaultLED = R.bool.pref_reminder_work_mode_led_default;
				prefIdVibrate = R.string.PREF_REMINDER_WORK_MODE_VIBRATE; idDefaultVibrate = R.bool.pref_reminder_work_mode_vibrate_default;
				break;
			case TYPE_NOTIFICATION_REMINDER_NIGHT:
				prefIdSound = R.string.PREF_REMINDER_NIGHT_MODE_SOUND_VALUE;
				prefIdLED = R.string.PREF_REMINDER_NIGHT_MODE_LED; idDefaultLED = R.bool.pref_reminder_night_mode_led_default;
				prefIdVibrate = R.string.PREF_REMINDER_NIGHT_MODE_VIBRATE; idDefaultVibrate = R.bool.pref_reminder_night_mode_vibrate_default;
				break;
		}

		final String stringSoundDefault = PrefUtils.getStringValue(prefIdSound, soundDefault);

		final Uri tone = stringSoundDefault != null ? Uri.parse(stringSoundDefault) : null;
		final boolean led = PrefUtils.getBooleanValue(prefIdLED, idDefaultLED);
		final boolean vibrate = PrefUtils.getBooleanValue(prefIdVibrate, idDefaultVibrate);

		final NotificationChannel notificationChannel = new NotificationChannel(getNotificationChannelId(type), getNotificationChannelName(TYPE_NOTIFICATION_REMINDER_NIGHT), NotificationManager.IMPORTANCE_DEFAULT);
		notificationChannel.setVibrationPattern(vibrationPattern);
		notificationChannel.setSound(tone, attributes);
		notificationChannel.setLightColor(colorLED);
		notificationChannel.enableLights(led);
		notificationChannel.enableVibration(vibrate);

		return notificationChannel;
	}
}
