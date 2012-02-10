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
package com.sensepost.SAPProx.datatype;

/**
 *
 * @author ian
 */
public class ProxyData {

    public static final int FROM_CLIENT = 0;
    public static final int FROM_SERVER = 1;

    private int _dir;
    private byte[] _com;
    private byte[] _dec;

    public ProxyData(int i, byte[] c, byte[] d) {
        this._dir = i;
        this._com = c;
        this._dec = d;
    }

    public int getDirection() {
        return this._dir;
    }

    public byte[] getCompressed() {
        return this._com;
    }

    public byte[] getDecompressed() {
        return this._dec;
    }
}
