package org.jbei.ice.lib.models;

/**
 * @author Hector Plahar
 */
public enum AuditType {

    READ("+r");

    private String abbrev;

    AuditType(String abbrev) {
        this.abbrev = abbrev;
    }

    public String getAbbrev() {
        return abbrev;
    }
}
