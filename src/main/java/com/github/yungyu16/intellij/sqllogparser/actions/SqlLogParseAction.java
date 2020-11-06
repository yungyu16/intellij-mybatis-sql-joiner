package com.github.yungyu16.intellij.sqllogparser.actions;

import com.github.yungyu16.intellij.sqllogparser.error.TipException;
import com.github.yungyu16.intellij.sqllogparser.logline.LogLineParser;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Optional;

/**
 * CreatedDate: 2020/11/5
 * Author: songjialin
 */
public class SqlLogParseAction extends AnAction {
    private static final Logger log = Logger.getInstance(LogLineParser.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        NotificationType notificationType = NotificationType.INFORMATION;
        String result = "SUCCESS";
        String detail = "拼接完成,结果已复制到粘贴板";
        try {
            String sqlLog = getClipContent().orElseThrow(() -> new TipException("请先复制sql日志到系统粘贴板"));
            String sqlStatement = LogLineParser.parse(sqlLog);
            setClipContent(sqlStatement);
        } catch (TipException ex) {
            result = "FAIL";
            detail = ex.getMessage();
            notificationType = NotificationType.WARNING;
        } catch (Exception ex) {
            result = "ERROR";
            detail = ex.getMessage();
            log.error(ex);
            notificationType = NotificationType.ERROR;
        } finally {
            NotificationGroup notificationGroup = new NotificationGroup("sqllog.actions.ParseSqlAction", NotificationDisplayType.BALLOON, false);
            Notification notification = notificationGroup.createNotification("SqlLogJoiner", result, detail, notificationType);
            Notifications.Bus.notify(notification);
        }
    }

    private Optional<String> getClipContent() {
        CopyPasteManager instance = CopyPasteManager.getInstance();
        Transferable trans = instance.getContents();
        try {
            if (trans != null) {
                if (trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return Optional.of((String) trans.getTransferData(DataFlavor.stringFlavor));
                }
            }
        } catch (Exception ex) {
            log.error("获取粘贴板信息异常", ex);
            throw new TipException("获取粘贴板信息异常");
        }
        return Optional.empty();
    }

    private void setClipContent(String sql) {
        CopyPasteManager instance = CopyPasteManager.getInstance();
        Transferable trans = new StringSelection(sql);
        instance.setContents(trans);
    }
}
