package org.afo.common.excel.convert;

import cn.hutool.core.util.StrUtil;
import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * LocalTime Excel converter.
 */
public class ExcelLocalTimeConvert implements Converter<LocalTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public Class<LocalTime> supportJavaTypeKey() {
        return LocalTime.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return null;
    }

    @Override
    public LocalTime convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                       GlobalConfiguration globalConfiguration) {
        String value = cellData.getStringValue();
        return StrUtil.isBlank(value) ? null : LocalTime.parse(value, FORMATTER);
    }

    @Override
    public WriteCellData<String> convertToExcelData(LocalTime value, ExcelContentProperty contentProperty,
                                                    GlobalConfiguration globalConfiguration) {
        return new WriteCellData<>(value == null ? "" : value.format(FORMATTER));
    }
}
