package com.oreo.launcher3.logging;
import com.oreo.launcher3.FolderInfo;
import com.oreo.launcher3.ItemInfo;
import com.oreo.launcher3.ShortcutInfo;
import com.oreo.launcher3.model.LauncherDumpProto.DumpTarget;
import java.util.ArrayList;
import java.util.List;

public class DumpTargetWrapper {
    public DumpTarget node;
    private List<DumpTargetWrapper> children = new ArrayList<>();

    public DumpTargetWrapper(int type, int value) { node = DumpTarget.newBuilder().build(); }
    public DumpTargetWrapper(ItemInfo info) { node = DumpTarget.newBuilder().build(); }

    public static DumpTargetWrapper fromItemInfo(ItemInfo info) { return new DumpTargetWrapper(info); }
    public static String getDumpTargetStr(DumpTarget t) { return ""; }

    public void writeToDumpTarget(ItemInfo info) {}
    public void writeToDumpTarget(FolderInfo info) {}
    public void writeToDumpTarget(ShortcutInfo info) {}
    public void add(DumpTargetWrapper child) { children.add(child); }
    public List<DumpTarget> getFlattenedList() { return new ArrayList<>(); }
}
