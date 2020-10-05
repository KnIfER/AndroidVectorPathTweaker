package ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import org.adrianwalker.multilinestring.Multiline;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;

import static ui.PathTweakerDialog.*;


public class SVGEXINDialog extends DialogWrapper {
    private boolean systemInitialized=false;
    private PathTweakerDialog attachedTweaker;
    private PathToolDialog attachedTools;
    int state;
    int lastY=-1;

    private static long firstflag;
    private JTextField etFieldvw;
    private JTextField etFieldvh;

    private JTextField etFieldscale;
    private JTextArea etField;
    
    static float viewportHeight=24, viewportWidth=24;
    static float scaler =  1f;
    private JButton APPLY_IMAGESIZE;
    
    String mText;
    String bakedExport;

    @Multiline(flagPos=0, shift=0) static boolean getScale(){ firstflag=firstflag; throw new RuntimeException(); }
    @Multiline(flagPos=0, shift=0) static void setScale(boolean val){ firstflag=firstflag; throw new RuntimeException(); }

    @Multiline(flagPos=1, shift=1) static boolean getUseSelection(){ firstflag=firstflag; throw new RuntimeException(); }
    @Multiline(flagPos=1, shift=1) static void setUseSelection(boolean val){ firstflag=firstflag; throw new RuntimeException(); }

    @Multiline(flagPos=2, shift=0) static boolean getExportToClipboard(){ firstflag=firstflag; throw new RuntimeException(); }
    @Multiline(flagPos=2, shift=0) static void setExportToClipboard(boolean val){ firstflag=firstflag; throw new RuntimeException(); }

    @Multiline(flagPos=3, shift=0) static boolean getExportOnlyFirst(){ firstflag=firstflag; throw new RuntimeException(); }
    @Multiline(flagPos=3, shift=0) static void setExportOnlyFirst(boolean val){ firstflag=firstflag; throw new RuntimeException(); }

    @Multiline(flagPos=4, shift=0) static boolean getExportOnlyPathData(){ firstflag=firstflag; throw new RuntimeException(); }
    @Multiline(flagPos=4, shift=0) static void setExportOnlyPathData(boolean val){ firstflag=firstflag; throw new RuntimeException(); }

    @Multiline(flagPos=5, shift=1) static boolean getMerge(){ firstflag=firstflag; throw new RuntimeException(); }
    @Multiline(flagPos=5, shift=1) static void setMerge(boolean val){ firstflag=firstflag; throw new RuntimeException(); }


    SVGEXINDialog(@Nullable PathToolDialog toolDialog, int reason) {
        super(toolDialog.attachedTweaker.mProject, false);
        attachedTweaker=toolDialog.attachedTweaker;
        attachedTools=toolDialog;
        setModal(false);
        init();
        getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ReleaseInstance();
            }
        });
        state=reason;
        toolDialog.svgExporter=this;
        refetchText();
        bakeExportedData();
        setOKButtonText(reason==0?"Export":"Import");
        //setOKActionEnabled(false);
    }

    private void bakeExportedData() {
        String text = attachedTools.bakeExportedData(this);
        etField.setText(text);
    }

    private void refetchText() {
        mText = attachedTweaker.getDocText(getUseSelection());
    }

    void ReleaseInstance() {
        systemInitialized=false;
        if(attachedTools!=null) {
            dispose();
            attachedTools.svgExporter=null;
            attachedTools=null;
            attachedTweaker=null;
            mText=null;
        }
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if(attachedTools!=null) {
            attachedTools.doExport(this);
        }
        ReleaseInstance();
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        ReleaseInstance();
    }
    
    private void refetchImageSize() {
        if(attachedTweaker!=null) {
            viewportWidth = attachedTweaker.viewportWidth;
            viewportHeight = attachedTweaker.viewportHeight;
            etFieldvw.setText(Float.toString(viewportWidth));
            etFieldvh.setText(Float.toString(viewportHeight));
            APPLY_IMAGESIZE.setEnabled(false);
        }
    }
    
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        final JPanel panel = new JPanel(new BorderLayout(16, 6));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        setTitle("SVG Tools");

        ItemListener itemListener= e -> {
            if(systemInitialized) {
                String name = ((JCheckBox)e.getItem()).getName();
                boolean checked = e.getStateChange()==ItemEvent.SELECTED;
                switch (parsint(name)){
                    case 1:
                        setScale(checked);
                        break;
                    case 2: {
                        setUseSelection(checked);
                        if (attachedTools != null) {
                            refetchText();
                        }
                    } break;
                    case 3: {
                        setExportToClipboard(checked);
                        checkEssence.setEnabled(checked);
                        if(!getExportOnlyPathData()) {
                            return;
                        }
                    } break;
                    case 4: {
                        setExportOnlyFirst(checked);
                        checkMerge.setEnabled(!checked);
                    } break;
                    case 5: {
                        setExportOnlyPathData(checked);
                    } break;
                    case 6: {
                        setMerge(checked);
                    } break;
                    default:
                    return;
                }
                if (attachedTools != null) {
                    bakeExportedData();
                }
            }
        };

        DocumentListener inputListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onTextChanged(e); }
            @Override public void removeUpdate(DocumentEvent e) { onTextChanged(e); }
            @Override public void changedUpdate(DocumentEvent e) {  }
            private void onTextChanged(DocumentEvent e) {
                if (systemInitialized && getScale()) {
                    javax.swing.text.Document doc = e.getDocument();
                    if(doc==etFieldscale.getDocument()) {
                        Float f=parsefloat(etFieldscale.getText());
                        scaler=f==null?1:f;
                        bakeExportedData();
                    }
                }
            }
        };

        LayouteatMan layoutEater = new LayouteatMan(itemListener, inputListener, mouseWheelListener);
        
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
        refetchImageSize();

        /* scale */
        Container row_scale = layoutEater.startNewLayout();
        layoutEater.eatLabelCheck(1, getScale(), "SCALE  ", true);
        layoutEater.eatLabel("X&Y:");
        etFieldscale = layoutEater.eatEt("1.0");

        /* editor */
        etField = layoutEater.eatEta(bakedExport, false);
        etField.setLineWrap(true);
        JBScrollPane JSP=new JBScrollPane(etField);
        int height=125;
        JSP.setPreferredSize(new Dimension(50, height));
        JSP.setMinimumSize  (new Dimension(0, height));
        JSP.setMaximumSize  (new Dimension(1050, height));
        
        /* first */
        Container row_first = layoutEater.startNewLayout();
        layoutEater.eatLabelCheck(6, getMerge(), "Merge", true);
        checkMerge = layoutEater.check;
        checkMerge.setEnabled(!getExportOnlyFirst());
        layoutEater.eatLabelCheck(4, getExportOnlyFirst(), "Export Only First", true);
        layoutEater.eatLabelCheck(5, getExportOnlyPathData(), "Only pathData", true);
        checkEssence = layoutEater.check;
        checkEssence.setEnabled(getExportOnlyPathData());
        
        /* selection */
        Container row_selection = layoutEater.startNewLayout();
        layoutEater.eatLabelCheck(2, getUseSelection(), "Use Selection ", true);
        layoutEater.eatLabelCheck(3, getExportToClipboard(), "Export to Clipboard ", true);
        
        
        panel.add(row_viewport);
        panel.add(row_scale);
        panel.add(JSP);
        panel.add(row_first);
        panel.add(row_selection);

        systemInitialized=true;
        return panel;
    }

    private void resizeImage() {
        
    }


    public boolean isDisposed() {
        try{
            return super.isDisposed();
        } catch (Exception e) {
            return false;
        }
    }

}
