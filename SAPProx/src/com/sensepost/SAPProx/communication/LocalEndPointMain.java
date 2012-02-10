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
import com.sensepost.SAPProx.gui.ProxyMain;
import com.sensepost.SAPProx.datatype.SockData;
import com.sensepost.SAPProx.gui.ProxyEdit;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
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
public class LocalEndPointMain implements Runnable {

    // Global vars
    private ProxyMain _form;
    // Local socket variables.
    private String _lhost;
    private int _lport;
    private ServerSocketChannel _lchan;
    private Selector _lselc;
    private LocalEndPointWork _lwork;
    private List _lchng;
    private Map _ldata;
    private Map _lsock;
    // Remote socket variables.
    private String _rhost;
    private int _rport;
    private RemoteEndPointWork _rwork;
    // Control variables
    private boolean _isrunning;
    // This stuff is for managing the fact that messages are not always complete... :/
    private Map _lsize;
    private Map _lbyte;

    public LocalEndPointMain(ProxyMain sp, String lh, int lp, String rh, int rp) throws Exception {
        this._form = sp;
        // Initialise local socket stuff...
        this._lhost = lh;
        this._lport = lp;
        this._lwork = new LocalEndPointWork();
        Thread l = new Thread(this._lwork);
        l.start();
        this._lchng = new LinkedList();
        this._ldata = new HashMap();
        this._lselc = this.initLocalSelector();
        this._lsock = new HashMap();
        // Initialise remote socket stuff...
        this._rhost = rh;
        this._rport = rp;
        // Initialise Maps...
        this._lsize = new HashMap();
        this._lbyte = new HashMap();
        this._rwork = new RemoteEndPointWork();
        Thread r = new Thread(this._rwork);
        r.start();
        // Initialise control variables...
        this._isrunning = false;
    }

    public boolean getInterceptRequest() {
        return this._form.getReqIntercept();
    }

    public boolean getInterceptResponse() {
        return this._form.getResIntercept();
    }

    public synchronized ProxyMain getForm() {
        return this._form;
    }

    // Helper methods
    public void stopProxyServer() {
        this._lwork.stopProxyLocalWorker();
        this._rwork.stopProxyRemoteWorker();
        Iterator i = this._lsock.keySet().iterator();
        while (i.hasNext()) {
            RemoteEndPointMain pc = (RemoteEndPointMain) i.next();
            pc.stopProxyClient();
        }
        this._isrunning = false;
        this._lselc.wakeup();
    }

    private Selector initLocalSelector() throws Exception {
        Selector socksel = SelectorProvider.provider().openSelector();
        this._lchan = ServerSocketChannel.open();
        this._lchan.configureBlocking(false);
        this._lchan.socket().bind(new InetSocketAddress(this._lhost, this._lport));
        this._lchan.register(socksel, SelectionKey.OP_ACCEPT);
        return socksel;
    }

    public void localSendFromWorker(SocketChannel socket, byte[] data) {
        SockData dat = new SockData(SockData.FROM_CLIENT, data);
        this._form.addData(dat);
        RemoteEndPointMain client = (RemoteEndPointMain) this._lsock.get(socket);
        client.localFromServer(data);
    }

    public void remoteSendFromWorker(RemoteEndPointMain client, byte[] data) {
        SockData dat = new SockData(SockData.FROM_SERVER, data);
        this._form.addData(dat);
        SocketChannel socket = client.getLocalSocket();
        synchronized (this._lchng) {
            this._lchng.add(new SockRequest(socket, SockRequest.CHANGEOPS, SelectionKey.OP_WRITE));
            synchronized (this._ldata) {
                List queue = (List) this._ldata.get(socket);
                if (queue == null) {
                    queue = new ArrayList();
                    this._ldata.put(socket, queue);
                }
                queue.add(ByteBuffer.wrap(data));
            }
        }
        this._lselc.wakeup();
    }

