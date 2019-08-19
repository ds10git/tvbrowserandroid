package org.tvbrowser.tvbrowser;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.core.content.FileProvider;

import org.tvbrowser.devplugin.PluginDefinition;
import org.tvbrowser.utils.CompatUtils;
import org.tvbrowser.utils.IOUtils;

import java.io.File;

class PluginUpdateHelperImpl extends PluginUpdateHelper {
	private static final int INSTALL_PLUGIN = 3;
	private static final int REQUEST_CODE_PERMISSION_GRANT = 94;
	public static final String XML_ELEMENT_DOWNLOAD_LINK = "donwloadlink";

	public static final String URL = "download/android-plugins.gz";
	private File mCurrentDownloadPlugin;
	private Runnable mInstallRunnable;

	private static boolean canRequestPackageInstalls(final Context context) {
		return !CompatUtils.isAtLeastAndroidO() || context.getPackageManager().canRequestPackageInstalls();
	}

	PluginUpdateHelperImpl(final TvBrowser tvBrowser) {
		super(tvBrowser);
	}

	@Override
	void prepareLinks(final StringBuilder pluginsText, final PluginDefinition news) {
		final String downloadLink = news.getUnknownValueForName(XML_ELEMENT_DOWNLOAD_LINK);

		if(news.isOnGooglePlay() && downloadLink != null && downloadLink.trim().length() > 0) {
			pluginsText.append("<p>").append(tvBrowser.getString(R.string.plugin_download_info)).append("</p>");
		}

		if (news.isOnGooglePlay()) {
			pluginsText.append("<p><a href=\"http://play.google.com/store/apps/details?id=");
			pluginsText.append(news.getPackageName());
			pluginsText.append("\">").append(tvBrowser.getString(R.string.plugin_open_google_play)).append("</a></p>");
		}

		if (downloadLink != null && downloadLink.trim().length() > 0) {
			pluginsText.append("<p><a href=\"");
			pluginsText.append(downloadLink.replace("http://", "plugin://").replace("https://", "plugins://"));
			pluginsText.append("\">").append(tvBrowser.getString(R.string.plugin_download_manually)).append("</a></p>");
		}
	}

	@Override
	void loadPlugin(String url) {
		if (url.startsWith("http://play.google.com/store/apps/details?id=")) {
			try {
				tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("http://play.google.com/store/apps", "market:/"))));
			} catch (android.content.ActivityNotFoundException anfe) {
				tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			}

			mLoadingPlugin = false;
		} else if (url.startsWith("plugin://") || url.startsWith("plugins://")) {
			final File path = IOUtils.getDownloadDirectory(tvBrowser.getApplicationContext(), IOUtils.TYPE_DOWNLOAD_DIRECTORY_OTHER);

			if (!path.isDirectory()) {
				//noinspection ResultOfMethodCallIgnored
				path.mkdirs();
			}

			if (url.startsWith("plugin://")) {
				url = url.replace("plugin://", "http://");
			} else if (url.startsWith("plugins://")) {
				url = url.replace("plugins://", "https://");
			}

			String name = url.substring(url.lastIndexOf("/") + 1);

			mCurrentDownloadPlugin = new File(path, name);

			if (mCurrentDownloadPlugin.isFile()) {
				//noinspection ResultOfMethodCallIgnored
				mCurrentDownloadPlugin.delete();
			}

			final String downloadUrl = url;

			tvBrowser.getHandler().post(() -> {
        AsyncTask<String, Void, Boolean> async = new AsyncTask<String, Void, Boolean>() {
          private ProgressDialog mProgress;
          private File mPluginFile;

          protected void onPreExecute() {
            mProgress = new ProgressDialog(tvBrowser);
            mProgress.setMessage(tvBrowser.getString(R.string.plugin_info_donwload).replace("{0}", mCurrentDownloadPlugin.getName()));
            mProgress.show();
          }

          @Override
          protected Boolean doInBackground(String... params) {
            mPluginFile = new File(params[0]);

            return IOUtils.saveUrl(params[0], params[1], 15000);
          }

          protected void onPostExecute(Boolean result) {
            mProgress.dismiss();
            if (result) {
              mInstallRunnable = () -> {
	              final Uri apkUri = FileProvider.getUriForFile(tvBrowser, tvBrowser.getString(R.string.authority_file_provider), mPluginFile);

	              Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
	              install.setData(apkUri);
	              install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	              tvBrowser.startActivityForResult(install, INSTALL_PLUGIN);
              };

              if (CompatUtils.isAtLeastAndroidN()) {
                if(!canRequestPackageInstalls(tvBrowser)) {
                  final AlertDialog.Builder builder = new AlertDialog.Builder(tvBrowser);
                  builder.setTitle(R.string.dialog_permission_title);
                  builder.setCancelable(false);
                  builder.setMessage(R.string.dialog_permission_message);
                  builder.setPositiveButton(R.string.dialog_permission_ok, (dialog, which) -> tvBrowser.startActivityForResult(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", tvBrowser.getPackageName()))), REQUEST_CODE_PERMISSION_GRANT));
                  builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> cleanup());
                  builder.show();
                }
                else {
                  mInstallRunnable.run();
                }
              }
              else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(mPluginFile), "application/vnd.android.package-archive");
                tvBrowser.startActivityForResult(intent, INSTALL_PLUGIN);
              }
            }

            mLoadingPlugin = false;
          }
        };

        async.execute(mCurrentDownloadPlugin.toString(), downloadUrl);
      });
		} else {
			mLoadingPlugin = false;
		}
	}

	void cleanup() {
		if (mCurrentDownloadPlugin != null && mCurrentDownloadPlugin.isFile()) {
			if (!mCurrentDownloadPlugin.delete()) {
				mCurrentDownloadPlugin.deleteOnExit();
			}
		}

		mInstallRunnable = null;
	}

	@Override
	boolean pluginSupported(PluginDefinition news) {
		return true;
	}

	@Override
	protected boolean onActivityResult(int requestCode, int resultCode, Intent data) {
		boolean result = false;

		if(requestCode == INSTALL_PLUGIN) {
			cleanup();
			result = true;
		}
		else if(requestCode == REQUEST_CODE_PERMISSION_GRANT) {
		  if (mInstallRunnable != null && canRequestPackageInstalls(tvBrowser)) {
				mInstallRunnable.run();
			} else {
				cleanup();
			}

			result = true;
		}


		return result;
	}
}