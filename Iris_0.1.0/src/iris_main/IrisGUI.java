package iris_main;

// Crappy GUI I put into place so that one could have a button to click on...

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class IrisGUI {
  public Component createComponents() {
//    final JLabel label = new JLabel(labelPrefix);
//    label.setForeground(Color.WHITE);
//    label.setBackground(Color.BLACK);
//    label.
    ImageIcon image =  new ImageIcon("res/icon.gif");
    JButton button = new JButton(image);
    button.setBackground(Color.BLACK);
    button.setMnemonic(KeyEvent.VK_I);
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {

    	  Iris.main(new String[]{});
      }
    });
   // label.setLabelFor(button);

    /*
     * An easy way to put space between a top-level container and its
     * contents is to put the contents in a JPanel that has an "empty"
     * border.
     */
    JPanel pane = new JPanel();
    pane.setBorder(BorderFactory.createEmptyBorder(0, //top
        0, //left
        0, //bottom
        0) //right
        );
    pane.setLayout(new GridLayout(0, 1));
    pane.add(button);
  //  pane.add(label);

    return pane;
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager
          .getCrossPlatformLookAndFeelClassName());
    } catch (Exception e) {
    }

    //Create the top-level container and add contents to it.
    JFrame frame = new JFrame("Iris");
    IrisGUI app = new IrisGUI(); 
    Component contents = app.createComponents();
    frame.getContentPane().add(contents, BorderLayout.CENTER);
    frame.setForeground(Color.RED);
    frame.setUndecorated(false);
    frame.setLocation(500,500);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
  }
}

