package com.cppcxy.unity;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class UnityRefresh extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        UnityLsAdapter.INSTANCE.loadUnityProject();
    }
}
