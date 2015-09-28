package org.jbei.ice.lib.dto;

import org.jbei.ice.lib.dao.IDataTransferModel;

/**
 * Types of configuration stored in the database. This acts as the base for
 * system configuration.
 *
 * @author Hector Plahar
 */
public enum ConfigurationKey implements IDataTransferModel {

    APPLICATION_VERSION("4.0.0"),
    DATABASE_SCHEMA_VERSION(""),
    TEMPORARY_DIRECTORY("/tmp"),
    DATA_DIRECTORY("data"),

    // deprecated. use the user account salt instead.
    SECRET_KEY("o6-v(yay5w@0!64e6-+ylbhcd9g03rv#@ezqh7axchds=q=$n+"),
    BULK_UPLOAD_APPROVER_EMAIL(""), //TODO this should be a role
    ADMIN_EMAIL(""), // ditto
    SMTP_HOST(""),
    ERROR_EMAIL_EXCEPTION_PREFIX("ERROR"),
    SEND_EMAIL_ON_ERRORS("NO"),
    PROJECT_NAME(""),
    URI_PREFIX("localhost:8443"),
    PART_NUMBER_PREFIX("TEST"),
    PART_NUMBER_DIGITAL_SUFFIX("000001"),
    PART_NUMBER_DELIMITER("_"),

    BLAST_INSTALL_DIR("/usr/bin"),

    NEW_REGISTRATION_ALLOWED("NO"),
    PASSWORD_CHANGE_ALLOWED("YES"),
    PROFILE_EDIT_ALLOWED("YES"),
    JOIN_WEB_OF_REGISTRIES("NO"),
    WEB_OF_REGISTRIES_MASTER("registry.jbei.org"),
    
    AUTHENTICATION_BACKEND("org.jbei.ice.lib.account.authentication.LocalAuthentication");

    private String defaultValue;

    ConfigurationKey(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
