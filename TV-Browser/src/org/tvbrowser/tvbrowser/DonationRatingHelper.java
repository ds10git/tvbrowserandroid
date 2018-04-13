package org.tvbrowser.tvbrowser;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.tvbrowser.settings.SettingConstants;

import java.util.Calendar;
import java.util.Locale;

abstract class DonationRatingHelper {

	final TvBrowser tvBrowser;

	DonationRatingHelper(final TvBrowser tvBrowser) {
		this.tvBrowser = tvBrowser;
	}

	abstract void handleExpiredVersion(final Calendar calendar);
	abstract boolean onActivityResult(int requestCode, int resultCode, Intent data);
	abstract void onDestroy();
	abstract void prepareInAppPayment();
	abstract boolean showDonationMenuItem();
	abstract void showRatingAndDonationInfo();

	void showDonationInfo() {
		AlertDialog.Builder alert = new AlertDialog.Builder(tvBrowser);
		alert.setTitle(R.string.donation);

		View view = tvBrowser.getLayoutInflater().inflate(R.layout.dialog_donations, tvBrowser.getParentViewGroup(), false);
		alert.setView(view);

		TextView webInfo = view.findViewById(R.id.donation_show_ways);
		Button openWeb = view.findViewById(R.id.donation_website_button);

		if(!showDonationWebsite()) {
			webInfo.setVisibility(View.GONE);
			openWeb.setVisibility(View.GONE);
		}

		alert.setNegativeButton(tvBrowser.getString(R.string.not_now).replace("{0}", ""), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		final AlertDialog d = alert.create();

		Button inAppDonation = view.findViewById(R.id.donation_in_app_button);
		if (inAppDonation!=null) {
			inAppDonation.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					d.dismiss();
					prepareInAppPayment();
				}
			});
		}

		openWeb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();

				tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SettingConstants.URL_SYNC_BASE + "index.php?id=donations")));
			}
		});

		tvBrowser.showAlertDialog(d);
	}

	private static boolean showDonationWebsite() {
		return "DE".equals(Locale.getDefault().getCountry());
	}

	protected void setRatingAndDonationInfoShown() {
		PreferenceManager.getDefaultSharedPreferences(tvBrowser.getApplicationContext()).edit().putBoolean(tvBrowser.getString(R.string.PREF_RATING_DONATION_INFO_SHOWN), true).apply();
	}
}