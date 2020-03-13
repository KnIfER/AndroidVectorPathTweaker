/*
 * Copyright 2020 KnIfER. https://github.com/KnIfER
 * Copyright 2018 Airsaid. (as template)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Select part of the vector's path data and do translations.
 * @author KnIfER
 */
public class PathTweakerDialog extends DialogWrapper {
    private AnActionEvent       mActionEvent;
    private Project             mProject;
    private Document            mDocument;
    private Editor              mEditor;
    private int currentStart;
    private int currentEnd;
    private JLabel maniOffset;
    private String currentText;

    private float viewportHeight=24, viewportWidth=24;
    private float scaler =  1f;
    private float scalerY = scaler;
    private float transX;
    private float transY;

    private static int firstflag;
    private JTextField etFieldvw;
    private JTextField etFieldvh;
    private JTextField etFieldx;
    private JTextField etFieldy;
    private JTextField etFieldscale;
    private JTextField etFieldscaleY;

    private boolean  getTranslate(){
        return (firstflag&0x1)==0;
    }
    private void  setTranslate(boolean val){
        firstflag&=~0x1;
        if(!val) firstflag|=0x1;
    }

    private boolean  getScale(){
        return (firstflag&0x2)==0;
    }
    private void  setScale(boolean val){
        firstflag&=~0x2;
        if(!val) firstflag|=0x2;
    }

    private boolean  getTranspose(){
        return (firstflag&0x4)!=0;
    }
    private void  setTranspose(boolean val){
        firstflag&=~0x4;
        if(val) firstflag|=0x4;
    }

    private boolean  getFlipX(){
        return (firstflag&0x8)!=0;
    }
    private void  setFlipX(boolean val){
        firstflag&=~0x8;
        if(val) firstflag|=0x8;
    }

    private boolean  getFlipY(){
        return (firstflag&0x10)!=0;
    }
    private void  setFlipY(boolean val){
        firstflag&=~0x10;
        if(val) firstflag|=0x10;
    }

    private boolean  getKeepOrg(){
        return (firstflag&0x20)==0;
    }
    private void  setKeepOrg(boolean val){
        firstflag&=~0x20;
        if(!val) firstflag|=0x20;
    }

    private boolean  getShrinkOrg(){
        return (firstflag&0x40)==0;
    }
    private void  setShrinkOrg(boolean val){
        firstflag&=~0x40;
        if(!val) firstflag|=0x40;
    }

    private boolean  getAutoUpadte(){
        return (firstflag&0x80)!=0;
    }
    private void  setAutoUpadte(boolean val){
        firstflag&=~0x80;
        if(val) firstflag|=0x80;
    }
    
    public PathTweakerDialog(Project project, AnActionEvent actionEvent) {
        super(project, false);
        mActionEvent = actionEvent;
        mProject = project;
        setTitle("Tweak Path Data by KnIfER");
        setResizable(true);
        init();
    }

    @Override
    protected void doOKAction() {
        doIt();
        super.doOKAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        final JPanel panel = new JPanel(new BorderLayout(16, 6));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        ItemListener itemListener= e -> {
            String name = ((JCheckBox)e.getItem()).getName();
            boolean checked = e.getStateChange()==ItemEvent.SELECTED;
            switch (parsint(name)){
                case 0:
                    setAutoUpadte(checked);
                break;
                case 1:
                    setTranslate(checked);
                break;
                case 2:
                    setScale(checked);
                break;
                case 3:
                    setFlipX(checked);
                break;
                case 4:
                    setFlipY(checked);
                break;
                case 5:
                    setTranspose(checked);
                break;
                case 6:
                    setKeepOrg(checked);
                break;
                case 7:
                    setShrinkOrg(checked);
                break;
            }
            if(getAutoUpadte()) doIt();
        };

        /* let's code hard Swing UI ! */

        DocumentListener inputListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { if(getAutoUpadte()) doIt(); }
            @Override public void removeUpdate(DocumentEvent e) { if(getAutoUpadte()) doIt(); }
            @Override public void changedUpdate(DocumentEvent e) {  }
        };

