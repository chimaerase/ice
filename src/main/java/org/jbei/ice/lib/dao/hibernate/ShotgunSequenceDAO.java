package org.jbei.ice.lib.dao.hibernate;

import org.apache.commons.io.IOUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jbei.ice.lib.dao.DAOException;
import org.jbei.ice.lib.dto.ConfigurationKey;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.models.ShotgunSequence;
import org.jbei.ice.lib.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

public class ShotgunSequenceDAO extends HibernateRepository<ShotgunSequence> {
    public static final String SHOTGUN_SEQUENCES_DIR = "shotgun_sequences";

    public ShotgunSequence create(String fileName, String depositor, Entry entry, String fileUUID, Date date)
            throws DAOException {

        ShotgunSequence result= new ShotgunSequence(entry, fileUUID, fileName, depositor, date);
        
        try {
            super.create(result);
        } catch (DAOException e) {
            throw new DAOException("Failed to create Shotgun Sequence!", e);
        }

        return result;
    }

    public void writeSequenceFileToDisk(String fileName, InputStream inputStream)
            throws IOException, DAOException {
        try {
            String dataDirectory        = Utils.getConfigValue(ConfigurationKey.DATA_DIRECTORY);
            File traceFilesDirectory    = Paths.get(dataDirectory, SHOTGUN_SEQUENCES_DIR).toFile();
            File file                   = new File(traceFilesDirectory + File.separator + fileName);

            if (!traceFilesDirectory.exists()) {
                if (!traceFilesDirectory.mkdirs()) {
                    throw new DAOException("Could not create shotgun sequences directory");
                }
            }

            if (!file.exists() && !file.createNewFile()) {
                throw new DAOException("Could not create sequences file " + file.getName());
            }

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                IOUtils.copy(inputStream, outputStream);
            }
        } catch (SecurityException e) {
            throw new DAOException(e);
        }
    }


    public List<ShotgunSequence> getByEntry(Entry entry, String userId) throws DAOException {
        List<ShotgunSequence> result = null;

        Session session = currentSession();
        try {
            String queryString = "from ShotgunSequence as shotgunSequence where shotgunSequence.entry = :entry order by"
                    + " shotgunSequence.creationTime asc";
            Query query = session.createQuery(queryString);

            query.setEntity("entry", entry);

            Object queryResult = query.list();

            if (queryResult != null) {
                result = query.list();
            }
        } catch (HibernateException e) {
            throw new DAOException("Failed to get shotgun sequence by entry!", e);
        }

        return result;
    }
    
    @Override
    public ShotgunSequence get(long id) {
        return super.get(ShotgunSequence.class, id);
    }
}
