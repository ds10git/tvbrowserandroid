package org.tvbrowser.tvbrowser;

import android.content.Intent;
import android.net.Uri;

import org.tvbrowser.devplugin.PluginDefinition;

class PluginUpdateHelperImpl extends PluginUpdateHelper {
	public static final String URL = "download/android-plugins.gz";
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
	}

	@Override
	void loadPlugin(final String url) {
		if (url.startsWith("http://play.google.com/store/apps/details?id=")) {
			try {
				tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("http://play.google.com/store/apps", "market:/"))));
			} catch (android.content.ActivityNotFoundException anfe) {
				tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			}
		}
		mLoadingPlugin = false;
	}

	@Override
	void cleanup() {
	}

	@Override
	boolean pluginSupported(PluginDefinition news) {
		return news.isOnGooglePlay();
	}
}