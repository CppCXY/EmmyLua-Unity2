package com.cppcxy.unity;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;

public class UnitySettingsPanel implements SearchableConfigurable, Configurable.NoScroll {
    private JPanel myPanel;
    private UnitySettings settings = UnitySettings.getInstance();
    private JComboBox framework;
    private JTextField unityNamespace;

    public UnitySettingsPanel() {
        DefaultComboBoxModel<UnityLuaFramework> model = new DefaultComboBoxModel<>();
        model.addElement(UnityLuaFramework.XLua);
        model.addElement(UnityLuaFramework.ToLua);

        framework.addActionListener(e -> {
            settings.setFramework((UnityLuaFramework) framework.getSelectedItem());
        });

        framework.setModel(model);
        unityNamespace.setText(settings.getShowNamespace());
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "UnitySettingsPanel2";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Unity Settings";
    }

    @Override
    public @Nullable JComponent createComponent() {
        return myPanel;
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() {
        settings.setUnityNamespace(Arrays.asList(unityNamespace.getText().split(";")));
        settings.fireChanged();
    }
}
