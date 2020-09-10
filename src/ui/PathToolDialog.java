package ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static ui.PathTweakerDialog.parsint;


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

    private void ReleaseInstance() {
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
            } else {
                show();
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
        layoutEater.eatJButton("Insert Background Canvas", null);

        panel.add(vTbg);


        return panel;

    }
}
