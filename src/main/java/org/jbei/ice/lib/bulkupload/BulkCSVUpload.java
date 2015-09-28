package org.jbei.ice.lib.bulkupload;

import com.opencsv.CSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.jbei.ice.lib.common.logging.Logger;
import org.jbei.ice.lib.dao.DAOFactory;
import org.jbei.ice.lib.dto.StorageLocation;
import org.jbei.ice.lib.dto.bulkupload.EntryField;
import org.jbei.ice.lib.dto.bulkupload.SampleField;
import org.jbei.ice.lib.dto.entry.AttachmentInfo;
import org.jbei.ice.lib.dto.entry.EntryType;
import org.jbei.ice.lib.dto.entry.PartData;
import org.jbei.ice.lib.dto.sample.PartSample;
import org.jbei.ice.lib.dto.sample.SampleType;
import org.jbei.ice.lib.entry.EntryUtil;
import org.jbei.ice.lib.entry.model.Entry;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper class for dealing with bulk CSV uploads
 *
 * @author Hector Plahar
 */
public class BulkCSVUpload {

    protected final Path csvFilePath;
    protected final String userId;
    protected final EntryType addType;
    protected EntryType subType;    // optional subType
    protected final List<EntryField> headerFields;
    protected final List<EntryField> linkedHeaders;
    protected final List<EntryField> requiredFields;

    public BulkCSVUpload(String userId, Path csvFilePath, EntryType addType) {
        this.addType = addType;
        this.userId = userId;
        this.csvFilePath = csvFilePath;
        this.requiredFields = new LinkedList<>();
        this.headerFields = BulkCSVUploadHeaders.getHeadersForType(addType);
        this.linkedHeaders = new LinkedList<>();
    }

    /**
     * Processes the csv upload
     *
     * @return id of created bulk upload or error message
     * @throws IOException on error processing the file
     */
    public final long processUpload() throws IOException {
        try (FileInputStream inputStream = new FileInputStream(csvFilePath.toFile())) {
            List<PartWithSample> updates = getBulkUploadDataFromFile(inputStream);

            // create actual entries
            BulkEntryCreator creator = new BulkEntryCreator();
            long uploadId = creator.createBulkUpload(userId, addType);

            // create entries
            if (!creator.createEntries(userId, uploadId, updates, null)) {
                String errorMsg = "Error creating entries for upload";
                Logger.error(errorMsg);
                throw new IOException(errorMsg);
                //todo: delete upload id
            }

            return uploadId;
        }
    }

    EntryType detectSubType(String field) {
        String[] fieldNames = field.split("\\s+");
        return EntryType.nameToType(fieldNames[0]);
    }

    HeaderValue detectSubTypeHeaderValue(EntryType subType, String fieldStr) throws IOException {
        int k = fieldStr.indexOf(subType.getDisplay());
        String headerValue = fieldStr.substring(k + subType.getDisplay().length());

        EntryField field = EntryField.fromString(headerValue.trim());
        if (field == null) {
            throw new IOException("Unknown field [" + fieldStr + "] for upload [" + addType.getDisplay() + "]");
        }

        return new EntryHeaderValue(true, field);
    }

    /**
     * @param headerArray list of column header values representing the entry fields
     * @return mapping of col number to the entry type
     * @throws IOException
     */
    HashMap<Integer, HeaderValue> processColumnHeaders(String[] headerArray) throws IOException {
        HashMap<Integer, HeaderValue> headers = new HashMap<>();

        for (int i = 0; i < headerArray.length; i += 1) {
            String fieldStr = headerArray[i].trim();

            // account for "*" that indicates a header is required
            if (fieldStr.lastIndexOf("*") != -1)
                fieldStr = fieldStr.substring(0, fieldStr.length() - 1);

            EntryField field = EntryField.fromString(fieldStr);
            if (field != null) {
                // field header maps as is to EntryField which indicates it is not a sub Type
                HeaderValue headerValue = new EntryHeaderValue(false, field);
                headers.put(i, headerValue);
                continue;
            }

            // check if sample field
            SampleField sampleField = SampleField.fromString(fieldStr);
            if (sampleField != null) {
                HeaderValue headerValue = new SampleHeaderValue(sampleField);
                headers.put(i, headerValue);
                continue;
            }

            // check sub Type
            if (subType != null) {
                HeaderValue headerValue = detectSubTypeHeaderValue(subType, fieldStr);
                headers.put(i, headerValue);
                continue;
            }

            // sub data is null, try to detect
            subType = detectSubType(fieldStr);
            if (subType == null) {
                throw new IOException("Unknown field [" + fieldStr + "] for upload [" + addType.getDisplay() + "]");
            }

            HeaderValue headerValue = detectSubTypeHeaderValue(subType, fieldStr);
            headers.put(i, headerValue);
        }

        if (headers.size() != headerArray.length) {
            throw new IOException("Header size and input header array length differ");
        }
        return headers;
    }

