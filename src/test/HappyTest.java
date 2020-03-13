package test;

import com.intellij.ui.components.JBCheckBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class HappyTest {
	public static void main(String[] args) {
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));


		//translate
		Container row_translate = new Container();
		row_translate.setLayout(new BoxLayout(row_translate, BoxLayout.X_AXIS));
		JBCheckBox check_translate = new JBCheckBox();
		JTextField etFieldx = new JTextField();
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
