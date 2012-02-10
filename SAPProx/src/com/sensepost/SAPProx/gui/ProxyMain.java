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
package com.sensepost.SAPProx.gui;

import com.sensepost.SAPProx.datatype.ProxyList;
import com.sensepost.SAPProx.jni.JniInterface;
import com.sensepost.SAPProx.communication.LocalEndPointMain;
import com.sensepost.SAPProx.datatype.KnuthPatternMatching;
import com.sensepost.SAPProx.datatype.ProxyData;
import com.sensepost.SAPProx.datatype.SockData;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author ian
 */
public class ProxyMain extends javax.swing.JFrame {

    private JniInterface _decoder;
    private Thread _proxythread;
    private LocalEndPointMain _proxyserver;
    private boolean _proxyrunning;
    private ArrayList _proxydata;
    private ProxyList _proxylist;
    private ProxyList _loglist;
    private boolean _reqintercept;
    private boolean _resintercept;
    private String _scriptsdir;

    /** Creates new form SAPProx */
    public ProxyMain() {
        this._decoder = new JniInterface();
        this._proxylist = new ProxyList();
        this._proxylist.setUpdate();
        this._loglist = new ProxyList();
        this._loglist.setUpdate();
        initComponents();
        this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        this._proxyrunning = false;
        this._proxydata = new ArrayList();
        this.tpnl_main.setSelectedIndex(1);
        this.tpn_decompressed.setEnabledAt(1, false);
        this.tpn_decompressed.setEnabledAt(2, false);
        this.tpn_decompressed.setEnabledAt(3, false);
        this.tpn_decompressed.setEnabledAt(4, false);
        this.addError("SAPProxy Initialised");
        this._scriptsdir = "";
    }

    public synchronized String getScriptsDir() {
        return this._scriptsdir;
    }
    
    public synchronized void addError(String s) {
        Date dnow = new Date();
        String e = dnow.toString() + " - " + s;
        this._loglist.addElement(e);
        this._loglist.setUpdate();
    }
    
    private void setBytes(int[] ia) {
        int[] PARAMS = {0x50, 0x41, 0x52, 0x41, 0x4d, 0x53, 0x03, 0x01, 0x03};
        int[] RFCQUEUE = {0x52, 0x46, 0x43, 0x5f, 0x51, 0x55, 0x45, 0x55, 0x45, 0x03, 0x01, 0x03};
        int[] VARS = {0x56, 0x41, 0x52, 0x53, 0x03, 0x01, 0x03};
        int[] VERBS = {0x56, 0x45, 0x52, 0x42, 0x53, 0x03, 0x01, 0x03};
        int[] MAGIC = {0x1f, 0x9d};

        int paramstart = KnuthPatternMatching.indexOf(ia, PARAMS, 0);
        int rfcqueuestart = KnuthPatternMatching.indexOf(ia, RFCQUEUE, 0);
        int varsstart = KnuthPatternMatching.indexOf(ia, VARS, 0);
        int verbsstart = KnuthPatternMatching.indexOf(ia, VERBS, 0);

        boolean b_params = false;
        boolean b_rfc = false;
        boolean b_vars = false;
        boolean b_verbs = false;

        if (paramstart > -1) {
            // Extract Stream over here...
            int n_m = KnuthPatternMatching.indexOf(ia, MAGIC, paramstart);
            if (n_m > -1) {
                n_m = n_m - 17;
                int[] params = new int[ia.length - n_m];
                System.arraycopy(ia, n_m, params, 0, ia.length - n_m);
                int[] pdec = this._decoder.doDecompress(params);
                byte[] _params = new byte[pdec.length];
                for (int z = 0; z < pdec.length; z++) {
                    _params[z] = (byte) pdec[z];
                }
                this.hex_params.setByteContent(_params);
                b_params = true;
            }
        }
        if (rfcqueuestart > -1) {
            int n_m = KnuthPatternMatching.indexOf(ia, MAGIC, rfcqueuestart);
            if (n_m > -1) {
                n_m = n_m - 17;
                int[] rfc = new int[ia.length - n_m];
                System.arraycopy(ia, n_m, rfc, 0, ia.length - n_m);
                int[] pdec = this._decoder.doDecompress(rfc);
                byte[] _rfcqueue = new byte[pdec.length];
                for (int z = 0; z < pdec.length; z++) {
                    _rfcqueue[z] = (byte) pdec[z];
                }
                this.hex_rfcqueue.setByteContent(_rfcqueue);
                b_rfc = true;
            }
        }
        if (varsstart > -1) {
            int n_m = KnuthPatternMatching.indexOf(ia, MAGIC, varsstart);
            if (n_m > -1) {
                n_m = n_m - 17;
                int[] vars = new int[ia.length - n_m];
                System.arraycopy(ia, n_m, vars, 0, ia.length - n_m);
                int[] pdec = this._decoder.doDecompress(vars);
                byte[] _vars = new byte[pdec.length];
                for (int z = 0; z < pdec.length; z++) {
                    _vars[z] = (byte) pdec[z];
                }
                this.hex_vars.setByteContent(_vars);
                b_vars = true;
            }
        }
        if (verbsstart > -1) {
            int n_m = KnuthPatternMatching.indexOf(ia, MAGIC, verbsstart);
            if (n_m > -1) {
                n_m = n_m - 17;
                int[] verbs = new int[ia.length - n_m];
                System.arraycopy(ia, n_m, verbs, 0, ia.length - n_m);
                int[] pdec = this._decoder.doDecompress(verbs);
                byte[] _verbs = new byte[pdec.length];
                for (int z = 0; z < pdec.length; z++) {
                    _verbs[z] = (byte) pdec[z];
                }
                this.hex_verbs.setByteContent(_verbs);
                b_verbs = true;
            }
        }
        this.tpn_decompressed.setEnabledAt(1, b_params);
        this.tpn_decompressed.setEnabledAt(2, b_rfc);
        this.tpn_decompressed.setEnabledAt(3, b_verbs);
        this.tpn_decompressed.setEnabledAt(4, b_vars);
    }

