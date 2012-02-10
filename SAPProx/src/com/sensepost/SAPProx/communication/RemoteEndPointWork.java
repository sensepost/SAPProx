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
public class RemoteEndPointWork implements Runnable {

    private boolean _isrunning;
    private List queue = new LinkedList();

    public void processRemoteData(LocalEndPointMain server, RemoteEndPointMain client, SocketChannel socket, byte[] data, int count) {
        byte[] dataCopy = new byte[count];
        System.arraycopy(data, 0, dataCopy, 0, count);
        synchronized (queue) {
            queue.add(new RemoteEndPointData(server, client, dataCopy));
            queue.notify();
        }
    }

    public void stopProxyRemoteWorker() {
        this._isrunning = false;
    }

    public void run() {
        this._isrunning = true;
        RemoteEndPointData dataEvent;
        while (this._isrunning) {
            synchronized (queue) {
                while (queue.isEmpty()) {
                    try {
                        queue.wait();
                        //queue.wait();
                    } catch (InterruptedException e) {
                    }
                }
                dataEvent = (RemoteEndPointData) queue.remove(0);
            }
            dataEvent.server.remoteSendFromWorker(dataEvent.client, dataEvent.data);
        }
    }

}
