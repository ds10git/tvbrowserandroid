package org.tvbrowser.tvbrowser;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabHelper.QueryInventoryFinishedListener;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;
import com.android.vending.billing.util.SkuDetails;

import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.CompatUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.tvbrowser.settings.SettingConstants.URL_SYNC_BASE;

class DonationRatingHelperImpl extends DonationRatingHelper {

	private static final String SKU_ONE_STAR_DONATION = "one_star_donation";
	private static final String SKU_TWO_STAR_DONATION = "two_star_donation";
	private static final String SKU_THREE_STAR_DONATION = "three_star_donation";
	private static final String SKU_FOUR_STAR_DONATION = "four_star_donation";
	private static final String SKU_FIVE_STAR_DONATION = "five_star_donation";
	private static final String SKU_EPG_DONATE_ONCE = "epg_donate_once";

	// private static final String SKU_EPG_DONATE_ABO = "epg_donate_abo";

	private static final List<String> SKU_LIST = new ArrayList<>();

	static {
		SKU_LIST.add(SKU_EPG_DONATE_ONCE);
		SKU_LIST.add(SKU_ONE_STAR_DONATION);
		SKU_LIST.add(SKU_TWO_STAR_DONATION);
		SKU_LIST.add(SKU_THREE_STAR_DONATION);
		SKU_LIST.add(SKU_FOUR_STAR_DONATION);
		SKU_LIST.add(SKU_FIVE_STAR_DONATION);
	}

	private IabHelper iabHelper;

	DonationRatingHelperImpl(final TvBrowser tvBrowser) {
		super(tvBrowser);
	}

	boolean onActivityResult(int requestCode, int resultCode, Intent data) {
		return iabHelper != null && iabHelper.handleActivityResult(requestCode, resultCode, data);
	}

	void onDestroy() {
		if (iabHelper!=null) {
			iabHelper.dispose();
			iabHelper = null;
		}
	}