    public synchronized void setReqIntercept(boolean b) {
        this.chk_reqintercept.setSelected(b);
        this._reqintercept = b;
    }

    public synchronized void setResIntercept(boolean b) {
        this.chk_resintercept.setSelected(b);
        this._resintercept = b;
    }

    public boolean getReqIntercept() {
        return this._reqintercept;
    }

    public boolean getResIntercept() {
        return this._resintercept;
    }

    public void addData(SockData data) {
        synchronized (this._proxydata) {
            int i = data.getDirection();
            byte[] c = data.getData();
            byte[] x;
            int[] ic = new int[c.length];
            for (int j = 0; j < c.length; j++) {
                ic[j] = (int) ((int) c[j] & 0xff);
            }
            if (ic.length > 18) {
                if (ic[17] == 31 && ic[18] == 157) {
                    int[] d = this._decoder.doDecompress(ic);
                    x = new byte[d.length];
                    for (int j = 0; j < d.length; j++) {
                        x[j] = (byte) d[j];
                    }
                } else {
                    String v = "ERROR : MESSAGE NOT COMPRESSED";
                    x = v.getBytes();
                }
            } else {
                String v = "ERROR : MESSAGE NOT COMPRESSED";
                x = v.getBytes();
            }
            int n = this._proxydata.size();
            String filename = this.txt_log.getText();
            this._proxydata.add(new ProxyData(i, c, x));
            String outstuff = "";
            if (data.getDirection() == 0) {
                outstuff += "GUI->SRV:";
                filename += "GUI";
            } else {
                outstuff += "SRV->GUI:";
                filename += "SRV";
            }

            outstuff += "CMP:" + c.length + " bytes:DEC:" + x.length + " bytes";


            if (this.chk_log.isSelected()) {
                try {
                    FileOutputStream fo = new FileOutputStream(filename + "-" + Integer.toString(n) + "-COMPRESSED");
                    DataOutputStream dt = new DataOutputStream(fo);
                    dt.write(c);
                    dt.close();
                    fo.close();
                    fo = new FileOutputStream(filename + "-" + Integer.toString(n) + "-DECOMPRESSED");
                    dt = new DataOutputStream(fo);
                    dt.write(x);
                    dt.close();
                    fo.close();
                } catch (Exception e) {
                    this.addError("Error logging to file:" + e.toString());
                }
            }
            this._proxylist.addElement(outstuff);
            this._proxylist.setUpdate();
            this.addError("SAP Message Received:" + outstuff);
        }
        // Refresh method here...
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mnu_loglist = new javax.swing.JPopupMenu();
        mni_logsave = new javax.swing.JMenuItem();
        mni_logclear = new javax.swing.JMenuItem();
        tpnl_main = new javax.swing.JTabbedPane();
        pnl_main = new javax.swing.JPanel();
        spn_proxy = new javax.swing.JSplitPane();
        scrl_list = new javax.swing.JScrollPane();
        lst_cons = new javax.swing.JList();
        tpn_prox = new javax.swing.JTabbedPane();
        tpn_decompressed = new javax.swing.JTabbedPane();
        pnl_decompressed = new javax.swing.JPanel();
        hex_dec = new at.HexLib.library.JHexEditor();
        pnl_params = new javax.swing.JPanel();
        hex_params = new at.HexLib.library.JHexEditor();
        pnl_rfcqueue = new javax.swing.JPanel();
        hex_rfcqueue = new at.HexLib.library.JHexEditor();
        pnl_verbs = new javax.swing.JPanel();
        hex_verbs = new at.HexLib.library.JHexEditor();
        pnl_vars = new javax.swing.JPanel();
        hex_vars = new at.HexLib.library.JHexEditor();
        pnl_compressed = new javax.swing.JPanel();
        hex_com = new at.HexLib.library.JHexEditor();
        pnl_config = new javax.swing.JPanel();
        lbl_laddr = new javax.swing.JLabel();
        txt_laddr = new javax.swing.JTextField();
        lbl_lport = new javax.swing.JLabel();
        txt_lport = new javax.swing.JTextField();
        lbl_raddr = new javax.swing.JLabel();
        txt_raddr = new javax.swing.JTextField();
        lbl_rport = new javax.swing.JLabel();
        txt_rport = new javax.swing.JTextField();
        lbl_log = new javax.swing.JLabel();
        txt_log = new javax.swing.JTextField();
        lbl_script = new javax.swing.JLabel();
        txt_script = new javax.swing.JTextField();
        chk_log = new javax.swing.JCheckBox();
        chk_reqintercept = new javax.swing.JCheckBox();
        chk_resintercept = new javax.swing.JCheckBox();
        btn_start = new javax.swing.JButton();
        btn_stop = new javax.swing.JButton();
        pnl_log = new javax.swing.JPanel();
        spn_log = new javax.swing.JScrollPane();
        lst_log = new javax.swing.JList();

        mni_logsave.setText("Save Log");
        mni_logsave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mni_logsaveActionPerformed(evt);
            }
        });
        mnu_loglist.add(mni_logsave);

        mni_logclear.setText("Clear List");
        mni_logclear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mni_logclearActionPerformed(evt);
            }
        });
        mnu_loglist.add(mni_logclear);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SAP Proxy");

        tpnl_main.setComponentPopupMenu(mnu_loglist);

        spn_proxy.setDividerLocation(271);
        spn_proxy.setOneTouchExpandable(true);

        lst_cons.setFont(new java.awt.Font("Courier New", 0, 12));
        lst_cons.setModel(this._proxylist);
        lst_cons.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lst_cons.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lst_consValueChanged(evt);
            }
        });
        scrl_list.setViewportView(lst_cons);

        spn_proxy.setLeftComponent(scrl_list);

        org.jdesktop.layout.GroupLayout pnl_decompressedLayout = new org.jdesktop.layout.GroupLayout(pnl_decompressed);
        pnl_decompressed.setLayout(pnl_decompressedLayout);
        pnl_decompressedLayout.setHorizontalGroup(
            pnl_decompressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 679, Short.MAX_VALUE)
            .add(pnl_decompressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_dec, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 679, Short.MAX_VALUE))
        );
        pnl_decompressedLayout.setVerticalGroup(
            pnl_decompressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 464, Short.MAX_VALUE)
            .add(pnl_decompressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, hex_dec, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE))
        );

        tpn_decompressed.addTab("Complete Message", pnl_decompressed);

        org.jdesktop.layout.GroupLayout pnl_paramsLayout = new org.jdesktop.layout.GroupLayout(pnl_params);
        pnl_params.setLayout(pnl_paramsLayout);
        pnl_paramsLayout.setHorizontalGroup(
            pnl_paramsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 679, Short.MAX_VALUE)
            .add(pnl_paramsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_params, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 679, Short.MAX_VALUE))
        );
        pnl_paramsLayout.setVerticalGroup(
            pnl_paramsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 464, Short.MAX_VALUE)
            .add(pnl_paramsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_params, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE))
        );

        tpn_decompressed.addTab("PARAMS", pnl_params);

        org.jdesktop.layout.GroupLayout pnl_rfcqueueLayout = new org.jdesktop.layout.GroupLayout(pnl_rfcqueue);
        pnl_rfcqueue.setLayout(pnl_rfcqueueLayout);
        pnl_rfcqueueLayout.setHorizontalGroup(
            pnl_rfcqueueLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 679, Short.MAX_VALUE)
            .add(pnl_rfcqueueLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_rfcqueue, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 679, Short.MAX_VALUE))
        );
        pnl_rfcqueueLayout.setVerticalGroup(
            pnl_rfcqueueLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 464, Short.MAX_VALUE)
            .add(pnl_rfcqueueLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_rfcqueue, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE))
        );

        tpn_decompressed.addTab("RFC_QUEUE", pnl_rfcqueue);

        org.jdesktop.layout.GroupLayout pnl_verbsLayout = new org.jdesktop.layout.GroupLayout(pnl_verbs);
        pnl_verbs.setLayout(pnl_verbsLayout);
        pnl_verbsLayout.setHorizontalGroup(
            pnl_verbsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 679, Short.MAX_VALUE)
            .add(pnl_verbsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_verbs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 679, Short.MAX_VALUE))
        );
        pnl_verbsLayout.setVerticalGroup(
            pnl_verbsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 464, Short.MAX_VALUE)
            .add(pnl_verbsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_verbs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE))
        );

        tpn_decompressed.addTab("VERBS", pnl_verbs);

        org.jdesktop.layout.GroupLayout pnl_varsLayout = new org.jdesktop.layout.GroupLayout(pnl_vars);
        pnl_vars.setLayout(pnl_varsLayout);
        pnl_varsLayout.setHorizontalGroup(
            pnl_varsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 679, Short.MAX_VALUE)
            .add(pnl_varsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_vars, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 679, Short.MAX_VALUE))
        );
        pnl_varsLayout.setVerticalGroup(
            pnl_varsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 464, Short.MAX_VALUE)
            .add(pnl_varsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_vars, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE))
        );

        tpn_decompressed.addTab("VARS", pnl_vars);

        tpn_prox.addTab("Decompressed", tpn_decompressed);

        org.jdesktop.layout.GroupLayout pnl_compressedLayout = new org.jdesktop.layout.GroupLayout(pnl_compressed);
        pnl_compressed.setLayout(pnl_compressedLayout);
        pnl_compressedLayout.setHorizontalGroup(
            pnl_compressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 700, Short.MAX_VALUE)
            .add(pnl_compressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_com, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE))
        );
        pnl_compressedLayout.setVerticalGroup(
            pnl_compressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 510, Short.MAX_VALUE)
            .add(pnl_compressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(hex_com, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 510, Short.MAX_VALUE))
        );

        tpn_prox.addTab("Compressed", pnl_compressed);

        spn_proxy.setRightComponent(tpn_prox);

        org.jdesktop.layout.GroupLayout pnl_mainLayout = new org.jdesktop.layout.GroupLayout(pnl_main);
        pnl_main.setLayout(pnl_mainLayout);
        pnl_mainLayout.setHorizontalGroup(
            pnl_mainLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(spn_proxy, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1003, Short.MAX_VALUE)
        );
        pnl_mainLayout.setVerticalGroup(
            pnl_mainLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(spn_proxy, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)
        );

        tpnl_main.addTab("SAP Connections & Messages", pnl_main);

        lbl_laddr.setText("Listen Address");

        txt_laddr.setText("127.0.0.1");

        lbl_lport.setText("Listen Port");

        txt_lport.setText("3200");

        lbl_raddr.setText("Remote Address");

        txt_raddr.setText("192.168.1.10");

        lbl_rport.setText("Remote Port");

        txt_rport.setText("3200");

        lbl_log.setText("Log Directory");

        lbl_script.setText("Script Directory");

        txt_script.setText("/Specify/Directory/Here");

        chk_log.setText("Log to file");

        chk_reqintercept.setText("Intercept Requests");
        chk_reqintercept.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chk_reqinterceptActionPerformed(evt);
            }
        });

        chk_resintercept.setText("Intercept Responses");
        chk_resintercept.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chk_resinterceptActionPerformed(evt);
            }
        });

        btn_start.setText("Start Proxy");
        btn_start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_startActionPerformed(evt);
            }
        });

        btn_stop.setText("Stop Proxy");
        btn_stop.setEnabled(false);
        btn_stop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_stopActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout pnl_configLayout = new org.jdesktop.layout.GroupLayout(pnl_config);
        pnl_config.setLayout(pnl_configLayout);
        pnl_configLayout.setHorizontalGroup(
            pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_configLayout.createSequentialGroup()
                .addContainerGap()
                .add(pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(chk_resintercept)
                    .add(chk_log)
                    .add(chk_reqintercept)
                    .add(btn_start)
                    .add(btn_stop)
                    .add(pnl_configLayout.createSequentialGroup()
                        .add(pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(lbl_laddr)
                            .add(lbl_lport)
                            .add(lbl_raddr)
                            .add(lbl_rport)
                            .add(lbl_log)
                            .add(lbl_script))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(txt_script, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 851, Short.MAX_VALUE)
                            .add(txt_laddr, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 851, Short.MAX_VALUE)
                            .add(txt_lport, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 851, Short.MAX_VALUE)
                            .add(txt_raddr, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 851, Short.MAX_VALUE)
                            .add(txt_rport, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 851, Short.MAX_VALUE)
                            .add(txt_log, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 851, Short.MAX_VALUE))))
                .addContainerGap())
        );
        pnl_configLayout.setVerticalGroup(
            pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_configLayout.createSequentialGroup()
                .add(20, 20, 20)
                .add(pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lbl_laddr)
                    .add(txt_laddr, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lbl_lport)
                    .add(txt_lport, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lbl_raddr)
                    .add(txt_raddr, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lbl_rport)
                    .add(txt_rport, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lbl_log)
                    .add(txt_log, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pnl_configLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lbl_script)
                    .add(txt_script, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(chk_log)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(chk_reqintercept)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(chk_resintercept)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(btn_start)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(btn_stop)
                .addContainerGap(191, Short.MAX_VALUE))
        );

        tpnl_main.addTab("Configuration & Control", pnl_config);

        lst_log.setModel(this._loglist);
        lst_log.setComponentPopupMenu(mnu_loglist);
        spn_log.setViewportView(lst_log);

        org.jdesktop.layout.GroupLayout pnl_logLayout = new org.jdesktop.layout.GroupLayout(pnl_log);
        pnl_log.setLayout(pnl_logLayout);
        pnl_logLayout.setHorizontalGroup(
            pnl_logLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 1003, Short.MAX_VALUE)
            .add(pnl_logLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, spn_log, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1003, Short.MAX_VALUE))
        );
        pnl_logLayout.setVerticalGroup(
            pnl_logLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 560, Short.MAX_VALUE)
            .add(pnl_logLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(spn_log, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE))
        );

        tpnl_main.addTab("Log", pnl_log);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tpnl_main, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1024, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tpnl_main, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 606, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_startActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_startActionPerformed
        this._scriptsdir = this.txt_script.getText();
        String lh = this.txt_laddr.getText();
        int lp = Integer.parseInt(this.txt_lport.getText());
        String rh = this.txt_raddr.getText();
        int rp = Integer.parseInt(this.txt_rport.getText());
        try {
            this._proxyserver = new LocalEndPointMain(this, lh, lp, rh, rp);
            this._proxythread = new Thread(this._proxyserver);
            this._proxythread.start();
            this._proxyrunning = true;
            this.btn_start.setEnabled(false);
            this.btn_stop.setEnabled(true);
            this.addError("SAP Proxy listening on " + lh + ":" + lp + " forwarding to " + rh + ":" + rp);
        } catch (Exception e) {
            this.addError("Error starting proxy listener:" + e.toString());
            return;
        }
        this.tpnl_main.setSelectedIndex(0);
    }//GEN-LAST:event_btn_startActionPerformed

    private void btn_stopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_stopActionPerformed
        try {
            this.btn_start.setEnabled(true);
            this.btn_stop.setEnabled(false);
            this._proxyrunning = false;
            this._proxyserver.stopProxyServer();
            this._proxythread.stop();
            this.addError("SAP Proxy stopped");
        } catch (Exception e) {
            this.addError("Error stopping SAP Proxy: " + e.toString());
        }
    }//GEN-LAST:event_btn_stopActionPerformed

    private void lst_consValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lst_consValueChanged
        try {
            ProxyData spd = (ProxyData) this._proxydata.get(this.lst_cons.getSelectedIndex());
            byte[] com = spd.getCompressed();
            byte[] dec = spd.getDecompressed();
            hex_com.setByteContent(com);
            hex_dec.setByteContent(dec);
            this.tpn_decompressed.setSelectedIndex(0);
            int[] ia = new int[dec.length];
            for (int i = 0; i < dec.length; i++) {
                ia[i] = (int) dec[i] & 0xff;
            }
            this.setBytes(ia);
            if ((int)dec[0] == 69 && (int)dec[1] == 82 && (int)dec[2] == 82 && (int)dec[3] == 79) {
                this.tpn_prox.setSelectedIndex(1);
            }
            else {
                this.tpn_prox.setSelectedIndex(0);
            }
        } catch (Exception e) {
        }
    }//GEN-LAST:event_lst_consValueChanged

    private void chk_reqinterceptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chk_reqinterceptActionPerformed
        if (this.chk_reqintercept.isSelected()) {
            this._reqintercept = true;
        } else {
            this._reqintercept = false;
        }
    }//GEN-LAST:event_chk_reqinterceptActionPerformed

    private void chk_resinterceptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chk_resinterceptActionPerformed
        if (this.chk_resintercept.isSelected()) {
            this._resintercept = true;
        } else {
            this._resintercept = false;
        }
    }//GEN-LAST:event_chk_resinterceptActionPerformed

    private void mni_logclearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mni_logclearActionPerformed
        this._loglist.clear();
        this._loglist.setUpdate();
    }//GEN-LAST:event_mni_logclearActionPerformed

    private void mni_logsaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mni_logsaveActionPerformed
        JFileChooser f_chooser = new JFileChooser();
        int status = f_chooser.showSaveDialog(this);
        if (status == JFileChooser.APPROVE_OPTION) {
            File f_filename = f_chooser.getSelectedFile();
            try {
                FileWriter f = new FileWriter(f_filename);
                BufferedWriter w = new BufferedWriter(f);
                for (int i = 0; i < this._loglist.getSize(); i++) {
                    String s = this._loglist.getElementAt(i).toString();
                    w.write(s);
                    w.newLine();
                }
                w.close();
                f.close();
                addError("Log saved to file");
                JOptionPane.showMessageDialog(this, "Log Saved", "Saved", JOptionPane.PLAIN_MESSAGE);
            } catch (Exception e) {
                addError("Error:" + e.toString());
                JOptionPane.showMessageDialog(this, "Could not save the log file:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Save log cancelled", "Save Log", JOptionPane.PLAIN_MESSAGE);
            return;
        }
    }//GEN-LAST:event_mni_logsaveActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new ProxyMain().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_start;
    private javax.swing.JButton btn_stop;
    private javax.swing.JCheckBox chk_log;
    private javax.swing.JCheckBox chk_reqintercept;
    private javax.swing.JCheckBox chk_resintercept;
    private at.HexLib.library.JHexEditor hex_com;
    private at.HexLib.library.JHexEditor hex_dec;
    private at.HexLib.library.JHexEditor hex_params;
    private at.HexLib.library.JHexEditor hex_rfcqueue;
    private at.HexLib.library.JHexEditor hex_vars;
    private at.HexLib.library.JHexEditor hex_verbs;
    private javax.swing.JLabel lbl_laddr;
    private javax.swing.JLabel lbl_log;
    private javax.swing.JLabel lbl_lport;
    private javax.swing.JLabel lbl_raddr;
    private javax.swing.JLabel lbl_rport;
    private javax.swing.JLabel lbl_script;
    private javax.swing.JList lst_cons;
    private javax.swing.JList lst_log;
    private javax.swing.JMenuItem mni_logclear;
    private javax.swing.JMenuItem mni_logsave;
    private javax.swing.JPopupMenu mnu_loglist;
    private javax.swing.JPanel pnl_compressed;
    private javax.swing.JPanel pnl_config;
    private javax.swing.JPanel pnl_decompressed;
    private javax.swing.JPanel pnl_log;
    private javax.swing.JPanel pnl_main;
    private javax.swing.JPanel pnl_params;
    private javax.swing.JPanel pnl_rfcqueue;
    private javax.swing.JPanel pnl_vars;
    private javax.swing.JPanel pnl_verbs;
    private javax.swing.JScrollPane scrl_list;
    private javax.swing.JScrollPane spn_log;
    private javax.swing.JSplitPane spn_proxy;
    private javax.swing.JTabbedPane tpn_decompressed;
    private javax.swing.JTabbedPane tpn_prox;
    private javax.swing.JTabbedPane tpnl_main;
    private javax.swing.JTextField txt_laddr;
    private javax.swing.JTextField txt_log;
    private javax.swing.JTextField txt_lport;
    private javax.swing.JTextField txt_raddr;
    private javax.swing.JTextField txt_rport;
    private javax.swing.JTextField txt_script;
    // End of variables declaration//GEN-END:variables
}
