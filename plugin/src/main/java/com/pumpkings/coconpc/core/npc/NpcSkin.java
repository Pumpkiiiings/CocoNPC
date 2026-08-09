package com.pumpkings.coconpc.core.npc;

import com.pumpkings.coconpc.CocoNPC;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NpcSkin {
    private final String key;
    private final Map<String, String> textures = new LinkedHashMap<>();
    private boolean slim = false;
    CocoNPC plugin;

    public NpcSkin(CocoNPC plugin, String key) {
        this.plugin = plugin;
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public String get(String partName) {
        return textures.get(partName);
    }

    public void set(String partName, String value) {
        textures.put(partName, value);
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(textures);
    }



    public void setSlim(boolean slim) {
        this.slim = slim;
    }

    public boolean isSlim() {
        return this.slim;
    }
}

