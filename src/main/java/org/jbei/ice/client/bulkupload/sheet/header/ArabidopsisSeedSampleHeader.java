package org.jbei.ice.client.bulkupload.sheet.header;

import java.util.ArrayList;

import org.jbei.ice.client.bulkupload.model.SheetCellData;
import org.jbei.ice.client.bulkupload.sheet.Header;
import org.jbei.ice.client.entry.view.model.SampleStorage;
import org.jbei.ice.shared.dto.StorageInfo;
import org.jbei.ice.shared.dto.entry.EntryInfo;

/**
 * @author Hector Plahar
 */
public class ArabidopsisSeedSampleHeader extends SampleHeaders {

    public ArabidopsisSeedSampleHeader(ArrayList<String> locationList) {
        super(locationList, null);
    }

    public SheetCellData extractValue(Header headerType, EntryInfo info) {
        SheetCellData data = super.extractCommon(headerType, info);
        if (data != null)
            return data;

        String value = "";
        if (!info.isHasSample()) {
            data = new SheetCellData();
            data.setValue(value);
            data.setId(value);
            return data;
        }

        SampleStorage sampleStorage = info.getOneSampleStorage();

        switch (headerType) {

            case SAMPLE_DRAWER:
                for (StorageInfo storageInfo : sampleStorage.getStorageList()) {
                    if (storageInfo.getType().equalsIgnoreCase("shelf")) {
                        value = storageInfo.getDisplay();
                        break;
                    }
                }
                break;

            case SAMPLE_BOX:
                for (StorageInfo storageInfo : sampleStorage.getStorageList()) {
                    if (storageInfo.getType().equalsIgnoreCase("box_unindexed")) {
                        value = storageInfo.getDisplay();
                        break;
                    }
                }
                break;


            case SAMPLE_TUBE:
                for (StorageInfo storageInfo : sampleStorage.getStorageList()) {
                    if (storageInfo.getType().equalsIgnoreCase("tube")) {
                        value = storageInfo.getDisplay();
                        break;
                    }
                }
                break;

            default:
                return null;
        }

        data = new SheetCellData();
        data.setValue(value);
        data.setId(value);
        return data;
    }
}
