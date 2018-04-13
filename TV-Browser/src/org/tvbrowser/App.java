package org.tvbrowser;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

import org.tvbrowser.tvbrowser.R;

public final class App extends Application {

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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannel();
		}
	}

	/**
	 * Returns the app's global notification channel identifier
	 * (which is equal to the package name of the app).
	 */
	public String notificationChannelId() {
		return getPackageName();
	}

	/**
	 * Returns the app's global notification channel name
	 * (which is equal to the localized name of the app).
	 */
	public String notificationChannelName() {
		return getString(R.string.app_name);
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
		final NotificationChannel notificationChannel = new NotificationChannel(
			notificationChannelId(), notificationChannelName(), NotificationManager.IMPORTANCE_DEFAULT);
		final NotificationManager service = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (service != null) {
			service.createNotificationChannel(notificationChannel);
		}
	}
}