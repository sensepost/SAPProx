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

import com.sensepost.SAPProx.gui.ProxyEdit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.script.ScriptEngineManager;
import javax.swing.JMenuItem;

/**
 *
 * @author ian
 */
public class ScriptManager {

    protected String _scriptsdir = "";
    protected ProxyEdit _form = null;
    protected ScriptInputAPI _scriptapi;
    protected ExecutorService _scriptexec = Executors.newCachedThreadPool();
    protected ScriptEngineManager _scriptfactory = new ScriptEngineManager();

    public ScriptManager(ProxyEdit f, String s) {
        this._form = f;
        this._scriptsdir = s;
        this._scriptapi = new ScriptInputAPI(this._form);
    }

    public synchronized ScriptInputAPI getApi() {
        return this._scriptapi;
    }

    public synchronized String getScriptsDir() {
        return this._scriptsdir;
    }

    public synchronized ScriptEngineManager getFactory() {
        return this._scriptfactory;
    }

    public ArrayList<JMenuItem> getMenuItems() {
        ArrayList<JMenuItem> result = new ArrayList<JMenuItem>();
        for (String filename : this.FilesFromDir(_scriptsdir)) {
            JMenuItem miPlugin = new JMenuItem();
            miPlugin.setText(filename);
            miPlugin.setName(filename);
            miPlugin.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    pluginMenuEvent(evt);
                }
            });
            result.add(miPlugin);
        }
        return result;
    }

    public synchronized void setOutputByte(int[] ia) {
        System.out.println("OUTPUTBYTE");
        byte[] b = new byte[ia.length];
        for (int i = 0; i < ia.length; i++) {
            System.out.println(ia[i]);
            b[i] = (byte)ia[i];
        }
        this._form.setDecByte(b);
    }

    public synchronized void addOutput(String message) {
        System.out.println(message);
        //this._form.addOutput(message);
    }

    public void executeScript(String scriptName) throws FileNotFoundException, javax.script.ScriptException {
        ScriptWorker scriptRunner = new ScriptWorker(scriptName, this);
        this._scriptexec.execute(scriptRunner);
    }

    public void scriptDone() {
        System.out.println("WE ARE DONE!");
    }

    public ArrayList<String> FilesFromDir(String Dir) {
        File dir = new File(Dir);
        File[] files = dir.listFiles();

        ArrayList<String> fileNames = new ArrayList<String>();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    fileNames.add(files[i].getName());
                }
            }
        }
        return fileNames;
    }

    public void pluginMenuEvent(ActionEvent evt) {
        if (evt.getSource().getClass() == JMenuItem.class) {
            try {
                String scriptName = (((JMenuItem) evt.getSource()).getName());
                this.executeScript(scriptName);
            } catch (Exception e) {
            }
        }
    }
}
