package org.tvbrowser.tvbrowser;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

class DonationRatingHelperImpl extends DonationRatingHelper {

	DonationRatingHelperImpl(final TvBrowser tvBrowser) {
		super(tvBrowser);
	}

	boolean onActivityResult(int requestCode, int resultCode, Intent data) {
		return false;
	}

	void onDestroy() {
	}

	void prepareInAppPayment() {
	}

	boolean showDonationMenuItem() {
		return "DE".equals(Locale.getDefault().getCountry());
	}

	void showRatingAndDonationInfo() {
		AlertDialog.Builder alert = new AlertDialog.Builder(tvBrowser);

		alert.setTitle(R.string.you_like_it);

		View view = tvBrowser.getLayoutInflater().inflate(R.layout.rating_and_donation, tvBrowser.getParentViewGroup(), false);

		Button donate = view.findViewById(R.id.donation_button);

		((TextView)view.findViewById(R.id.donation_info)).setText(Html.fromHtml(tvBrowser.getString(R.string.donate_text)));

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

		d.setOnShowListener(dialog -> new Thread("Cancel wait thread") {
      @Override
      public void run() {
        setRatingAndDonationInfoShown();

        int count = 9;

        while(--count >= 0) {
          final int countValue = count+1;

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

		tvBrowser.showAlertDialog(d,true);
	}

	void handleExpiredVersion(final Calendar calendar) {
		AlertDialog.Builder builder = new AlertDialog.Builder(tvBrowser);
		builder.setTitle(R.string.versionExpired);

		Calendar test = (Calendar)calendar.clone();
		test.add(Calendar.DAY_OF_YEAR, 7);

		final int diff = (int)((test.getTimeInMillis() - System.currentTimeMillis()) / (24 * 60 * 60000));

		String expiredMessage = diff == 0 ? tvBrowser.getString(R.string.versionExpiredMsgLast) : tvBrowser.getString(R.string.versionExpiredMsg);
		expiredMessage = expiredMessage.replace("{0}", String.valueOf(diff));

		String updateText = tvBrowser.getString(R.string.update_website);

		builder.setMessage(expiredMessage);
		builder.setPositiveButton(updateText, (dialog, which) -> {
      tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://android.tvbrowser.org/index.php?id=download")));
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