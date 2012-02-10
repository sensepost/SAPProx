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

import com.sensepost.SAPProx.datatype.KnuthPatternMatching;
import com.sensepost.SAPProx.jni.JniInterface;
import com.sensepost.SAPProxy.api.ScriptManager;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 *
 * @author ian
 */
public class ProxyEdit extends JDialog {

    private ProxyMain _form;
    private String _titl;
    private byte[] _comp;
    private byte[] _deco;
    private byte[] _retr;
    private JniInterface _code;
    private ScriptManager _scriptman;

    /** Creates new form ProxyEdit */
    public ProxyEdit(ProxyMain f, String t, byte[] b) {
        super();
        this._form = f;
        this._titl = t;
        this._comp = b;
        this._code = new JniInterface();
        initComponents();
        this.mni_creqi.setSelected(this._form.getReqIntercept());
        this.mni_cresi.setSelected(this._form.getResIntercept());
        this.mni_dreqi.setSelected(this._form.getReqIntercept());
        this.mni_dresi.setSelected(this._form.getResIntercept());
        this.setTitle(this._titl);
        this._scriptman = new ScriptManager(this, this._form.getScriptsDir());
        for (JMenuItem mi:this._scriptman.getMenuItems()) {
            this.mnu_dscripts.add(mi);
        }
        // We've now got to...
        // a: Get the decompressed data by type-casting to int[], decompressing and type-casting to byte[]
        int[] ic = new int[this._comp.length];
        for (int j = 0; j < this._comp.length; j++) {
            ic[j] = (int) ((int) this._comp[j] & 0xff);
        }
        if (ic.length > 18) {
            if (ic[17] == 31 && ic[18] == 157) {
                int[] d = this._code.doDecompress(ic);
                this._deco = new byte[d.length];
                for (int j = 0; j < d.length; j++) {
                    this._deco[j] = (byte) d[j];
                }
            } else {
                String v = "ERROR : MESSAGE NOT COMPRESSED";
                this.tpn_edit.setSelectedIndex(1);
                this._deco = v.getBytes();
            }
        } else {
            String v = "ERROR : MESSAGE NOT COMPRESSED";
            this.tpn_edit.setSelectedIndex(1);
            this._deco = v.getBytes();
        }
        this.setModal(true);
        // b: We set the hex editor byte arrays... :)
        this.he_com.setByteContent(this._comp);
        this.he_dec.setByteContent(this._deco);
        setSubBytes(this.he_dec.getByteContent());
    }

    public void setDecByte(byte[] b) {
        this.he_dec.setByteContent(b);
        setSubBytes(b);
    }
    
