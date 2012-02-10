/*
SAPProx - SAP Proxy server component

Copyright (C) 2011 SensePost <ian@sensepost.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sensepost.SAPProx.communication;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author ian
 */
public class LocalEndPointWork implements Runnable {

    private List queue = new LinkedList();
    private boolean _isrunning;

    public void stopProxyLocalWorker() {
        this._isrunning = false;
    }

    public void processLocalData(LocalEndPointMain server, SocketChannel socket, byte[] data, int count) {
        byte[] dataCopy = new byte[count];
        System.arraycopy(data, 0, dataCopy, 0, count);
        synchronized (queue) {
            queue.add(new LocalEndPointData(server, socket, dataCopy));
            queue.notify();
        }
    }

    public void run() {
        this._isrunning = true;
        LocalEndPointData dataEvent;
        while (this._isrunning) {
            synchronized (queue) {
                while (queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                    }
                }
                dataEvent = (LocalEndPointData) queue.remove(0);
            }
            dataEvent.server.localSendFromWorker(dataEvent.socket, dataEvent.data);
        }
    }
}
