package com.modcrafting.luyten;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by Barak on 31/7/2014.
 */
public class FinaAllBox {
    public static JFrame frame = new JFrame("Find All");
    public static DefaultListModel demoList = new DefaultListModel();
    public static void init()
    {
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "EXIT");
        frame.getRootPane().getActionMap().put("EXIT", new AbstractAction(){
            public void actionPerformed(ActionEvent e)
            {
                frame.dispose();
            }
        });

        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        initItems();
        frame.pack();
        frame.setSize(400,500);
    }

    private static void initItems()
    {
        JList list = new JList(demoList); //data has type Object[]
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        list.setVisibleRowCount(-1);
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(500, 500));

        JLabel jlbempty = new JLabel("text to search:");

        final JTextField titleText = new JTextField("");
        titleText.setPreferredSize(new Dimension(200, 25));
        JPanel wrapper = new JPanel( new FlowLayout(0, 0, FlowLayout.LEADING) );

        JButton b1 = new JButton("search");
        b1.setVerticalTextPosition(AbstractButton.CENTER);
        b1.setHorizontalTextPosition(AbstractButton.LEADING);
        b1.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                demoList.clear();
                MainMenuBar.searchall(titleText.getText());

            }
        });

        wrapper.add(jlbempty);
        wrapper.add(titleText);
        wrapper.add(b1);
        wrapper.add(listScroller);
        frame.getContentPane().add(wrapper);
    }
}
