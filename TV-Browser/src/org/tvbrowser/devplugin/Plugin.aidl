/*
 * Plugin Interface for TV-Browser for Android
 * Copyright (c) 2014 René Mach (rene@tvbrowser.org)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.devplugin;

import java.util.List;

import org.tvbrowser.devplugin.Channel;
import org.tvbrowser.devplugin.PluginManager;
import org.tvbrowser.devplugin.PluginMenu;
import org.tvbrowser.devplugin.Program;
import org.tvbrowser.devplugin.ReceiveTarget;

/**
 * Interface for Plugins of the TV-Browser app.
 */
interface Plugin {
    /**
     * Get the version of this Plugin.
     * NOTE: May be called without prior call to onActivation(PluginManager pluginManager);
     */
	String getVersion();
	
	/**
	 * Gets the name of this Plugin,
	 * NOTE: May be called without prior call to onActivation(PluginManager pluginManager);
	 */
	String getName();

	/**
	 * Get the description of this Plugin.
	 * NOTE: May be called without prior call to onActivation(PluginManager pluginManager);
	 */
	String getDescription();
	
	/**
	 * Get the author of this Plugin.
	 * NOTE: May be called without prior call to onActivation(PluginManager pluginManager);
	 */
	String getAuthor();
		
    /**
     * Get the license of this Plugin.
     * NOTE: May be called without prior call to onActivation(PluginManager pluginManager);
     */
    String getLicense();
    
    /**
     * Gets the data of the mark icon of this Plugin
     * ATTENTION: Currently not used, included only for future use.
     */
    byte[] getMarkIcon();
    
    /**
     * Get the available PluginMenu actions for the context menu of the given program 
     * @param program The program to get the context menus for.
     * @return A PluginMenu array with the context menu actions for the given program 
     *         or <code>null</code> if there is no context menu action for the given program.
     */
	PluginMenu[] getContextMenuActionsForProgram(in Program program);
	
	/** 
	 * Called when user selected a context menu of this Plugin
	 * @param program The program the user selected the context menu for
	 * @param contextMenuAction The context menu entry name the menu was selected for
	 * @return <code>true</code> if the program should be marked. <code>false</code> otherwise.
	 */
	boolean onProgramContextMenuSelected(in Program program, in PluginMenu pluginMenu);
	
	/**
	 * Gets if this Plugin has preferences.
	 * @return <code>true</code> if this Plugin has preferences, <code>false</code> otherwise. 
	 */
	boolean hasPreferences();
	
	/**
	 * Called when the preferences for this Plugin should be opened.
	 * @param subscribedChannels A list with all currently subscribed channels.
	 */
	void openPreferences(in List<Channel> subscribedChannels);
	
	/**
	 * Called at activation of this Plugin
	 * @return An array of long with all program ids that are marked by this plugin.
	 */
	long[] getMarkedPrograms();
	
	/**
	 * Gets if the Program with the given id is marked by this plugin.
	 * @param programId The id of the program to check.
	 * @return <code>true</code> if the program with the given id is marked by
	 *         this plugin, <code>false</code> if not.
	 */
	boolean isMarked(in long programId);
	
	/**
	 * Called at activation of this Plugin to inform about the first currently known
	 * id of the programs. All programs with ids that are smaller than this one don't
	 * exist anymore.
	 * NOTE: A <code>programId</code> of <code>-1</code> means that no programs are
	 *       available in TV-Browser, so if this Plugin has any ids stored it should
	 *       dismiss them all.
	 */
	void handleFirstKnownProgramId(in long programId);
	
	
	/**
	 * Gets the available ReceiveTargets for this Plugin.
	 * ATTENTION: Currently not used, included only for future use.
	 * @return The ReceiveTargets for this Plugin or <code>null</code>
	 *         if the Plugin cannot receive programs.
	 */
	ReceiveTarget[] getAvailableProgramReceiveTargets();
	
	/**
	 * Receive programs with a target to handle them according to target.
	 * ATTENTION: Currently not used, included only for future use.
	 * @param programs The programs that are send with the target.
	 * @param target The target for handling of the programs.
	 */
	void receivePrograms(in Program[] programs, in ReceiveTarget target);
	
	/**
	 * Called at any activation of this Plugin
	 * @param pluginManager The plugin manager of TV-Browser, can be used to get Programs
	 *                      and other values.
	 */
	void onActivation(PluginManager pluginManager);
	
	/**
	 * Caleld at any deactivation of this Plugin
	 */
	void onDeactivation();
}