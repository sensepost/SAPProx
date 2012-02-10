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
package com.sensepost.SAPProxy.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import javax.script.ScriptEngine;
import javax.swing.SwingWorker;

/**
 *
 * @author ian
 */
public class ScriptWorker extends SwingWorker {

    private String _scriptname = "";
    private ScriptManager _parent = null;
    private BufferedReader _input = null;
    private ScriptEngine _engine = null;

    public ScriptWorker(String scriptName, ScriptManager parent) throws FileNotFoundException {
        super();
        this._scriptname = scriptName;
        this._parent = parent;
        File file = new File(this._parent.getScriptsDir() + File.separatorChar + this._scriptname);
        this._engine = this._parent.getFactory().getEngineByExtension(getFileExtension(this._scriptname));
        this._input = new BufferedReader(new FileReader(file));
        _engine.put("controller", this._parent);
        _engine.put("api", this._parent.getApi());
    }

    public String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        String ext = filename.substring(idx + 1);
        return ext;
    }

    @Override
    protected String doInBackground() throws javax.script.ScriptException {
        this._parent.addOutput("Executing script here");
        this._engine.eval(_input);
        return "Done";
    }

    @Override
    protected void done() {
        // Call prent script done.scriptDone();
        this._parent.scriptDone();
    }
}