    private void setSubBytes(byte[] b) {
        int[] ia = new int[b.length];
        for (int i = 0; i < b.length; i++) {
            ia[i] = (int)b[i] & 0xff;
        }
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
                int[] pdec = this._code.doDecompress(params);
                byte[] _params = new byte[pdec.length];
                for (int z = 0; z < pdec.length; z++) {
                    _params[z] = (byte) pdec[z];
                }
                this.he_params.setByteContent(_params);
                b_params = true;
            }
        }
        if (rfcqueuestart > -1) {
            int n_m = KnuthPatternMatching.indexOf(ia, MAGIC, rfcqueuestart);
            if (n_m > -1) {
                n_m = n_m - 17;
                int[] rfc = new int[ia.length - n_m];
                System.arraycopy(ia, n_m, rfc, 0, ia.length - n_m);
                int[] pdec = this._code.doDecompress(rfc);
                byte[] _rfcqueue = new byte[pdec.length];
                for (int z = 0; z < pdec.length; z++) {
                    _rfcqueue[z] = (byte) pdec[z];
                }
                this.he_rfcqueue.setByteContent(_rfcqueue);
                b_rfc = true;
            }
        }
        if (varsstart > -1) {
            int n_m = KnuthPatternMatching.indexOf(ia, MAGIC, varsstart);
            if (n_m > -1) {
                n_m = n_m - 17;
                int[] vars = new int[ia.length - n_m];
                System.arraycopy(ia, n_m, vars, 0, ia.length - n_m);
                int[] pdec = this._code.doDecompress(vars);
                byte[] _vars = new byte[pdec.length];
                for (int z = 0; z < pdec.length; z++) {
                    _vars[z] = (byte) pdec[z];
                }
                this.he_vars.setByteContent(_vars);
                b_vars = true;
            }
        }
        if (verbsstart > -1) {
            int n_m = KnuthPatternMatching.indexOf(ia, MAGIC, verbsstart);
            if (n_m > -1) {
                n_m = n_m - 17;
                int[] verbs = new int[ia.length - n_m];
                System.arraycopy(ia, n_m, verbs, 0, ia.length - n_m);
                int[] pdec = this._code.doDecompress(verbs);
                byte[] _verbs = new byte[pdec.length];
                for (int z = 0; z < pdec.length; z++) {
                    _verbs[z] = (byte) pdec[z];
                }
                this.he_verbs.setByteContent(_verbs);
                b_verbs = true;
            }
        }
        this.tpn_decompressed.setEnabledAt(1, b_params);
        this.tpn_decompressed.setEnabledAt(2, b_rfc);
        this.tpn_decompressed.setEnabledAt(3, b_verbs);
        this.tpn_decompressed.setEnabledAt(4, b_vars);
    }

    public byte[] getData() {
        this.setVisible(true);
        return this._retr;
    }

    public byte[] getByPassData() {
        return this._comp;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mnu_dmore = new javax.swing.JPopupMenu();
        mnu_dintercept = new javax.swing.JMenu();
        mni_dreqi = new javax.swing.JCheckBoxMenuItem();
        mni_dresi = new javax.swing.JCheckBoxMenuItem();
        mnu_dscripts = new javax.swing.JMenu();
        mnu_cmore = new javax.swing.JPopupMenu();
        mnu_cintercept = new javax.swing.JMenu();
        mni_creqi = new javax.swing.JCheckBoxMenuItem();
        mni_cresi = new javax.swing.JCheckBoxMenuItem();
        tpn_edit = new javax.swing.JTabbedPane();
        pnl_decompressed = new javax.swing.JPanel();
        tpn_decompressed = new javax.swing.JTabbedPane();
        pnl_message = new javax.swing.JPanel();
        he_dec = new at.HexLib.library.JHexEditor();
        pnl_dopt = new javax.swing.JPanel();
        btn_dsend = new javax.swing.JButton();
        btn_dload = new javax.swing.JButton();
        btn_dextend = new javax.swing.JButton();
        pnl_params = new javax.swing.JPanel();
        he_params = new at.HexLib.library.JHexEditor();
        pnl_paramopt = new javax.swing.JPanel();
        btn_paramupdate = new javax.swing.JButton();
        pnl_rfc = new javax.swing.JPanel();
        he_rfcqueue = new at.HexLib.library.JHexEditor();
        pbl_rfcopt = new javax.swing.JPanel();
        btn_rfcupdate = new javax.swing.JButton();
        pnl_verbs = new javax.swing.JPanel();
        he_verbs = new at.HexLib.library.JHexEditor();
        pnl_verbopt = new javax.swing.JPanel();
        btn_verbupdate = new javax.swing.JButton();
        pnl_vars = new javax.swing.JPanel();
        he_vars = new at.HexLib.library.JHexEditor();
        pnl_varopt = new javax.swing.JPanel();
        btn_varupdate = new javax.swing.JButton();
        pnl_compressed = new javax.swing.JPanel();
        he_com = new at.HexLib.library.JHexEditor();
        pnl_copt = new javax.swing.JPanel();
        btn_csend = new javax.swing.JButton();
        btn_cload = new javax.swing.JButton();
        btn_cextend = new javax.swing.JButton();

        mnu_dintercept.setText("Interception");

        mni_dreqi.setSelected(true);
        mni_dreqi.setText("Intercept Requests");
        mni_dreqi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mni_dreqiActionPerformed(evt);
            }
        });
        mnu_dintercept.add(mni_dreqi);

        mni_dresi.setSelected(true);
        mni_dresi.setText("Intercept Responses");
        mni_dresi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mni_dresiActionPerformed(evt);
            }
        });
        mnu_dintercept.add(mni_dresi);

        mnu_dmore.add(mnu_dintercept);

        mnu_dscripts.setText("Load Scripts");
        mnu_dmore.add(mnu_dscripts);

        mnu_cintercept.setText("Interception");

        mni_creqi.setSelected(true);
        mni_creqi.setText("Intercept Requests");
        mni_creqi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mni_creqiActionPerformed(evt);
            }
        });
        mnu_cintercept.add(mni_creqi);

        mni_cresi.setSelected(true);
        mni_cresi.setText("Intercept Responses");
        mni_cresi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mni_cresiActionPerformed(evt);
            }
        });
        mnu_cintercept.add(mni_cresi);

        mnu_cmore.add(mnu_cintercept);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Editor");

        btn_dsend.setText("Send");
        btn_dsend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_dsendActionPerformed(evt);
            }
        });

        btn_dload.setText("Load");
        btn_dload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_dloadActionPerformed(evt);
            }
        });

        btn_dextend.setText("More >");
        btn_dextend.setComponentPopupMenu(mnu_dmore);
        btn_dextend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_dextendActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout pnl_doptLayout = new org.jdesktop.layout.GroupLayout(pnl_dopt);
        pnl_dopt.setLayout(pnl_doptLayout);
        pnl_doptLayout.setHorizontalGroup(
            pnl_doptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_doptLayout.createSequentialGroup()
                .addContainerGap()
                .add(btn_dsend)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(btn_dload)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(btn_dextend)
                .addContainerGap(635, Short.MAX_VALUE))
        );
        pnl_doptLayout.setVerticalGroup(
            pnl_doptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_doptLayout.createSequentialGroup()
                .addContainerGap()
                .add(pnl_doptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(btn_dsend)
                    .add(btn_dload)
                    .add(btn_dextend))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout pnl_messageLayout = new org.jdesktop.layout.GroupLayout(pnl_message);
        pnl_message.setLayout(pnl_messageLayout);
        pnl_messageLayout.setHorizontalGroup(
            pnl_messageLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_dopt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(he_dec, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE)
        );
        pnl_messageLayout.setVerticalGroup(
            pnl_messageLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, pnl_messageLayout.createSequentialGroup()
                .add(he_dec, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 479, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pnl_dopt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        tpn_decompressed.addTab("Complete Message", pnl_message);

        btn_paramupdate.setText("Update PARAMS");
        btn_paramupdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_paramupdateActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout pnl_paramoptLayout = new org.jdesktop.layout.GroupLayout(pnl_paramopt);
        pnl_paramopt.setLayout(pnl_paramoptLayout);
        pnl_paramoptLayout.setHorizontalGroup(
            pnl_paramoptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_paramoptLayout.createSequentialGroup()
                .addContainerGap()
                .add(btn_paramupdate)
                .addContainerGap(743, Short.MAX_VALUE))
        );
        pnl_paramoptLayout.setVerticalGroup(
            pnl_paramoptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_paramoptLayout.createSequentialGroup()
                .addContainerGap()
                .add(btn_paramupdate)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout pnl_paramsLayout = new org.jdesktop.layout.GroupLayout(pnl_params);
        pnl_params.setLayout(pnl_paramsLayout);
        pnl_paramsLayout.setHorizontalGroup(
            pnl_paramsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_paramopt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(he_params, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE)
        );
        pnl_paramsLayout.setVerticalGroup(
            pnl_paramsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, pnl_paramsLayout.createSequentialGroup()
                .add(he_params, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 479, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pnl_paramopt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        tpn_decompressed.addTab("PARAMS", pnl_params);

        btn_rfcupdate.setText("Update RFC_QUEUE");
        btn_rfcupdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_rfcupdateActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout pbl_rfcoptLayout = new org.jdesktop.layout.GroupLayout(pbl_rfcopt);
        pbl_rfcopt.setLayout(pbl_rfcoptLayout);
        pbl_rfcoptLayout.setHorizontalGroup(
            pbl_rfcoptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pbl_rfcoptLayout.createSequentialGroup()
                .addContainerGap()
                .add(btn_rfcupdate)
                .addContainerGap(721, Short.MAX_VALUE))
        );
        pbl_rfcoptLayout.setVerticalGroup(
            pbl_rfcoptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pbl_rfcoptLayout.createSequentialGroup()
                .addContainerGap()
                .add(btn_rfcupdate)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout pnl_rfcLayout = new org.jdesktop.layout.GroupLayout(pnl_rfc);
        pnl_rfc.setLayout(pnl_rfcLayout);
        pnl_rfcLayout.setHorizontalGroup(
            pnl_rfcLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pbl_rfcopt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(he_rfcqueue, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE)
        );
        pnl_rfcLayout.setVerticalGroup(
            pnl_rfcLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, pnl_rfcLayout.createSequentialGroup()
                .add(he_rfcqueue, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 479, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pbl_rfcopt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        tpn_decompressed.addTab("RFC_QUEUE", pnl_rfc);

        btn_verbupdate.setText("Update VERBS");
        btn_verbupdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_verbupdateActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout pnl_verboptLayout = new org.jdesktop.layout.GroupLayout(pnl_verbopt);
        pnl_verbopt.setLayout(pnl_verboptLayout);
        pnl_verboptLayout.setHorizontalGroup(
            pnl_verboptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_verboptLayout.createSequentialGroup()
                .addContainerGap()
                .add(btn_verbupdate)
                .addContainerGap(757, Short.MAX_VALUE))
        );
        pnl_verboptLayout.setVerticalGroup(
            pnl_verboptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_verboptLayout.createSequentialGroup()
                .addContainerGap()
                .add(btn_verbupdate)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout pnl_verbsLayout = new org.jdesktop.layout.GroupLayout(pnl_verbs);
        pnl_verbs.setLayout(pnl_verbsLayout);
        pnl_verbsLayout.setHorizontalGroup(
            pnl_verbsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_verbopt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(he_verbs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE)
        );
        pnl_verbsLayout.setVerticalGroup(
            pnl_verbsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, pnl_verbsLayout.createSequentialGroup()
                .add(he_verbs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 479, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pnl_verbopt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        tpn_decompressed.addTab("VERBS", pnl_verbs);

        btn_varupdate.setText("Update VARS");
        btn_varupdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_varupdateActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout pnl_varoptLayout = new org.jdesktop.layout.GroupLayout(pnl_varopt);
        pnl_varopt.setLayout(pnl_varoptLayout);
        pnl_varoptLayout.setHorizontalGroup(
            pnl_varoptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_varoptLayout.createSequentialGroup()
                .addContainerGap()
                .add(btn_varupdate)
                .addContainerGap(762, Short.MAX_VALUE))
        );
        pnl_varoptLayout.setVerticalGroup(
            pnl_varoptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_varoptLayout.createSequentialGroup()
                .addContainerGap()
                .add(btn_varupdate)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout pnl_varsLayout = new org.jdesktop.layout.GroupLayout(pnl_vars);
        pnl_vars.setLayout(pnl_varsLayout);
        pnl_varsLayout.setHorizontalGroup(
            pnl_varsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_varopt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(he_vars, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 903, Short.MAX_VALUE)
        );
        pnl_varsLayout.setVerticalGroup(
            pnl_varsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, pnl_varsLayout.createSequentialGroup()
                .add(he_vars, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 479, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pnl_varopt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        tpn_decompressed.addTab("VARS", pnl_vars);

        org.jdesktop.layout.GroupLayout pnl_decompressedLayout = new org.jdesktop.layout.GroupLayout(pnl_decompressed);
        pnl_decompressed.setLayout(pnl_decompressedLayout);
        pnl_decompressedLayout.setHorizontalGroup(
            pnl_decompressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 924, Short.MAX_VALUE)
            .add(pnl_decompressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(tpn_decompressed, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 924, Short.MAX_VALUE))
        );
        pnl_decompressedLayout.setVerticalGroup(
            pnl_decompressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 588, Short.MAX_VALUE)
            .add(pnl_decompressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(tpn_decompressed, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE))
        );

        tpn_edit.addTab("Decompressed", pnl_decompressed);

        btn_csend.setText("Send");
        btn_csend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_csendActionPerformed(evt);
            }
        });

        btn_cload.setText("Load");
        btn_cload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cloadActionPerformed(evt);
            }
        });

        btn_cextend.setText("More >");
        btn_cextend.setComponentPopupMenu(mnu_cmore);
        btn_cextend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cextendActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout pnl_coptLayout = new org.jdesktop.layout.GroupLayout(pnl_copt);
        pnl_copt.setLayout(pnl_coptLayout);
        pnl_coptLayout.setHorizontalGroup(
            pnl_coptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_coptLayout.createSequentialGroup()
                .addContainerGap()
                .add(btn_csend)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(btn_cload)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(btn_cextend)
                .addContainerGap(656, Short.MAX_VALUE))
        );
        pnl_coptLayout.setVerticalGroup(
            pnl_coptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_coptLayout.createSequentialGroup()
                .addContainerGap()
                .add(pnl_coptLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(btn_csend)
                    .add(btn_cload)
                    .add(btn_cextend))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout pnl_compressedLayout = new org.jdesktop.layout.GroupLayout(pnl_compressed);
        pnl_compressed.setLayout(pnl_compressedLayout);
        pnl_compressedLayout.setHorizontalGroup(
            pnl_compressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnl_copt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(he_com, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 924, Short.MAX_VALUE)
        );
        pnl_compressedLayout.setVerticalGroup(
            pnl_compressedLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, pnl_compressedLayout.createSequentialGroup()
                .add(he_com, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 525, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pnl_copt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        tpn_edit.addTab("Compressed", pnl_compressed);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tpn_edit, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 945, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tpn_edit, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 634, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_csendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_csendActionPerformed
        // This is the compressed send action.
        // We can return the compressed hex editor byte content
        this._retr = this.he_com.getByteContent();
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_btn_csendActionPerformed

    private void btn_dsendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_dsendActionPerformed
        // This is the decompressed send action.
        // We must:
        // a: get decompressed hex editor bytes
        byte[] work = this.he_dec.getByteContent();
        // b: compress decompressed hex editor bytes
        int[] ic = new int[work.length];
        for (int j = 0; j < work.length; j++) {
            ic[j] = (int) ((int) work[j] & 0xff);
        }
        int[] comp = this._code.doCompress(ic);
        // DEBUGGING:
        // c: Add static header, modify length (first 4 bytes of array) and type-cast to byte[]
        work = new byte[comp.length + 12];
        // The length is the length - 4 bytes (ie: the bytes specifying the length of the packet)
        work[0] = (byte)((comp.length + 8) >>> 24);
        work[1] = (byte)((comp.length + 8) >>> 16);
        work[2] = (byte)((comp.length + 8) >>> 8);
        work[3] = (byte)(comp.length + 8);
        int x1 = (int)((int)work[0] & 0xff);
        int x2 = (int)((int)work[1] & 0xff);
        int x3 = (int)((int)work[2] & 0xff);
        int x4 = (int)((int)work[3] & 0xff);
        work[4] = 0;
        work[5] = 0;
        work[6] = 17;
        work[7] = 0;
        work[8] = 0;
        work[9] = 0;
        work[10] = 0;
        work[11] = 1;
        int j = 12;
        for (int i = 0; i < comp.length; i++) {
            work[j] = (byte)comp[i];
            j++;
        }
        // d: return bytes
        this._retr = work;
        // e: close the form
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_btn_dsendActionPerformed

    private void UpdateMessage(int[] SEARCH, int t) {
        byte[] b = new byte[0];
        switch(t) {
            case 0:
                // PARAMS
                b = this.he_params.getByteContent();
                break;
            case 1:
                // RFC_QUEUE
                b = this.he_rfcqueue.getByteContent();
                break;
            case 2:
                // VERBS
                b = this.he_verbs.getByteContent();
                break;
            case 3:
                // VARS
                b = this.he_vars.getByteContent();
                break;
        }
        // Type-case byte array to int and compress.
        int[] ia = new int[b.length];
        for (int i = 0; i < b.length; i++) {
            ia[i] = (int)b[i] & 0xff;
        }
        int[] im = this._code.doCompress(ia);

        // Get the message byte, convert to int.
        byte[] m = this.he_dec.getByteContent();
        ia = new int[m.length];
        for (int i = 0; i < m.length; i++) {
            ia[i] = (int)m[i] & 0xff;
        }

        // Find start of BLOCK and MAGIC.
        int[] MAGIC = {0x1f, 0x9d};
        int paramstart = KnuthPatternMatching.indexOf(ia, SEARCH, 0);
        if (paramstart == -1) {
            return;
        }
        int magicstart = KnuthPatternMatching.indexOf(ia, MAGIC, paramstart);
        if (magicstart == -1) {
            return;
        }
        

        int j = magicstart;
        int k = 5;
        while (k < im.length) {
            ia[j] = im[k];
            j++;
            k++;
        }
        //ia[k] = 0x00;
        //ia[k+1] = 0x03;
        m = new byte[ia.length];
        for (int i = 0; i < ia.length; i++) {
            m[i] = (byte)ia[i];
        }
        // Update message content
        this.he_dec.setByteContent(m);
    }
    private void btn_paramupdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_paramupdateActionPerformed
        int[] PARAMS = {0x50, 0x41, 0x52, 0x41, 0x4d, 0x53, 0x03, 0x01, 0x03};
        this.UpdateMessage(PARAMS, 0);
    }//GEN-LAST:event_btn_paramupdateActionPerformed

    private void btn_rfcupdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_rfcupdateActionPerformed
        int[] RFCQUEUE = {0x52, 0x46, 0x43, 0x5f, 0x51, 0x55, 0x45, 0x55, 0x45, 0x03, 0x01, 0x03};
        this.UpdateMessage(RFCQUEUE, 1);
    }//GEN-LAST:event_btn_rfcupdateActionPerformed

    private void btn_verbupdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_verbupdateActionPerformed
        int[] VERBS = {0x56, 0x45, 0x52, 0x42, 0x53, 0x03, 0x01, 0x03};
        this.UpdateMessage(VERBS, 2);
    }//GEN-LAST:event_btn_verbupdateActionPerformed

    private void btn_varupdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_varupdateActionPerformed
        int[] VARS = {0x56, 0x41, 0x52, 0x53, 0x03, 0x01, 0x03};
        this.UpdateMessage(VARS, 3);
    }//GEN-LAST:event_btn_varupdateActionPerformed

    private void btn_dloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_dloadActionPerformed
        JFileChooser f_chooser = new JFileChooser();
        int status = f_chooser.showOpenDialog(this);
        if (status == JFileChooser.APPROVE_OPTION) {
            File f_file = f_chooser.getSelectedFile();
            try {
                FileInputStream fis = new FileInputStream(f_file);
                DataInputStream dis = new DataInputStream(fis);
                byte[] b = new byte[(int)f_file.length()];
                dis.readFully(b);
                this.he_dec.setByteContent(b);
                setSubBytes(this.he_dec.getByteContent());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Could not load the decompressed file:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    }//GEN-LAST:event_btn_dloadActionPerformed

    private void btn_cloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_cloadActionPerformed
        JFileChooser f_chooser = new JFileChooser();
        int status = f_chooser.showOpenDialog(this);
        if (status == JFileChooser.APPROVE_OPTION) {
            File f_file = f_chooser.getSelectedFile();
            try {
                FileInputStream fis = new FileInputStream(f_file);
                DataInputStream dis = new DataInputStream(fis);
                byte[] b = new byte[(int)f_file.length()];
                dis.readFully(b);
                this.he_com.setByteContent(b);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Could not load the compressed file:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    }//GEN-LAST:event_btn_cloadActionPerformed

    private void btn_dextendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_dextendActionPerformed
        this.btn_dextend.getComponentPopupMenu().show(this.btn_dextend, this.btn_dextend.getWidth(), this.btn_dextend.getHeight());
    }//GEN-LAST:event_btn_dextendActionPerformed

    private void btn_cextendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_cextendActionPerformed
        this.btn_cextend.getComponentPopupMenu().show(this.btn_cextend, this.btn_cextend.getWidth(), this.btn_cextend.getHeight());
    }//GEN-LAST:event_btn_cextendActionPerformed

    private void mni_dreqiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mni_dreqiActionPerformed
        this._form.setReqIntercept(this.mni_dreqi.getState());
        this.mni_creqi.setSelected(this.mni_dreqi.getState());
    }//GEN-LAST:event_mni_dreqiActionPerformed

    private void mni_creqiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mni_creqiActionPerformed
        this._form.setReqIntercept(this.mni_creqi.getState());
        this.mni_dreqi.setSelected(this.mni_creqi.getState());
    }//GEN-LAST:event_mni_creqiActionPerformed

    private void mni_dresiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mni_dresiActionPerformed
        this._form.setResIntercept(this.mni_dresi.getState());
        this.mni_cresi.setSelected(this.mni_dresi.getState());
    }//GEN-LAST:event_mni_dresiActionPerformed

    private void mni_cresiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mni_cresiActionPerformed
        this._form.setResIntercept(this.mni_cresi.getState());
        this.mni_dresi.setSelected(this.mni_cresi.getState());
    }//GEN-LAST:event_mni_cresiActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_cextend;
    private javax.swing.JButton btn_cload;
    private javax.swing.JButton btn_csend;
    private javax.swing.JButton btn_dextend;
    private javax.swing.JButton btn_dload;
    private javax.swing.JButton btn_dsend;
    private javax.swing.JButton btn_paramupdate;
    private javax.swing.JButton btn_rfcupdate;
    private javax.swing.JButton btn_varupdate;
    private javax.swing.JButton btn_verbupdate;
    private at.HexLib.library.JHexEditor he_com;
    private at.HexLib.library.JHexEditor he_dec;
    private at.HexLib.library.JHexEditor he_params;
    private at.HexLib.library.JHexEditor he_rfcqueue;
    private at.HexLib.library.JHexEditor he_vars;
    private at.HexLib.library.JHexEditor he_verbs;
    private javax.swing.JCheckBoxMenuItem mni_creqi;
    private javax.swing.JCheckBoxMenuItem mni_cresi;
    private javax.swing.JCheckBoxMenuItem mni_dreqi;
    private javax.swing.JCheckBoxMenuItem mni_dresi;
    private javax.swing.JMenu mnu_cintercept;
    private javax.swing.JPopupMenu mnu_cmore;
    private javax.swing.JMenu mnu_dintercept;
    private javax.swing.JPopupMenu mnu_dmore;
    private javax.swing.JMenu mnu_dscripts;
    private javax.swing.JPanel pbl_rfcopt;
    private javax.swing.JPanel pnl_compressed;
    private javax.swing.JPanel pnl_copt;
    private javax.swing.JPanel pnl_decompressed;
    private javax.swing.JPanel pnl_dopt;
    private javax.swing.JPanel pnl_message;
    private javax.swing.JPanel pnl_paramopt;
    private javax.swing.JPanel pnl_params;
    private javax.swing.JPanel pnl_rfc;
    private javax.swing.JPanel pnl_varopt;
    private javax.swing.JPanel pnl_vars;
    private javax.swing.JPanel pnl_verbopt;
    private javax.swing.JPanel pnl_verbs;
    private javax.swing.JTabbedPane tpn_decompressed;
    private javax.swing.JTabbedPane tpn_edit;
    // End of variables declaration//GEN-END:variables
}
