package org.jbei.ice.lib.dto;

import org.jbei.ice.lib.account.AccountTransfer;
import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.common.logging.Logger;
import org.jbei.ice.lib.dao.DAOFactory;
import org.jbei.ice.lib.dao.IDataTransferModel;
import org.jbei.ice.lib.dao.hibernate.ShotgunSequenceDAO;
import org.jbei.ice.lib.models.ShotgunSequence;
import org.jbei.ice.lib.utils.Utils;

import java.io.*;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ShotgunSequenceDTO implements IDataTransferModel {
    public String filename;
    public String call;
    public double score;
    public String fileId;
    public AccountTransfer depositor;

    public ShotgunSequenceDTO(ShotgunSequence s) {
        filename = s.getFilename();
        fileId = s.getFileId();
        setQuality(fileId);

        Account a = DAOFactory.getAccountDAO().getByEmail(s.getDepositor());
        depositor = new AccountTransfer();
        depositor.setEmail(a.getEmail());
        depositor.setFirstName(a.getFirstName());
        depositor.setLastName(a.getLastName());
    }

    private void setQuality(String fileId) {
        call = "fakestring";
        score = -1;

        String dataDirectory = Utils.getConfigValue(ConfigurationKey.DATA_DIRECTORY);
        File traceFilesDirectory = Paths.get(dataDirectory, ShotgunSequenceDAO.SHOTGUN_SEQUENCES_DIR).toFile();
        File file = new File(traceFilesDirectory + File.separator + fileId);

        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Logger.error(e);
            return;
        }

        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            while (true) {
                zipEntry = zis.getNextEntry();

                if (zipEntry != null) {
                    if (!zipEntry.isDirectory() && zipEntry.getName().startsWith("call")) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        int c;
                        while ((c = zis.read()) != -1) {
                            byteArrayOutputStream.write(c);
                        }

                        this.call = byteArrayOutputStream.toString().trim();
                    } else if (!zipEntry.isDirectory() && zipEntry.getName().startsWith("score")) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        int c;
                        while ((c = zis.read()) != -1) {
                            byteArrayOutputStream.write(c);
                        }

                        Scanner scan = new Scanner(byteArrayOutputStream.toString());
                        this.score = scan.nextDouble();
                    }
                } else {
                    return;
                }
            }
        } catch (IOException e) {
            String errMsg = ("Could not parse zip file.");
            Logger.error(errMsg);
        }
    }
}
