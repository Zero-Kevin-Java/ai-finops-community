package org.afo.common.excel.convert;

import cn.idev.excel.metadata.data.WriteCellData;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class ExcelLocalTimeConvertTest {

    @Test
    void convertToExcelDataFormatsLocalTime() {
        ExcelLocalTimeConvert converter = new ExcelLocalTimeConvert();

        WriteCellData<String> cellData = converter.convertToExcelData(LocalTime.of(8, 5, 6), null, null);

        assertEquals("08:05:06", cellData.getStringValue());
    }
}
