package integration;

import com.haulmont.yarg.formatters.ReportFormatter;
import com.haulmont.yarg.formatters.factory.DefaultFormatterFactory;
import com.haulmont.yarg.formatters.factory.FormatterFactoryInput;
import com.haulmont.yarg.structure.BandData;
import com.haulmont.yarg.structure.ReportOutputType;
import com.haulmont.yarg.structure.impl.ReportTemplateImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;

/**
 * @author birin
 * @version $Id$
 */
public class CsvIntegrationTest {

    @Test
    public void testCsv() throws Exception {
        BandData root = new BandData("Root");
        BandData header = new BandData("Header", root);

        BandData first = new BandData("First", root);
        first.addData("firstName", "first");
        first.addData("lastName", "last");
        first.addData("amount", 24132432);
        first.addData("date", LocalDate.of(2017, Month.AUGUST, 10));

        BandData second = new BandData("Second", root);
        second.addData("firstName", "second");
        second.addData("lastName", "last 2");
        second.addData("amount", 324324324);
        second.addData("date", LocalDate.of(2017, Month.AUGUST, 11));

        root.addChildren(Arrays.asList(header, first, second));

        FileOutputStream outputStream = new FileOutputStream("./result/integration/result.csv");
        ReportFormatter formatter = new DefaultFormatterFactory().createFormatter(new FormatterFactoryInput("csv", root,
                new ReportTemplateImpl("", "test.csv", "./modules/core/test/integration/test.csv", ReportOutputType.csv), outputStream));
        formatter.renderDocument();

        IOUtils.closeQuietly(outputStream);

        File sample = new File("./modules/core/test/integration/ethalon.csv");
        File result = new File("./result/integration/result.csv");
        boolean isTwoEqual = FileUtils.contentEquals(sample, result);

        Assert.assertTrue("Files are not equal", isTwoEqual);
    }
}