	private void showInAppDonations(final Inventory inv) {
		tvBrowser.updateProgressIcon(false);

		AlertDialog.Builder alert = new AlertDialog.Builder(tvBrowser);

		alert.setTitle(R.string.donation);

		View view = tvBrowser.getLayoutInflater().inflate(R.layout.in_app_donations, tvBrowser.getParentViewGroup(), false);
		LinearLayout layout = view.findViewById(R.id.donation_in_app_layout);

		alert.setView(view);

		alert.setNegativeButton(tvBrowser.getString(R.string.not_now).replace("{0}", ""), null);

		if (Locale.getDefault().getCountry().equals("DE")) {
			alert.setPositiveButton(R.string.donation_info_website, (dialog, which) -> tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_SYNC_BASE + "index.php?id=donations"))));
		}

		final AlertDialog d = alert.create();

		View.OnClickListener onDonationClick = v -> {
      d.dismiss();
      openDonation((SkuDetails) v.getTag());
    };

		Purchase donated = null;
		SkuDetails donatedDetails = null;

		for (String sku : SKU_LIST) {
			SkuDetails details = inv.getSkuDetails(sku);
			Purchase donatedTest = inv.getPurchase(sku);

			if (donatedTest != null && details != null) {
				donated = donatedTest;
				donatedDetails = details;
			}
			if (details != null) {
				if (!details.getSku().equals(SKU_EPG_DONATE_ONCE) || hasEpgDonateChannelsSubscribed()) {
					String title = details.getTitle().substring(0, details.getTitle().indexOf("(") - 1);

					Button donation = new Button(tvBrowser);
					donation.setText(String.format("%s: %s", title, details.getPrice()));
					donation.setTag(details);
					donation.setOnClickListener(onDonationClick);

					layout.addView(donation);

					LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) donation.getLayoutParams();
					params.setMargins(0, 0, 0, 8); //left, top, right, bottom
					donation.setLayoutParams(params);
				}
			}
		}

		if (donated == null) {
			tvBrowser.showAlertDialog(d);
		} else {
			AlertDialog.Builder alert2 = new AlertDialog.Builder(tvBrowser);

			alert2.setTitle(R.string.donation);

			String message = tvBrowser.getString(R.string.already_donated).replace("{1}",
				DateFormat.getLongDateFormat(tvBrowser).format(new Date(donated.getPurchaseTime()))).replace("{0}", donatedDetails.getPrice());

			alert2.setMessage(message);

			final Purchase toConsume = donated;

			alert2.setPositiveButton(R.string.donate_again, (dialog, which) -> {
        tvBrowser.updateProgressIcon(true);

        iabHelper.consumeAsync(toConsume, (purchase, result) -> {
          tvBrowser.updateProgressIcon(false);

          if (result.isSuccess()) {
            tvBrowser.showAlertDialog(d);
          } else {
            tvBrowser.getHandler().post(() -> Toast.makeText(tvBrowser, "", Toast.LENGTH_LONG).show());
          }
        });
      });

			alert2.setNegativeButton(R.string.stop_donation, (dialog, which) -> {
      });

			tvBrowser.showAlertDialog(alert2);
		}
	}

	private void showInAppError() {
		tvBrowser.updateProgressIcon(false);

		AlertDialog.Builder alert = new AlertDialog.Builder(tvBrowser);

		alert.setTitle(R.string.donation);

		boolean showOthers = true;

		if (!"DE".equals(Locale.getDefault().getCountry())) {
			showOthers = false;
		}

		String message = tvBrowser.getString(R.string.in_app_error_1);

		if (showOthers) {
			message += " " + tvBrowser.getString(R.string.in_app_error_2);
		} else {
			message += ".";
		}

		alert.setMessage(message);

		alert.setNegativeButton(android.R.string.ok, null);

		if (showOthers) {
			alert.setPositiveButton(R.string.donation_open_website, (dialog, which) -> tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_SYNC_BASE + "index.php?id=donations"))));
		}

		tvBrowser.showAlertDialog(alert);
	}

	private void listPurchaseItems() {
		try {
			if (iabHelper!=null) {
				iabHelper.queryInventoryAsync(true, SKU_LIST, (result, inv) -> {
          if (result.isFailure()) {
            showInAppError();
          } else {
            tvBrowser.getHandler().post(() -> showInAppDonations(inv));
          }
        });
			}
		} catch (IllegalStateException e) {
			showInAppError();
		}
	}

	void prepareInAppPayment() {
		tvBrowser.updateProgressIcon(true);

		if (iabHelper == null) {
			String a2b = "UpRRJ1sxrOwIDAQAB";
			String a2a = "xUV6DkVjK0pioTw9fM27t0vj7ZKA02T1BunYV7WHLFID";
			String ag2 = "rFxFvNvdJBrYtlQrcw";
			String u6c = "PbMd2DTmgh5nXMr6+CR9i5+";
			String ab2 = "BjlFAgXyKJvMGWuEI2UcMVt2K1T5ENa91XolVnIAlDzoNi9YA1";
			String hm5 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArgS";
			String ot8 = "Vwf9rH6cgY7+2HXr/g6J3lPRcySSDLL";
			String bt4 = "dixLF8/z/qUWVH36xbYURI3UZBPNkgoRGnjQTE4C+NJ0xRU09SQnaw8t2";
			String ddx = "RsQ70MVT9fJMkFAeYs3ZnbN/J/JSRyLmmMTi8Br2J4uLh0P2hYq6R9DamUlaA/I1N3";
			String iz4 = "CRVp1K32FczuxXMa7UdhUpyyIFu/xwz8HSrjlbx";

			String hrq = hm5 + a2a + ab2 + bt4 + ddx + iz4 + u6c + ag2 + ot8 + a2b;

			iabHelper = new IabHelper(tvBrowser, hrq);
			iabHelper.startSetup(result -> {
        if (!result.isSuccess()) {
          showInAppError();
        } else {
          tvBrowser.getHandler().post(() -> listPurchaseItems());
        }
      });
		} else {
			listPurchaseItems();
		}
	}

	private void openDonation(final SkuDetails skuDetails) {
		if (skuDetails != null && iabHelper != null) {
			AlertDialog.Builder alert = new AlertDialog.Builder(tvBrowser);

			alert.setTitle(R.string.donation);

			View view = tvBrowser.getLayoutInflater().inflate(R.layout.open_donation, tvBrowser.getParentViewGroup(), false);

			alert.setView(view);

			((TextView) view.findViewById(R.id.donation_open_info)).setText(tvBrowser.getString(R.string.make_donation_info).replace("{0}", skuDetails.getPrice()));

			alert.setNegativeButton(R.string.stop_donation, (dialog, which) -> {
      });

			alert.setPositiveButton(R.string.make_donation, (dialog, which) -> tvBrowser.getHandler().post(() -> iabHelper.launchPurchaseFlow(tvBrowser, skuDetails.getSku(), 1012, (result, info) -> {
        if (result.isSuccess()) {
          AlertDialog.Builder alert2 = new AlertDialog.Builder(tvBrowser);

          alert2.setTitle(R.string.donation);
          alert2.setMessage(R.string.thanks_for_donation);

          alert2.setPositiveButton(android.R.string.ok, (dialog1, which1) -> {
          });

          tvBrowser.showAlertDialog(alert2);
        }
      }, Long.toHexString(Double.doubleToLongBits(Math.random())))));

			tvBrowser.showAlertDialog(alert);
		}
	}

	private boolean hasEpgDonateChannelsSubscribed() {
		return tvBrowser.getEpgDonateChannelsCount() > 0;
	}

	boolean showDonationMenuItem() {
		return true;
	}

	void showRatingAndDonationInfo() {
		AlertDialog.Builder alert = new AlertDialog.Builder(tvBrowser);

		alert.setTitle(R.string.you_like_it);

		View view = tvBrowser.getLayoutInflater().inflate(R.layout.rating_and_donation, tvBrowser.getParentViewGroup(), false);

		TextView ratingInfo = view.findViewById(R.id.rating_info);
		Button rate = view.findViewById(R.id.rating_button);
		Button donate = view.findViewById(R.id.donation_button);

		if (!installedFromGooglePlay()) {
			ratingInfo.setVisibility(View.GONE);
			rate.setVisibility(View.GONE);
		}

		ratingInfo.setText(CompatUtils.fromHtml(tvBrowser.getString(R.string.rating_text)));
		((TextView) view.findViewById(R.id.donation_info)).setText(CompatUtils.fromHtml(tvBrowser.getString(R.string.donate_text)));

		final Button cancel = view.findViewById(R.id.rating_donation_cancel);
		cancel.setEnabled(false);

		alert.setView(view);
		alert.setCancelable(false);

		final AlertDialog d = alert.create();

		cancel.setOnClickListener(v -> {
      setRatingAndDonationInfoShown();
      d.dismiss();
      tvBrowser.finish();
    });

		donate.setOnClickListener(v -> {
      setRatingAndDonationInfoShown();
      d.dismiss();
      showDonationInfo();
    });

		rate.setOnClickListener(v -> {
      setRatingAndDonationInfoShown();
      d.dismiss();
      final String appPackageName = tvBrowser.getPackageName(); // getPackageName() from Context or Activity object
      try {
        tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
      } catch (android.content.ActivityNotFoundException anfe) {
        tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
      }
      tvBrowser.finish();
    });

		d.setOnShowListener(dialog -> new Thread("Cancel wait thread") {
      @Override
      public void run() {
        setRatingAndDonationInfoShown();

        int count = 9;
        while (--count >= 0) {
          final int countValue = count + 1;

          tvBrowser.getHandler().post(() -> cancel.setText(tvBrowser.getString(R.string.not_now).replace("{0}", " (" + countValue + ")")));

          try {
            sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        tvBrowser.getHandler().post(() -> {
          cancel.setText(tvBrowser.getString(R.string.not_now).replace("{0}", ""));
          cancel.setEnabled(true);
        });
      }
    }.start());

		tvBrowser.showAlertDialog(d, true);
	}

	@Override
	boolean isToShowWebDonation() {
		return false;
	}

	private boolean installedFromGooglePlay() {
		return "com.android.vending".equals(tvBrowser.getPackageManager()
			.getInstallerPackageName(tvBrowser.getPackageName()));
	}

	void handleExpiredVersion(final Calendar calendar) {
		AlertDialog.Builder builder = new AlertDialog.Builder(tvBrowser);
		builder.setTitle(R.string.versionExpired);

		Calendar test = (Calendar)calendar.clone();
		test.add(Calendar.DAY_OF_YEAR, 7);

		final int diff = (int)((test.getTimeInMillis() - System.currentTimeMillis()) / (24 * 60 * 60000));

		String expiredMessage = diff == 0 ? tvBrowser.getString(R.string.versionExpiredMsgLast) : tvBrowser.getString(R.string.versionExpiredMsg);
		expiredMessage = expiredMessage.replace("{0}", String.valueOf(diff));

		String updateText = installedFromGooglePlay() ? tvBrowser.getString(R.string.update_google_play) : tvBrowser.getString(R.string.update_website);

		builder.setMessage(expiredMessage);
		builder.setPositiveButton(updateText, (dialog, which) -> {
      if (installedFromGooglePlay()) {
        final String appPackageName = tvBrowser.getPackageName(); // getPackageName() from Context or Activity object
        try {
          tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
          tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
      }
      else {
        tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SettingConstants.URL_SYNC_BASE + "/index.php?id=download")));
      }

      System.exit(0);
    });
		builder.setNegativeButton(R.string.update_not_now, (dialog, which) -> {
      if(diff < 0) {
        System.exit(0);
      }
    });
		builder.setCancelable(false);

		tvBrowser.showAlertDialog(builder);
	}
}