    // NOTE: this also validates the part data (with the exception of the actual files)
    List<PartWithSample> getBulkUploadDataFromFile(InputStream inputStream) throws IOException {
        List<PartWithSample> partDataList = new LinkedList<>();

        // initialize parser to null; when not-null in the loop below, then the header has been parsed
        CSVParser parser = null;
        HashMap<Integer, HeaderValue> headers = null;

        // parse CSV file
        try {
            LineIterator it = IOUtils.lineIterator(inputStream, "UTF-8");
            int index = 0;
            while (it.hasNext()) {
                String line = it.nextLine().trim();

                // check if first time parsing (first line)
                if (parser == null) {

                    // check the separator char (header will use the same separator)
                    // to indicate the type of parser to use (tab or comma separated)
                    if (line.contains("\t") && !line.contains(","))
                        parser = new CSVParser('\t');
                    else
                        parser = new CSVParser();

                    // get column headers
                    String[] fieldStrArray = parser.parseLine(line);
                    headers = processColumnHeaders(fieldStrArray);
                    continue;
                }

                // skip any empty lines (holes) in the csv file
                if (StringUtils.isBlank(line) || line.replaceAll(",", "").trim().isEmpty())
                    continue;

                // at this point we must have headers since that should be the first item in the file
                if (headers == null)
                    throw new IOException("Could not parse file headers");

                // parser != null; process line contents with available headers
                String[] valuesArray = parser.parseLine(line);
                PartData partData = new PartData(addType);
                PartSample partSample = null;

                if (subType != null) {
                    partData.getLinkedParts().add(new PartData(subType));
                }

                // for each column
                for (int i = 0; i < valuesArray.length; i += 1) {
                    HeaderValue headerForColumn = headers.get(i);

                    // process sample information
                    if (headerForColumn.isSampleField()) {
                        // todo : move to another method
                        if (partSample == null)
                            partSample = new PartSample();
                        setPartSampleData(((SampleHeaderValue) headerForColumn).getSampleField(),
                                partSample, valuesArray[i]);

                    } else {
                        EntryHeaderValue entryHeaderValue = (EntryHeaderValue) headerForColumn;
                        EntryField field = entryHeaderValue.getEntryField();
                        PartData data;
                        String value = valuesArray[i];
                        boolean isSubType = entryHeaderValue.isSubType();

                        if (isSubType)
                            data = partData.getLinkedParts().get(0);
                        else
                            data = partData;

                        // get the data for the field
                        switch (field) {
                            case ATT_FILENAME:
                                ArrayList<AttachmentInfo> attachments = data.getAttachments();
                                if (attachments == null) {
                                    attachments = new ArrayList<>();
                                }
                                attachments.clear();
                                attachments.add(new AttachmentInfo(value));
                                break;

                            case SEQ_FILENAME:
                                data.setSequenceFileName(value);
                                break;

                            case SEQ_TRACE_FILES:
                                // todo
                                break;

                            case EXISTING_PART_NUMBER:
                                Entry entry = DAOFactory.getEntryDAO().getByPartNumber(value);
                                if (entry == null)
                                    throw new IOException("Could not locate part number \"" + value + "\" for linking");
                                PartData toLink = entry.toDataTransferObject();
                                data.getLinkedParts().add(toLink);
                                break;

                            default:
                                partData = EntryUtil.setPartDataFromField(partData, value, field, isSubType);
                        }
                    }
                }

                // validate
                List<EntryField> fields = validate(partData);
                if (!fields.isEmpty()) {
                    StringBuilder fieldsString = new StringBuilder();
                    for (EntryField field : fields) {
                        fieldsString.append("\n").append(field.getLabel());
                    }
                    throw new IOException("Missing required fields:\n" + fieldsString.toString());
                }

                partData.setIndex(index);
                PartWithSample partWithSample = new PartWithSample(partSample, partData);
                partDataList.add(partWithSample);
                index += 1;
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        return partDataList;
    }

    protected List<EntryField> validate(PartData partData) {
        return EntryUtil.validates(partData);
    }

    protected void setPartSampleData(SampleField sampleField, PartSample partSample, String data) {
        switch (sampleField) {
            case LABEL:
                partSample.setLabel(data);
                break;

            case SHELF:
                StorageLocation storageLocation = new StorageLocation();
                storageLocation.setType(SampleType.SHELF);
                storageLocation.setDisplay(data);
                partSample.setLocation(storageLocation);
                break;

            case BOX:
                StorageLocation childLocation = new StorageLocation();
                childLocation.setDisplay(data);
                childLocation.setType(SampleType.BOX_INDEXED);
                partSample.getLocation().setChild(childLocation);
                break;

            case WELL:
                StorageLocation grandChildLocation = new StorageLocation();
                grandChildLocation.setType(SampleType.WELL);
                grandChildLocation.setDisplay(data);
                partSample.getLocation().getChild().setChild(grandChildLocation);
                break;
        }
    }
}
