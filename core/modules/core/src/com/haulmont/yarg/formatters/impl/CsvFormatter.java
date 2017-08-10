package com.haulmont.yarg.formatters.impl;

import com.haulmont.yarg.exception.UnsupportedFormatException;
import com.haulmont.yarg.formatters.factory.FormatterFactoryInput;
import com.haulmont.yarg.structure.BandData;
import com.haulmont.yarg.structure.ReportOutputType;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;


/**
 * @author birin
 * @version $Id$
 */
public class CsvFormatter extends AbstractFormatter {
    protected String header;
    protected List<String> parametersToInsert = new ArrayList<>();

    public CsvFormatter(FormatterFactoryInput formatterFactoryInput) {
        super(formatterFactoryInput);
        supportedOutputTypes.add(ReportOutputType.csv);
        readTemplateData();
    }

    @Override
    public void renderDocument() {
        ReportOutputType outputType = reportTemplate.getOutputType();
        if (ReportOutputType.csv.equals(outputType)) {
            writeCsvDocument(rootBand, outputStream);
        } else {
            throw new UnsupportedFormatException();
        }
    }

    protected void writeCsvDocument(BandData rootBand, OutputStream outputStream) {
        try {
            List<BandData> actualData = getActualData(rootBand);
            CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream), ',', CSVWriter.NO_QUOTE_CHARACTER);

            writer.writeNext(new String[] { header });

            for (BandData row : actualData) {
                String[] entries = new String[parametersToInsert.size()];
                for (int i = 0; i < parametersToInsert.size(); i++) {
                    entries[i] = String.valueOf(row.getData().get(parametersToInsert.get(i)));
                }
                writer.writeNext(entries);
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected List<BandData> getActualData(BandData rootBand) {
        List<BandData> resultData = new ArrayList<>();
        Map<String, List<BandData>> childrenBands = rootBand.getChildrenBands();

        if (childrenBands != null && !childrenBands.isEmpty()) {
            childrenBands.forEach((s, bandDataList) -> bandDataList.forEach(bandData -> {
                if (bandData.getData() != null && !bandData.getData().isEmpty()) {
                    resultData.add(bandData);
                }
            }));
        }

        return resultData;
    }

    protected void readTemplateData() {
        InputStream documentContent = reportTemplate.getDocumentContent();
        BufferedReader in = new BufferedReader(new InputStreamReader(documentContent));

        StringBuilder headerData = new StringBuilder();
        try {
            String line;
            while((line = in.readLine()) != null) {
                Matcher matcher = UNIVERSAL_ALIAS_PATTERN.matcher(line);
                if (!matcher.find())
                    headerData.append(line);
                else {
                    matcher.reset();
                    while (matcher.find()) {
                        String parameterName = unwrapParameterName(matcher.group());
                        parametersToInsert.add(parameterName);
                    }
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        header = headerData.toString();
    }
}
