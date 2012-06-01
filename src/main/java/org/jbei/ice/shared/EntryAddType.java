package org.jbei.ice.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum EntryAddType implements IsSerializable {

    PLASMID("Plasmid"), STRAIN("Strain"), PART("Part"), STRAIN_WITH_PLASMID(
            "Strain with One Plasmid"), ARABIDOPSIS("Arabidopsis Seed");

    private String display;

    EntryAddType(String display) {
        this.display = display;
    }

    // TODO : 
    public static EntryAddType stringToType(String str) {
        if (str == null)
            return null;

        if (str.contains(PLASMID.toString().toLowerCase())
                && str.contains(STRAIN.toString().toLowerCase()))
            return STRAIN_WITH_PLASMID;

        for (EntryAddType type : EntryAddType.values()) {
            if (str.equalsIgnoreCase(type.toString()))
                return type;
        }
        return null;
    }

    public String getDisplay() {
        return this.display;
    }

    @Override
    public String toString() {
        return this.display;
    }
}