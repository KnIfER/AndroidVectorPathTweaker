package ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ui.PathTweakerDialog.*;


public class PathToolDialog extends DialogWrapper {
    private PathTweakerDialog attachedTweaker;
    
    private int lastY=-1;

    PathToolDialog(@Nullable Project project) {
        super(project, false);
        setModal(false);
        init();
        getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ReleaseInstance();
            }
        });
        
        //setOKActionEnabled(false);
        //getCancelAction().getValue()
    }

    void ReleaseInstance() {
        if(attachedTweaker!=null) {
            attachedTweaker.removeComponentListener(dockMover);
        }
        PathTweakerDialog.ToolsDialog=null;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        ReleaseInstance();
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        ReleaseInstance();
    }
    
    void invokeModed() {
        if(attachedTweaker!=null) {
            dockMover.componentMoved(new ComponentEvent(attachedTweaker.getWindow(), ActionEvent.ACTION_PERFORMED));
        }
    }

    private ComponentListener dockMover = new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent e) {
            //Log("componentMoved");
            Point tweaker_location = e.getComponent().getLocation();
            Window window = getWindow();
            Point this_location = window.getLocation();
            int thisY=this_location.y;
            if(lastY==-1) {
                thisY = tweaker_location.y+(attachedTweaker.getWindow().getHeight()-window.getHeight())/2;
            } else {
                thisY += tweaker_location.y-lastY;
            }
            lastY = tweaker_location.y;
            window.setLocation(tweaker_location.x-window.getWidth(), thisY);
        }
    };

    void syncAndToggle(PathTweakerDialog pathTweakerDialog) {
        if(attachedTweaker==pathTweakerDialog&&isShowing()) {
            getWindow().setVisible(false);
        } else {
            if(attachedTweaker!=pathTweakerDialog) {
                if(attachedTweaker!=null) {
                    attachedTweaker.removeComponentListener(dockMover);
                }
                lastY=-1;
                show();
                attachedTweaker=pathTweakerDialog;
                attachedTweaker.addComponentListener(dockMover);
                invokeModed();
            } else {
                lastY=-1;
                getWindow().setVisible(true);
                invokeModed();
            }
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        final JPanel panel = new JPanel(new BorderLayout(16, 6));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        setTitle("Vector Tools");

        ItemListener itemListener= e -> {
            String name = ((JCheckBox)e.getItem()).getName();
            boolean checked = e.getStateChange()==ItemEvent.SELECTED;
            switch (parsint(name)){
                case 0:
                break;
            }
        };

        PathTweakerDialog.LayouteatMan layoutEater = new PathTweakerDialog.LayouteatMan(itemListener, null, null);

        Container vTbg = layoutEater.startNewLayout();
        layoutEater.eatJButton("Insert/Remove Background Canvas", e-> InjectBG(true) );
        layoutEater.eatJButton("  / Copy", e-> InjectBG(false) );

        Container ft1 = layoutEater.startNewLayout();
        layoutEater.eatJButton("Format coords to get processable data (L0 0 -> L0,0)", e-> ForCoords(true) );
        Container ft2 = layoutEater.startNewLayout();
        layoutEater.eatJButton("Unformat coords to remove commas ( L0,0 -> L0 0 )", e-> ForCoords(false) );
        
        Container cPtx = layoutEater.startNewLayout();
        layoutEater.eatJButton("Export SVG...  ", e-> ShowSvgExporter() );
        layoutEater.eatJButton("Copy last initial path data", e-> copyText(attachedTweaker.currentText) );

        panel.add(vTbg);
        panel.add(ft1);
        panel.add(ft2);
        //panel.add(cPtx);

        return panel;
    }

    private void ShowSvgExporter() {
        
    }

    /** Remove or insert ',' between coords. */
    private void ForCoords(boolean format) {
        PathTweakerDialog tweaker = attachedTweaker;
        if(tweaker!=null && tweaker.mDocument!=null) {
            String pathdata = tweaker.currentText;
            if(pathdata!=null) {
                if(format) {
                    StringBuilder pathbuilder = universal_buffer;
                    pathbuilder.setLength(0);
                    pathdata = pathdata.replaceAll("[\\s]+", " ");
                    pathdata = pathdata.replaceAll(" ?([a-zA-Z]) ?", "$1");
                    Matcher m = Pattern.compile("[a-zA-Z ]").matcher(pathdata);
                    int idx = 0;
                    int numberCount=0;
                    while (m.find()) {
                        int now = m.start();
                        if (idx != -1 && now > idx) {
                            //String currentPhrase = pathdata.substring(idx, now);
                            String command = pathdata.substring(idx, idx + 1);
                            String arr = pathdata.substring(idx + 1, now);
                            if (!command.equals(" ")) {
                                //lastCommand = command;
                                numberCount=0;
                                if(parsefloat(arr)!=null) {
                                    numberCount++;
                                }
                                pathbuilder.append(command);
                            } else {
                                pathbuilder.append((numberCount+1)%2==0?","
                                        : command);
                                if(parsefloat(arr)!=null) {
                                    numberCount++;
                                }
                            }
                            pathbuilder.append(arr);
                        } else
                            pathbuilder.append(pathdata, idx, now);
                        //if(debug)Log(pathdata.substring(0,));
                        idx = now;
                    }
                    pathbuilder.append(pathdata.substring(idx));
                    String newData = pathbuilder.toString();
                    if(true) {
                        newData=newData.replaceAll("(:?<[\\S])([a-zA-Z])", "$1");
                    }
                    tweaker.replaceSelectedPathdata(newData);
                }
                else {
                    tweaker.replaceSelectedPathdata(pathdata.replace(',', ' '));
                }
            }
        }
    }

    StringBuilder universal_buffer = new StringBuilder(4096);
    
    
    private void InjectBG(boolean inject) {
        PathTweakerDialog tweaker = attachedTweaker;
        if(tweaker!=null && tweaker.mDocument!=null) {
            String data = tweaker.mDocument.getText();
            if(inject) {
                int tagIdx = data.indexOf("android:name=\"bg\"");
                boolean removed=false;
                int length = data.length();
                if(tagIdx>0) {
                    int removeSt = data.lastIndexOf("<", tagIdx);
                    int removeEd = data.indexOf(">", tagIdx);
                    if(removeEd>removeSt && removeSt>0) {
                        removed=true;
                        if(removeEd<length&&data.charAt(removeEd+1)<=' ') {
                            removeEd+=1;
                            if(removeEd<length&&data.charAt(removeEd+1)<=' ') {
                                removeEd+=1;
                            }
                        }
                        universal_buffer.setLength(0);
                        universal_buffer.append(data, 0, removeSt-1);
                        universal_buffer.append(data, removeEd+1, length);
                        tweaker.setDocumentText(universal_buffer, -(removeEd-removeSt+2));
                    }
                }
                if(!removed) {
                    tagIdx = data.indexOf("vector");
                    if(tagIdx>0) {
                        tagIdx = data.indexOf(">", tagIdx);
                        if(tagIdx>0) {
                            int insIdx = data.indexOf("\n", tagIdx);
                            if(insIdx<0) insIdx = tagIdx+1;
                            if(insIdx< length) {
                                String text = prepareCanvas(data).toString();
                                universal_buffer.setLength(0);
                                universal_buffer.append(data, 0, insIdx);
                                universal_buffer.append(text);
                                universal_buffer.append(data, insIdx, length);
                                tweaker.setDocumentText(universal_buffer, text.length());
                            }
                        }
                    }
                }
            } else {
                copyText(prepareCanvas(data).toString());
            }
        }
    }

    private void copyText(String text) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable tText = new StringSelection(text);
        clip.setContents(tText, null);
    }

    private StringBuilder prepareCanvas(String docData) {
        int documentImageWidth = (int) parseFloatAttr(docData, "viewportWidth", 24);
        int documentImageHeight = (int) parseFloatAttr(docData, "viewportHeight", 24);
        universal_buffer.setLength(0);
        universal_buffer.append("\n\t<path android:fillColor=\"#D4F5D4\" android:name=\"bg\" android:pathData=\"M0,0 ").append(documentImageWidth).append(",0 ").append(documentImageWidth).append(",").append(documentImageHeight).append(" 0,").append(documentImageHeight).append("\"/>\n").toString();
        return universal_buffer;
    }

    boolean isDockedTo(PathTweakerDialog dlg) {
        return dlg==attachedTweaker;
    }

    public boolean isDisposed() {
        try{
            return super.isDisposed();
        } catch (Exception e) {
            return false;
        }
    }
}
