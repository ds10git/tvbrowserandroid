package org.tvbrowser.tvbrowser;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import org.tvbrowser.devplugin.PluginDefinition;
import org.tvbrowser.utils.IOUtils;

import java.io.File;

class PluginUpdateHelperImpl extends PluginUpdateHelper {
	public static final String XML_ELEMENT_DOWNLOAD_LINK = "donwloadlink";

	public static final String URL = "download/android-plugins-full.gz";
	private File mCurrentDownloadPlugin;

	PluginUpdateHelperImpl(final TvBrowser tvBrowser) {
		super(tvBrowser);
	}

	@Override
	void prepareLinks(final StringBuilder pluginsText, final PluginDefinition news) {
		if (news.isOnGooglePlay()) {
			pluginsText.append("<p><a href=\"http://play.google.com/store/apps/details?id=");
			pluginsText.append(news.getPackageName());
			pluginsText.append("\">").append(tvBrowser.getString(R.string.plugin_open_google_play)).append("</a></p>");
		}

		final String downloadLink = news.getUnknownValueForName(XML_ELEMENT_DOWNLOAD_LINK);

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
			final File path = IOUtils.getDownloadDirectory(tvBrowser.getApplicationContext());

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

			tvBrowser.getHandler().post(new Runnable() {
				@Override
				public void run() {
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
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(Uri.fromFile(mPluginFile), "application/vnd.android.package-archive");
								tvBrowser.startActivityForResult(intent, TvBrowser.INSTALL_PLUGIN);
							}

							mLoadingPlugin = false;
						}
					};

					async.execute(mCurrentDownloadPlugin.toString(), downloadUrl);
				}
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
	}

	@Override
	boolean pluginSupported(PluginDefinition news) {
		return true;
	}
}