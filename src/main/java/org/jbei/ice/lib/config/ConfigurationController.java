package org.jbei.ice.lib.config;

import java.util.HashMap;

import org.jbei.ice.controllers.common.ControllerException;
import org.jbei.ice.lib.dao.DAOException;
import org.jbei.ice.lib.logging.Logger;
import org.jbei.ice.lib.models.Configuration;
import org.jbei.ice.lib.shared.dto.ConfigurationKey;

/**
 * @author Hector Plahar
 */
public class ConfigurationController {
    private final ConfigurationDAO dao;

    public ConfigurationController() {
        dao = new ConfigurationDAO();
    }

    public String retrieveDatabaseVersion() throws ControllerException {
        try {
            Configuration configuration = dao.get(ConfigurationKey.DATABASE_SCHEMA_VERSION);
            if (configuration == null)
                return null;
            return configuration.getValue();
        } catch (DAOException e) {
            Logger.error(e);
            throw new ControllerException(e);
        }
    }

    public void updateDatabaseVersion(String newVersion) throws ControllerException {
        try {
            Configuration configuration = dao.get(ConfigurationKey.DATABASE_SCHEMA_VERSION);
            configuration.setValue(newVersion);
            dao.save(configuration);
        } catch (DAOException de) {
            throw new ControllerException(de);
        }
    }

    public Configuration getConfiguration(ConfigurationKey key) throws ControllerException {
        try {
            return dao.get(key);
        } catch (DAOException de) {
            throw new ControllerException(de);
        }
    }

    public String getPropertyValue(ConfigurationKey key) throws ControllerException {
        try {
            Configuration config = dao.get(key);
            if (config == null)
                return key.getDefaultValue();
            return config.getValue();
        } catch (DAOException de) {
            throw new ControllerException(de);
        }
    }

    public String getPropertyValue(String key) throws ControllerException {
        try {
            Configuration config = dao.get(key);
            if (config == null)
                throw new ControllerException("Could not retrieve config with key " + key);
            return config.getValue();
        } catch (DAOException de) {
            throw new ControllerException(de);
        }
    }

    public HashMap<String, String> retrieveSystemSettings() throws ControllerException {
        try {
            HashMap<String, String> results = new HashMap<String, String>();
            for (Configuration configuration : dao.getAllSettings()) {
                results.put(configuration.getKey(), configuration.getValue());
            }
            return results;
        } catch (DAOException de) {
            throw new ControllerException(de);
        }
    }

    public void setPropertyValue(ConfigurationKey key, String value) throws ControllerException {
        try {
            Configuration configuration = dao.get(key);
            if (configuration == null) {
                configuration = new Configuration();
                configuration.setKey(key.name());
            }

            configuration.setValue(value);
            dao.save(configuration);
        } catch (DAOException de) {
            Logger.error(de);
            throw new ControllerException();
        }
    }

    /**
     * Initializes the database on new install
     */
    public void initPropertyValues() throws ControllerException {
        for (ConfigurationKey key : ConfigurationKey.values()) {
            try {
                Configuration config = dao.get(key);
                if (config != null || key.getDefaultValue().isEmpty())
                    continue;

                Logger.info("Setting value for " + key.toString() + " to " + key.getDefaultValue());
                setPropertyValue(key, key.getDefaultValue());
            } catch (DAOException e) {
                Logger.warn(e.getMessage());
            }
        }
    }
}