    // These methods get called by the local selector - ie: for local accepts, reads and writes.
    private void doLocalAccept(SelectionKey key) throws Exception {
        ServerSocketChannel serversocketchannel = (ServerSocketChannel) key.channel();
        SocketChannel sockchannel = serversocketchannel.accept();
        Socket socket = sockchannel.socket();
        this._form.addError("Accepted new connection:" + socket.getLocalAddress().toString() + ":" + socket.getLocalPort() + " FROM:" + socket.getRemoteSocketAddress().toString());
        sockchannel.configureBlocking(false);
        sockchannel.register(this._lselc, SelectionKey.OP_READ);
        // This is where we will have to start grouping the stuff...
        RemoteEndPointMain prxc = new RemoteEndPointMain(this._rhost, this._rport, this, sockchannel, this._rwork);
        this._lsock.put(sockchannel, prxc);
        Thread t = new Thread(prxc);
        t.start();
    }

    private void doLocalRead(SelectionKey key) throws Exception {
        SocketChannel socketchannel = (SocketChannel) key.channel();
        // The read is going to be a little bit tricksy...
        // Firstly, the message may be spread across a number of different packets.
        // The first four bytes of the message contain the length of the message (minus 4 bytes).
        // So, we first read the length, and then we will read the message.
        // Partial messages are stored in a hashmap associated with the socketchannel until the message is complete.
        // At this point, it will be forwarded to the SAP server.
        ByteBuffer bb;
        int TotalLength = -1;
        int numread;
        // We first see whether this message length has been stored in the HashMap
        try {
            TotalLength = Integer.parseInt(this._lsize.get(socketchannel).toString());
        } catch (Exception e) {
            TotalLength = -1;
        }
        if (TotalLength == -1) {
            bb = ByteBuffer.allocate(4);
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
            TotalLength = Integer.parseInt(sz_hex, 16);
            this._lsize.put(socketchannel, TotalLength);
            this._lbyte.put(socketchannel, bb);
        }
        bb = (ByteBuffer) this._lbyte.get(socketchannel);
        ByteBuffer cc = ByteBuffer.allocate(TotalLength);
        
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
        ByteBuffer dd = ByteBuffer.allocate(numread + bb.array().length);
        dd.put(bb.array(), 0, bb.array().length);
        dd.put(cc.array(), 0, numread);
        this._lsize.put(socketchannel, TotalLength);
        this._lbyte.put(socketchannel, dd);
        if (dd.remaining() == 0) {
            this._lsize.remove(socketchannel);
            this._lbyte.remove(socketchannel);
            byte[] zz;
            // We have to check whether we have to intercept...
            if (this.getInterceptRequest()) {
                this._form.addError("Intercepting request: GUI -> SRV");
                ProxyEdit pe = new ProxyEdit(this._form, "Intercept Request (GUI -> SRV)", dd.array());
                zz = pe.getData();
            }
            else {
                ProxyEdit pe = new ProxyEdit(this._form, "Intercept Request (GUI -> SRV)", dd.array());
                zz = pe.getByPassData();
            }
            // Worker process here - we process the data...
            this._form.addError("Forwarding request: GUI -> SRV");
            this._lwork.processLocalData(this, socketchannel, zz, zz.length);
        }
    }

    private void doLocalWrite(SelectionKey key) throws Exception {
        SocketChannel socketchannel = (SocketChannel) key.channel();
        synchronized (this._ldata) {
            List queue = (List) this._ldata.get(socketchannel);
            while (!queue.isEmpty()) {
                ByteBuffer bb = (ByteBuffer) queue.get(0);
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

    public void run() {
        this._isrunning = true;
        while (this._isrunning) {
            // We first poll the local socket...
            try {
                synchronized (this._lchng) {
                    Iterator changes = this._lchng.iterator();
                    while (changes.hasNext()) {
                        SockRequest change = (SockRequest) changes.next();
                        switch (change.type) {
                            case SockRequest.CHANGEOPS:
                                SelectionKey key = change.socket.keyFor(this._lselc);
                                key.interestOps(change.ops);
                        }
                    }
                    this._lchng.clear();
                }
                this._lselc.select();
                Iterator selectedkeys = this._lselc.selectedKeys().iterator();
                while (selectedkeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedkeys.next();
                    selectedkeys.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        this.doLocalAccept(key);
                    } else if (key.isReadable()) {
                        this.doLocalRead(key);
                    } else if (key.isWritable()) {
                        this.doLocalWrite(key);
                    }
                }
            } catch (Exception e) {
            }
        }
    }
}
