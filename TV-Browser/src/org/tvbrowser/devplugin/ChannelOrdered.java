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

public class ChannelOrdered {
  private final Channel mChannel;
  private final int mOrderNumber;

  /**
   * Creates an instance of this class.
   * <p>
   * @param channel The channel for the order number;
   * @param orderNumber The order number of the channel or -1 for no order.
   */
  public ChannelOrdered(final Channel channel, final int orderNumber) {
    mChannel = channel;
    mOrderNumber = orderNumber;
  }

  /**
   * Gets the channel of this class.
   * <p>
   * @return The channel of this class.
   */
  public Channel getChannel() {
    return mChannel;
  }

  /**
   * Gets the order number of this classes channel.
   * <p>
   * @return The order number of this classes channel or -1 if channel has no order number.
   */
  public int getOrderNumber() {
    return mOrderNumber;
  }
}
