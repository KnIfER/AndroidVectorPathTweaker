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
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBInsets;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Select part of the vector's path data and do transformations.
 * @author KnIfER
 */
public class PathTweakerDialog extends DialogWrapper {
    private static boolean debug = true;
    private AnActionEvent       mActionEvent;
    private Project             mProject;
    private Document            mDocument;
    private Editor              mEditor;
    private int currentStart;
    private int currentEnd;
    private JLabel maniOffset;
    private JTextArea logview;
    private String currentText;

    private float viewportHeight=24, viewportWidth=24;
    private float scaler =  1f;
    private float scalerY = scaler;
    private float transX;
    private float transY;

    private static int flagStore;
    private int firstflag;
    private JTextField etFieldvw;
    private JTextField etFieldvh;
    private JTextField etFieldx;
    private JTextField etFieldy;
    private JTextField etFieldscale;
    private JTextField etFieldscaleY;
    private JPanel contentPanel;
    private JButton APPLY_IMAGESIZE;

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
        return (firstflag&0x80)==0;
    }
    private void  setAutoUpadte(boolean val){
        firstflag&=~0x80;
        if(!val) firstflag|=0x80;
    }

    private boolean  getSyncTrans(){
        return (firstflag&0x100)!=0;
    }
    private void  setSyncTrans(boolean val){
        firstflag&=~0x100;
        if(val) firstflag|=0x100;
    }
    
    private boolean  getSyncScale(){
        return (firstflag&0x200)!=0;
    }
    private void  setSyncScale(boolean val){
        firstflag&=~0x200;
        if(val) firstflag|=0x200;
    }
    
    public PathTweakerDialog(Project project, AnActionEvent actionEvent) {
        super(project, false);
        //firstflag = flagStore;
        mActionEvent = actionEvent;
        mProject = project;
        setResizable(true);
        setModal(false);
        //getWindow().setIconImage(Toolkit.getDefaultToolkit().getImage("/icons/icon.png"));
        init();
    }

    @Override
    protected void doOKAction() {
        doIt();
        flagStore = firstflag;
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        flagStore = firstflag;
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
                case 8:
                    setSyncTrans(checked);
                break;
                case 9:
                    setSyncScale(checked);
                break;
            }
            if(getAutoUpadte()) doIt();
        };

        /* let's code hard Swing UI ! */

        DocumentListener inputListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onTextChanged(e); }
            @Override public void removeUpdate(DocumentEvent e) { onTextChanged(e); }
            @Override public void changedUpdate(DocumentEvent e) {  }

            private void onTextChanged(DocumentEvent e) {
                if(APPLY_IMAGESIZE!=null && (e.getDocument()==etFieldvw.getDocument()||e.getDocument()==etFieldvh.getDocument())) {
                    APPLY_IMAGESIZE.setEnabled(true);
                } else if(getAutoUpadte()) {
                    doIt();
                }
            }

        };

        MouseWheelListener mouseWheelListener = e -> {
            JTextField fieldToModify = (JTextField) e.getSource();
            String valstr = fieldToModify.getText();
            Float val = parsefloat(valstr);
            float now = 0;
            double quantity = 0.1;
            int keep=2;
            int xiaoshudian=-1;
            int caret = -1;
			if(val!=null){
				now = val;
				int len = valstr.length();
				int idx = valstr.lastIndexOf(".");
				xiaoshudian = idx;
				if(idx>0){
					keep = len-idx-1;
				} else {
					xiaoshudian = len;
				}
				if(fieldToModify.isFocusOwner()){
					caret = fieldToModify.getCaretPosition();
					int level;
					if(caret>=0 && caret<=len){
					    if(valstr.trim().startsWith("-")){
                            int negationIdx = valstr.indexOf("-");
                            if(caret<=negationIdx) caret=negationIdx+1;
                        }
						if(caret==len) caret=len-1;
						if(idx<0) idx = len;
						level = idx - caret;
						if(idx>caret) level-=1;
					} else {
						if(idx>0){//找到小数级别
							level = -len+idx+1;
						} else {
							level = idx<0?0:-1;
						}
					}
					quantity = (float) Math.pow(10, level);
				}
			}
            if(keep>3) keep=3;
            valstr = String.format("%."+keep+"f", now-quantity*e.getWheelRotation());
			fieldToModify.setText(valstr);
            if(caret!=-1) {
                int xs2 = valstr.lastIndexOf(".");
                if(xs2!=xiaoshudian) caret = caret + xs2 - xiaoshudian;
                if(caret>=valstr.length()) caret=valstr.length();
                fieldToModify.setCaretPosition(caret);
            }
        };

        LayouteatMan layoutEater = new LayouteatMan(itemListener, inputListener, mouseWheelListener);
        
        /* offset */
        Container row_offset = layoutEater.startNewLayout();
        layoutEater.eatLabel("Current Selection : ");
        row_offset.add(maniOffset = new JLabel());
        layoutEater.eatJButton("Rebase", e -> Rebase());
        layoutEater.eatJButton("Revert", e -> Revert());

        /* viewport */
        Container row_viewport = layoutEater.startNewLayout();
        layoutEater.eatLabel("Viewport Width");
        layoutEater.label.addMouseListener(new MouseAdapter() {
            long lastClickTime;
            @Override
            public void mouseClicked(MouseEvent e) {
                long now = System.currentTimeMillis();
                if(now-lastClickTime<450) {
                    refetchImageSize();
                    lastClickTime = 0;
                } else {
                    lastClickTime = now;
                }
            }
        });
        etFieldvw = layoutEater.eatEt("24.0");
        row_viewport.add(new JLabel("Height"));
        etFieldvh = layoutEater.eatEt("24.0");
        APPLY_IMAGESIZE = layoutEater.eatJButton("APPLY", e -> resizeImage());
        
        /* translate */
        Container row_translate = layoutEater.startNewLayout();
        layoutEater.eatLabelCheck(1, getTranslate(), "TRANSLATION ", true);
        JBCheckBox LabelCheck = layoutEater.check;
        layoutEater.eatLabel("X:");
        etFieldx = layoutEater.eatEt("0.0");
        layoutEater.eatLabel("Y:");
        etFieldy = layoutEater.eatEt("0.0");
        row_translate.add(decorated_reset_btn(e -> {
            etFieldx.setText("0.0");
            etFieldy.setText("0.0");
            LabelCheck.setSelected(true);
        }));
        
        /* scale */
        Container row_scale = layoutEater.startNewLayout();
        layoutEater.eatLabelCheck(2, getScale(), "SCALE  ", true);
        JBCheckBox LabelCheck1 = layoutEater.check;
        layoutEater.eatLabel("X:");
        etFieldscale = layoutEater.eatEt("1.0");
        layoutEater.eatLabel("Y:");
        etFieldscaleY = layoutEater.eatEt("1.0");
        row_scale.add(decorated_reset_btn(e -> {
            etFieldscale.setText("1.0");
            etFieldscaleY.setText("1.0");
            LabelCheck1.setSelected(true);
        }));

        /* flipx flipy transpose */
        Container fft = layoutEater.startNewLayout();
        layoutEater.eatCheckLabel(3, getFlipX(), "FLIP X: ", true);
        layoutEater.eatCheckLabel(4, getFlipY(), "  FLIP Y: ", true);
        layoutEater.eatCheckLabel(5, getTranspose(), "  TRANSPOSE: ", true);
        layoutEater.eatCheckLabel(8, getSyncTrans(), "   Sync Trans: ", true);

        /* keeporg shrinkorg */
        Container ks = layoutEater.startNewLayout();
        layoutEater.eatCheckLabel(6, getKeepOrg(), "KEEP ORIGIN: ", false);
        layoutEater.eatCheckLabel(7, getShrinkOrg(), "    SHRINK ORIGIN: ", false);
        layoutEater.eatCheckLabel(9, getSyncScale(), "  Sync Scale: ", true);

        /* auto update and do it */
        Container ad = layoutEater.startNewLayout();
        layoutEater.eatLabelCheck(0, getAutoUpadte(), "AUTO UPDATE ", true);
        layoutEater.eatButton("DO IT！", e -> doIt());

        panel.add(row_offset);
        panel.add(row_viewport);
        panel.add(row_translate);
        panel.add(row_scale);
        panel.add(fft);
        panel.add(ks);
        panel.add(ad);

        Rebase();

        contentPanel = panel;

        //addLogView();

        return panel;
    }

    private void refetchImageSize() {
        if(mDocument!=null) {
            String data = mDocument.getText();
            viewportWidth = parseFloatAttr(data, "viewportWidth", viewportWidth);
            viewportHeight = parseFloatAttr(data, "viewportHeight", viewportHeight);
            etFieldvw.setText(Float.toString(viewportWidth));
            etFieldvh.setText(Float.toString(viewportHeight));
            APPLY_IMAGESIZE.setEnabled(false);
        }
    }


    static class CheckableLable extends MouseAdapter {
        JBCheckBox check;
        CheckableLable(JBCheckBox item) {
            check = item;
        }
        static JLabel instantiate(String text, JBCheckBox item) {
            JLabel ret = new JLabel(text);
            ret.addMouseListener(new CheckableLable(item));
            return ret;
        }
        
        public void mouseClicked(MouseEvent e){
            check.setSelected(!check.isSelected());
        }
    }
    
    static class LayouteatMan {
        ItemListener _itemListener;
        DocumentListener _inputListener;
        MouseWheelListener _mouseWheelListener;
        Container fft;
        JBCheckBox check;
        Button button;
        JLabel label;
        JTextField etFloat;
        private JButton jbutton;

        public LayouteatMan(ItemListener itemListener, DocumentListener inputListener, MouseWheelListener mouseWheelListener) {
            _itemListener = itemListener;
            _inputListener = inputListener;
            _mouseWheelListener = mouseWheelListener;
        }

        public Container startNewLayout () {
            fft = new Container();
            fft.setLayout(new BoxLayout(fft, BoxLayout.X_AXIS));
            return fft;
        }

        public void eatCheckable(int id, boolean checked) {
            check = new JBCheckBox();
            check.setName(Integer.toString(id)); check.setSelected(checked); check.addItemListener(_itemListener);
            fft.add(check);
        }

        public void eatLabel(String text) {
            fft.add(label=new JLabel(text));
        }

        public void eatCheckLabel(int id, boolean checked, String text, boolean checkLabel) {
            eatLabel(text);
            eatCheckable(id, checked);
            if(checkLabel) {
                label.addMouseListener(new CheckableLable(check));
            }
        }
        
        public void eatLabelCheck(int id, boolean flipX, String text, boolean checkLabel) {
            eatCheckable(id, flipX);
            eatLabel(text);
            if(checkLabel) {
                label.addMouseListener(new CheckableLable(check));
            }
        }

        public void eatButton(String text, ActionListener actionListener) {
            button = new Button(text);
            button.addActionListener(actionListener);
            fft.add(button);
        }

        public JButton eatJButton(String text, ActionListener actionListener) {
            jbutton = new JButton(text);
            jbutton.addActionListener(actionListener);
            fft.add(jbutton);
            return jbutton;
        }
        
        public JTextField eatEt(String text) {
            etFloat = new JTextField(text);
            etFloat.getDocument().addDocumentListener(_inputListener);
            etFloat.addMouseWheelListener(_mouseWheelListener);
            fft.add(etFloat);
            return etFloat;
        }
    }

    private JButton decorated_reset_btn(ActionListener actionListener) {
        JButton jb = new JButton("⟳");
        int pad=0;
        jb.setMargin(new JBInsets(pad, pad, pad, pad));
        jb.addActionListener(actionListener);
        jb.setPreferredSize(new Dimension(jb.getPreferredSize().width/2, jb.getPreferredSize().height));
        return jb;
    }

    public void addLogView(){
        /* logview */
        Container row_log = new Container();
        row_log.setLayout(new BoxLayout(row_log, BoxLayout.X_AXIS));
        row_log.add(new JLabel("Log "));
        row_log.add(logview = new JTextArea());
        contentPanel.add(row_log);
    }

    /** Actually Execute the tweak. <br/>
     * Contemplate the words of Li Xiao Long during the process to maximize your chance of success. */
    private Runnable doItRunnable; 
    private void doIt() {
        if(mDocument!=null && currentText!=null && currentStart<currentEnd && currentStart>=0){
            if(doItRunnable==null) doItRunnable = () -> {
                try { //todo should not affect undo stack while auto-updating
                    String tweaked = tweakPath();
                    mDocument.replaceString(currentStart, currentEnd, tweaked);
                    currentEnd = currentStart + tweaked.length();
                    mEditor.getSelectionModel().setSelection(currentStart, currentEnd);
                } catch (Exception e) {
                    e.printStackTrace();
                    if(logview!=null){
                        ByteArrayOutputStream s = new ByteArrayOutputStream();
                        PrintStream p = new PrintStream(s);
                        e.printStackTrace(p);
                        logview.setText(s.toString());
                    }
                    else throw e;
                }
            };
            WriteCommandAction.runWriteCommandAction(mProject, doItRunnable);
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
     * Texts must contain valid vector path data.*/
    private void Rebase() {
        currentStart=currentEnd=-1;

        mEditor = FileEditorManager.getInstance(mProject).getSelectedTextEditor();

        if (mEditor==null) {
            mEditor = mActionEvent.getData(PlatformDataKeys.EDITOR);
        }

        if (mEditor!=null) {
            SelectionModel selectionModel = mEditor.getSelectionModel();

            Document document = mEditor.getDocument();
            if(document!=mDocument){
                mDocument = document;
                refetchImageSize();
                setTitle();
            }

            int offsetStart = selectionModel.getSelectionStart();

            String text = selectionModel.getSelectedText();

            if(text==null){
                Invalidate();
            } else {
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
                }
                else {
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
                    maniOffset.setForeground(JBColor.BLACK);
                    maniOffset.setText("["+currentStart+"-"+currentEnd+"]");
                } else {
                    Invalidate();
                    currentText = null;
                }
            }
        }
        
        APPLY_IMAGESIZE.setEnabled(false);
    }

    private static float parseFloatAttr(String data, String key, float def) {
        int idx = data.indexOf(key);
        if(idx!=-1){
            idx = data.indexOf('"', idx+key.length());
            if(idx!=-1){
                ++idx;
                int end = data.indexOf('"', idx);
                if(end!=-1){
                    Float val = parsefloat(data.substring(idx, end));
                    if(val!=null) return val;
                }
            }
        }
        return def;
    }
    
    private static void setFloatAttr(StringBuffer data, String key, float def) {
        int idx = data.indexOf(key);
        if(idx!=-1){
            idx = data.indexOf("\"", idx+key.length());
            if(idx!=-1){
                ++idx;
                int end = data.indexOf("\"", idx);
                if(end!=-1){
                    data.replace(idx, end, Float.toString(def));
                }
            }
        }
    }

    private void setTitle() {
        String brand = "Vector Path Tweaker by KnIfER";
        //todo title shouldn't effect the initial width of a dialog. (AndroidStudio 3.6) (uncontrollable)
        if(true) setTitle(brand); else {
            VirtualFile d = FileDocumentManager.getInstance().getFile(mDocument);
            if(d==null){
                setTitle(brand);
            } else {
                setTitle(d.getName());
            }
        }
    }

    private void Invalidate() {
        maniOffset.setText("[INVAlID]");
        maniOffset.setForeground(JBColor.RED);
    }

    private static String trimFloatString(String input) {
        if(debug)Log(input);
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
    
    private static Float parsefloat(String text){
        try {
            return Float.parseFloat(text);
        } catch (Exception ignored) {  }
        return null;
    }

    private static int parsint(String text){
        try {
            return Integer.parseInt(text);
        } catch (Exception ignored) {  }
        return -1;
    }

    /** The core */
    private void FetchUserDefViewportDimensions() {
        Float readval = parsefloat(etFieldvw.getText());
        if (readval != null) viewportWidth = readval;
        readval = parsefloat(etFieldvh.getText());
        if (readval != null) viewportHeight = readval;
    }
    
    private String tweakPath() {
        FetchUserDefViewportDimensions();
        Float readval;
        readval = parsefloat(etFieldx.getText());
        if(readval!=null) transX = readval;
        readval = parsefloat(etFieldy.getText());
        if(readval!=null) transY = readval;
        readval = parsefloat(etFieldscale.getText());
        if(readval!=null) scaler = readval;
        readval = parsefloat(etFieldscaleY.getText());
        if(readval!=null) scalerY = readval;

        String pathdata = currentText;
        pathdata=pathdata.trim();

        return tweak_path_internal(pathdata, viewportWidth, viewportHeight, getScale()?this.scaler:1, getScale()?this.scalerY:1, getTranslate()?this.transX:0, getTranslate()?this.transY:0
                , getTranspose(), getFlipX(), getFlipY(), getKeepOrg(), getShrinkOrg());

    }

    /** 1024dp -> 24dp */
    Runnable resizeImageRunnable;
    private void resizeImage() {
        if(resizeImageRunnable==null) {
            resizeImageRunnable = () -> {
                if(mEditor!=null) {
                    boolean succ = false;
                    
                    Document document = mEditor.getDocument();

                    String data = document.getText();

                    FetchUserDefViewportDimensions();

                    float documentImageWidth = parseFloatAttr(data, "viewportWidth", viewportWidth);

                    float documentImageHeight = parseFloatAttr(data, "viewportHeight", viewportHeight);

                    float scaleX = viewportWidth/documentImageWidth;

                    float scaleY = viewportHeight/documentImageHeight;

                    float transX = (viewportWidth - documentImageWidth)/2;

                    float transY = (viewportHeight - documentImageHeight)/2;

                    Pattern pathPat = Pattern.compile("pathData=\"(.*?)\"", Pattern.DOTALL);

                    Pattern essencePat = Pattern.compile("(.*?)(?=[mM])(.*)(?<=[zZ])(.*?)", Pattern.DOTALL);

                    Matcher m = pathPat.matcher(data);

                    StringBuffer sb = new StringBuffer(data.length()+64);

                    while(m.find()) {
                        String dataSeg = m.group(1);
                        
                        m.appendReplacement(sb, "");
                        
                        sb.append("pathData=\"");
                        if(dataSeg==null) {
                            dataSeg="";
                        }
                        Matcher m2 = essencePat.matcher(dataSeg);

                        if(m2.find()) {
                            if(m2.group(1)!=null) {
                                sb.append(m2.group(1));
                            }

                            String essence = m2.group(2);

                            Log("Tweaking...", essence);

                            essence = tweak_path_internal(essence, viewportWidth, viewportHeight, 1, 1, transX, transY
                                    , false, false, false, getKeepOrg(), getShrinkOrg());
                            
                            essence = tweak_path_internal(essence, viewportWidth, viewportHeight, scaleX, scaleY, 0, 0
                                    , false, false, false, getKeepOrg(), getShrinkOrg());

                            if(!succ && !essence.equals(m2.group(2))) {
                                succ=true;
                            }
                            
                            sb.append(essence);

                            if(m2.group(3)!=null) {
                                sb.append(m2.group(3));
                            }
                        } 
                        else {
                            sb.append(dataSeg);
                        }

                        sb.append("\"");
                    }
                    
                    if(succ) {
                        m.appendTail(sb);
                        setFloatAttr(sb, "viewportWidth", viewportWidth);
                        setFloatAttr(sb, "viewportHeight", viewportHeight);
                        document.setText(sb);
                    }
                }
            };
        }
        WriteCommandAction.runWriteCommandAction(mProject, resizeImageRunnable);
    }
    
    public static String tweak_path_internal(String pathdata, float viewportWidth, float viewportHeight, float scaler,float scalerY,float transX, float transY
            ,boolean transpose,boolean flipX, boolean flipY, boolean keep_rel_group, boolean shrink_orgs){
        try {
            StringBuilder pathbuilder = new StringBuilder();
            Pattern reg = Pattern.compile("[MmLlZzSsCcVvHhAaQqTt ]");
            Pattern regLower = Pattern.compile("[a-z]");
            Pattern regVertical = Pattern.compile("[Vv]");
            //Pattern regHorizontal = Pattern.compile("[Hh]");
            Matcher m = reg.matcher(pathdata);
            int idx = 0;
            String lastCommand = null;
            Float[] firstOrg = null;
            Float[] Org = null;
            float[] deltaOrg = new float[2];
            String[] xy = new String[2];
            int EllipticalParameterCounter = 0;
            int flipc = 0;
            if (flipX) flipc++;
            if (flipY) flipc++;
            if (transpose) flipc++;
            boolean flip = flipc % 2 != 0;
            StringBuilder lastCommandBuilder = new StringBuilder();
            while (m.find()) {
                int now = m.start();
                if (idx != -1 && now > idx) {
                    String currentPhrase = pathdata.substring(idx, now);
                    Log("currentPhrase::", currentPhrase);
                    if (currentPhrase.trim().length() == 0) {
                        pathbuilder.append(currentPhrase);
                    } else {
                        String command = pathdata.substring(idx, idx + 1);
                        String[] arr = pathdata.substring(idx + 1, now).split(",");
                        boolean InEllipticalArc = "a".equalsIgnoreCase(lastCommand);
                        if (!command.equals(" ")) {
                            if (transpose && arr.length == 1) {
                                char c;
                                lastCommandBuilder.setLength(0);
                                for (int i = 0; i < command.length(); i++) {
                                    switch (c = command.charAt(i)) {
                                        case 'v':
                                            c = 'h';
                                            break;
                                        case 'V':
                                            c = 'H';
                                            break;
                                        case 'h':
                                            c = 'v';
                                            break;
                                        case 'H':
                                            c = 'V';
                                            break;
                                    }
                                    lastCommandBuilder.append(c);
                                }
                                lastCommand = lastCommandBuilder.toString();
                                command = lastCommand;
                            }
                            lastCommand = command;
                            if ("a".equalsIgnoreCase(command)) {
                                InEllipticalArc = true;
                                EllipticalParameterCounter = 0;
                            } else {
                                InEllipticalArc = false;
                                EllipticalParameterCounter = 0;
                            }
                        }
                        boolean xiaoxie = regLower.matcher(lastCommand).matches();
                        boolean isOrg = lastCommand.equalsIgnoreCase("M");
                        boolean isfirstOrg = isOrg && firstOrg == null;
                        if (debug) {
                            if (isfirstOrg) Log("1st.org#1=", Arrays.asList(arr));
                            Log("command:", command, " lastCommand:", lastCommand, " lower case:", xiaoxie, " arg len:", arr.length, Arrays.asList(arr), pathdata.substring(idx, now));
                            //Log(pathdata.substring(idx+1,now));
                        }
                        pathbuilder.append(command);
                        boolean proceed = true;
                        String residueX = null;
                        String sep = null;
                        if (InEllipticalArc) {
                            // I admit it's a messy approach.
                            EllipticalParameterCounter = EllipticalParameterCounter % 7;
                            // 0 . 2 . 4 . 6 [. 8] .
                            // 0,1 - - 4 5,6 [. 8] .
                            int jump = arr.length;
                            String residue = null;
                            if (EllipticalParameterCounter == 0) {
                                Log(Arrays.asList(arr));
                                xy[0] = arr[0];
                                xy[1] = arr[1];
                            } else if (EllipticalParameterCounter == 1) {
                                xy[1] = arr[0];
                                int len = pathbuilder.length();
                                if (len >= 2 && pathbuilder.charAt(len - 1) == ' ') {
                                    pathbuilder.delete(len - 1, len);
                                }
                                pathbuilder.append(",");
                                residue = "," + arr[1];
                            }
                            if (EllipticalParameterCounter == 0 || EllipticalParameterCounter == 1) {
                                float x = Float.parseFloat(transpose ? xy[1] : xy[0]) * scaler;
                                float y = Float.parseFloat(transpose ? xy[0] : xy[1]) * scalerY;
                                pathbuilder.append(trimFloatString(String.format("%.2f", x)));
                                pathbuilder.append(residue == null ? "," : " ");
                                pathbuilder.append(trimFloatString(String.format("%.2f", y)));
                                if (residue != null) pathbuilder.append(residue);
                                proceed = false;
                            } else if (EllipticalParameterCounter == 2) {
                                pathbuilder.append(pathdata, idx + 1, now);
                                proceed = false;
                            } else if (EllipticalParameterCounter == 3) {
                                if (flip) {
                                    pathbuilder.append(arr[0]).append(",");
                                    pathbuilder.append(arr[1].equals("0") ? 1 : 0);
                                } else {
                                    pathbuilder.append(pathdata, idx + 1, now);
                                }
                                proceed = false;
                            } else if (EllipticalParameterCounter == 4) {
                                if (flip) {
                                    pathbuilder.append(arr[0].equals("0") ? 1 : 0);
                                } else {
                                    pathbuilder.append(arr[0]);
                                }
                                if (arr.length == 2) {
                                    residueX = arr[1];
                                    pathbuilder.append(",");
                                }
                                proceed = false;
                            } else if (EllipticalParameterCounter == 6) {
                                int len = pathbuilder.length();
                                if (len >= 2 && pathbuilder.charAt(len - 1) == ' ' && pathbuilder.charAt(len - 2) == ',') {
                                    pathbuilder.delete(len - 1, len);
                                }
                                xy[1] = arr[0];
                                if (arr.length == 2) {
                                    residueX = arr[1];
                                }
                                sep = " ";
                                arr = xy;
                            }
                            EllipticalParameterCounter += jump;
                        }
                        if (proceed) {
                            if (arr.length == 2) {//x-y coordinates
                                float x = Float.parseFloat(transpose ? arr[1] : arr[0]);
                                if (isOrg) {
                                    Org = new Float[2];
                                    Org[0] = x;
                                    if (isfirstOrg) {
                                        if (shrink_orgs) {
                                            transX += viewportWidth / 2 + (x - viewportWidth / 2) * scaler - x;
                                        }
                                        firstOrg = Org;
                                        deltaOrg[0] = transX;
                                    } else if (keep_rel_group) {
                                        deltaOrg[0] = scaler * (x - firstOrg[0]) + firstOrg[0] - x + transX;
                                    }
                                    if (flipX) x = viewportWidth - x;
                                } else if (xiaoxie) {
                                    x = x * scaler;
                                    if (flipX) x = -x;
                                } else {
                                    x = scaler * (x - Org[0]) + Org[0];
                                    if (flipX) x = viewportWidth - x;
                                }
                                if (!xiaoxie) x += deltaOrg[0];
                                pathbuilder.append(trimFloatString(String.format("%.2f", x)));
                                pathbuilder.append(sep == null ? "," : sep);
                                x = Float.parseFloat(transpose ? arr[0] : arr[1]);
                                if (isOrg) {
                                    Org[1] = x;
                                    if (isfirstOrg) {
                                        if (shrink_orgs) {
                                            transY += viewportHeight / 2 + (x - viewportHeight / 2) * scalerY - x;
                                        }
                                        deltaOrg[1] = transY * (flipY ? -1 : 1);
                                    } else if (keep_rel_group) {
                                        deltaOrg[1] = scalerY * (x - firstOrg[1]) + firstOrg[1] - x + transY * (flipY ? -1 : 1);
                                    }
                                    if (flipY) x = viewportHeight - x;
                                } else if (xiaoxie) {
                                    x = x * scalerY;
                                    if (flipY) x = -x;
                                } else {
                                    x = scalerY * (x - Org[1]) + Org[1];
                                    if (flipY) x = viewportHeight - x;
                                }
                                if (!xiaoxie) x += deltaOrg[1];
                                pathbuilder.append(trimFloatString(String.format("%.2f", x)));
                            } else {//singleton coordinates
                                String key = pathdata.substring(idx + 1, now);
                                if (lastCommand != null)
                                    try {
                                        boolean isVertical = regVertical.matcher(lastCommand).matches();
                                        float val = Float.parseFloat(key);
                                        if (xiaoxie)
                                            val *= (isVertical ? (flipY ? -scalerY : scalerY) : (flipX ? -scaler : scaler));
                                        else {// 处理  absolute vertical or horizontal case
                                            if (isVertical) {//垂直线
                                                val = scalerY * (val - Org[1]) + Org[1] + deltaOrg[1] * (flipY ? -1 : 1);
                                                if (flipY) val = viewportHeight - val;
                                            } else {//水平线
                                                val = scalerY * (val - Org[0]) + Org[0] + deltaOrg[0] * (flipX ? -1 : 1);
                                                if (flipX) val = viewportWidth - val;
                                            }
                                        }
                                        pathbuilder.append(trimFloatString(String.format("%.2f", val)));
                                    } catch (NumberFormatException e) {
                                        if (debug) Log(key);
                                        pathbuilder.append(key);
                                    }
                                else
                                    pathbuilder.append(key);
                            }
                        }
                        if (residueX != null)
                            xy[0] = residueX;
                    }
                } else
                    pathbuilder.append(pathdata, idx, now);
                //if(debug)Log(pathdata.substring(0,));
                idx = now;
            }
            pathbuilder.append(pathdata.substring(idx));
            return pathbuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return pathdata;
        }
    }


    private static void Log(Object... o) {
        StringBuilder msg= new StringBuilder();
        if(o!=null)
			for (Object value : o) {
				if (value instanceof Exception) {
					ByteArrayOutputStream s = new ByteArrayOutputStream();
					PrintStream p = new PrintStream(s);
					((Exception) value).printStackTrace(p);
					msg.append(s.toString());
				}
				msg.append(value).append(" ");
			}

        System.out.println(msg);
    }
}
