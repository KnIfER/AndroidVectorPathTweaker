package ui;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ui.PathTweakerDialog.*;


public class PathToolDialog extends DialogWrapper {
    PathTweakerDialog attachedTweaker;
    SVGEXINDialog svgExporter;
    
    private int lastY=-1;
    private File lastExportPath;
    private File lastImportPath;

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
        if(svgExporter!=null) {
            svgExporter.close(1);
            svgExporter.ReleaseInstance();
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
    
    void invokeMoved() {
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
            if(svgExporter!=null)
            {
                window = svgExporter.getWindow();
                this_location = window.getLocation();
                thisY=this_location.y;
                if(svgExporter.lastY==-1) {
                    thisY = tweaker_location.y+(attachedTweaker.getWindow().getHeight()-window.getHeight())/2;
                } else {
                    thisY += tweaker_location.y-svgExporter.lastY;
                }
                svgExporter.lastY = tweaker_location.y;
                window.setLocation(tweaker_location.x-window.getWidth(), thisY);
            }
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
                invokeMoved();
            } else {
                lastY=-1;
                getWindow().setVisible(true);
                invokeMoved();
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
        
        Container row_export = layoutEater.startNewLayout();
        layoutEater.eatJButton("Export SVG...  ", e-> ShowSvgExporter(0) );
        row_export.add(decorated_small_btn(">>", e->doExport(svgExporter)));
        layoutEater.eatJButton("Copy last initial path data", e-> copyText(attachedTweaker.currentText) );

        Container row_import = layoutEater.startNewLayout();
        layoutEater.eatJButton("Import SVG.. ", e-> ShowSvgExporter(1) );
        row_import.add(decorated_small_btn(">>", e->doExport(svgExporter)));

        panel.add(vTbg);
        panel.add(ft1);
        panel.add(ft2);
        panel.add(row_export);
        //panel.add(row_import);

        return panel;
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

    public boolean _is_disposed;
    
    @Override
    protected void dispose() {
        _is_disposed=true;
        super.dispose();
    }

    private void ShowSvgExporter(int mode) {
        if(svgExporter==null||svgExporter.state!=mode) {
            if(svgExporter!=null) svgExporter.close(0);
            new SVGEXINDialog(this, mode);
        }
        svgExporter.lastY=-1;
        svgExporter.show();
        invokeMoved();
    }

    void doImport(SVGEXINDialog svgExporter) {
        String data = null;
        if(SVGEXINDialog.getImportFromClipboard()) {
            if(svgExporter!=null && svgExporter.state==1) {
                data = svgExporter.bakedExport;
            }
            if(data==null) {
                data = SVGEXINDialog.getClipboardText();
            }
        } 
        else {
            //todo import from file
        }
        if(data!=null) {
            float viewportWidth = attachedTweaker.viewportWidth;
            float viewportHeight = attachedTweaker.viewportHeight;
            float documentImageWidth = parseFloatAttr(data, "width", viewportWidth);
            float documentImageHeight = parseFloatAttr(data, "height", viewportHeight);
            if(svgExporter!=null && SVGEXINDialog.getUseViewport1()) {
                svgExporter.refetchImageSize(true);
                viewportWidth = SVGEXINDialog.viewportWidth;
                viewportHeight = SVGEXINDialog.viewportHeight;
            }
            float scaleX = viewportWidth/documentImageWidth;  // expected size / xml size
            float scaleY = viewportHeight/documentImageHeight; // expected size / xml size
            float transX = (viewportWidth - documentImageWidth)/2;
            float transY = (viewportHeight - documentImageHeight)/2;
            StringBuffer sb = new StringBuffer((int) (data.length()*1.5));
            Pattern pathPat = Pattern.compile("\\sd=['\"](.*?)['\"]");
            Matcher m = pathPat.matcher(data);
            while(m.find()) {
                sb.append("<path android:fillColor=\"")
                    .append("#acafa0")
                    .append("\" android:pathData=\"");
                String pathData = m.group(1);
                pathData = attachedTweaker.moveThenScaleEssence(pathData, viewportWidth, viewportHeight, transX, transY, scaleX, scaleY);
                sb.append(pathData);
                sb.append("\"/>\n");
            }
            attachedTweaker.addDocumentText(sb);
        }
    }
    
    void doExport(SVGEXINDialog svgExporter) {
        String text = svgExporter==null?null:svgExporter.bakedExport;
        if(text==null) {
            text = bakeExportedData(svgExporter);
        }
        if(text!=null) {
            if(SVGEXINDialog.getExportToClipboard()) {
                copyText(text);
                if(svgExporter!=null) {
                    svgExporter.close(0);
                }
            } else {
                JFileChooser filePicker = new JFileChooser();
                filePicker.setFileFilter(new FileNameExtensionFilter("Scalable Vector Graphics", "svg"));
                filePicker.setSelectedFile(InferExportPath());
                int returnVal = filePicker.showSaveDialog(new JPanel());
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    if(svgExporter!=null) {
                        svgExporter.close(0);
                    }
                    File path = filePicker.getSelectedFile();
                    lastExportPath = path.getParentFile();
                    try {
                        FileOutputStream fout = new FileOutputStream(path);
                        fout.write(text.getBytes(StandardCharsets.UTF_8));
                        fout.flush();
                        fout.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private File InferExportPath() {
        if(attachedTweaker!=null) { 
            VirtualFile d = FileDocumentManager.getInstance().getFile(attachedTweaker.mDocument);
            if(d!=null) {
                String name = d.getName();
                int idx = name.lastIndexOf(".");
                if(idx>0) {
                    name = name.substring(0, idx);
                }
                name=name+".svg";
                if(lastExportPath==null) {
                    lastExportPath = new File(d.getPath());
                }
                return new File(lastExportPath, name);
            }
        }
        return null;
    }

    private String reverseFetchFill(String data, int start) {
        int pathStart = data.lastIndexOf('<', start);
        //Log("reverseFetchFill::", start, pathStart, data.substring(start));
        if(pathStart<0||(start-pathStart)>256) {
            pathStart = start;
        }
        start = data.indexOf("fillColor", pathStart);
        if(start>0) {
            int st = data.indexOf("#", start)+1;
            if(st>1) {
                int ed = data.indexOf("\"", st);
                if(ed>0) {
                    data = data.substring(st, ed).trim();
                }
                if(data.matches("[0-9a-fA-F]+")) {
                    if(data.length()==8) {
                        data = data.substring(2)+data.substring(0, 2);
                    }
                    return "#"+data;
                }
            }
        }
        return "#00f";
    }

    public String bakeExportedData(SVGEXINDialog svgExporter) {
        String data;
        if(svgExporter!=null && svgExporter.state==0) {
            data = svgExporter.mText;
        } else {
            data = attachedTweaker.getDocText(SVGEXINDialog.getUseSelection());
        }
        if(data!=null) {
            float documentImageWidth = SVGEXINDialog.viewportWidth;
            float documentImageHeight = SVGEXINDialog.viewportHeight;
            float scale = SVGEXINDialog.getScale()?SVGEXINDialog.scaler:1;
            Pattern pathPat = Pattern.compile("pathData=\"(.*?)\"", Pattern.DOTALL);
            Pattern essencePat = Pattern.compile("(.*?)(?=[mM])(.*)(?<=[zZ])(.*?)", Pattern.DOTALL);
            Matcher m = pathPat.matcher(data);
            StringBuffer sb = new StringBuffer((int) (data.length()*1.5));
            boolean essencify = SVGEXINDialog.getExportToClipboard()&&SVGEXINDialog.getExportOnlyPathData();
            boolean pathFound=false;
            boolean pathMerge=!essencify&&SVGEXINDialog.getMerge();
            while(m.find()) {
                String dataSeg = m.group(1);
                //m.appendReplacement(sb, "");
                if(!essencify&&(!pathMerge||!pathFound)) {
                    sb.append("<svg width='").append(documentImageWidth)
                            .append("' height='").append(documentImageHeight)
                            .append("' viewBox='0 0 ")
                            .append(attachedTweaker.Width).append(" ")
                            .append(attachedTweaker.Height).append("' xmlns=\"http://www.w3.org/2000/svg\">\n");
                }
                sb.append("  <path fill='")
                    .append(reverseFetchFill(data, m.start())) // 找到颜色
                    .append("' d='");
                pathFound=true;
                if (dataSeg == null) {
                    dataSeg = "";
                }
                Matcher m2;
                if(scale!=1&&(m2 = essencePat.matcher(dataSeg)).find()) {
                    if(m2.group(1)!=null) {
                        sb.append(m2.group(1));
                    }
                    String essence = m2.group(2);
                    essence = tweak_path_internal(universal_buffer, essence, documentImageWidth, documentImageHeight, scale, scale, 0, 0
                            , false, false, false, true, true);
                    sb.append(essence);
                    if(m2.group(3)!=null) {
                        sb.append(m2.group(3));
                    }
                }
                else {
                    sb.append(dataSeg);
                }
                sb.append("'/>\n");
                if(!essencify&&!pathMerge) {
                    sb.append("</svg>\n");
                }
                if(SVGEXINDialog.getExportOnlyFirst()) {
                    break;
                }
            }
            if(!essencify&&pathMerge&&pathFound) {
                sb.append("</svg>\n");
            }
            //m.appendTail(sb);
            return sb.toString();
        }
        return null;
    }
}
