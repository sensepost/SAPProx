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

import com.sensepost.SAPProx.datatype.SockRequest;
import com.sensepost.SAPProx.gui.ProxyEdit;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ian
 */
public class RemoteEndPointMain implements Runnable {

    private String _rhost;
    private int _rport;
    private SocketChannel _rchan;
    private Selector _rselc;
    private RemoteEndPointWork _rwork;
    private List _rchng;
    private Map _rdata;
    private Map _rsock;
    private SocketChannel _lchan;
    private LocalEndPointMain _servr;
    // Control variables
    private boolean _isrunning;
    private List queue = new LinkedList();
    // Buffer Variables...
    private int _rsize;
    private ByteBuffer _rbyte;

    public RemoteEndPointMain(String rh, int rp, LocalEndPointMain server, SocketChannel ls, RemoteEndPointWork rw) throws Exception {
        this._rhost = rh;
        this._rport = rp;
        this._servr = server;
        this._lchan = ls;
        this._rwork = rw;
        this._rchng = new LinkedList();
        this._rdata = new HashMap();
        this._rselc = this.initRemoteSelector();
        this._rchan = initRemoteConnection();
        this._rsize = -1;
    }

    public void stopProxyClient() {
        this._isrunning = false;
        this._rselc.wakeup();
    }

    public void localFromServer(byte[] data) {
        SocketChannel socket = this._rchan;
        synchronized (this._rchng) {
            this._rchng.add(new SockRequest(socket, SockRequest.CHANGEOPS, SelectionKey.OP_WRITE));
            synchronized(this._rdata) {
                List queue = (List) this._rdata.get(socket);
                if (queue == null) {
                    queue = new ArrayList();
                    this._rdata.put(socket, queue);
                }
                queue.add(ByteBuffer.wrap(data));
            }
        }
        this._rselc.wakeup();
    }

    public SocketChannel getLocalSocket() {
        return this._lchan;
    }

    private Selector initRemoteSelector() throws Exception {
        Selector socksel = SelectorProvider.provider().openSelector();
        return socksel;
    }

    private SocketChannel initRemoteConnection() throws Exception {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(this._rhost, this._rport));
        socketChannel.register(this._rselc, SelectionKey.OP_CONNECT);
        return socketChannel;
    }

    private void doRemoteRead(SelectionKey key) throws Exception {
        SocketChannel socketchannel = (SocketChannel)key.channel();
        int numread;
        if (this._rsize == -1) {
            ByteBuffer bb = ByteBuffer.allocate(4);
            try {
                numread = socketchannel.read(bb);
            } catch (Exception e) {
                key.cancel();
                socketchannel.close();
                return;
            }
            if (numread == -1) {
                key.channel().close();
                key.cancel();
                return;
            }
            byte[] b = bb.array();
            String sz_hex = "";
            for (int i = 0; i < b.length; i++) {
                int j = (int) ((int) b[i] & 0xff);
                String x = Integer.toHexString(j);
                if (x.length() < 2) {
                    x = "0" + x;
                }
                sz_hex += x;
            }
            this._rsize = Integer.parseInt(sz_hex, 16);
            this._rbyte = bb;
        }
        ByteBuffer cc = ByteBuffer.allocate(this._rsize);
        try {
            numread = socketchannel.read(cc);
        }
        catch (Exception e) {
            key.cancel();
            socketchannel.close();
            return;
        }
        if (numread == -1) {
            key.channel().close();
            key.cancel();
            return;
        }
        ByteBuffer dd = ByteBuffer.allocate(numread + this._rbyte.array().length);
        dd.put(this._rbyte.array(), 0, this._rbyte.array().length);
        dd.put(cc.array(), 0, numread);
        this._rsize = this._rsize - numread;
        this._rbyte = dd;
        if (this._rsize == 0) {
            byte[] zz;
            if (this._servr.getInterceptResponse()) {
                this._servr.getForm().addError("Intercepting request: SRV -> GUI");
                ProxyEdit pe = new ProxyEdit(this._servr.getForm(), "Intercept Response (SRV -> GUI)", this._rbyte.array());
                zz = pe.getData();
            }
            else {
                ProxyEdit pe = new ProxyEdit(this._servr.getForm(), "Intercept Response (SRV -> GUI)", this._rbyte.array());
                zz = pe.getByPassData();
            }
            this._servr.getForm().addError("Forwarding request: SRV -> GUI");
            this._rwork.processRemoteData(this._servr, this, socketchannel, zz, zz.length);
            this._rsize = -1;
            this._rbyte = null;
        }
    }

    private void doRemoteWrite(SelectionKey key) throws Exception {
        SocketChannel socketchannel = (SocketChannel)key.channel();
        synchronized (this._rdata) {
            List queue = (List)this._rdata.get(socketchannel);
            while (!queue.isEmpty()) {
                ByteBuffer bb = (ByteBuffer)queue.get(0);
                socketchannel.write(bb);
                if (bb.remaining() > 0) {
                    break;
                }
                queue.remove(0);
            }
            if (queue.isEmpty()) {
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private void doRemoteConnect(SelectionKey key) throws Exception {
        SocketChannel socketChannel = (SocketChannel)key.channel();
        try {
            socketChannel.finishConnect();
        }
        catch (Exception e) {
            key.cancel();
            return;
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    public void run() {
        this._isrunning = true;
        while (this._isrunning) {
            try {
                synchronized (this._rchng) {
                    Iterator changes = this._rchng.iterator();
                    while (changes.hasNext()) {
                        SockRequest change = (SockRequest)changes.next();
                        switch(change.type) {
                            case SockRequest.CHANGEOPS:
                                SelectionKey key = change.socket.keyFor(this._rselc);
                                key.interestOps(change.ops);
                        }
                    }
                    this._rchng.clear();
                }
                this._rselc.select();
                Iterator selectedkeys = this._rselc.selectedKeys().iterator();
                while (selectedkeys.hasNext()) {
                    SelectionKey key = (SelectionKey)selectedkeys.next();
                    selectedkeys.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isConnectable()) {
                        this.doRemoteConnect(key);
                    }
                    else if (key.isReadable()) {
                        this.doRemoteRead(key);
                    }
                    else if (key.isWritable()) {
                        this.doRemoteWrite(key);
                    }
                }
            }
            catch (Exception e) {
            }
        }
    }

}
