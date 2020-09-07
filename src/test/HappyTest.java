package test;

import com.intellij.ui.components.JBCheckBox;
import org.junit.Test;
import ui.PathTweakerDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HappyTest {
	private static Float parsefloat(String text){
		try {
			return Float.parseFloat(text);
		} catch (Exception ignored) {  }
		return null;
	}





	@Test
	public void TestPat(){
		Pattern essencePat = Pattern.compile("(.*)?(?=[mM])(.*)(?<=[zZ])(.*)?", Pattern.DOTALL);

		Matcher m = essencePat.matcher("asadasdMasdsaZ");

		while(m.find()) {
			String dataSeg = m.group(1);
			Log(dataSeg);
		}
	}

	@Test
	public void TestCoreMethod(){
		//改造vector path
		float viewportHeight=1024, viewportWidth=1024;
		float scaler =  1f;
		float scalerY = scaler;
		float transX=0f;
		float transY=0f;
		boolean transpose = false;
		boolean flipX = false;
		boolean flipY = false;
		boolean keep_rel_group = true;
		boolean shrink_orgs = true;

		String pathdata = "M810.53,724.66 A511.6,511.6 0,0 1,331.99 33.06,511.36 511.36,0 1,0 990.77,692.32 a509.45,509.45 0,0 1,-180.24 32.35z\n";


		System.out.println(PathTweakerDialog.tweak_path_internal(pathdata, viewportWidth, viewportHeight, scaler, scalerY, transX, transY
			, transpose, flipX, flipY, keep_rel_group, shrink_orgs));

		System.out.println(pathdata);
	}

	public static void main(String[] args) {
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel maniOffset = new JLabel();


		MouseWheelListener mouseWheelListener = e -> {
			JTextField fieldToModify = (JTextField) e.getSource();
			String valstr = fieldToModify.getText();
			Float val = parsefloat(valstr);
			float now = 0;
			double quantity = 0.01;
			int keep=2;
			int caret = -1;
			if(val!=null){
				if(fieldToModify.isFocusOwner())
					caret = fieldToModify.getCaretPosition();
				//maniOffset.setText(caret+"");
				now = val;
				int len = valstr.length();
				int idx = valstr.lastIndexOf(".");
				if(idx>0){
					keep = len-idx-1;
				}
				int level;
				if(caret>=0 && caret<=len){
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
			if(keep>5) keep=5;
			valstr = String.format("%."+keep+"f", now-quantity*e.getWheelRotation());
			fieldToModify.setText(valstr);
			if(caret!=-1) {
				if(caret>=valstr.length()) caret=valstr.length();
				fieldToModify.setCaretPosition(caret);
			}
		};


		/* offset */
		Container row_offset = new Container();
		row_offset.setLayout(new BoxLayout(row_offset, BoxLayout.X_AXIS));
		JLabel titleOffset = new JLabel("Current Selection : ");
		JButton buttonSelect = new JButton("Rebase");
		JButton buttonRevert = new JButton("Revert");
		row_offset.add(titleOffset);
		row_offset.add(maniOffset);
		row_offset.add(buttonRevert);
		row_offset.add(buttonSelect);

		//translate
		Container row_translate = new Container();
		row_translate.setLayout(new BoxLayout(row_translate, BoxLayout.X_AXIS));
		JBCheckBox check_translate = new JBCheckBox();
		JTextField etFieldx = new JTextField(); etFieldx.addMouseWheelListener(mouseWheelListener);
		JTextField etFieldy = new JTextField();
		JLabel titleTranslate = new JLabel("TRANLATION ");
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



		//scale
		Container row_scale = new Container();
		row_scale.setLayout(new BoxLayout(row_scale, BoxLayout.X_AXIS));
		JBCheckBox check_scale = new JBCheckBox();
		JTextField etFieldscale = new JTextField();
		JLabel titleScale = new JLabel("SCALE: ");
		titleScale.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				check_scale.setSelected(!check_scale.isSelected());
			}
		});
		row_scale.add(check_scale);
		row_scale.add(titleScale);
		row_scale.add(etFieldscale);


		panel.add(row_offset);
		panel.add(row_translate);
		panel.add(row_scale);


		final Container container = new Container();
		panel.add(container, BorderLayout.SOUTH);

		JFrame mainFrame = new JFrame("第一个程序");

		mainFrame.setSize(500,500);
		mainFrame.add(panel);

		mainFrame.setVisible(true);
	}

	private static void Log(String name) {
		System.out.println(name);
	}
}