        /* offset */
        Container row_offset = new Container();
        row_offset.setLayout(new BoxLayout(row_offset, BoxLayout.X_AXIS));
        JLabel titleOffset = new JLabel("Current Selection : ");
        JButton buttonSelect = new JButton("Reset");
        buttonSelect.addActionListener(e -> Rebase());
        JButton buttonRevert = new JButton("Revert");
        buttonRevert.addActionListener(e -> Revert());
        maniOffset = new JLabel();
        row_offset.add(titleOffset);
        row_offset.add(maniOffset);
        row_offset.add(buttonRevert);
        row_offset.add(buttonSelect);

        /* viewport */
        Container row_viewport = new Container();
        row_viewport.setLayout(new BoxLayout(row_viewport, BoxLayout.X_AXIS));
        etFieldvw = new JTextField(); etFieldvw.getDocument().addDocumentListener(inputListener);
        etFieldvh = new JTextField(); etFieldvh.getDocument().addDocumentListener(inputListener);
        row_viewport.add(new JLabel("Viewport Width"));
        row_viewport.add(etFieldvw); etFieldvw.setText("24");
        row_viewport.add(new JLabel("Viewport Height"));
        row_viewport.add(etFieldvh); etFieldvh.setText("24");

        /* translate */
        Container row_translate = new Container();
        row_translate.setLayout(new BoxLayout(row_translate, BoxLayout.X_AXIS));
        JBCheckBox check_translate = new JBCheckBox();
        check_translate.setName(Integer.toString(1));
        check_translate.setSelected(getTranslate()); check_translate.addItemListener(itemListener);
        etFieldx = new JTextField(); etFieldx.setText("0.0"); etFieldx.getDocument().addDocumentListener(inputListener);
        etFieldy = new JTextField(); etFieldy.setText("0.0"); etFieldy.getDocument().addDocumentListener(inputListener);
        JLabel titleTranslate = new JLabel("TRANSLATION ");
        titleTranslate.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
            check_translate.setSelected(!check_translate.isSelected());
            }
        });
        row_translate.add(check_translate);
        row_translate.add(titleTranslate);
        row_translate.add(new JLabel("X:"));
        row_translate.add(etFieldx);
        row_translate.add(new JLabel("Y:"));
        row_translate.add(etFieldy);

        /* scale */
        Container row_scale = new Container();
        row_scale.setLayout(new BoxLayout(row_scale, BoxLayout.X_AXIS));
        JBCheckBox check_scale = new JBCheckBox();
        check_scale.setSelected(getTranslate()); check_scale.addItemListener(itemListener);
        check_scale.setName(Integer.toString(2));
        etFieldscale = new JTextField(); etFieldscale.setText("1.0"); etFieldscale.getDocument().addDocumentListener(inputListener);
        etFieldscaleY = new JTextField(); etFieldscaleY.setText("1.0"); etFieldscaleY.getDocument().addDocumentListener(inputListener);
        JLabel titleScale = new JLabel("SCALE  ");
        titleScale.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
            check_scale.setSelected(!check_scale.isSelected());
            }
        });
        row_scale.add(check_scale);
        row_scale.add(titleScale);
        row_scale.add(new JLabel("X:"));
        row_scale.add(etFieldscale);
        row_scale.add(new JLabel("Y:"));
        row_scale.add(etFieldscaleY);

        /* flipx flipy transpose */
        Container fft = new Container();
        fft.setLayout(new BoxLayout(fft, BoxLayout.X_AXIS));
        JBCheckBox check = new JBCheckBox();
        check.setName(Integer.toString(3)); check.setSelected(getFlipX()); check.addItemListener(itemListener);
        fft.add(new JLabel("FLIP X: ")); fft.add(check);
        check = new JBCheckBox();
        check.setName(Integer.toString(4)); check.setSelected(getFlipY()); check.addItemListener(itemListener);
        fft.add(new JLabel("  FLIP Y: ")); fft.add(check);
        check = new JBCheckBox();
        check.setName(Integer.toString(5)); check.setSelected(getTranspose()); check.addItemListener(itemListener);
        fft.add(new JLabel("  TRANSPOSE: ")); fft.add(check);

        /* keeporg shrinkorg */
        Container ks = new Container();
        ks.setLayout(new BoxLayout(ks, BoxLayout.X_AXIS));
        check = new JBCheckBox();
        check.setName(Integer.toString(6)); check.setSelected(getKeepOrg()); check.addItemListener(itemListener);
        ks.add(new JLabel("KEEP ORIGIN: ")); ks.add(check);
        check = new JBCheckBox();
        check.setName(Integer.toString(7)); check.setSelected(getShrinkOrg()); check.addItemListener(itemListener);
        ks.add(new JLabel("    SHRINK ORIGIN: ")); ks.add(check);

        /* auto update and do it */
        Container ad = new Container();
        ad.setLayout(new BoxLayout(ad, BoxLayout.X_AXIS));
        JBCheckBox check_auto = new JBCheckBox();
        check_auto.setName(Integer.toString(0)); check_auto.setSelected(getAutoUpadte()); check_auto.addItemListener(itemListener);
        JLabel titleAuto = new JLabel("AUTO UPDATE ");
        titleAuto.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                check_auto.setSelected(!check_auto.isSelected());
            }
        });
        Button executeBtn = new Button("DO IT！");
        executeBtn.addActionListener(e -> doIt());
        ad.add(check_auto);
        ad.add(titleAuto);
        ad.add(executeBtn);

        panel.add(row_offset);
        panel.add(row_viewport);
        panel.add(row_translate);
        panel.add(row_scale);
        panel.add(fft);
        panel.add(ks);
        panel.add(ad);

        Rebase();

        return panel;
    }

    /** Actually Execute the tweak. <br/>
     * Contemplate the words of Li Xiao Long during the process to maximize your chance of success. */
    private void doIt() {
        if(mDocument!=null && currentText!=null && currentStart<currentEnd && currentStart>=0){
            String tweaked = tweakPath();
            Runnable runnable = () -> {
                mDocument.deleteString(currentStart, currentEnd);
                mDocument.insertString(currentStart, tweaked);
                currentEnd = currentStart + tweaked.length();
                mEditor.getSelectionModel().setSelection(currentStart, currentEnd);
            };
            WriteCommandAction.runWriteCommandAction(mProject, runnable);
        }
    }

    /** Revert the string that is first fetched via {@link #Rebase} */
    private void Revert() {
        String text = currentText;
        if(mDocument!=null && text!=null && currentStart<currentEnd && currentStart>=0) {
            Runnable runnable = () -> {
                mDocument.deleteString(currentStart, currentEnd);
                mDocument.insertString(currentStart, text);
                currentEnd = currentStart + text.length();
                mEditor.getSelectionModel().setSelection(currentStart, currentEnd);
            };
            WriteCommandAction.runWriteCommandAction(mProject, runnable);
        }
    }

    /** Fetch and parse the selected text <br/>
     * Texts must contain valid vector path data. */
    private void Rebase() {
        currentStart=currentEnd=-1;

        mEditor = mActionEvent.getData(PlatformDataKeys.EDITOR);

        if (mEditor==null) return;

        SelectionModel selectionModel = mEditor.getSelectionModel();

        mDocument = mEditor.getDocument();

        int offsetStart = selectionModel.getSelectionStart();

        String text = selectionModel.getSelectedText();

        if(text==null){
            Invalidate();
            return;
        }

        int len = text.length();

        int regularStart = text.indexOf("pathData");

        if(regularStart!=-1){
            regularStart = text.indexOf("\"", regularStart);
            if(regularStart!=-1) {
                regularStart+=1;
                for (int i = regularStart; i < len; i++) {
                    char c = text.charAt(i);
                    if(c=='"'){
                        currentEnd = i;
                        break;
                    }
                    if(c=='z'||c=='Z'){
                        currentEnd = i+1;
                    }
                }
                currentStart = regularStart;
            }
        } else {
            for (int i = 0; i < len; i++) {
                char c = text.charAt(i);
                if(currentStart==-1){
                    if(c=='m'||c=='M') currentStart = i;
                } else if(c=='z'||c=='Z') currentEnd = i+1;
            }
        }

        if(currentStart<currentEnd && currentStart>=0){
            currentText = text.substring(currentStart, currentEnd);
            currentStart = offsetStart+currentStart;
            currentEnd = offsetStart+currentEnd;
            maniOffset.setForeground(Color.BLACK);
            maniOffset.setText("["+currentStart+"-"+currentEnd+"]");
        } else {
            Invalidate();
            currentText = null;
        }
    }

    private void Invalidate() {
        maniOffset.setText("[INVAlID]");
        maniOffset.setForeground(Color.RED);
    }

    static String trimFloatString(String input) {
        //CMN.Log(input);
        int len = input.length();
        int st = 0;

        while ((st < len) && (input.charAt(len - 1) <= ' ' || input.charAt(len - 1) == '0')) {
            len--;
            if(input.charAt(len - 1) == '.') {
                len--;
                break;
            }
        }
        return ((st > 0) || (len < input.length())) ? input.substring(st, len) : input;
    }
    
    static Float parsefloat(String text){
        try {
            return Float.parseFloat(text);
        } catch (Exception ignored) {  }
        return null;
    }

    static int parsint(String text){
        try {
            return Integer.parseInt(text);
        } catch (Exception ignored) {  }
        return -1;
    }

    /** The core */
    String tweakPath() {
        Float readval = parsefloat(etFieldvw.getText());
        if(readval!=null) viewportWidth = readval;
        readval = parsefloat(etFieldvh.getText());
        if(readval!=null) viewportHeight = readval;
        readval = parsefloat(etFieldx.getText());
        if(readval!=null) transX = readval;
        readval = parsefloat(etFieldy.getText());
        if(readval!=null) transY = readval;
        readval = parsefloat(etFieldscale.getText());
        if(readval!=null) scaler = readval;
        readval = parsefloat(etFieldscaleY.getText());
        if(readval!=null) scalerY = readval;
        
        float scaler  = getScale()?this.scaler:1;
        float scalerY = getScale()?this.scalerY:1;
        float transX  = getTranslate()?this.transX:0;
        float transY  = getTranslate()?this.transY:0;
        boolean transpose = getTranspose();
        boolean flipX = getFlipX();
        boolean flipY = getFlipY();
        boolean keep_rel_group = getKeepOrg();
        boolean shrink_orgs = getShrinkOrg();

        String pathdata = currentText;
        pathdata=pathdata.trim();
        StringBuilder pathbuilder = new StringBuilder();
        Pattern reg = Pattern.compile("[MlLzscCSVvhH ]");
        Pattern regLower = Pattern.compile("[a-z]");
        Pattern regVertical = Pattern.compile("[Vv]");
        Pattern regHorizontal = Pattern.compile("[Hh]");
        Matcher m = reg.matcher(pathdata);
        int idx=0;
        String lastCommand = null;
        Float[] firstOrg=null;
        Float[] Org=null;
        float[] deltaOrg=new float[2];
        while(m.find()) {
            int now =m.start();
            if(idx!=-1 && now>idx) {
                String command = pathdata.substring(idx,idx+1);
                String[] arr = pathdata.substring(idx+1,now).split(",");
                if(!command.equals(" ")) {
                    if(transpose && arr.length==1) {
                        char c;
                        lastCommand="";
                        for(int i=0;i<command.length();i++) {
                            switch(c=command.charAt(i)) {
                                case 'v':
                                    c='h';
                                break;
                                case 'V':
                                    c='H';
                                break;
                                case 'h':
                                    c='v';
                                break;
                                case 'H':
                                    c='V';
                                break;
                            }
                            lastCommand+=c;
                        }
                        command=lastCommand;
                    }
                    lastCommand=command;
                }
                boolean xiaoxie = regLower.matcher(lastCommand).matches();
                boolean isOrg = lastCommand.equals("M");
                boolean isfirstOrg = isOrg?firstOrg==null:false;
                //if(isfirstOrg)CMN.Log("1st.org#1=", arr);
                //CMN.Log("command: "+command+" "+lastCommand+" "+xiaoxie);
                //CMN.Log(pathdata.substring(idx+1,now));
                pathbuilder.append(command);
                if(arr.length==2) {//x-y coordinates
                    float x=Float.valueOf(transpose?arr[1]:arr[0]);
                    if(isOrg) {
                        Org=new Float[2];
                        Org[0]=x;
                        if(isfirstOrg) {
                            if(shrink_orgs) {
                                transX+=viewportWidth/2+(x-viewportWidth/2)*scaler-x;
                            }
                            firstOrg=Org;
                            deltaOrg[0]=transX;
                        }else if(keep_rel_group) {
                            deltaOrg[0]=scaler*(x-firstOrg[0])+firstOrg[0]-x+transX;
                        }
                        if(flipX) x = viewportWidth-x;
                    }else if(xiaoxie){
                        x=x*scaler;
                        if(flipX) x = -x;
                    }else {
                        x=scaler*(x-Org[0])+Org[0];
                        if(flipX) x = viewportWidth-x;
                    }
                    if(!xiaoxie)x+=deltaOrg[0];
                    pathbuilder.append(trimFloatString(String.format("%.2f", x)));
                    pathbuilder.append(",");
                    x=Float.valueOf(transpose?arr[0]:arr[1]);
                    if(isOrg) {
                        Org[1]=x;
                        if(isfirstOrg) {
                            if(shrink_orgs) {
                                transY+=viewportHeight/2+(x-viewportHeight/2)*scalerY-x;
                            }
                            deltaOrg[1]=transY*(flipY?-1:1);
                        }else if(keep_rel_group) {
                            deltaOrg[1]=scalerY*(x-firstOrg[1])+firstOrg[1]-x+transY*(flipY?-1:1);
                        }
                        if(flipY) x = viewportHeight-x;
                    }else if(xiaoxie){
                        x=x*scalerY;
                        if(flipY) x = -x;
                    }else {
                        x=scalerY*(x-Org[1])+Org[1];
                        if(flipY) x = viewportHeight-x;
                    }
                    if(!xiaoxie)x+=deltaOrg[1];
                    pathbuilder.append(trimFloatString(String.format("%.2f", x)));
                }
                else {//singleton coordinates
                    String key = pathdata.substring(idx+1,now);
                    if(lastCommand!=null)
                        try {
                            boolean isVertical = regVertical.matcher(lastCommand).matches();
                            float val = Float.valueOf(key);
                            if(xiaoxie)
                                val*=(isVertical?(flipY?-scalerY:scalerY):(flipX?-scaler:scaler));
                            else {// 处理  absolute vertical or horizontal case
                                if(isVertical) {//垂直线
                                    val=scalerY*(val-Org[1])+Org[1]+deltaOrg[1]*(flipY?-1:1);
                                    if(flipY) val = viewportHeight-val;
                                }else {//水平线
                                    val=scalerY*(val-Org[0])+Org[0]+deltaOrg[0]*(flipX?-1:1);
                                    if(flipX) val = viewportWidth-val;
                                }
                            }
                            pathbuilder.append(trimFloatString(String.format("%.2f", val)));
                        } catch (NumberFormatException e) {
                            //CMN.Log(key);
                            pathbuilder.append(key);
                        }
                    else
                        pathbuilder.append(key);
                }
            }else
                pathbuilder.append(pathdata.substring(idx,now));
            //CMN.Log(pathdata.substring(0,));
            idx=now;
        }
        pathbuilder.append(pathdata.substring(idx,pathdata.length()));
        //CMN.Log(pathbuilder);
        return pathbuilder.toString();
    }

}
