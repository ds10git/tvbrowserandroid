package org.tvbrowser.tvbrowser;

import android.content.DialogInterface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;

import org.tvbrowser.devplugin.PluginDefinition;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.devplugin.PluginServiceConnection;
import org.tvbrowser.utils.PrefUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class PluginUpdateHelper {

	final TvBrowser tvBrowser;

	protected boolean mLoadingPlugin = false;

	PluginUpdateHelper(final TvBrowser tvBrowser) {
		this.tvBrowser = tvBrowser;
	}

	void searchPlugins(final boolean showChannelUpdateInfo) {
		if (tvBrowser.isOnline()) {
			new Thread("SEARCH FOR PLUGINS THREAD") {
				@Override
				public void run() {
					tvBrowser.updateProgressIcon(true);
					PluginDefinition[] availablePlugins = PluginDefinition.loadAvailablePluginDefinitions();

					final List<PluginDefinition> newPlugins = new ArrayList<>();
					final PluginServiceConnection[] connections = PluginHandler.getAvailablePlugins();

					for (PluginDefinition pluginDefinition : availablePlugins) {
						if (Build.VERSION.SDK_INT >= pluginDefinition.getMinApiVersion()) {
							String packageName = pluginDefinition.getPackageName();
							String[] services = pluginDefinition.getServices();

							for (String service : services) {
								if (service.startsWith(".")) {
									service = packageName + service;
								}

								String[] parts = service.split(":");

								boolean wasAdded = false;
								boolean wasFound = false;

								if (connections != null && connections.length > 0) {
									for (PluginServiceConnection connection : connections) {
										if (connection.getId().equals(parts[0])) {
											wasFound = true;

											String currentVersion = connection.getPluginVersion();

											if (currentVersion != null && !currentVersion.equals(parts[1])) {
												newPlugins.add(pluginDefinition);
												pluginDefinition.setIsUpdate();
												wasAdded = true;
												break;
											}
										}
									}
								}

								if (wasAdded) {
									break;
								} else if (!wasFound) {
									newPlugins.add(pluginDefinition);
								}
							}
						}
					}

					StringBuilder pluginsText = new StringBuilder();

					Collections.sort(newPlugins);

					for (PluginDefinition news : newPlugins) {
						if (pluginsText.length() > 0) {
							pluginsText.append("<line>LINE</line>");
						}

						pluginsText.append("<h3>");
						pluginsText.append(news.getName());
						if (news.isUpdate()) {
							pluginsText.append(" (Update)");
						}
						pluginsText.append("</h3>");

						pluginsText.append(news.getDescription());

						pluginsText.append("<p><i>");
						pluginsText.append(tvBrowser.getString(R.string.author)).append(" ");
						pluginsText.append(news.getAuthor());
						pluginsText.append(" <right>").append(tvBrowser.getString(R.string.version)).append(" ");
						pluginsText.append(news.getVersion());
						pluginsText.append("</i></right></p>");

						prepareLinks(pluginsText, news);
					}

					String title = tvBrowser.getString(R.string.plugin_available_title);

					if (newPlugins.isEmpty()) {
						title = tvBrowser.getString(R.string.plugin_available_not_title);
						pluginsText.append(tvBrowser.getString(R.string.plugin_available_not_message));
					}

					final AlertDialog.Builder builder = new AlertDialog.Builder(tvBrowser);

					builder.setTitle(title);
					builder.setCancelable(false);
					builder.setMessage(getClickableText(Html.fromHtml(pluginsText.toString(), null, new TvBrowser.NewsTagHandler())));

					builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (!newPlugins.isEmpty()) {
								PluginHandler.shutdownPlugins(tvBrowser);

								tvBrowser.getHandler().postDelayed(new Runnable() {
									@Override
									public void run() {
										PluginHandler.loadPlugins(tvBrowser.getApplicationContext());
										tvBrowser.togglePluginPreferencesMenuItem();
									}
								}, 2000);
							}

							if (showChannelUpdateInfo) {
								tvBrowser.getHandler().post(new Runnable() {
									@Override
									public void run() {
										tvBrowser.showChannelUpdateInfo();
									}
								});
							}
						}
					});

					tvBrowser.getHandler().post(new Runnable() {
						@Override
						public void run() {
							tvBrowser.showAlertDialog(builder, true);
						}
					});
					tvBrowser.updateProgressIcon(false);
				}
			}.start();
		}
	}

	abstract void prepareLinks(final StringBuilder pluginsText, final PluginDefinition news);

	private void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
		int start = strBuilder.getSpanStart(span);
		int end = strBuilder.getSpanEnd(span);
		int flags = strBuilder.getSpanFlags(span);
		ClickableSpan clickable = new ClickableSpan() {
			public void onClick(View view) {
				if (!mLoadingPlugin) {
					mLoadingPlugin = true;
					loadPlugin(span.getURL());
				}
			}
		};
		strBuilder.setSpan(clickable, start, end, flags);
		strBuilder.removeSpan(span);
	}

	private SpannableStringBuilder getClickableText(final CharSequence sequence) {
		SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
		URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
		for (URLSpan span : urls) {
			makeLinkClickable(strBuilder, span);
		}
		return strBuilder;
	}

	void showPluginInfo() {
		Log.d("info6", "showPluginInfo");
		if (!PrefUtils.getBooleanValue(R.string.PLUGIN_INFO_SHOWN, false)) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(tvBrowser);

			builder.setTitle(R.string.plugin_info_title);
			builder.setCancelable(false);
			builder.setMessage(R.string.plugin_info_message);

			builder.setPositiveButton(R.string.plugin_info_load, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					savePluginInfoShown();

					if (tvBrowser.isOnline()) {
						searchPlugins(true);
					} else {
						tvBrowser.showNoInternetConnection(tvBrowser.getString(R.string.no_network_info_data_search_plugins), new Runnable() {
							@Override
							public void run() {
								searchPlugins(true);
							}
						});
					}
				}
			});

			builder.setNegativeButton(tvBrowser.getString(R.string.not_now).replace("{0}", ""), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					savePluginInfoShown();
					tvBrowser.showChannelUpdateInfo();
				}
			});

			tvBrowser.showAlertDialog(builder);
		} else {
			tvBrowser.showChannelUpdateInfo();
		}
	}

	private void savePluginInfoShown() {
		PreferenceManager.getDefaultSharedPreferences(tvBrowser)
			.edit().putBoolean(tvBrowser.getString(R.string.PLUGIN_INFO_SHOWN), true).apply();
	}

	abstract void loadPlugin(final String url);

	abstract void cleanup();
}