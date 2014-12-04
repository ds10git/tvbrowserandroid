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
import org.tvbrowser.devplugin.Program;
import org.tvbrowser.devplugin.TvBrowserSettings;

/**
 * Interface for access to TV-Browser app from plugins.
 */
interface PluginManager {
    /**
     * Gets the program with the given unique id.
     *
     * @param programId The unique id of the program to get
     * @return The Program with the unique id or <code>null</code> if 
     *         there is no program with that id.
     */
	Program getProgramWithId(in long programId);
	
	/**
     * Gets the program of the channel with the given unique channel id
     * and the given start time in milliseconds since 1970 in UTC timezone.
     * <p>
     * @param channelId The unique id of the channel of the program to get
     * @param startTimeInUTC The start time in milliseconds since 1970 in
     *                       UTC timezone of the program to get
     * @return The Program with the given parameters or <code>null</code> if there 
     *         is no program that matches the given parameter.
     */
	Program getProgramForChannelAndTime(in int channelId, in long startTimeInUTC);
	
	/**
	 * Gets a list of all subscribed channels of TV-Browser.
	 * <p>
	 * @return A list with all currently subscribed channels of TV-Browser
	 */
	List<Channel> getSubscribedChannels();
	
	/**
	 * Gets the TvBrowserSettings of TV-Browser.
	 * <p>
	 * @return The TvBrowserSettings of TV-Browser.
	 */
	TvBrowserSettings getTvBrowserSettings();
	
	/**
	 * Marks the program for the plugin with the given id.
	 * 
	 * @param program The program to mark.
	 * @return <code>true</code> if the program was successfully marked (or was already marked),
	 *         <code>false</code> if the program could not be marked. 
	 */
	boolean markProgram(in Program program);
	
	/**
	 * Unmarks the program for the plugin with the given id.
	 * NOTE: The program is only completely unmarked if no other plugin has marked it.
	 *
	 * @param program The program to unmark.
	 * @return <code>true</code> if the program exists and could be updated (if it is completely unmarked
	 *         depends on other plugin markings), <code>false</code> if the program didn't exists or could
	 *         not be updated.  
	 */
	boolean unmarkProgram(in Program program);
}