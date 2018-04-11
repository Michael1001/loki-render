package net.whn.loki.master.utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

public class CheckBoxListener implements ActionListener {

    /**
     * It's a relative blend file name path.
     * Could have value like: "Rocks\Scenes\LOOKDEV\Props\Rocks\Link TEST.blend"
     * or simply "Link TEST.blend", if the file is not relative to any folder
     */
    private String runnableBlendFileName;
    private JButton okButton;
    private List<JCheckBox> checkBoxes;
    private final Map<String, String> blendFileNamesMap;

    public CheckBoxListener(JButton okButton, Map<String, String> blendFileNamesMap) {
        this.okButton = okButton;
        this.blendFileNamesMap = blendFileNamesMap;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JCheckBox currentCheckBox = (JCheckBox) e.getSource();
        boolean selected = currentCheckBox.isSelected();
        okButton.setEnabled(selected);
        if (selected) {
            String blendFileName = currentCheckBox.getText();
            runnableBlendFileName = blendFileNamesMap.get(blendFileName);
            for (JCheckBox checkBox : checkBoxes) {
                if (!checkBox.equals(currentCheckBox)) {
                    checkBox.setEnabled(false);
                }
            }
        } else {
            runnableBlendFileName = null;
            for (JCheckBox checkBox : checkBoxes) {
                checkBox.setEnabled(true);
            }
        }
    }

    public String getRunnableBlendFileName() {
        return runnableBlendFileName;
    }

    public void addCheckBoxes(List<JCheckBox> checkBoxes) {
        this.checkBoxes = checkBoxes;
    }
}
